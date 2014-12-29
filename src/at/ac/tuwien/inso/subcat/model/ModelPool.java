package at.ac.tuwien.inso.subcat.model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.sqlite.SQLiteConfig;

import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;


public class ModelPool {

	//
	// DB-Settings:
	//

	private static final String ENABLE_WAL =
		"PRAGMA journal_mode=WAL";
	private static final String SYNCHRONOUS_DB =
		"PRAGMA synchronous=NORMAL";


    //
    //
    //

	private LinkedList<Connection> connections = new LinkedList<Connection> ();
	private int connPoolSize;

	private LinkedList<ModelModificationListener> listeners = new LinkedList<ModelModificationListener> ();
	private String name;

	private String[] extensions;


	//
	// Public API:
	//

	public ModelPool (String name, String[] extensions) throws ClassNotFoundException, SQLException {
		this (name, 1, extensions);
	}

	public ModelPool (String name) throws ClassNotFoundException, SQLException {
		this (name, new String[0]);
	}

	public ModelPool (String name, int connPoolSize) throws ClassNotFoundException, SQLException {
		this (name, connPoolSize, new String[0]);
	}

	public ModelPool (String name, int connPoolSize, String[] extensions) throws ClassNotFoundException, SQLException {
		assert (connPoolSize >= 1);
		assert (name != null);
		assert (extensions != null);

		Class.forName ("org.sqlite.JDBC");
		this.name = name;
		this.extensions = extensions;

		this.connPoolSize = 0;
		setConnectionPoolSize (connPoolSize);
	}

