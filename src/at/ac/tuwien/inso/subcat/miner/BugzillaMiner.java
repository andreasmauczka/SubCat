/* BugzillaMiner.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.miner;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.ac.tuwien.inso.subcat.bugzilla.BugzillaBug;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaChange;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaComment;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaContext;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaException;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaHistory;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaProduct;
import at.ac.tuwien.inso.subcat.bugzilla.BugzillaUser;
import at.ac.tuwien.inso.subcat.model.Attachment;
import at.ac.tuwien.inso.subcat.model.AttachmentStatus;
import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Component;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Priority;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.Severity;
import at.ac.tuwien.inso.subcat.model.Status;
import at.ac.tuwien.inso.subcat.model.User;
import at.ac.tuwien.inso.subcat.utility.Reporter;


public class BugzillaMiner extends Miner {
	private Pattern patternMailValidator = Pattern.compile ("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

	private int pageSize;
	private int passSize;

	// LinkedBlockingQueue does not allow `null`
	// as poison
	private class QueueEntry {
		public BugzillaBug bug;
		
		public QueueEntry (BugzillaBug bug) {
			this.bug = bug;
		}
	}
	
	private LinkedBlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<QueueEntry> ();
	private boolean run = true;

	private Reporter reporter;
	private BugzillaContext context;
	private Settings settings;
	private ModelPool pool;
	private Model model;

	private Map<String, AttachmentStatus> attachmentStatus = new HashMap<String, AttachmentStatus> ();
	private Map<String, Component> components = new HashMap<String, Component> ();
	private Map<String, Priority> priorities = new HashMap<String, Priority> ();
	private Map<String, Severity> severities = new HashMap<String, Severity> ();
	private Map<String, Identity> identities = new HashMap<String, Identity> ();
	private Map<String, Status> status = new HashMap<String, Status> ();
	private Project project;

	private List<Worker> workers = new LinkedList<Worker> ();
	private MinerException storedException;
	
	private boolean processComments;
	private boolean processHistory;

	private class BugzillaAttachment {
		public String id;
		public Comment comment;
		public LinkedList<BugzillaAttachmentHistoryEntry> history = new LinkedList<BugzillaAttachmentHistoryEntry> ();
	}
	
	private class BugzillaAttachmentHistoryEntry {
		public Identity identity;
		public String status;
		public Date date;
		public Boolean isObsolete;
	}
	
	private class Worker extends Thread {

		@Override
		public void run () {
			LinkedList<BugzillaBug> bbugs = new LinkedList<BugzillaBug> ();
			LinkedList<Integer> bzBugIds = new LinkedList<Integer> ();
			boolean stopped = false;

			while (!Thread.currentThread().isInterrupted () && !stopped) {
				try {
					for (int i = 0; i < passSize; i++) {
						BugzillaBug bbug = queue.take ().bug;
						if (bbug == null) {
							stopped = true;
							break;
						}

						bzBugIds.add (bbug.getId ());
						bbugs.add (bbug);
					}

					process (bbugs, bzBugIds);
					emitTasksProcessed (bbugs.size ());

					Thread.sleep (settings.bugCooldownTime);
				} catch (InterruptedException e) {
					Thread.currentThread ().interrupt ();
				} catch (BugzillaException e) {
					abortRun (new MinerException ("Bugzilla-Exception: " + e.getMessage (), e));
				} catch (SQLException e) {
					abortRun (new MinerException ("SQL-Exception: " + e.getMessage (), e));
				} finally {
					bbugs.clear ();
					bzBugIds.clear ();
				}
			}
		}

		private boolean isSaxError (Throwable e) {
			while (e != null) {
				if (e instanceof org.xml.sax.SAXParseException) {
					return true;
				}
				
				e = e.getCause ();
			}

			return false;
		}
		
		private void process (List<BugzillaBug> bzBugs, List<Integer> bzBugIds) throws BugzillaException, SQLException {
			try {
				_process (bzBugs, bzBugIds);
				return ;
			} catch (BugzillaException e) {
				if (isSaxError (e) == false) {
					throw e;
				}
			}

			// Likely triggered by invalid characters.
			// Get all bugs one by one, skip the broken ones.

			for (BugzillaBug bzBug : bzBugs) {
				try {
					LinkedList<BugzillaBug> _bzBugs = new LinkedList<BugzillaBug> ();
					_bzBugs.add (bzBug);

					LinkedList<Integer> _bzBugIds = new LinkedList<Integer> ();
					_bzBugIds.add (bzBug.getId ());

					_process (_bzBugs, _bzBugIds);
				} catch (BugzillaException e) {
					if (isSaxError (e)) {
						reporter.warning (BugzillaMiner.this.getName ().toLowerCase (), "Could not process and store bug #" + bzBug.getId () + ": " + e.getMessage ());
					} else {
						throw e;
					}
				}
			}
		}

		private void _process (List<BugzillaBug> bzBugs, List<Integer> bzBugIds) throws BugzillaException, SQLException {
			assert (bzBugs != null);
			assert (bzBugIds != null);
			assert (bzBugs.size () == bzBugIds.size ());

			// Get data:
			Map<Integer, BugzillaHistory[]> _histories;
			Map<Integer, BugzillaComment[]> _comments;

			_histories = context.getHistory (bzBugIds);
			_comments = context.getComments (bzBugIds);
			
			for (BugzillaBug bzBug : bzBugs) {
				BugzillaComment[] comments = _comments.get (bzBug.getId ());
				Identity creator = null;
				if (comments != null && comments.length > 0) {
					creator = resolveIdentity (comments[0].getCreator ());
				}

				Component component = resolveComponent (bzBug.getComponent ());
				Severity severity = resolveSeverity (bzBug.getSeverity ());
				Priority priority = resolvePriority (bzBug.getPriority ());
				Integer identifier = bzBug.getId ();
				Date creation = bzBug.getCreationTime ();
				String title = bzBug.getSummary ();


				List<BugzillaAttachment> attachments = new LinkedList<BugzillaAttachment> ();

				// Add to model:
				Bug bug = model.addBug (identifier, creator, component, title, creation, priority, severity);
				if (processComments) {
					assert (comments != null);
					addComments (bug, comments, attachments);
				}
	
				if (processHistory) {
					BugzillaHistory[] histories = _histories.get (bzBug.getId ());
					addHistory (bug, histories, attachments);
				}

				
				HashMap<String, Attachment> storedAttachments = new HashMap<String, Attachment> ();
				for (BugzillaAttachment att : attachments) {
					Attachment attachment = model.addAttachment (att.id, att.comment);
					for (BugzillaAttachmentHistoryEntry histo : att.history) {
						if (histo.status != null) {
							AttachmentStatus attStatus = resolveAttachmentStatus (histo.status);
							model.addAttachmentHistory (histo.identity, attStatus, attachment, histo.date);
						}
						if (histo.isObsolete != null) {
							model.setAttachmentIsObsolete (attachment, histo.identity, histo.date, histo.isObsolete);
						}
					}

					storedAttachments.put (att.id, attachment);
				}
			}
		}

		private void extractPatchId (Comment cmnt, List<BugzillaAttachment> attachments) {
			String text = cmnt.getContent ();

			// TODO: Regex
			
			final String attachmentPrefixOld = "Created an attachment (id=";
			final String attachmentPrefixNew = "Created attachment ";
			char endChar;
			int idStart;
			
			if (text.startsWith (attachmentPrefixOld)) {
				endChar = ')';
				idStart = attachmentPrefixOld.length ();
			} else if (text.startsWith (attachmentPrefixNew)) {
				idStart = attachmentPrefixNew.length ();
				endChar = '\n';
			} else {
				return ;
			}

			int iter = idStart;

			while (Character.isDigit (text.charAt (iter))) {
				iter++;
			}

			if (text.charAt (iter) == endChar) {
				BugzillaAttachment attachment = new BugzillaAttachment ();
				attachment.id = text.substring (idStart, iter);
				attachment.comment = cmnt;
				attachments.add (attachment);
			}
		}

		private void addComments (Bug bug, BugzillaComment[] comments, List<BugzillaAttachment> attachments) throws SQLException, BugzillaException {
			assert (bug != null);
			assert (comments != null);

			for (BugzillaComment comment : comments) {
				
				if (Thread.currentThread().isInterrupted ()) {
					return ;
				}
				
				Identity cmntCreator = resolveIdentity (comment.getCreator ());
				Date cmntCreation = comment.getTime ();
				String cmntContent = comment.getText ();

				Comment cmnt = model.addComment (bug, cmntCreation, cmntCreator, cmntContent);
				// TODO: Duplications
				// TODO: Patch Reviews / Comment on Attachment
				extractPatchId (cmnt, attachments);
			}
		}
		
		private BugzillaAttachment getAttachment (List<BugzillaAttachment> attachments, String id) {
			assert (attachments != null);
			assert (id != null);

			for (BugzillaAttachment att : attachments) {
				if (att.id.equals (id)) {
					return att;
				}
			}

			assert (false);
			return null;
		}
		
		private void addHistory (Bug bug, BugzillaHistory[] history, List<BugzillaAttachment> attachments) throws SQLException, BugzillaException {
			assert (bug != null);

			if (history == null) {
				return ;
			}

			for (BugzillaHistory entry : history) {
				BugzillaChange[] changes = entry.getChanges ();
				for (BugzillaChange change : changes) {
					if (Thread.currentThread().isInterrupted ()) {
						return ;
					}

					// TODO: resolution
					if ("bug_status".equals (change.getFieldName ())) {
						Identity identity = resolveIdentity (entry.getWho ());
						Status bugStat = resolveStatus (change.getAdded ());
						Date date = entry.getWhen ();

						model.addBugHistory (bug, bugStat, identity, date);
					}

					// TODO: Store fieldName=blocks, fieldName==depends_on, CC					
					if (change.getAttachmentId () != null) {
						BugzillaAttachmentHistoryEntry attHisto = new BugzillaAttachmentHistoryEntry ();

						String status = change.getFieldName ();
						if (status.equals ("attachments.isobsolete")) {
							if (change.getAdded ().equals ("1")) {
								attHisto.isObsolete = true;
							} else {
								attHisto.isObsolete = false;
							}
						} else if (status.equals ("attachments.gnome_attachment_status") || status.equals ("flagtypes.name")) {
							attHisto.status = change.getAdded ();
						} else {
							continue;
						}


						attHisto.date = entry.getWhen ();
						Identity identity = resolveIdentity (entry.getWho ());
						attHisto.identity = identity;

						BugzillaAttachment bugzillaAttachment = getAttachment (attachments, Integer.toString (change.getAttachmentId ()));
						bugzillaAttachment.history.add (attHisto);
					}
				}
			}
		}
	}
	

	public BugzillaMiner (Settings settings, Project project, ModelPool pool, Reporter reporter) {
		assert (settings != null);
		assert (project != null);
		assert (pool != null);
		
		this.reporter = reporter;
		this.settings = settings;
		this.project = project;
		this.pool = pool;
	}

	@Override
	public void run () throws MinerException, ParameterException {
		processComments = settings.bugGetParameter (this, "process-comments", true);
		processHistory = settings.bugGetParameter (this, "process-history", true);
		passSize = settings.bugGetParameter (this, "pass-size", 20);
		pageSize = settings.bugGetParameter (this, "page-size", 200);

		for (int i = 0; i < settings.bugThreads; i++) {
			Worker worker = new Worker ();
			workers.add (worker);
			worker.start ();
		}

		emitStart ();

		try {
			model = pool.getModel ();
			model.addFlag (project, Model.FLAG_BUG_INFO);
			if (processComments == true) {
				model.addFlag (project, Model.FLAG_BUG_COMMENTS);
				model.addFlag (project, Model.FLAG_BUG_ATTACHMENTS);
			}
			if (processHistory == true) {
				model.addFlag (project, Model.FLAG_BUG_HISTORY);
			}

			_run ();
		} catch (SQLException e) {
			abortRun (new MinerException ("SQL-Error: " + e.getMessage (), e));
		} catch (BugzillaException e) {
			abortRun (new MinerException ("Bugzilla-Error: " + e.getMessage (), e));
		} catch (MinerException e) {
			abortRun (e);
		} catch (InterruptedException e) {
			abortRun (null);
		}

		for (Worker worker : workers) {
			try {
				worker.join ();
			} catch (InterruptedException e) {
			}
		}

		if (model != null) {
			model.close ();
		}

		if (storedException != null) {
			throw storedException;
		}

		emitEnd ();
	}

	private void abortRun (MinerException e) {
		if (storedException == null) {
			storedException = e;
		}
		
		run = false;
		queue.clear ();

		for (Worker worker : workers) {
			worker.interrupt ();
		}
	}
	
	public void _run () throws MinerException, SQLException, BugzillaException, InterruptedException {
		try {
			context = new BugzillaContext (settings.bugRepository);
		} catch (MalformedURLException e) {
			throw new MinerException ("Malformed tracker URL: " + e.getMessage (), e);
		}

		if (settings.bugEnableUntrustedCertificates) {
			context.enableUntrustedCertificates ();
		}

		if (settings.bugLoginUser != null && settings.bugLoginPw != null) {
			context.login (settings.bugLoginUser, settings.bugLoginPw);
		}

		BugzillaProduct product = getBugzillaProduct (settings.bugProductName);
		if (product == null) {
			throw new MinerException ("Unknown product `" + settings.bugProductName + "'");
		}

		model.setDefaultStatus (resolveStatus ("UNCO"));

		int bugsTotal = 0;
		for (int page = 1; run && !Thread.currentThread().isInterrupted () ; page++) {
			BugzillaBug[] bugs = context.getBugs (settings.bugProductName, page, pageSize);
			if (bugs.length < 1) {
				break;
			}

			bugsTotal += bugs.length;
			this.emitTasksTotal (bugsTotal);

			for (BugzillaBug bzBug : bugs) {
				queue.add (new QueueEntry (bzBug));
			}

			Thread.sleep (settings.bugCooldownTime);
		}


		// Poisoned Pills:
		for (int i = 0; i < workers.size (); i++) {
			queue.add (new QueueEntry (null));
		}
	}

	@Override
	public synchronized void stop () {
		queue.clear ();
		run = false;
		abortRun (null);
		emitStop ();
	}


	//
	// Helper:
	//

	private synchronized BugzillaProduct getBugzillaProduct (String productName) throws BugzillaException, MinerException {
		assert (productName != null);

		Integer[] ids = context.getAccessibleProducts ();
		BugzillaProduct[] products = context.getProducts (ids);
		for (BugzillaProduct product : products) {
			if (productName.equals (product.getName ())) {
				return product;
			}
		}

		throw new MinerException ("Product '" + productName + "' not found.");
	}

	private synchronized Status resolveStatus (String name) throws SQLException {
		assert (name != null);

		Status stat = status.get (name);
		if (stat == null) {
			stat = model.addStatus (project, name);
			status.put (name, stat);
		}
		
		return stat;
	}

	private synchronized Component resolveComponent (String name) throws SQLException {
		assert (name != null);

		Component component = components.get (name);
		if (component == null) {
			component = model.addComponent (project, name);
			components.put (name, component);
		}
		
		return component;
	}

	private synchronized Severity resolveSeverity (String name) throws SQLException {
		assert (name != null);

		Severity severity = severities.get (name);
		if (severity == null) {
			severity = model.addSeverity (project, name);
			severities.put (name, severity);
		}
		
		return severity;
	}

	private synchronized Identity resolveIdentity (String name) throws SQLException, BugzillaException {
		assert (name != null);

		Identity identity = identities.get (name);
		if (identity == null) {
			BugzillaUser[] bugUserList = context.getUsers (name);
			assert (bugUserList.length == 1);
			BugzillaUser bugUser = bugUserList[0];

			
			String _mail = bugUser.getEmail ();
			if (_mail == null && bugUser.getName () != null) {
				Matcher mm = patternMailValidator.matcher (bugUser.getName ());
				if (mm.matches ()) {
					_mail = bugUser.getName ();
				}
			}
			
			StringBuilder builder = new StringBuilder ();
			if (bugUser.getName () != null) {
				builder.append (bugUser.getName ());
			}
			if (bugUser.getRealName () != null) {
				if (builder.length () > 0) {
					builder.append (' ');
				}

				builder.append (bugUser.getRealName ());
			}


			User user = model.addUser (project, builder.toString ());
			identity = model.addIdentity (Model.CONTEXT_BUG, _mail, builder.toString (), user);
			identities.put (name, identity);
		}

		return identity;
	}
	
	private synchronized Priority resolvePriority (String name) throws SQLException {
		assert (name != null);

		Priority priority = priorities.get (name);
		if (priority == null) {
			priority = model.addPriority (project, name);
			priorities.put (name, priority);
		}
		
		return priority;
	}

	private synchronized AttachmentStatus resolveAttachmentStatus (String name) throws SQLException {
		assert (name != null);

		AttachmentStatus status = attachmentStatus.get (name);
		if (status == null) {
			status = model.addAttachmentStatus (project, name);
			attachmentStatus.put (name, status);
		}
		
		return status;		
	}

	@Override
	public String getName () {
		return "BugZilla";
	}
}

