/* GitMiner.java
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.User;


public class GitMiner extends Miner {
	private final static String DEFAULT_HEAD = "refs/heads/master";
	
	private Settings settings;
	private Project project;
	private Model model;

	private class DiffOutputStream extends OutputStream {
		boolean lastWasNewline = false;
		int added = 0;
		int removed = 0;

		public int getLinesAdded () {
			return this.added;
		}

		public int getLinesRemoved () {
			return this.removed;
		}
		
		@SuppressWarnings ("unused")
		public void reset () {
			this.added = 0;
			this.removed = 0;
		}
		
		@Override
		public void write (int b) throws IOException {
			if (b == '\n') {
				lastWasNewline = true;
			} else if (lastWasNewline == true) {
				if (b == '+') {
					added++;
				} else if (b == '-') {
					removed++;
				}
			}
		}
	}
	
	public GitMiner (Settings settings, Project project, Model model) {
		assert (settings != null);
		assert (project != null);
		assert (model != null);
		
		this.settings = settings;
		this.project = project;
		this.model = model;
	}


	//
	// Helper:
	//
	
	private HashMap<String, Identity> identities = new HashMap<String, Identity> ();


	private Identity resolveIdentity (PersonIdent author) throws SQLException {
		String mail = author.getEmailAddress ();
		String name = author.getName ();

		assert (mail != null || name != null);

		String mapKey = (mail != null)? mail : name;
		name = (name != null)? name : mail;

		Identity identity = identities.get (mapKey);
		if (identity == null) {
			User user = model.addUser (project, name);
			identity = model.addIdentity (mail, name, user);
			identities.put (mapKey, identity);
		}

		return identity;
	}


	//
	// Runner:
	//
	
	@Override
	public void run () throws MinerException {
		try {
			emitStart ();
			_run ();
			emitEnd ();
		} catch (IOException e) {
			throw new MinerException ("IO-Error: " + e.getMessage (), e);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		}
	}

	private void _run () throws IOException, MinerException, SQLException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder ();
		Repository repository = builder
			.setGitDir (new File (settings.srcLocalPath, ".git"))
			.readEnvironment()
			.findGitDir()
			.build();

		/*
		Map<String,Ref> refs = repository.getAllRefs();
		for (Map.Entry<String, Ref> ref : refs.entrySet ()) {
			System.out.println (ref.getKey ());
		}
		*/

		Ref head = repository.getRef (DEFAULT_HEAD);
		if (head == null) {
			throw new MinerException ("Unknown reference: '" + DEFAULT_HEAD + "'");
		}

		RevWalk walk = new RevWalk (repository);
		RevCommit commit = walk.parseCommit (head.getObjectId ());
		walk.markStart (commit);

		for (RevCommit rev : walk) {
			processCommit (repository, walk, rev);
		}
		
		walk.dispose();
		repository.close();
	}

	private void processCommit (Repository repository, RevWalk walk, RevCommit rev) throws SQLException, IOException {
		Identity author = resolveIdentity (rev.getAuthorIdent ());
		Identity committer = resolveIdentity (rev.getCommitterIdent ());
		Date date = new Date (rev.getCommitTime ());
		String message = rev.getFullMessage ();

		DiffOutputStream outputStream = new DiffOutputStream ();
		processDiff (repository, walk, rev, outputStream);

		int linesAdded = outputStream.getLinesAdded ();
		int linesRemoved = outputStream.getLinesRemoved ();

		model.addCommit (project, author, committer, date, message, linesAdded, linesRemoved, null);

		//System.out.println (repository + ": +" + linesAdded + " -" + linesRemoved);
	}
	
	private void processDiff (Repository repository, RevWalk walk, RevCommit current, DiffOutputStream outputStream) throws IOException {
		try {
			DiffFormatter df = new DiffFormatter (outputStream);
			df.setDiffComparator (RawTextComparator.WS_IGNORE_CHANGE);
			df.setRepository (repository);
			df.setDetectRenames (true);

			List<DiffEntry> entries;
			if (current.getParentCount () > 0) {
				RevCommit parent = current.getParent (0);
				ObjectId oldTree = walk.parseCommit (parent).getTree ();
				ObjectId newTree = current.getTree ();
				entries = df.scan (oldTree, newTree);
			} else {
				entries = df.scan (new EmptyTreeIterator(),
					new CanonicalTreeParser (null, walk.getObjectReader(), current.getTree()));
			}

			

			for (DiffEntry de : entries) {
				switch (de.getChangeType ()) {
				case ADD:
					break;
				case DELETE:
					break;
				case MODIFY:
					break;
				default:
					continue;
				}

				// TODO: File rename handling?
				// TODO: Git blame
				// TODO: File count?

				df.format(de);
				df.flush();
			}
		} catch (IOException e) {
			throw e;
		}
	}
	
	@Override
	public void stop () {
		// TODO
		emitStop ();
	}
}
