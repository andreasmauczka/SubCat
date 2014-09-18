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
import java.text.SimpleDateFormat;
import java.text.ParseException;

import at.ac.tuwien.inso.subcat.utility.XmlReader;
import at.ac.tuwien.inso.subcat.utility.XmlReaderException;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.User;

public class SvnMiner extends Miner {

	private Settings settings;
	private Project project;
	private Model model;
	private XmlReader reader;
	
	public SvnMiner (Settings settings, Project project, Model model) {
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
	
	private void processCommit (String aAuthor, Date aDate, String aPath ,String aMsg) throws SQLException, IOException {
		Identity author = resolveIdentity (aAuthor);
		Identity committer = resolveIdentity (aAuthor);
		
		Date date = aDate;
		String msg = aMsg;
		String path = aPath;
		
		assert (date != null || msg != null || path != null);
		
		System.out.println("Commit: " + author);
		model.addCommit (project, author, committer, date, msg, 0, 0, null);
		// TODO store path
	
	}
	
	public void parse () throws XmlReaderException, ParseException, IOException, SQLException {
		reader = new XmlReader (settings.srcLocalPath);
		reader.expectStart ("log", true);
		reader.acceptStart("log", true);
		while (reader.isStart("logentry")){
			parseLogentry();
		}
		reader.expectEnd ("log", true);
	}

	
	private void parseLogentry () throws XmlReaderException, ParseException, IOException, SQLException {
		
		String revision = reader.getAttribute("revision");
		
		reader.acceptStart("logentry", true);
		
		//<author> might be omitted, e.g. during cvs2svn migration
		String author = parseOptional("author");
		
		//parse date element
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
		Date date = format.parse(parseSingle("date"));
		
		//<paths> must have at least one element <path> but may have more
		String path = parseMulti(new String[]{"paths","path"});
		
		//parse commit msg
		String msg = parseSingle("msg");
		
		//store the commit in the DB
		processCommit (author, date, path, msg);
		reader.expectEnd ("logentry", true);
	}

	private String parseSingle (String element) throws XmlReaderException {
		reader.acceptStart(element,true);
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
	
	private String parseMulti (String [] elements) throws XmlReaderException{
		reader.acceptStart(elements[0],true);
		String content = parseSingle(elements[1]);
		try {
			reader.expectEnd(elements[0], true);
		}
		catch (XmlReaderException e){
			content = parseMulti(elements);
		}
		
		return content;
	}
	
	@Override
	public void run() throws MinerException {
		// TODO Auto-generated method stub
		try {
			_run ();
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
		parse();
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
	
	}
