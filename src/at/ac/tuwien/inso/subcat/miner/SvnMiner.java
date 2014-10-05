/* SvnMiner.java
 *
 * Copyright (C) 2014 Florian Brosch
 * Copyright (C) 2014 Andreas Mauczka
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
 *       Andreas Mauczka <andreas.mauczka(at)inso.tuwien.ac.at>
 */

package at.ac.tuwien.inso.subcat.miner;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import at.ac.tuwien.inso.subcat.utility.XmlReader;
import at.ac.tuwien.inso.subcat.utility.XmlReaderException;
import at.ac.tuwien.inso.subcat.utility.Lemmatizer;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.ManagedFile;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.User;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;


public class SvnMiner extends Miner {

	private Settings settings;
	private Project project;
	private Model model;
	private XmlReader reader;
	private Lemmatizer lemmatizer;
	
	public SvnMiner (Settings settings, Project project, Model model) {
		assert (settings != null);
		assert (project != null);
		assert (model != null);
		
		this.settings = settings;
		this.project = project;
		this.model = model;
		
		//initialize lemmatizer once - this seems to be memory intensive
		lemmatizer = new Lemmatizer();
	}
	
	//
	// Helper:
	//
	
	private HashMap<String, Identity> identities = new HashMap<String, Identity> ();
	
	private Identity resolveIdentity (String aUser) throws SQLException {
		//using identities, even though we dont have emails or committers
		String mail = aUser;
		String name = aUser;

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
	
	private class FileStats {
		public String oldPath;
		public ChangeType type;
	}
	
	private enum ChangeType {ADD, MODIFY, DELETE, RENAME}
	
	private void processCommit (String aAuthor, Date aDate, List <String> pathslist ,String aMsg) throws SQLException, IOException {
		Identity author = resolveIdentity (aAuthor);
		Identity committer = resolveIdentity (aAuthor);
		
		Date date = aDate;
		String msg = aMsg;
		
		// Lemmatize input - delivers String List, convert it to string
		msg = StringUtils.join(lemmatizer.lemmatize(msg), ", ");
		
		int filecount = pathslist.size();
		
		assert (date != null || msg != null || pathslist != null);
		
		System.out.println ("Commit: " + author);
		System.out.println (StringUtils.join(pathslist, ", "));
		model.addCommit (project, author, committer, date, msg, filecount, 0, 0, null);
		// TODO store path
	}
		/*Map<String, FileStats> fileStats = new HashMap<String, FileStats> ();
		
		

		for (Map.Entry<String, FileStats> item : fileStats.entrySet ()) {
			if (stopped == true) {
				break;
			}
			
			FileStats stats = item.getValue ();
			String path = item.getKey ();
			
			// TODO: binary files
			
			switch (stats.type) {
			case ADD:
				ManagedFile addedFile = model.addManagedFile (project, path);
				model.addFileChange (commit, addedFile, stats.linesAdded, stats.linesRemoved, stats.emptyLinesAdded, stats.emptyLinesRemoved, stats.chunks);
				fileCache.put (path, addedFile);
				break;

			case DELETE:
				ManagedFile deletedFile = fileCache.get (path);
				assert (deletedFile != null);
				model.addFileDeletion (deletedFile, commit);
				fileCache.remove (stats.oldPath);				
				break;

			case MODIFY:
				ManagedFile modifiedFile = fileCache.get (path);
				assert (modifiedFile != null);
				model.addFileChange (commit, modifiedFile, stats.linesAdded, stats.linesRemoved, stats.emptyLinesAdded, stats.emptyLinesRemoved, stats.chunks);
				break;

			case RENAME:
				ManagedFile renamedFile = fileCache.get (stats.oldPath);
				assert (renamedFile != null);
				model.addFileRename (renamedFile, commit, stats.oldPath);
				fileCache.put (path, renamedFile);
				fileCache.remove (stats.oldPath);
				break;
	
			default:
				assert (false);
			}
		}

		emitTasksProcessed (1);
		
	
	}
	
	private void populateFileStats (Repository repository, Map<String, FileStats> fileStatsMap) throws IOException {
		assert (repository != null);
		assert (fileStatsMap != null);

				
		try {
			

			for (DiffEntry de : entries) {
				if (stopped == true) {
					break;
				}

			
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
	}*/
	
	public void parse () throws XmlReaderException, ParseException, IOException, SQLException {
		reader = new XmlReader (settings.srcLocalPath);
		reader.expectStart ("log", true);
		reader.acceptStart ("log", true);
		while (reader.isStart ("logentry")){
			parseLogentry ();
		}
		reader.expectEnd ("log", true);
	}

	
	private void parseLogentry () throws XmlReaderException, ParseException, IOException, SQLException {
		
		//String revision = reader.getAttribute ("revision");
		
		reader.acceptStart ("logentry", true);
		
		//<author> might be omitted, e.g. during cvs2svn migration
		String author = parseOptional ("author");
		List <String> pathslist = new ArrayList<String>();
		//parse date element
		SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
		Date date = format.parse (parseSingle("date"));
		
		//<paths> must have at least one element <path> but may have more
		parseMulti (new String[]{"paths","path"}, pathslist);
		
		//parse commit msg
		String msg = parseSingle ("msg");
		
		//store the commit in the DB
		processCommit (author, date, pathslist, msg);
		reader.expectEnd ("logentry", true);
	}

	private String parseSingle (String element) throws XmlReaderException {
		reader.acceptStart (element,true);
		String content = reader.getText();
		reader.expectEnd (element, true);
		return content;
	}
	
	private String parseOptional (String element) throws XmlReaderException {
		if (!reader.acceptStart (element, true)) {
			return null;
		}

		String content = reader.getText ();
		reader.expectEnd (element, true);
		return content;
	}
	
	private void parseMulti (String [] elements, List <String> pathslist) throws XmlReaderException{
		reader.acceptStart(elements[0],true);		
		while (reader.acceptStart(elements[1], true)){
			pathslist.add(parseSingle(elements[1]));			
		}
		reader.acceptEnd(elements[0],true);
	}
	
	
	@Override
	public void run () throws MinerException {
		// TODO Auto-generated method stub
		try {
			emitStart ();

			model.addFlag (project, Model.FLAG_SRC_INFO);
			model.addFlag (project, Model.FLAG_SRC_FILE_STATS);

			_run ();
			emitEnd ();
		} catch (IOException e) {
			throw new MinerException ("IO-Error: " + e.getMessage (), e);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		} catch (ParseException e) {
			throw new MinerException ("Date Parsing-Error: " + e.getMessage (), e);
		} catch (XmlReaderException e) {
			throw new MinerException ("XML Reader-Error: " + e.getMessage (), e);
		}
		
	}
	private void _run () throws IOException, MinerException, SQLException, ParseException, XmlReaderException {
		parse ();	
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		emitStop ();
	}
}