	public synchronized Model getModel () throws SQLException {
		Connection conn = popConnection ();
		return new Model (this, conn, extensions);
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

	public synchronized boolean close () {
		try {
			for (Connection conn : connections) {
					conn.close ();
			}
	
			connections = null;
			connPoolSize = -1;
			name = null;
			return true;
		} catch (SQLException e) {
			return false;
		}
	}


	//
	// Helper:
	//

	private synchronized Connection popConnection () throws SQLException {
		assert (connections != null);
		
		Connection conn = connections.poll ();
		if (connections.size () == 0) {
			conn = createConnection ();
		}
		
		return conn;
	}

	synchronized void pushConnection (Connection conn) throws SQLException {
		assert (conn != null);

		if (connections == null || connections.size () == connPoolSize) {
			conn.close ();
		} else {
			connections.add (conn);
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

	public synchronized boolean remove() throws SQLException {
		assert (name != null);

		close ();
		File file = new File (name + ".db");
		boolean res = file.delete ();
		
		return res;
	}

	private Connection createConnection () throws SQLException {
		// TODO: Escape path
		SQLiteConfig config = new SQLiteConfig ();
		config.enableLoadExtension (true);

		Connection conn = DriverManager.getConnection ("jdbc:sqlite:" + name, config.toProperties());
		
		Statement stmt = conn.createStatement();
		//stmt.executeUpdate ("PRAGMA journal_mode=MEMORY");
		stmt.executeUpdate ("PRAGMA temp_store=OFF");
		stmt.executeUpdate (SYNCHRONOUS_DB);
		stmt.executeUpdate ("PRAGMA synchronous=OFF");
		stmt.executeUpdate ("PRAGMA count_changes=OFF");
		stmt.executeUpdate (ENABLE_WAL);
		stmt.close ();
		
		return conn;
	}


	//
	// Emitter:
	//

	synchronized void emitFileChangeAdded (FileChange change) {
		for (ModelModificationListener listener : listeners) {
			listener.fileChangeAdded (change);
		}
	}

	synchronized void emitCategoryAdded (Category category) {
		for (ModelModificationListener listener : listeners) {
			listener.categoryAdded (category);
		}
	}

	synchronized void emitComponentAdded (Component component) {
		for (ModelModificationListener listener : listeners) {
			listener.componentAdded (component);
		}
	}

	synchronized void emitAttachmentAdded (Attachment attachment) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentAdded (attachment);
		}
	}

	synchronized void emitAttachmentHistoryAdded (AttachmentHistory history) {	
		for (ModelModificationListener listener : listeners) {
			listener.attachmentHistoryAdded (history);
		}
	}

	synchronized void emitBugAdded (Bug bug) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAdded (bug);
		}
	}

	synchronized void emitBugHistoryAdded (BugHistory history) {
		for (ModelModificationListener listener : listeners) {
			listener.bugHistoryAdded (history);
		}
	}

	synchronized void emitUserAdded (User user) {
		for (ModelModificationListener listener : listeners) {
			listener.userAdded (user);
		}
	}

	synchronized void emitBugfixCommitAdded (BugfixCommit bugfix) {
		for (ModelModificationListener listener : listeners) {
			listener.bugfixCommitAdded (bugfix);
		}
	}

	synchronized void emitFileRenameAdded (FileRename rename) {
		for (ModelModificationListener listener : listeners) {
			listener.fileRenameAdded (rename);
		}
	}

	synchronized void emitFileDeletionAdded (FileDeletion deletion) {
		for (ModelModificationListener listener : listeners) {
			listener.fileDeletedAdded (deletion);
		}
	}
	
	synchronized void emitManagedFileAdded (ManagedFile file) {
		for (ModelModificationListener listener : listeners) {
			listener.managedFileAdded (file);
		}
	}

	synchronized void emitManagedFileCopyAdded (ManagedFileCopy copy) {
		for (ModelModificationListener listener : listeners) {
			listener.managedFileCopyAdded (copy);
		}
	}

	synchronized void emitCommitAdded (Commit commit) {
		for (ModelModificationListener listener : listeners) {
			listener.commitAdded (commit);
		}
	}

	synchronized void emitStatusAdded (Status status) {	
		for (ModelModificationListener listener : listeners) {
			listener.statusAdded (status);
		}
	}

	synchronized void emitAttachmentStatusAdded (AttachmentStatus status) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentStatusAdded (status);
		}
	}

	synchronized void emitCommentAdded (Comment cmnt) {
		for (ModelModificationListener listener : listeners) {
			listener.commentAdded (cmnt);
		}
	}

	synchronized void emitPriorityAdded (Priority priority) {
		for (ModelModificationListener listener : listeners) {
			listener.priorityAdded (priority);
		}
	}

	synchronized void emitSeverityAdded (Severity severity) {	
		for (ModelModificationListener listener : listeners) {
			listener.severityAdded (severity);
		}
	}

	synchronized void emitInteraction (Interaction relation) {
		for (ModelModificationListener listener : listeners) {
			listener.interactionAdded (relation);
		}
	}
	
	synchronized void emitIdentityAdded (Identity identity) {
		for (ModelModificationListener listener : listeners) {
			listener.identityAdded (identity);
		}
	}
	
	synchronized void emitProjectAdded (Project project) {
		for (ModelModificationListener listener : listeners) {
			listener.projectAdded (project);
		}
	}

	synchronized void emitBugCategoryAdded (Bug bug, Category category) {
		for (ModelModificationListener listener : listeners) {
			listener.bugCategoryAdded (bug, category);
		}
	}

	synchronized void emitCommitCategoryAdded (Commit commit, Category category) {
		for (ModelModificationListener listener : listeners) {
			listener.commitCategoryAdded (commit, category);
		}
	}

	public synchronized void emitDictionaryAdded (Dictionary dict) {
		for (ModelModificationListener listener : listeners) {
			listener.commitDictionaryAdded (dict);
		}
	}

	public synchronized void emitAttachmentReplacementAdded (Attachment oldAtt,
			Attachment newAtt) {

		for (ModelModificationListener listener : listeners) {
			listener.attachmentReplacementAdded (oldAtt, newAtt);
		}
	}

	public synchronized void emitSentimentAdded (Comment comment,
			Sentiment<Identity> sentiment) {

		for (ModelModificationListener listener : listeners) {
			listener.sentimentAdded (comment, sentiment);
		}
	}	
}


