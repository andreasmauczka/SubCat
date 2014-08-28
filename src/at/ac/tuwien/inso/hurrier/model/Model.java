/* Model.java
 *
 * Copyright (C) 2014  Brosch Florian
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
 * 	Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.hurrier.model;

import java.io.File;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import at.ac.tuwien.inso.hurrier.config.DistributionAttributesConfig;
import at.ac.tuwien.inso.hurrier.config.DistributionChartConfig;
import at.ac.tuwien.inso.hurrier.config.DistributionChartOptionConfig;
import at.ac.tuwien.inso.hurrier.config.DropDownConfig;
import at.ac.tuwien.inso.hurrier.config.OptionListConfig;
import at.ac.tuwien.inso.hurrier.config.PieChartConfig;
import at.ac.tuwien.inso.hurrier.config.Query;
import at.ac.tuwien.inso.hurrier.config.SemanticException;
import at.ac.tuwien.inso.hurrier.config.TrendChartConfig;
import at.ac.tuwien.inso.hurrier.config.TrendChartPlotConfig;


public class Model {
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Connection conn;
	private String name;

	private LinkedList<ModelModificationListener> listeners = new LinkedList<ModelModificationListener> ();

	
	//
	// DB-Settings:
	//
	
	private static final String ENABLE_FOREIGN_KEYS =
			"PRAGMA foreign_keys = ON";

	
	//
	// Table Creation:
	//

	// Note: Dates are not supported by Sqlite

	private static final String PROJECT_TABLE =
		"CREATE TABLE IF NOT EXISTS Projects ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "domain		TEXT								NOT NULL,"
		+ "product		TEXT								NOT NULL,"
		+ "revision		TEXT								        ,"
		+ "defaultStatusId INT										 "
//		+ "FOREIGN KEY (defaultStatusId) REFERENCES StatusTable"
		+ ")";

	private static final String USER_TABLE =
		"CREATE TABLE IF NOT EXISTS Users ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String IDENTITY_TABLE =
		"CREATE TABLE IF NOT EXISTS Identities ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "mail			TEXT								        ,"
		+ "name			TEXT								NOT NULL,"
		+ "user			INT									NOT NULL,"
		+ "FOREIGN KEY(user) REFERENCES Users (id)"
		+ ")";

	private static final String INTERACTION_TABLE =
		"CREATE TABLE IF NOT EXISTS Interactions ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "start		INT									NOT NULL,"
		+ "end			INT									NOT NULL,"
		+ "quotes		INT									NOT NULL,"
		+ "closed		INT									NOT NULL,"
		+ "pos			FLOAT								NOT NULL,"
		+ "neg			FLOAt								NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "FOREIGN KEY(start) REFERENCES Identities (id),"
		+ "FOREIGN KEY(end) REFERENCES Identities (id)"
		+ ")";
	
	private static final String COMPONENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Components ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String PRIORITY_TABLE =
		"CREATE TABLE IF NOT EXISTS Priorities ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String SEVERITY_TABLE =
		"CREATE TABLE IF NOT EXISTS Severity ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";
	
	private static final String BUG_TABLE =
		"CREATE TABLE IF NOT EXISTS Bugs ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "component	INT									NOT NULL,"
		+ "title		TEXT								NOT NULL,"
		+ "creation		TEXT								NOT NULL,"
		+ "priority		INT									NOT NULL,"
		+ "severity		INT									NOT NULL,"
		+ "category		INT									        ,"
		+ "comments		INT									NOT NULL DEFAULT 0,"
		+ "curStat		INT									NOT NULL,"
		+ "FOREIGN KEY(priority) REFERENCES Priorities (id),"
		+ "FOREIGN KEY(severity) REFERENCES Severity (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(category) REFERENCES Categories (id),"
		+ "FOREIGN KEY(component) REFERENCES Components (id),"
		+ "FOREIGN KEY(curStat) REFERENCES Status (id)"
		+ ")";

	

	// TODO: Add date
	private static final String COMMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Comments ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "creation		TEXT								NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "content		TEXT								NOT NULL,"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";

	private static final String STATUS_TABLE =
		"CREATE TABLE IF NOT EXISTS Status ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String BUG_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS BugHistories ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "status		INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(status) REFERENCES Status (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";

	private static final String CATEGORY_TABLE =
		"CREATE TABLE IF NOT EXISTS Categories ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String COMMIT_TABLE =
		"CREATE TABLE IF NOT EXISTS Commits ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "autor		INT									NOT NULL,"
		+ "committer	INT									NOT NULL,"	// TODO: Rename to Pusher
		+ "date			TEXT								NOT NULL,"
		+ "title		TEXT								NOT NULL,"
		+ "linesAdded	INT									NOT NULL,"
		+ "linesRemoved	INT									NOT NULL,"
		+ "category		INT									NOT NULL,"
		+ "FOREIGN KEY(committer) REFERENCES Identities (id),"
		+ "FOREIGN KEY(autor) REFERENCES Identities (id),"
		+ "FOREIGN KEY(category) REFERENCES Categories (id),"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String BUGFIX_COMMIT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugfixCommit ("
		+ "bug			INTEGER								NOT NULL,"
		+ "commitId		INTEGER								NOT NULL,"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "PRIMARY KEY(bug, commitId)"
		+ ")";
		
	
	// TODO: Commit<>Bug

	//
	// Triggers:
	//

	private static final String BUG_STATUS_UPDATE_TRIGGER =
		"CREATE TRIGGER IF NOT EXISTS update_curStat AFTER INSERT ON BugHistories "
		+ "BEGIN "
		+ "  UPDATE Bugs "
		+ "  SET curStat = (SELECT status FROM BugHistories "
		+ "					WHERE BugHistories.bug = NEW.bug "
		+ "					ORDER BY strftime('%s', BugHistories.date), BugHistories.id DESC "
		+ "					LIMIT 1)"
		+ "  WHERE Bugs.id = NEW.bug;"
		+ "END ";

	private static final String BUG_COMMENT_COUNT_UPDATE_TRIGGER =
		"CREATE TRIGGER IF NOT EXISTS update_comments AFTER INSERT ON Comments "
		+ "BEGIN "
		+ "  UPDATE Bugs "
		+ "  SET comments = comments + 1 "
		+ "  WHERE Bugs.id = NEW.bug; "
		+ "END ";


	//
	// Insertions:
	//

	private static final String PROJECT_INSERTION =
		"INSERT INTO Projects"
		+ "(date, domain, product, revision, defaultStatusId)"
		+ "VALUES (?,?,?,?,?)";

	private static final String USER_INSERTION =
		"INSERT INTO Users"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String IDENTITY_INSERTION =
		"INSERT INTO Identities"
		+ "(mail, name, user)"
		+ "VALUES (?,?,?)";

	private static final String INTERACTION_INSERTION =
		"INSERT INTO Interactions"
		+ "(start, end, quotes, pos, neg, date, closed)"
		+ "VALUES (?,?,?,?,?,?,?)";

	private static final String PRIORITY_INSERTION =
		"INSERT INTO Priorities"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String COMPONENT_INSERTION =
		"INSERT INTO Components"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String CATEGORY_INSERTION =
		"INSERT INTO Categories"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String BUG_INSERTION =
		"INSERT INTO Bugs "
		+ "(identity, component, title, creation, priority, severity, category, curStat)"
		+ "SELECT ?, ?, ?, ?, ?, ?, ?, defaultStatusId "
		+ "FROM Projects WHERE id=?";

	private static final String COMMENT_INSERTION =
		"INSERT INTO Comments"
		+ "(bug, creation, identity, content)"
		+ "VALUES (?,?,?,?)";	
	
	private static final String STATUS_INSERTION =
		"INSERT INTO Status"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String BUG_HISTORY_INSERTION =
		"INSERT INTO BugHistories"
		+ "(bug, status, identity, date)"
		+ "VALUES (?,?,?,?)";

	private static final String COMMIT_INSERTION =
		"INSERT INTO Commits"
		+ "(project, autor, committer, date, title, linesAdded, linesRemoved, category)"
		+ "VALUES (?,?,?,?,?,?,?,?)";

	private static final String BUGFIX_COMMIT_INSERTION =
		"INSERT INTO BugfixCommit"
		+ "(bug, commitId)"
		+ "VALUES (?,?)";

	private static final String SEVERITY_INSERTION =
		"INSERT INTO Severity "
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String UPDATE_DEFAULT_STATUS =
		"UPDATE Projects SET defaultStatusId = ? WHERE id = ?";

	
	
	//
	// Creation & Destruction:
	//
	
	public Model (String name) throws SQLException, ClassNotFoundException {
		assert (name != null);
		
		//Set config
		org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig ();
		config.enforceForeignKeys (true);

	    
		// TODO: Escape path
		Class.forName ("org.sqlite.JDBC");
		conn = DriverManager.getConnection ("jdbc:sqlite:" + name,
				config.toProperties());

		// Prepare database:
		createTables ();

		this.name = name;
	}

	public synchronized void close () throws SQLException {
		conn.close ();
	}

	public synchronized boolean remove() throws SQLException {
		assert (name != null);
		close ();

		File file = new File (name + ".db");
		boolean res = file.delete ();
		
		name = null;
		return res;
	}

	
	
	//
	// Data Insertion API:
	//

	
	public Project addProject (Date date, String domain, String product, String revision) throws SQLException {
		Project proj = new Project (null, date, domain, product, revision);
		add (proj);
		return proj;
	}

	public synchronized void setDefaultStatus (Status status) throws SQLException {
		assert (status != null);
		assert (status.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (UPDATE_DEFAULT_STATUS);
	
		stmt.setInt (1, status.getId ());
		stmt.setInt (2, status.getProject ().getId ());
		stmt.executeUpdate();
		stmt.close ();
	}
	
	public synchronized void add (Project project) throws SQLException {
		assert (project != null);
		assert (project.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (PROJECT_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setString (1, dateFormat.format (project.getDate ()));
		stmt.setString (2, project.getDomain ());
		stmt.setString (3, project.getProduct ());
		stmt.setString (4, project.getRevision ());
		stmt.executeUpdate();

		project.setId (getLastInsertedId (stmt));

		stmt.close ();


		for (ModelModificationListener listener : listeners) {
			listener.projectAdded (project);
		}
	}

	
	public synchronized User addUser (Project project, String name) throws SQLException {
		User user = new User (null, project, name);
		add (user);
		return user;
	}

	public synchronized void add (User user) throws SQLException {
		assert (user != null);
		Project project = user.getProject ();
		assert (project.getId () != null);
		assert (user.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (USER_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString (2, user.getName ());
		stmt.executeUpdate();

		user.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.userAdded (user);
		}
	}
	
	
	public synchronized Identity addIdentity (String mail, String name, User user) throws SQLException {
		Identity identity = new Identity (null, mail, name, user);
		add (identity);
		return identity;
	}

	public synchronized void add (Identity identity) throws SQLException {
		assert (identity != null);
		assert (identity.getUser ().getId () != null);
		assert (identity.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (IDENTITY_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setString (1, identity.getMail ());
		stmt.setString (2, identity.getName ());
		stmt.setInt (3, identity.getUser ().getId ());
		stmt.executeUpdate();

		identity.setId (getLastInsertedId (stmt));

		stmt.close ();


		for (ModelModificationListener listener : listeners) {
			listener.identityAdded (identity);
		}
	}
	
	
	public synchronized Interaction addInteraction (User from, User to, boolean closed, int quotes, float pos, float neg, Date date) throws SQLException {
		Interaction relation = new Interaction (null, from, to, closed, quotes, pos, neg, date);
		add (relation);
		return relation;
	}

	public synchronized void add (Interaction relation) throws SQLException {
		assert (relation != null);
		assert (relation.getFrom ().getId () != null);
		assert (relation.getTo ().getId () != null);
		
		PreparedStatement stmt = conn.prepareStatement (INTERACTION_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, relation.getFrom ().getId ());
		stmt.setInt (2, relation.getTo ().getId ());
		stmt.setInt (3, relation.getQuotes ());
		stmt.setFloat (4, relation.getPos ());
		stmt.setFloat (5, relation.getNeg ());
		stmt.setString (6, dateFormat.format (relation.getDate ()));
		stmt.setInt (7, (relation.isClosed ())? 1 : 0);
		stmt.executeUpdate();

		relation.setId (getLastInsertedId (stmt));

		stmt.close ();


		for (ModelModificationListener listener : listeners) {
			listener.interactionAdded (relation);
		}
	}

		
	public synchronized Severity addSeverity (Project project, String name) throws SQLException {
		Severity sev = new Severity (null, project, name);
		add (sev);
		return sev;
	}

	public synchronized void add (Severity severity) throws SQLException {
		assert (severity != null);
		Project project = severity.getProject ();
		assert (project.getId () != null);
		assert (severity.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (SEVERITY_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, severity.getName ());
		stmt.executeUpdate();

		severity.setId (getLastInsertedId (stmt));

		stmt.close ();		

	
		for (ModelModificationListener listener : listeners) {
			listener.severityAdded (severity);
		}
	}

	
	public synchronized Priority addPriority (Project project, String name) throws SQLException {
		Priority priority = new Priority (null, project, name);
		add (priority);
		return priority;
	}
	
	public synchronized void add (Priority priority) throws SQLException {
		assert (priority != null);
		Project project = priority.getProject ();
		assert (project.getId () != null);
		assert (priority.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (PRIORITY_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, priority.getName ());
		stmt.executeUpdate();

		priority.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.priorityAdded (priority);
		}
	}


	public synchronized Category addCategory (Project project, String name) throws SQLException {
		Category category = new Category (null, project, name);
		add (category);
		return category;
	}

	public synchronized void add (Category category) throws SQLException {
		assert (category != null);
		Project project = category.getProject ();
		assert (project.getId () != null);
		assert (category.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (CATEGORY_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, category.getName ());
		stmt.executeUpdate();

		category.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.categoryAdded (category);
		}
	}


	public synchronized Component addComponent (Project project, String name) throws SQLException {
		Component component = new Component (null, project, name);
		add (component);
		return component;
	}

	public synchronized void add (Component component) throws SQLException {
		assert (component != null);
		Project project = component.getProject ();
		assert (project.getId () != null);
		assert (component.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (COMPONENT_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, component.getName ());
		stmt.executeUpdate();

		component.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.componentAdded (component);
		}
	}


	public synchronized Bug addBug (Identity identity, Component component,
			String title, Date creation, Priority priority, Severity severity, Category category) throws SQLException {
		Bug bug = new Bug (null, identity, component, title, creation, priority, severity, category);
		add (bug);
		return bug;
	}
	
	public synchronized void add (Bug bug) throws SQLException {
		assert (bug != null);
		assert (bug.getId () == null);
		assert (bug.getIdentity () != null);
		assert (bug.getComponent () != null);
		assert (bug.getPriority () != null);
		assert (bug.getSeverity() != null);
		assert (bug.getIdentity ().getId () != null);
		assert (bug.getComponent ().getId () != null);
		assert (bug.getPriority ().getId () != null);
		assert (bug.getSeverity().getId () != null);
		assert (bug.getCategory () == null || bug.getCategory ().getId () != null);


		PreparedStatement stmt = conn.prepareStatement (BUG_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getIdentity ().getId ());
		stmt.setInt (2, bug.getComponent ().getId ());
		stmt.setString (3, bug.getTitle ());
		stmt.setString (4, dateFormat.format (bug.getCreation ()));
		stmt.setInt (5, bug.getPriority ().getId ());
		stmt.setInt (6, bug.getSeverity ().getId ());
		if (bug.getCategory () != null) {
			stmt.setInt (7, bug.getCategory ().getId ());
		} else {
			stmt.setNull (7, Types.INTEGER);
		}
		stmt.setInt (8, bug.getComponent ().getProject ().getId ());
		stmt.executeUpdate();

		bug.setId (getLastInsertedId (stmt));

		stmt.close ();


		for (ModelModificationListener listener : listeners) {
			listener.bugAdded (bug);
		}
	}


	public synchronized BugHistory addBugHistory (Bug bug, Status status, Identity identity, Date date) throws SQLException {
		BugHistory history = new BugHistory(null, bug, status, identity, date);
		add (history);
		return history;
	}

	public synchronized void add (BugHistory history) throws SQLException {
		assert (history != null);
		assert (history.getId () == null);
		assert (history.getBug ().getId () != null);
		assert (history.getStatus ().getId () != null);
		assert (history.getIdentity ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_HISTORY_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, history.getBug ().getId ());
		stmt.setInt (2, history.getStatus ().getId ());
		stmt.setInt (3, history.getIdentity ().getId ());
		stmt.setString (4, dateFormat.format (history.getDate ()));
		stmt.executeUpdate();

		history.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.bugHistoryAdded (history);
		}
	}


	public synchronized Comment addComment (Bug bug, Date creation, Identity identity, String content) throws SQLException {
		Comment cmnt = new Comment(null, bug, creation, identity, content);
		add (cmnt);
		return cmnt;
	}
	
	public synchronized void add (Comment cmnt) throws SQLException {
		assert (cmnt != null);
		assert (cmnt.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (COMMENT_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, cmnt.getBug ().getId ());
		stmt.setString (2, dateFormat.format (cmnt.getCreationDate ()));
		stmt.setInt (3, cmnt.getIdentity ().getId ());
		stmt.setString (4, cmnt.getContent ());
		stmt.executeUpdate();

		cmnt.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.commentAdded (cmnt);
		}
	}


	public synchronized Status addStatus (Project project, String name) throws SQLException {
		Status status = new Status (null, project, name);
		add (status);
		return status;
	}	
	
	public synchronized void add (Status status) throws SQLException {
		assert (status != null);
		Project project = status.getProject ();
		assert (project.getId () != null);
		assert (status.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (STATUS_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, status.getName ());
		stmt.executeUpdate();

		status.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.statusAdded (status);
		}
	}

		
	public synchronized Commit addCommit (Project project, Identity autor,
			Identity committer, Date date, String title, int linesAdded,
			int linesRemoved, Category category)
		throws SQLException
	{
		Commit commit = new Commit (null, project, autor,
				committer, date, title, linesAdded,
				linesRemoved, category);
		add (commit);
		return commit;
	}
	
	public synchronized void add (Commit commit) throws SQLException {
		assert (commit != null);
		Project project = commit.getProject ();
		assert (commit.getId () == null);
		assert (commit.getAutor ().getId () != null);
		assert (commit.getCommitter ().getId () != null);
		assert (commit.getCategory ().getId () != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (COMMIT_INSERTION,
			Statement.RETURN_GENERATED_KEYS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, commit.getAutor ().getId ());
		stmt.setInt (3, commit.getCommitter ().getId ());
		stmt.setString (4, dateFormat.format (commit.getDate ()));
		stmt.setString (5, commit.getTitle ());
		stmt.setInt (6, commit.getLinesAdded ());
		stmt.setInt (7, commit.getLinesRemoved ());
		stmt.setInt (8, commit.getCategory ().getId ());
		stmt.executeUpdate();

		commit.setId (getLastInsertedId (stmt));

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.commitAdded (commit);
		}
	}

	
	public synchronized BugfixCommit addBugfixCommit (Commit commit, Bug bug) throws SQLException {
		BugfixCommit bugfix = new BugfixCommit (commit, bug);
		add (bugfix);
		return bugfix;
	}

	public synchronized void add (BugfixCommit bugfix) throws SQLException {
		assert (bugfix != null);

		PreparedStatement stmt = conn.prepareStatement (BUGFIX_COMMIT_INSERTION);

		stmt.setInt (1, bugfix.getBug ().getId ());
		stmt.setInt (2, bugfix.getCommit ().getId ());
		stmt.executeUpdate();

		stmt.close ();

	
		for (ModelModificationListener listener : listeners) {
			listener.bugfixCommitAdded (bugfix);
		}
	}


	//
	// Get Data:
	//
	
	private synchronized PreparedStatement buildPreparedStmt (Query queryConfig, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (queryConfig != null);
		assert (vars != null);
		
		// System.out.println ("queryCofnig: " + queryConfig);
		// System.out.println ("vars: " + vars);
		
		LinkedList<Query.VariableSegment> paramOrder = new LinkedList<Query.VariableSegment> ();
		String query = queryConfig.getQuery (vars.keySet (), paramOrder);

		// System.out.println(query.replaceAll("\\s+", " "));

		// Create the query:
		PreparedStatement stmt = conn.prepareStatement (query);
		int i = 1;

		for (Query.VariableSegment param : paramOrder) {
			Object val = vars.get (param.getName ());

			// System.out.println(" ?" + i + " = '" + val + "'");

			if (val instanceof Integer) {
				stmt.setInt (i, (Integer) val);
			} else if (val instanceof Long) {
				stmt.setLong (i, (Long) val);
			} else if (val instanceof String) {
				stmt.setString(i, (String) val);
			} else if (val instanceof Date) {
				stmt.setString (i, dateFormat.format ((Date) val));
			} else if (val instanceof Integer[]) {
				Array arr = conn.createArrayOf ("INTEGER", (Integer[]) val);
				stmt.setArray(i, arr);
			} else if (val instanceof Long[]) {
				Array arr = conn.createArrayOf ("INTEGER", (Long[]) val);
				stmt.setArray(i, arr);			
			} else if (val instanceof String[]) {
				Array arr = conn.createArrayOf ("TEXT", (String[]) val);
				stmt.setArray(i, arr);
			} else if (val instanceof Date[]) {
				Date[] origArr = (Date[]) val;
				String[] resArr = new String[origArr.length];

				for (int ai = 0; ai < origArr.length ; ai++) {
					resArr[ai] = dateFormat.format (origArr[ai]);
				}

				Array arr = conn.createArrayOf ("TEXT", resArr);
				stmt.setArray(i, arr);
			} else {
				throw new SemanticException ("semantic error: Unsupported type " + val.getClass ().getName ()
					+ " for `" + param.getName () + "'", param.getStart (), param.getEnd ());
			}
			i++;
		}
		
		return stmt;
	}

	public synchronized DistributionChartConfigData getDistributionChartData (DistributionChartConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		DistributionChartConfigData data = new DistributionChartConfigData (config.getName ());

		for (DistributionChartOptionConfig optionConfig : config.getOptions ()) {
			DistributionChartOptionConfigData optionData = getDistributionChartOptionConfigData (optionConfig, vars);
			data.addOption (optionData);
		}
		
		return data;
	}
	
	public synchronized DistributionChartOptionConfigData getDistributionChartOptionConfigData (DistributionChartOptionConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		DistributionAttributesConfig attData = config.getAttributes ();
		DistributionChartOptionConfigData data = new DistributionChartOptionConfigData (config.getName (), attData, config);

		for (DropDownConfig dropConf : config.getFilter()) {
			DropDownData dropData = getDropDownData (dropConf, vars);
			data.addFilter (dropData);
		}

		return data;
	}
	
	public synchronized TrendChartData getTrendChartData (TrendChartPlotConfig trendChartPlotConfig, Map<String, Object> vars) throws SemanticException {
		assert (trendChartPlotConfig != null);
		assert (vars != null);

		Query queryConfig = trendChartPlotConfig.getDataQuery ();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars);
			TrendChartData data = new TrendChartData ();


			// Execution:
			ResultSet res = stmt.executeQuery ();
	
			
			// Result Checks:
			ResultSetMetaData meta = res.getMetaData ();
			int colCount = meta.getColumnCount ();
			if (colCount != 2) {
				throw new SemanticException ("semantic error: invalid column count, expected: (<int>, <int>)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}

			if (res.next()) {
				// The check is only vaild in case
				// the result set is not empty.
				
				for (int i = 1; i <= 2 ; i++) {
					int paramType = meta.getColumnType (i);
					if (paramType != Types.INTEGER) {
						
						throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <int>), got ("
							+ "<" + meta.getColumnTypeName (1) + ">, <" + meta.getColumnTypeName (2) + ">)",
							queryConfig.getStart (), queryConfig.getEnd ());
					}
				}

				// Data Collection:
				do {
					int month = res.getInt (1);
					int value = res.getInt (2);

					data.add (month, value);
				} while (res.next());
			}
			
			return data;
		} catch (SQLException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
				queryConfig.getStart (), queryConfig.getEnd ());
		} catch (IllegalArgumentException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
					queryConfig.getStart (), queryConfig.getEnd ());
		}
	}
	
	public synchronized TrendChartConfigData getChartGroupConfigData (TrendChartConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		OptionListConfigData optionListData = getOptionListData (config.getOptionList (), vars);
		TrendChartConfigData data = new TrendChartConfigData (config.getName (), optionListData);

		for (DropDownConfig dropConf : config.getDropDownConfigs ()) {
			DropDownData dropData = getDropDownData (dropConf, vars);
			data.addDropDown (dropData);
		}
		
		return data;
	}
	
	private synchronized DropDownData getDropDownData (DropDownConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars);
	
			// Execution:
			ResultSet res = stmt.executeQuery ();
			DropDownData data = new DropDownData (config);
	
			
			// Result Checks:
			ResultSetMetaData meta = res.getMetaData ();
			int colCount = meta.getColumnCount ();
			if (colCount != 2) {
				throw new SemanticException ("semantic error: invalid column count, expected: (<int>, <text>)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			int paramType = meta.getColumnType (1);
			if (paramType != Types.INTEGER) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <text>), got ("
					+ "<" + meta.getColumnTypeName (1) + ">, <" + meta.getColumnTypeName (2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			paramType = meta.getColumnType (2);
			if (paramType != Types.VARCHAR && paramType != Types.NCHAR && paramType != Types.NVARCHAR) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <text>), got ("
					+ "<" + meta.getColumnTypeName (1) + ">, <" + meta.getColumnTypeName (2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			
			// Data Collection:
			while (res.next()) {
				int id = res.getInt (1);
				String name = res.getString (2);
	
				data.add (id, name);
			}
			
			return data;
		} catch (SQLException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
				queryConfig.getStart (), queryConfig.getEnd ());
		}
	}
	
	private synchronized OptionListConfigData getOptionListData (OptionListConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars);

			// Execution:
			ResultSet res = stmt.executeQuery ();
			OptionListConfigData data = new OptionListConfigData (config);
	
			
			// Result Checks:
			ResultSetMetaData meta = res.getMetaData ();
			int colCount = meta.getColumnCount ();
			if (colCount != 2) {
				throw new SemanticException ("semantic error: invalid column count, expected: (<int>, <text>)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			int paramType = meta.getColumnType (1);
			if (paramType != Types.INTEGER) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <text>), got ("
					+ "<" + meta.getColumnTypeName(1) + ">, <" + meta.getColumnTypeName(2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			paramType = meta.getColumnType (2);
			if (paramType != Types.VARCHAR && paramType != Types.NCHAR && paramType != Types.NVARCHAR) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <text>), got ("
					+ "<" + meta.getColumnTypeName(1) + ">, <" + meta.getColumnTypeName(2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			
			// Data Collection:
			while (res.next()) {
				int id = res.getInt (1);
				String name = res.getString (2);
	
				data.add (id, name);
			}
			
			return data;
		} catch (SQLException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
				queryConfig.getStart (), queryConfig.getEnd ());
		}
	}
	
	public synchronized PieChartData getPieChart (PieChartConfig config, Map<String, Object> vars) throws SemanticException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars);

			// Execution:
			ResultSet res = stmt.executeQuery ();
			PieChartData data = new PieChartData (config.getName (), config.getShowTotal ());
	
			
			// Result checks:
			ResultSetMetaData meta = res.getMetaData ();
			int colCount = meta.getColumnCount ();
			if (colCount != 2) {
				throw new SemanticException ("semantic error: invalid column count, expected: (<text>, <int>)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			int paramType = meta.getColumnType (1);
			if (paramType != Types.VARCHAR && paramType != Types.NCHAR && paramType != Types.NVARCHAR) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<text>, <int>), got ("
					+ "<" + meta.getColumnTypeName(1) + ">, <" + meta.getColumnTypeName(2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
			paramType = meta.getColumnType (2);
			if (paramType != Types.INTEGER) {
				throw new SemanticException ("semantic error: invalid column type, expected: (<text>, <int>), got ("
					+ "<" + meta.getColumnTypeName(1) + "> , <" + meta.getColumnTypeName(2) + ">)",
					queryConfig.getStart (), queryConfig.getEnd ());
			}
	
	
			// Data collection:
			while (res.next()) {
				String name = res.getString (1);
				int val = res.getInt (2);
	
				data.setFraction (name, val);
			}
			
			return data;
		} catch (SQLException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
				queryConfig.getStart (), queryConfig.getEnd ());
		}
	}
	
	public synchronized DistributionChartData getDistributionChartData (Query query, Map<String, Object> vars) throws SemanticException {
		assert (query != null);
		assert (vars != null);


		double[][] data = new double[12][7];

		try {
			PreparedStatement stmt = buildPreparedStmt (query, vars);

			// Execution:
			ResultSet res = stmt.executeQuery ();
	
			
			// Result Checks:
			ResultSetMetaData meta = res.getMetaData ();
			int colCount = meta.getColumnCount ();
			if (colCount != 8) {
				throw new SemanticException ("semantic error: invalid column count, expected: (<int>, <double>, <double>, <double>, <double>, <double>, <double>, <double>)",
					query.getStart (), query.getEnd ());
			}
	
			if (res.next()) {
				// The check is only vaild in case
				// the result set is not empty.
				
				for (int i = 1; i <= 8 ; i++) {
					int paramType = meta.getColumnType (i);
					
					if ((i == 1 && paramType != Types.INTEGER)
						|| (i > 1 && paramType != Types.INTEGER && paramType != Types.DOUBLE && paramType != Types.FLOAT))
					{
						throw new SemanticException ("semantic error: invalid column type, expected: (<int>, <double>, <double>, <double>, <double>, <double>, <double>, <double>), got ("
							+ "<" + meta.getColumnTypeName (1) + ">, <" + meta.getColumnTypeName (2) + ">, "
							+ "<" + meta.getColumnTypeName (3) + ">, <" + meta.getColumnTypeName (4) + ">, "
							+ "<" + meta.getColumnTypeName (5) + ">, <" + meta.getColumnTypeName (6) + ">, "
							+ "<" + meta.getColumnTypeName (7) + ">, <" + meta.getColumnTypeName (8) + ">)",
							query.getStart (), query.getEnd ());
					}
				}

				
				// Data Collection:
				do {
					int month = res.getInt (1);
					if (month < 1 || month > 12) {
						throw new SemanticException ("semantic error: invalid value '" + month + "' for month", query.getStart (), query.getEnd ());
					}

					month--;
					
					data[month][0] = res.getDouble (2);
					data[month][1] = res.getDouble (3);
					data[month][2] = res.getDouble (4);
					data[month][3] = res.getDouble (5);
					data[month][4] = res.getDouble (6);
					data[month][5] = res.getDouble (7);
					data[month][6] = res.getDouble (8);
	
				} while (res.next());
			}
		} catch (SQLException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
				query.getStart (), query.getEnd ());
		}
		
		return new DistributionChartData (data);
	}


	//
	// Listener:
	//

	public synchronized void addListener (ModelModificationListener listener) {
		assert (listener != null);

		listeners.add (listener);
	}

	public synchronized void removeListener (ModelModificationListener listener) {
		assert (listener != null);

		listeners.remove (listener);
	}

	
	//
	// Helper:
	//


	private Integer getLastInsertedId (PreparedStatement stmt) throws SQLException {
		assert (stmt != null);

		ResultSet res = stmt.getGeneratedKeys ();
		boolean hadNext = res.next ();
		assert (hadNext);
		return res.getInt (1);
	}

	private void createTables () throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate (ENABLE_FOREIGN_KEYS);
		stmt.executeUpdate (PROJECT_TABLE);
		stmt.executeUpdate (USER_TABLE);
		stmt.executeUpdate (IDENTITY_TABLE);
		stmt.executeUpdate (INTERACTION_TABLE);
		stmt.executeUpdate (COMPONENT_TABLE);
		stmt.executeUpdate (PRIORITY_TABLE);
		stmt.executeUpdate (SEVERITY_TABLE);
		stmt.executeUpdate (BUG_TABLE);
		stmt.executeUpdate (COMMENT_TABLE);
		stmt.executeUpdate (STATUS_TABLE);
		stmt.executeUpdate (BUG_HISTORY_TABLE);
		stmt.executeUpdate (CATEGORY_TABLE);
		stmt.executeUpdate (COMMIT_TABLE);
		stmt.executeUpdate (BUGFIX_COMMIT_TABLE);

		stmt.executeUpdate (BUG_STATUS_UPDATE_TRIGGER);
		stmt.executeUpdate (BUG_COMMENT_COUNT_UPDATE_TRIGGER);
		stmt.close ();
	}
}
