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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.ManagedFile;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.User;


public class GitMiner extends Miner {
	private final static String DEFAULT_HEAD = "refs/heads/master";
	
	private Settings settings;
	private Project project;
	private ModelPool pool;
	private Model model;
	private boolean stopped;

	private boolean processDiffs;
	private String startRef;


	private static class DiffOutputStream extends OutputStream {
		private enum State {
			NONE,
			ADDED,
			REMOVED
		}
		
		private boolean lastWasNewline = false;
		private int totalAdded = 0;
		private int totalRemoved = 0;
		private int totalChunks = 0;
		private int totalAddedEmpty = 0;
		private int totalRemovedEmpty = 0;

		private State state = State.NONE;
		private boolean empty = false;
		
		public int getTotalLinesAdded () {
			return this.totalAdded;
		}

		public int getTotalEmptyLinesAdded () {
			return this.totalAddedEmpty;
		}

		public int getTotalLinesRemoved () {
			return this.totalRemoved;
		}

		public int getTotalEmptyLinesRemoved () {
			return this.totalRemovedEmpty;
		}

		public int getTotalChunks () {
			return this.totalChunks;
		}

		@SuppressWarnings ("unused")
		public void reset () {
			this.totalAdded = 0;
			this.totalRemoved = 0;
			this.totalAddedEmpty = 0;
			this.totalRemovedEmpty = 0;
			this.totalChunks = 0;
			this.lastWasNewline = false;
			this.state = State.NONE;
			this.empty = false;
		}
		
		@Override
		public void write (int b) {
			if (b == '\n') {
				lastWasNewline = true;
				if (empty == true) {
					if (state == State.ADDED) {
						totalAddedEmpty++;
					} else if (state == State.REMOVED) {
						totalRemovedEmpty++;				
					}
				}
				state = State.NONE;
				empty = true;
			} else if (lastWasNewline == true) {
				if (b == '+') {
					totalAdded++;
					state = State.ADDED;
				} else if (b == '-') {
					state = State.REMOVED;
					totalRemoved++;
				} else if (b == '@') {
					totalChunks++;
				}

				lastWasNewline = false;
			} else if (b != '\r' && b != ' ' && b != '\t') {
				empty = false;
			}
		}
	}

	public GitMiner (Settings settings, Project project, ModelPool pool) {
		assert (settings != null);
		assert (project != null);
		assert (pool != null);
		
		this.settings = settings;
		this.project = project;
		this.pool = pool;
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
			identity = model.addIdentity (Model.CONTEXT_SRC, mail, name, user);
			identities.put (mapKey, identity);
		}

