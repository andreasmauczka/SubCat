/* Model.java
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

package at.ac.tuwien.inso.subcat.model;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.inso.subcat.config.DistributionAttributesConfig;
import at.ac.tuwien.inso.subcat.config.DistributionChartConfig;
import at.ac.tuwien.inso.subcat.config.DistributionChartOptionConfig;
import at.ac.tuwien.inso.subcat.config.DropDownConfig;
import at.ac.tuwien.inso.subcat.config.OptionListConfig;
import at.ac.tuwien.inso.subcat.config.PieChartConfig;
import at.ac.tuwien.inso.subcat.config.Query;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.config.TrendChartConfig;
import at.ac.tuwien.inso.subcat.config.TrendChartPlotConfig;


public class Model {
	public static final String FLAG_SRC_FILE_STATS = "SRC_FILE_STATS";
	public static final String FLAG_SRC_LINE_STATS = "SRC_LINE_STATS";
	public static final String FLAG_BUG_COMMENTS = "BUG_COMMENTS";
	public static final String FLAG_BUG_HISTORY = "BUG_HISTORY";
	public static final String FLAG_SRC_INFO = "SRC_INFO";

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String name;

	private LinkedList<Connection> connections = new LinkedList<Connection> ();
	private int connPoolSize;

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
		+ "domain		TEXT								        ,"
		+ "product		TEXT								        ,"
		+ "revision		TEXT								        ,"
		+ "defaultStatusId INT										 "
//		+ "FOREIGN KEY (defaultStatusId) REFERENCES StatusTable"
		+ ")";

	private static final String PROJECT_FLAG_TABLE =
		"CREATE TABLE IF NOT EXISTS ProjectFlags ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "flag			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
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
		+ "identity		INT											,"
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
		+ "author		INT									NOT NULL,"
		+ "committer	INT									NOT NULL,"	// TODO: Rename to Pusher
		+ "date			TEXT								NOT NULL,"
		+ "title		TEXT								NOT NULL,"
		+ "changedFiles INT									NOT NULL,"
		+ "linesAdded	INT									NOT NULL,"
		+ "linesRemoved	INT									NOT NULL,"
		+ "category		INT									        ,"
		+ "FOREIGN KEY(committer) REFERENCES Identities (id),"
		+ "FOREIGN KEY(author) REFERENCES Identities (id),"
		+ "FOREIGN KEY(category) REFERENCES Categories (id),"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String FILE_TABLE =
		"CREATE TABLE IF NOT EXISTS Files ("
		+ "id				INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project			INT									NOT NULL,"
		+ "name				TEXT								NOT NULL,"
		+ "touched			INTEGER				DEFAULT 1		NOT NULL,"
		+ "linesAdded		INTEGER								NOT NULL,"
		+ "linesRemoved		INTEGER				DEFAULT 0		NOT NULL,"
		+ "chunksChanged	INTEGER				DEFAULT	1		NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String FILE_DELETION_TABLE =
		"CREATE TABLE IF NOT EXISTS FileDeletion ("
		+ "fileId			INTEGER		NOT NULL,"
		+ "commitId			INTEGER		NOT NULL,"
		+ "PRIMARY KEY (fileId, commitId),"
		+ "FOREIGN KEY(fileId) REFERENCES Files (id),"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id)"
		+ ")";

	private static final String FILE_COPY_TABLE =
		"CREATE TABLE IF NOT EXISTS FileCopy ("
		+ "fileId			INTEGER		NOT NULL,"
		+ "commitId			INTEGER		NOT NULL,"
		+ "originalFileId	INTEGER		NOT NULL,"
		+ "PRIMARY KEY (fileId, commitId),"
		+ "FOREIGN KEY(fileId) REFERENCES Files (id),"
		+ "FOREIGN KEY(originalFileId) REFERENCES Files (id),"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id)"
		+ ")";
	
	private static final String FILE_RENAMES_TABLE =
		"CREATE TABLE IF NOT EXISTS FileRenames ("
		+ "file			INTEGER		NOT NULL,"
		+ "commitId		INTEGER		NOT NULL,"
		+ "oldName		TEXT		NOT NULL,"
		+ "PRIMARY KEY (commitId, file),"
		+ "FOREIGN KEY(file) REFERENCES Files (id),"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id)"
		+ ")";

	private static final String FILE_CHANGES_TABLE =
		"CREATE TABLE IF NOT EXISTS FileChanges ("
		+ "commitId				INTEGER		NOT NULL,"
		+ "file					INTEGER		NOT NULL,"
		+ "linesAdded			INTEGER		NOT NULL,"
		+ "linesRemoved			INTEGER		NOT NULL,"
		+ "emptyLinesAdded		INTEGER		NOT NULL,"
		+ "emptyLinesRemoved	INTEGER 	NOT NULL,"
		+ "chunksChanged		INTEGER		NOT NULL,"
		+ "PRIMARY KEY (commitId, file),"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id),"
		+ "FOREIGN KEY(file) REFERENCES Files (id)"
		+ ")";

	// TODO: Files.touched handler
	// TODO: creation functions fertig stellen
	
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

	private static final String SELECT_ALL_COMMITS =
		"SELECT"
		+ " Commits.id				AS cId,"
		+ " Commits.date			AS cDate,"
		+ " Commits.title			AS cTitle,"
		+ " Commits.linesAdded		AS cLinesAdded,"
		+ " Commits.linesRemoved	AS cLinesAdded,"
		+ " Commits.category		AS cCategory,"
		+ " AuthorIdentity.id		AS aiId,"
		+ " AuthorIdentity.name		AS aiName,"
		+ " AuthorIdentity.mail		AS aiName,"
		+ " AuthorUser.id			AS auId,"
		+ " AuthorUser.name			AS auName,"
		+ " CommitterIdentity.id	AS aiId,"
		+ " CommitterIdentity.name	AS aiName,"
		+ " CommitterIdentity.mail	AS aiName,"
		+ " CommitterUser.id		AS auId,"
		+ " CommitterUser.name		AS auName,"
		+ " Commits.changedFiles "
		+ "FROM"
		+ " Commits "
		+ "LEFT JOIN Identities AuthorIdentity"
		+ " ON Commits.author = AuthorIdentity.id "
		+ "LEFT JOIN Users AuthorUser"
		+ " ON AuthorUser.id = AuthorIdentity.user "
		+ "LEFT JOIN Identities CommitterIdentity"
		+ " ON Commits.author = CommitterIdentity.id "
		+ "LEFT JOIN Users CommitterUser"
		+ " ON CommitterUser.id = CommitterIdentity.user "
		+ "WHERE"
		+ " Commits.project = ?";

	private static final String SELECT_ALL_BUGS = 
		"SELECT"
		+ " Bugs.id,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiName,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Bugs.component,"
		+ " Bugs.title,"
		+ " Bugs.creation,"
		+ " Bugs.priority,"
		+ " Bugs.severity,"
		+ " Bugs.category,"
		+ " Bugs.comments,"
		+ " Bugs.curStat "
		+ "FROM"
		+ " Bugs "
		+ "LEFT JOIN Identities Identity"
		+ " ON Bugs.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "JOIN Components"
		+ " ON Components.id = Bugs.component "
		+ "WHERE"
		+ " Components.project = ?";

	private static final String SELECT_ALL_CATEGORIES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Categories "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_SEVERITIES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Severity "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_STATUSES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM "
		+ " Status "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_PRIORITIES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Priorities "
		+ "WHERE "
		+ " project = ?";

	private static final String SELECT_ALL_COMPONENTS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Components "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_PROJECTS =
		"SELECT"
		+ " id,"
		+ " date,"
		+ " domain,"
		+ " product,"
		+ " revision "
		+ "FROM"
		+ " Projects";

	private static final String SELECT_ALL_FLAGS =
		"SELECT"
		+ " flag "
		+ "FROM"
		+ " ProjectFlags "
		+ "WHERE"
		+ " project = ?";
	
	private static final String SELECT_ALL_COMMENTS =
		"SELECT"
		+ " Comments.id,"
		+ " Comments.creation,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiName,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Comments.content "
		+ "FROM"
		+ " Comments "
		+ "LEFT JOIN Identities Identity"
		+ " ON Comments.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "WHERE"
		+ " Comments.bug = ?";

	private static final String SELECT_FULL_HISTORY =
		"SELECT"
		+ " BugHistories.id,"
		+ " Status.id,"
		+ " Status.name,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiName,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " BugHistories.date "
		+ "FROM"
		+ " BugHistories "
		+ "LEFT JOIN Identities Identity"
		+ " ON BugHistories.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "LEFT JOIN Status"
		+ " ON BugHistories.status = Status.id "
		+ "WHERE"
		+ " BugHistories.bug = ? "
		+ "ORDER BY"
		+ " BugHistories.id";


	//
	// Insertions:
	//

	private static final String PROJECT_INSERTION =
		"INSERT INTO Projects"
		+ "(date, domain, product, revision, defaultStatusId)"
		+ "VALUES (?,?,?,?,?)";

	private static final String PROJECT_FLAG_INSERTION =
			"INSERT INTO ProjectFlags"
			+ "(project, flag)"
			+ "VALUES (?,?)";
	
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
		+ "(project, author, committer, date, title, linesAdded, linesRemoved, changedFiles, category)"
		+ "VALUES (?,?,?,?,?,?,?,?,?)";

	private static final String BUGFIX_COMMIT_INSERTION =
		"INSERT INTO BugfixCommit"
		+ "(bug, commitId)"
		+ "VALUES (?,?)";

	private static final String SEVERITY_INSERTION =
		"INSERT INTO Severity "
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String FILE_INSERTION =
		"INSERT INTO Files "
		+ "(project, name, linesAdded)"
		+ "VALUES (?,?,?)";
	
	private static final String FILE_RENAME_INSERTION =
		"INSERT INTO FileRenames"
		+ "(file, commitId, oldName)"
		+ "VALUES (?,?,?)";

	private static final String FILE_CHANGE_INSERTION =
		"INSERT INTO FileChanges"
		+ "(commitId, file, linesAdded, linesRemoved, emptyLinesAdded, emptyLinesRemoved, chunksChanged)"
		+ "VALUES (?,?,?,?,?,?,?)";

	private static final String FILE_DELETION_INSERTION =
		"INSERT INTO FileDeletion"
		+ "(fileId, commitId)"
		+ "VALUES (?,?)";

	private static final String FILE_COPY_INSERTION =
		"INSERT INTO FileCopy"
		+ "(fileId, commitId, originalFileId)"
		+ "VALUES (?,?,?)";
	
	private static final String UPDATE_DEFAULT_STATUS =
		"UPDATE Projects SET defaultStatusId = ? WHERE id = ?";



	
	//
	// Creation & Destruction:
	//
	
	public Model (String name) throws ClassNotFoundException, SQLException {
		this (name, 1);
	}
	
	public Model (String name, int connPoolSize) throws SQLException, ClassNotFoundException {
		assert (connPoolSize >= 1);
		assert (name != null);

		Class.forName ("org.sqlite.JDBC");
		this.name = name;

		this.connPoolSize = 0;
		setConnectionPoolSize (connPoolSize);

		// Prepare database:
		createTables ();
	}

	
	//
	// Connection helper:
	//
	
	private synchronized Connection popConnection () throws SQLException {
		assert (connections != null);
		
		Connection conn = connections.poll ();
		if (connections.size () == 0) {
			conn = createConnection ();
		}
		
		return conn;
	}

	private synchronized void pushConnection (Connection conn) throws SQLException {
		assert (conn != null);

		if (connections == null || connections.size () == connPoolSize) {
			conn.close ();
		} else {
			connections.add (conn);
		}
	}

	public synchronized void close () throws SQLException {
		for (Connection conn : connections) {
			conn.close ();
		}

		connections = null;
		connPoolSize = -1;
		name = null;
	}

	public synchronized boolean remove() throws SQLException {
		assert (name != null);
		close ();

		File file = new File (name + ".db");
		boolean res = file.delete ();
		
		return res;
	}
	
	public synchronized void setConnectionPoolSize (int poolSize) throws SQLException {
		assert (poolSize > 0);

		if (poolSize > this.connPoolSize) {
			for (int i = this.connPoolSize; i <= poolSize ; i++) {
				Connection conn = createConnection ();
				connections.add (conn);				
			}
		} else if (poolSize < this.connPoolSize) {
			while (connections.size () > poolSize) {
				connections.pollFirst ().close ();
			}
		}

		this.connPoolSize = poolSize;
	}

	private Connection createConnection () throws SQLException {
		org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig ();
		config.enforceForeignKeys (true);

		// TODO: Escape path
		Connection conn = DriverManager.getConnection ("jdbc:sqlite:" + name,
			config.toProperties());

		Statement stmt = conn.createStatement();
		stmt.executeUpdate (ENABLE_FOREIGN_KEYS);
		stmt.close ();

		return conn;
	}
	
	
	//
	// Data Insertion API:
	//

	
	public void addFlag (Project proj, String flag) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);
		assert (flag != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (PROJECT_FLAG_INSERTION);
		
			stmt.setInt (1, proj.getId ());
			stmt.setString (2, flag);
			stmt.executeUpdate();
			stmt.close ();
		} finally {
			pushConnection (conn);
		}
	}

	public List<String> getFlags (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_FLAGS);
			stmt.setInt (1, proj.getId ());

			// Collect data:
			LinkedList<String> flags = new LinkedList<String> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				String flag = res.getString (1);	
				flags.add (flag);
			}
	
			return flags;
		} finally {
			pushConnection (conn);
		}
	}
	
	public Project addProject (Date date, String domain, String product, String revision) throws SQLException {
		Project proj = new Project (null, date, domain, product, revision);
		add (proj);
		return proj;
	}

	public void setDefaultStatus (Status status) throws SQLException {
		assert (status != null);
		assert (status.getId () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (UPDATE_DEFAULT_STATUS);
		
			stmt.setInt (1, status.getId ());
			stmt.setInt (2, status.getProject ().getId ());
			stmt.executeUpdate();
			stmt.close ();
		} finally {
			pushConnection (conn);
		}
	}
	
	public void add (Project project) throws SQLException {
		assert (project != null);
		assert (project.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}

	
	public User addUser (Project project, String name) throws SQLException {
		User user = new User (null, project, name);
		add (user);
		return user;
	}

	public void add (User user) throws SQLException {
		assert (user != null);
		Project project = user.getProject ();
		assert (project.getId () != null);
		assert (user.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}
	
	
	public Identity addIdentity (String mail, String name, User user) throws SQLException {
		Identity identity = new Identity (null, mail, name, user);
		add (identity);
		return identity;
	}

	public void add (Identity identity) throws SQLException {
		assert (identity != null);
		assert (identity.getUser ().getId () != null);
		assert (identity.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}
	
	
	public Interaction addInteraction (User from, User to, boolean closed, int quotes, float pos, float neg, Date date) throws SQLException {
		Interaction relation = new Interaction (null, from, to, closed, quotes, pos, neg, date);
		add (relation);
		return relation;
	}

	public void add (Interaction relation) throws SQLException {
		assert (relation != null);
		assert (relation.getFrom ().getId () != null);
		assert (relation.getTo ().getId () != null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}

		
	public Severity addSeverity (Project project, String name) throws SQLException {
		Severity sev = new Severity (null, project, name);
		add (sev);
		return sev;
	}

	public void add (Severity severity) throws SQLException {
		assert (severity != null);
		Project project = severity.getProject ();
		assert (project.getId () != null);
		assert (severity.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}

	
	public Priority addPriority (Project project, String name) throws SQLException {
		Priority priority = new Priority (null, project, name);
		add (priority);
		return priority;
	}
	
	public void add (Priority priority) throws SQLException {
		assert (priority != null);
		Project project = priority.getProject ();
		assert (project.getId () != null);
		assert (priority.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}


	public Category addCategory (Project project, String name) throws SQLException {
		Category category = new Category (null, project, name);
		add (category);
		return category;
	}

	public void add (Category category) throws SQLException {
		assert (category != null);
		Project project = category.getProject ();
		assert (project.getId () != null);
		assert (category.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}


	public Component addComponent (Project project, String name) throws SQLException {
		Component component = new Component (null, project, name);
		add (component);
		return component;
	}

	public void add (Component component) throws SQLException {
		assert (component != null);
		Project project = component.getProject ();
		assert (project.getId () != null);
		assert (component.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}


	public Bug addBug (Identity identity, Component component,
			String title, Date creation, Priority priority, Severity severity, Category category) throws SQLException {
		Bug bug = new Bug (null, identity, component, title, creation, priority, severity, category);
		add (bug);

		return bug;
	}
	
	public void add (Bug bug) throws SQLException {
		assert (bug != null);
		assert (bug.getId () == null);
		assert (bug.getComponent () != null);
		assert (bug.getPriority () != null);
		assert (bug.getSeverity() != null);
		assert (bug.getIdentity () == null || bug.getIdentity ().getId () != null);
		assert (bug.getComponent ().getId () != null);
		assert (bug.getPriority ().getId () != null);
		assert (bug.getSeverity().getId () != null);
		assert (bug.getCategory () == null || bug.getCategory ().getId () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (BUG_INSERTION,
				Statement.RETURN_GENERATED_KEYS);
	
			if (bug.getIdentity () != null) {
				stmt.setInt (1, bug.getIdentity ().getId ());
			} else {
				stmt.setNull (1, Types.INTEGER);
			}
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
		} finally {
			pushConnection (conn);
		}
	}


	public BugHistory addBugHistory (Bug bug, Status status, Identity identity, Date date) throws SQLException {
		BugHistory history = new BugHistory(null, bug, status, identity, date);
		add (history);
		return history;
	}

	public void add (BugHistory history) throws SQLException {
		assert (history != null);
		assert (history.getId () == null);
		assert (history.getBug ().getId () != null);
		assert (history.getStatus ().getId () != null);
		assert (history.getIdentity ().getId () != null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}

	public Comment addComment (Bug bug, Date creation, Identity identity, String content) throws SQLException {
		Comment cmnt = new Comment(null, bug, creation, identity, content);
		add (cmnt);
		return cmnt;
	}
	
	public void add (Comment cmnt) throws SQLException {
		assert (cmnt != null);
		assert (cmnt.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}


	public Status addStatus (Project project, String name) throws SQLException {
		Status status = new Status (null, project, name);
		add (status);
		return status;
	}	
	
	public void add (Status status) throws SQLException {
		assert (status != null);
		Project project = status.getProject ();
		assert (project.getId () != null);
		assert (status.getId () == null);

		Connection conn = popConnection ();

		try {
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
		} finally {
			pushConnection (conn);
		}
	}

		
	public Commit addCommit (Project project, Identity author,
			Identity committer, Date date, String title, int changedFiles,
			int changedLines, int linesRemoved, Category category)
		throws SQLException
	{
		Commit commit = new Commit (null, project, author,
				committer, date, title, changedFiles,
				changedLines, linesRemoved, category);
		add (commit);
		return commit;
	}
	
	public void add (Commit commit) throws SQLException {
		assert (commit != null);
		Project project = commit.getProject ();
		assert (commit.getId () == null);
		assert (commit.getAuthor ().getId () != null);
		assert (commit.getCommitter ().getId () != null);
		assert (project.getId () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (COMMIT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);
			stmt.setInt (1, project.getId ());
			stmt.setInt (2, commit.getAuthor ().getId ());
			stmt.setInt (3, commit.getCommitter ().getId ());
			stmt.setString (4, dateFormat.format (commit.getDate ()));
			stmt.setString (5, commit.getTitle ());
			stmt.setInt (6, commit.getLinesAdded ());
			stmt.setInt (7, commit.getLinesRemoved ());
			stmt.setInt (8, commit.getChangedFiles ());
			if (commit.getCategory () != null) {
				stmt.setInt (9, commit.getCategory ().getId ());
			} else {
				stmt.setNull (9, Types.INTEGER);
			}
			stmt.executeUpdate();
	
			commit.setId (getLastInsertedId (stmt));
	
			stmt.close ();
	
		
			for (ModelModificationListener listener : listeners) {
				listener.commitAdded (commit);
			}
		} finally {
			pushConnection (conn);
		}
	}
		
	
	public ManagedFileCopy addIsCopy (ManagedFile copy, Commit commit, ManagedFile original) throws SQLException {
		ManagedFileCopy _copy = new ManagedFileCopy (copy, commit, original);
		add (_copy);
		return _copy;
	}

	public void add (ManagedFileCopy copy) throws SQLException {
		assert (copy != null);
		assert (copy.getFile ().getId () != null);
		assert (copy.getOriginal ().getId () != null);
		assert (copy.getCommit ().getId () != null);

	
		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (FILE_COPY_INSERTION);
	
			stmt.setInt (1, copy.getFile ().getId ());
			stmt.setInt (2, copy.getCommit ().getId ());
			stmt.setInt (3, copy.getOriginal ().getId ());
			stmt.executeUpdate();
			stmt.close ();

	
			for (ModelModificationListener listener : listeners) {
				listener.managedFileCopyAdded (copy);
			}
		} finally {
			pushConnection (conn);
		}
	}

	// TODO: calculate lines-added, lines-removed, chunks-changed
	public ManagedFile addManagedFile (Project project, String name) throws SQLException {
		ManagedFile file = new ManagedFile (null, project, name, null, 1, 0, 0, 1);
		add (file);
		return file;
	}

	public void add (ManagedFile file) throws SQLException {
		assert (file != null);
		assert (file.getId () == null);
		assert (file .getChunksChanged () == 1);
		assert (file.getLinesRemoved () == 0);
		assert (file.getTouched () == 1);
		assert (file.getProject ().getId () != null);

		
		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (FILE_INSERTION);
	
			stmt.setInt (1, file.getProject ().getId ());
			stmt.setString (2, file.getName ());
			stmt.setInt (3, file.getLinesAdded ());
			stmt.executeUpdate();
			stmt.close ();

			file.setId (getLastInsertedId (stmt));
			
			for (ModelModificationListener listener : listeners) {
				listener.managedFileAdded (file);
			}
		} finally {
			pushConnection (conn);
		}
	}

	
	public FileDeletion addFileDeletion (ManagedFile file, Commit commit) throws SQLException {
		FileDeletion deletion = new FileDeletion (file, commit);
		add (deletion);
		return deletion;
	}

	public void add (FileDeletion deletion) throws SQLException {
		assert (deletion != null);
		assert (deletion.getCommit ().getId () != null);
		assert (deletion.getFile ().getId () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (FILE_DELETION_INSERTION);
	
			stmt.setInt (1, deletion.getFile ().getId ());
			stmt.setInt (2, deletion.getCommit ().getId ());
			stmt.executeUpdate();
			
			stmt.close ();
	

			for (ModelModificationListener listener : listeners) {
				listener.fileDeletedAdded (deletion);
			}
		} finally {
			pushConnection (conn);
		}
	}


	public FileRename addFileRename (ManagedFile file, Commit commit, String oldName) throws SQLException {
		FileRename rename = new FileRename (file, commit, oldName);
		add (rename);
		return rename;
	}

	public void add (FileRename rename) throws SQLException {
		assert (rename != null);
		assert (rename.getCommit ().getId () != null);
		assert (rename.getFile ().getId () != null);
		assert (rename.getOldName () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (FILE_RENAME_INSERTION);
	
			stmt.setInt (1, rename.getFile ().getId ());
			stmt.setInt (2, rename.getCommit ().getId ());
			stmt.setString (3, rename.getOldName ());
			stmt.executeUpdate();
			
			stmt.close ();
	

			for (ModelModificationListener listener : listeners) {
				listener.fileRenameAdded (rename);
			}
		} finally {
			pushConnection (conn);
		}
	}

	
	public FileChange addFileChange (Commit commit, ManagedFile file, int linesAdded, int linesRemoved, int emptyLinesAdded, int emptyLinesRemoved, int changedChunks) throws SQLException {
		FileChange change = new FileChange (commit, file, linesAdded, linesRemoved, emptyLinesAdded, emptyLinesRemoved, changedChunks);
		add (change);
		return change;
	}

	public void add (FileChange change) throws SQLException {
		assert (change != null);
		assert (change.getCommit ().getId () != null);
		assert (change.getFile ().getId () != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (FILE_CHANGE_INSERTION);
	
			stmt.setInt (1, change.getCommit ().getId ());
			stmt.setInt (2, change.getFile ().getId ());
			stmt.setInt (3, change.getLinesAdded ());
			stmt.setInt (4, change.getLinesRemoved ());
			stmt.setInt (5, change.getEmptyLinesAdded ());
			stmt.setInt (6, change.getEmptyLinesRemoved ());
			stmt.setInt (7, change.getChangedChunks ());
			stmt.executeUpdate();
			
			stmt.close ();
	

			for (ModelModificationListener listener : listeners) {
				listener.fileChangeAdded (change);
			}
		} finally {
			pushConnection (conn);
		}
	}
	
	
	public BugfixCommit addBugfixCommit (Commit commit, Bug bug) throws SQLException {
		BugfixCommit bugfix = new BugfixCommit (commit, bug);
		add (bugfix);
		return bugfix;
	}

	public void add (BugfixCommit bugfix) throws SQLException {
		assert (bugfix != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (BUGFIX_COMMIT_INSERTION);
	
			stmt.setInt (1, bugfix.getBug ().getId ());
			stmt.setInt (2, bugfix.getCommit ().getId ());
			stmt.executeUpdate();
	
			stmt.close ();


			for (ModelModificationListener listener : listeners) {
				listener.bugfixCommitAdded (bugfix);
			}
		} finally {
			pushConnection (conn);
		}
	}


	//
	// Get Data:
	//

	private PreparedStatement buildPreparedStmt (Query queryConfig, Map<String, Object> vars, Connection conn) throws SemanticException, SQLException {
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

	private PreparedStatement buildPreparedStmt (String query, Map<String, Object> vars, Connection conn) throws SemanticException, SQLException {
		assert (query != null);
		assert (vars != null);
		
		// System.out.println ("queryCofnig: " + queryConfig);
		// System.out.println ("vars: " + vars);
		
		LinkedList<Query.VariableSegment> paramOrder = new LinkedList<Query.VariableSegment> ();

		// Create the query:
		PreparedStatement stmt = conn.prepareStatement (query);
		int i = 1;

		for (Query.VariableSegment param : paramOrder) {
			Object val = vars.get (param.getName ());

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

	public DistributionChartConfigData getDistributionChartData (DistributionChartConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (config != null);
		assert (vars != null);

		DistributionChartConfigData data = new DistributionChartConfigData (config.getName ());

		for (DistributionChartOptionConfig optionConfig : config.getOptions ()) {
			DistributionChartOptionConfigData optionData = getDistributionChartOptionConfigData (optionConfig, vars);
			data.addOption (optionData);
		}
		
		return data;
	}
	
	public DistributionChartOptionConfigData getDistributionChartOptionConfigData (DistributionChartOptionConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
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
	
	public TrendChartData getTrendChartData (TrendChartPlotConfig trendChartPlotConfig, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (trendChartPlotConfig != null);
		assert (vars != null);

		Query queryConfig = trendChartPlotConfig.getDataQuery ();
		Connection conn = popConnection ();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars, conn);
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
		} catch (IllegalArgumentException e) {
			throw new SemanticException ("semantic error: " + e.getMessage (),
					queryConfig.getStart (), queryConfig.getEnd ());
		} finally {
			pushConnection (conn);
		}
	}
	
	public TrendChartConfigData getChartGroupConfigData (TrendChartConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
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
	
	private DropDownData getDropDownData (DropDownConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		Connection conn = popConnection ();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars, conn);
	
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
		} finally {
			pushConnection (conn);
		}
	}
	
	private OptionListConfigData getOptionListData (OptionListConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		Connection conn = popConnection ();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars, conn);

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
		} finally {
			pushConnection (conn);
		}
	}
	
	public PieChartData getPieChart (PieChartConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
		Connection conn = popConnection ();
		try {
			PreparedStatement stmt = buildPreparedStmt (queryConfig, vars, conn);

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
		} finally {
			pushConnection (conn);
		}
	}
	
	public DistributionChartData getDistributionChartData (Query query, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (query != null);
		assert (vars != null);


		double[][] data = new double[12][7];
		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = buildPreparedStmt (query, vars, conn);

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
		} finally {
			pushConnection (conn);
		}
		
		return new DistributionChartData (data);
	}

	public void foreachCommit (Project proj, ObjectCallback<Commit> callback) throws SQLException, Exception {
		assert (proj != null);
		assert (proj.getId () != null);
		assert (callback != null);

		Map<Integer, Category> categories = getCategories (proj);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMMITS);
			stmt.setInt (1, proj.getId ());
	
			// Execution:
			ResultSet res = stmt.executeQuery ();
			boolean do_next = true;
			while (res.next () && do_next) {
				Integer id = res.getInt (1);
				Date date = res.getDate (2);
				String title = res.getString (3);
				Integer linesAdded = res.getInt (4);
				Integer linesRemoved = res.getInt (5);
				Category category = categories.get (res.getInt (6));
				User authorUser = userFromResult (res, proj, 10, 11);
				Identity author = identityFromResult (res, authorUser, 7, 8, 9);
				User committerUser = userFromResult (res, proj, 15, 16);
				Identity committer = identityFromResult (res, committerUser, 12, 13, 14);
				Integer changedFiles = res.getInt (15);
	
				Commit commit = new Commit (id, proj, author, committer, date, title, changedFiles, linesAdded, linesRemoved, category);
				do_next = callback.processResult (commit);
			}
	
			res.close ();
		} finally {
			pushConnection (conn);
		}
	}

	public void foreachBug (Project proj, ObjectCallback<Bug> callback) throws SQLException, Exception {
		assert (proj != null);
		assert (proj.getId () != null);
		assert (callback != null);

		// Cache some values:
		Map<Integer, Category> categories = getCategories (proj);
		Map<Integer, Severity> severities = getSeverities (proj);
		//Map<Integer, Status> statuses = getStatuses (proj);
		Map<Integer, Priority> priorities = getPriorities (proj);
		Map<Integer, Component> components = getComponents (proj);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUGS);
			stmt.setInt (1, proj.getId ());
	
			// Execution:
			ResultSet res = stmt.executeQuery ();
			boolean do_next = true;
			while (res.next () && do_next) {
				User user = null;
				Identity identity = null;

				Integer id = res.getInt (1);
				if (res.getObject (2, Integer.class) != null) {
					user = userFromResult (res, proj, 5, 6);
					identity = identityFromResult (res, user, 2, 3, 4);
				}
				Component component = components.get (res.getInt (7));
				String title = res.getString (8);
				Date creation = res.getDate (9);
				Priority priority = priorities.get (res.getInt (10));
				Severity severity = severities.get (res.getInt (11));
				Category category = categories.get (res.getInt (12));
				// Integer comments = res.getInt (13);
				// Status status = statuses.get (res.getInt (14));
	
				Bug bug = new Bug (id, identity, component,
					title, creation, priority, severity,
					category);
				do_next = callback.processResult (bug);
			}
	
			res.close ();
		} finally {
			pushConnection (conn);
		}
	}

	private User userFromResult (ResultSet res, Project proj, int idCol, int nameCol) throws SQLException {
		Integer id = res.getInt (idCol);
		String name = res.getString (nameCol);
		return new User (id, proj, name);
	}

	private Identity identityFromResult (ResultSet res, User user, int idCol, int nameCol, int mailCol) throws SQLException {
		Integer id = res.getInt (idCol);
		String mail = res.getString (mailCol);
		String name = res.getString (nameCol);		
		return new Identity (id, mail, name, user);
	}
	
	public void rawForeach (Query query, Map<String, Object> vars, ResultCallback callback) throws SQLException, Exception {
		assert (query != null);
		assert (vars != null);
		assert (callback != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = buildPreparedStmt (query, vars, conn);
	
			// Execution:
			ResultSet res = stmt.executeQuery ();
			callback.processResult (res);
		} finally {
			pushConnection (conn);
		}
	}

	public void rawForeach (String query, Map<String, Object> vars, ResultCallback callback) throws SQLException, Exception {
		assert (query != null);
		assert (callback != null);

		Connection conn = popConnection ();

		try {
			PreparedStatement stmt = buildPreparedStmt (query, vars, conn);
		
			// Execution:
			ResultSet res = stmt.executeQuery ();
			callback.processResult (res);
		} finally {
			pushConnection (conn);
		}
	}

	public Map<Integer, Category> getCategories (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_CATEGORIES);
			stmt.setInt (1, proj.getId ());
	
			// Collect data:
			HashMap<Integer, Category> categories = new HashMap<Integer, Category> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Category category = new Category (res.getInt (1), proj, res.getString (2));
				categories.put (category.getId (), category);
			}
	
			return categories;
		} finally {
			pushConnection (conn);
		}
	}

	public Map<Integer, Component> getComponents (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMPONENTS);
			stmt.setInt (1, proj.getId ());
	
			// Collect data:
			HashMap<Integer, Component> components = new HashMap<Integer, Component> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Component category = new Component (res.getInt (1), proj, res.getString (2));
				components.put (category.getId (), category);
			}
	
			return components;
		} finally {
			pushConnection (conn);
		}
	}

	public Map<Integer, Severity> getSeverities (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_SEVERITIES);
			stmt.setInt (1, proj.getId ());
	
			// Collect data:
			HashMap<Integer, Severity> severities = new HashMap<Integer, Severity> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Severity severity = new Severity (res.getInt (1), proj, res.getString (2));
				severities.put (severity.getId (), severity);
			}
	
			return severities;
		} finally {
			pushConnection (conn);
		}
	}

	public Map<Integer, Status> getStatuses (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_STATUSES);
			stmt.setInt (1, proj.getId ());
	
			// Collect data:
			HashMap<Integer, Status> statuses = new HashMap<Integer, Status> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Status status = new Status (res.getInt (1), proj, res.getString (2));
				statuses.put (status.getId (), status);
			}
	
			return statuses;
		} finally {
			pushConnection (conn);
		}
	}

	public Map<Integer, Priority> getPriorities (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PRIORITIES);
			stmt.setInt (1, proj.getId ());
	
			// Collect data:
			HashMap<Integer, Priority> priorities = new HashMap<Integer, Priority> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Priority priority = new Priority (res.getInt (1), proj, res.getString (2));
				priorities.put (priority.getId (), priority);
			}
	
			return priorities;
		} finally {
			pushConnection (conn);
		}
	}

	public List<BugHistory> getBugHistory (Project proj, Bug bug) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);


		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_FULL_HISTORY);
			stmt.setInt (1, bug.getId ());
	
			// Collect data:
			LinkedList<BugHistory> history = new LinkedList<BugHistory> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Integer id = res.getInt (1);
				Status status = new Status (res.getInt (2), proj, res.getString (3));
				User user = userFromResult (res, proj, 7, 8);
				Identity identity = identityFromResult (res, user, 4, 5, 6);
				Date creation = res.getDate (9);

				BugHistory entry = new BugHistory (id, bug, status, identity, creation);
				history.add (entry);
			}
	
			return history;
		} finally {
			pushConnection (conn);
		}	
	}
	
	public List<Comment> getComments (Project proj, Bug bug) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (proj != null);
		assert (proj.getId () != null);

		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMMENTS);
			stmt.setInt (1, bug.getId ());
	
			// Collect data:
			LinkedList<Comment> comments = new LinkedList<Comment> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Integer id = res.getInt (1);
				Date creation = res.getDate (2);
				User user = userFromResult (res, proj, 6, 7);
				Identity identity = identityFromResult (res, user, 3, 4, 5);
				String content = res.getString (8);

				Comment comment = new Comment (id, bug, creation, identity, content);
				comments.add (comment);
			}
	
			return comments;
		} finally {
			pushConnection (conn);
		}
	}
	
	public List<Project> getProjects () throws SQLException {
		Connection conn = popConnection ();

		try {
			// Statement:
			PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PROJECTS);
	
			// Collect data:
			LinkedList<Project> projects = new LinkedList<Project> ();
			ResultSet res = stmt.executeQuery ();
			while (res.next ()) {
				Integer id = res.getInt (1);
				Date date = res.getDate (2);
				String domain = res.getString (3);
				String product = res.getString (4);
				String revision = res.getString (5);
	
				Project proj = new Project (id, date, domain, product, revision);
				projects.add (proj);
			}
	
			return projects;
		} finally {
			pushConnection (conn);
		}
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
		Connection conn = popConnection ();

		try {
			Statement stmt = conn.createStatement();
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
			stmt.executeUpdate (FILE_TABLE);
			stmt.executeUpdate (FILE_RENAMES_TABLE);
			stmt.executeUpdate (FILE_CHANGES_TABLE);
			stmt.executeUpdate (FILE_DELETION_TABLE);
			stmt.executeUpdate (FILE_COPY_TABLE);
			stmt.executeUpdate (BUG_STATUS_UPDATE_TRIGGER);
			stmt.executeUpdate (BUG_COMMENT_COUNT_UPDATE_TRIGGER);
			stmt.executeUpdate (PROJECT_FLAG_TABLE);
			stmt.close ();
		} finally {
			pushConnection (conn);
		}
	}
}