		return identity;
	}

	private class FileStats {
		public int linesAdded = 0;
		public int linesRemoved = 0;
		public int emptyLinesAdded = 0;
		public int emptyLinesRemoved = 0;
		public int chunks = 0;
		public String oldPath;
		public ChangeType type;
	}

	//
	// Runner:
	//
	
	@Override
	public void run () throws MinerException {
		stopped = false;

		try {
			model = pool.getModel ();
			emitStart ();

			model.addFlag (project, Model.FLAG_SRC_INFO);
			processDiffs = settings.srcGetParameter ("process-diffs", true);
			startRef = settings.srcGetParameter ("start-ref", DEFAULT_HEAD);
			if (processDiffs == true) {
				model.addFlag (project, Model.FLAG_SRC_FILE_STATS);
				model.addFlag (project, Model.FLAG_SRC_LINE_STATS);
			}

			_run ();
			emitEnd ();
		} catch (IOException e) {
			throw new MinerException ("IO-Error: " + e.getMessage (), e);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		} finally {
			if (model != null) {
				model.close ();
			}
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

		Ref head = repository.getRef (startRef);
		if (head == null) {
			throw new MinerException ("Unknown reference: '" + startRef + "'");
		}

		RevWalk walk = new RevWalk (repository);
		RevCommit commit = walk.parseCommit (head.getObjectId ());
		walk.markStart (commit);
		walk.sort (RevSort.REVERSE);

		// count commit: (fast)
		int commitCount = 0;
		Iterator<RevCommit> iter = walk.iterator ();
		while (iter.hasNext ()) {
			iter.next ();
			commitCount++;
		}

		emitTasksTotal (commitCount);
		

		// process commits: (slow)
		walk.reset ();
		walk.markStart (commit);
		walk.sort (RevSort.REVERSE);

		Map<String, ManagedFile> fileCache = new HashMap<String, ManagedFile> ();

		for (RevCommit rev : walk) {
			if (stopped == true) {
				break;
			}

			processCommit (repository, walk, rev, fileCache);
		}
		
		walk.dispose();
		repository.close();
	}
	
	private void processCommit (Repository repository, RevWalk walk, RevCommit rev, Map<String, ManagedFile> fileCache) throws SQLException, IOException {
		Identity author = resolveIdentity (rev.getAuthorIdent ());
		Identity committer = resolveIdentity (rev.getCommitterIdent ());
		Date date = new Date (rev.getCommitTime ());
		String message = rev.getFullMessage ();

		Map<String, FileStats> fileStats = new HashMap<String, FileStats> ();
		DiffOutputStream outputStream = new DiffOutputStream ();
		processDiff (repository, walk, rev, outputStream, fileStats);

		String revision = rev.getId ().getName();
		int totalLinesAdded = outputStream.getTotalLinesAdded ();
		int totalLinesRemoved = outputStream.getTotalLinesRemoved ();
		int fileCount = fileStats.size ();


		Commit commit = model.addCommit (revision, project, author, committer, date, message, fileCount, totalLinesAdded, totalLinesRemoved);

		for (Map.Entry<String, FileStats> item : fileStats.entrySet ()) {
			if (stopped == true) {
				break;
			}
			
			FileStats stats = item.getValue ();
			String path = item.getKey ();

			switch (stats.type) {
			case COPY:

				// There is no active copy in git,
				// use ADD instead.

				/*
				ManagedFile originalFile = fileCache.get (stats.oldPath);
				assert (originalFile != null);

				ManagedFile copiedFile = model.addManagedFile (project, path);
				model.addFileChange (commit, copiedFile, stats.linesAdded, stats.linesRemoved, stats.emptyLinesAdded, stats.emptyLinesRemoved, stats.chunks);
				model.addIsCopy (copiedFile, commit, originalFile);
				fileCache.put (path, copiedFile);
				break;
				 */

			case ADD:
				ManagedFile addedFile = model.addManagedFile (project, path);
				model.addFileChange (commit, addedFile, stats.linesAdded, stats.linesRemoved, stats.emptyLinesAdded, stats.emptyLinesRemoved, stats.chunks);
				fileCache.put (path, addedFile);
				break;

			case DELETE:
				ManagedFile deletedFile = fileCache.get (path);
				// Merge handling
				if (deletedFile != null) {
					model.addFileDeletion (deletedFile, commit);
					fileCache.remove (stats.oldPath);
				}
				break;

			case MODIFY:
				ManagedFile modifiedFile = fileCache.get (path);
				assert (modifiedFile != null);
				model.addFileChange (commit, modifiedFile, stats.linesAdded, stats.linesRemoved, stats.emptyLinesAdded, stats.emptyLinesRemoved, stats.chunks);
				break;

			case RENAME:
				ManagedFile renamedFile = fileCache.get (stats.oldPath);
				// E.g. on merges after a rename.
				if (renamedFile != null) {
					model.addFileRename (renamedFile, commit, stats.oldPath, path);
					fileCache.put (path, renamedFile);
				}
				break;
	
			default:
				assert (false);
			}
		}

		emitTasksProcessed (1);
	}
	
	private void processDiff (Repository repository, RevWalk walk, RevCommit current, DiffOutputStream outputStream, Map<String, FileStats> fileStatsMap) throws IOException {
		assert (repository != null);
		assert (walk != null);
		assert (current != null);
		assert (outputStream != null);
		assert (fileStatsMap != null);

		if (processDiffs == false) {
			return ;
		}
		
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
				if (stopped == true) {
					break;
				}

				int emptyLinesAddedStart = outputStream.getTotalEmptyLinesAdded ();
				int emptyLinesRemovedStart = outputStream.getTotalEmptyLinesRemoved ();
				int linesAddedStart = outputStream.getTotalLinesAdded ();
				int linesRemovedStart = outputStream.getTotalLinesRemoved ();
				int chunksStart = outputStream.getTotalChunks ();
				String oldPath = null;
				String path = null;
				
				switch (de.getChangeType ()) {
				case ADD:
					path = de.getNewPath ();
					break;
				case DELETE:
					path = de.getOldPath ();
					break;
				case MODIFY:
					path = de.getOldPath ();
					break;
				case COPY:
					oldPath = de.getOldPath ();
					path = de.getNewPath ();
					break;
				case RENAME:
					oldPath = de.getOldPath ();
					path = de.getNewPath ();
					break;
				default:
					continue;
				}
				
				assert (fileStatsMap.containsKey (path) == false);
				assert (path != null);

				FileStats fileStats = new FileStats ();
				fileStatsMap.put (path, fileStats);

				df.format(de);
				df.flush();

				fileStats.emptyLinesAdded = outputStream.getTotalEmptyLinesAdded () - emptyLinesAddedStart;
				fileStats.emptyLinesRemoved = outputStream.getTotalEmptyLinesRemoved () - emptyLinesRemovedStart;
				fileStats.linesAdded += outputStream.getTotalLinesAdded () - linesAddedStart;
				fileStats.linesRemoved += outputStream.getTotalLinesRemoved () - linesRemovedStart;
				fileStats.chunks += outputStream.getTotalChunks () - chunksStart;

				fileStats.type = de.getChangeType ();
				fileStats.oldPath = oldPath;
			}
		} catch (IOException e) {
			throw e;
		}
	}
	
	@Override
	public void stop () {
		stopped = true;
		emitStop ();
	}

	@Override
	public String getName () {
		return "GIT";
	}
}
