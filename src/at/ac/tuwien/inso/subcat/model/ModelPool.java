package at.ac.tuwien.inso.subcat.model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;

import org.sqlite.SQLiteConfig;

import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;


public class ModelPool {

	private LinkedList<Connection> connections = new LinkedList<Connection> ();
	private int connPoolSize;

	private LinkedList<ModelModificationListener> listeners = new LinkedList<ModelModificationListener> ();
	private String name;

	private boolean printTemplates = false;
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
		Model model = new Model (this, conn, extensions);
		model.setPrintTemplates (printTemplates);
		return model;
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
		
		// TODO: Wait until we can set SQLITE_CONFIG_MULTITHREAD
		// 		 ...

		Connection conn = DriverManager.getConnection ("jdbc:sqlite:" + name, config.toProperties());
		
		Statement stmt = conn.createStatement();
		//stmt.executeUpdate ("PRAGMA journal_mode=MEMORY");
		stmt.executeUpdate ("PRAGMA temp_store=OFF");
		stmt.executeUpdate ("PRAGMA synchronous=OFF");
		stmt.executeUpdate ("PRAGMA count_changes=OFF");
		stmt.executeUpdate ("PRAGMA journal_mode=WAL");
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

	public void emitAttachmentUpdated (Attachment attachment) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentUpdated (attachment);
		}
	}

	synchronized void emitAttachmentStatusHistoryAdded (AttachmentStatusHistory history) {	
		for (ModelModificationListener listener : listeners) {
			listener.attachmentStatusHistoryAdded (history);
		}
	}

	synchronized void emitBugAdded (Bug bug) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAdded (bug);
		}
	}

	synchronized void emitBugUpdated (Bug bug) {
		for (ModelModificationListener listener : listeners) {
			listener.bugUpdated (bug);
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

	synchronized void emitProjectUpdated (Project project) {
		for (ModelModificationListener listener : listeners) {
			listener.projectUpdated (project);
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

	public synchronized void emitSentimentAdded (Sentiment sentiment) {

		for (ModelModificationListener listener : listeners) {
			listener.sentimentAdded (sentiment);
		}
	}

	public void setPrintTemplates (boolean printTemplates) {
		this.printTemplates = printTemplates;
	}

	public boolean getPrintTemplates () {
		return this.printTemplates;
	}

	synchronized void emitAttachmentIsObsoleteAdded (Attachment attachment, Identity identity, Date date, boolean oldValue, boolean newValue) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentIsObsoleteAdded (attachment, identity, date, oldValue, newValue);
		}
	}

	synchronized void emitBugCcHistoryAdded (Bug bug, Date date, Identity addedBy, Identity cc, String ccMail, boolean removed) {
		for (ModelModificationListener listener : listeners) {
			listener.bugCcHistoryAdded (bug, date, addedBy, cc, ccMail, removed);
		}
	}

	synchronized void emitBugBlocksAdded (Bug bug, Date date, Identity addedBy,
			boolean removed) {
		for (ModelModificationListener listener : listeners) {
			listener.bugBlocksAdded (bug, date, addedBy, removed);
		}
	}

	synchronized void emitBugAliasAdded (Bug bug, Identity addedBy, Date date, String alias) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAliasAdded (bug, addedBy, date, alias);
		}
	}

	synchronized void emitSeverityHistoryAdded (Bug bug, Identity addedBy, Date date, Severity oldSeverity, Severity newSeverity) {
		for (ModelModificationListener listener : listeners) {
			listener.severityHistoryAdded (bug, addedBy, date, oldSeverity, newSeverity);
		}
	}

	synchronized void emitPriorityHistoryAdded (Bug bug, Identity addedBy, Date date,
			Priority oldPriority, Priority newPriority) {
		for (ModelModificationListener listener : listeners) {
			listener.priorityHistoryAdded (bug, addedBy, date, oldPriority, newPriority);
		}
	}

	synchronized void emitStatusHistoryAdded (Bug bug, Identity addedBy, Date date,
			Status oldStatus, Status newStatus) {
		for (ModelModificationListener listener : listeners) {
			listener.statusHistoryAdded (bug, addedBy, date, oldStatus, newStatus);
		}
	}

	synchronized void emitResolutionAdded (Resolution resolution) {
		for (ModelModificationListener listener : listeners) {
			listener.resolutionAdded (resolution);
		}
	}

	synchronized void emitResolutionHistoryAdded (Bug bug, Identity addedBy,
			Date date, Resolution resolution) {
		for (ModelModificationListener listener : listeners) {
			listener.resolutionHistoryAdded (bug, addedBy, date, resolution);
		}
	}

	synchronized void emitConfirmedHistoryAdded (Bug bug, Identity addedBy,
			Date date, boolean removed) {
		for (ModelModificationListener listener : listeners) {
			listener.confiremdHistoryAdded (bug, addedBy, date, removed);
		}
	}

	synchronized void emitVersionAdded (Version version) {
		for (ModelModificationListener listener : listeners) {
			listener.versionAdded (version);
		}
	}

	synchronized void emitVersionHistoryAdded (Bug bug, Identity addedBy, Date date,
			Version oldVersion, Version newVersion) {
		for (ModelModificationListener listener : listeners) {
			listener.versionHistoryAdded (bug, addedBy, date, oldVersion, newVersion);
		}
	}

	synchronized void emitOperatingSystemAdded (OperatingSystem os) {
		for (ModelModificationListener listener : listeners) {
			listener.operatingSystemAdded (os);
		}
	}

	synchronized void emitOperatingSystemHistoryAdded (Bug bug, Identity addedBy,
			Date date, OperatingSystem oldOs, OperatingSystem newOs) {
		for (ModelModificationListener listener : listeners) {
			listener.operatingSystemHistoryAdded (bug, addedBy, date, oldOs, newOs);
		}
	}

	synchronized void emitAttachmentHistoryAdded (Attachment attachment,
			Identity addedBy, Date date, String fieldName, String oldValue,
			String newValue) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentHistoryAdded (attachment, addedBy, date, fieldName, oldValue, newValue);
		}
	}

	synchronized void emitKeywordAdded (Keyword keyword) {
		for (ModelModificationListener listener : listeners) {
			listener.keywordAdded (keyword);
		}
	}

	synchronized void emitKeywordHistoryAdded (Bug bug, Identity addedBy, Date date,
			Keyword keyword, boolean removed) {
		for (ModelModificationListener listener : listeners) {
			listener.keywordHistoryAdded (bug, addedBy, date, keyword, removed);
		}
	}

	synchronized void emitMilestoneAdded (Milestone ms) {
		for (ModelModificationListener listener : listeners) {
			listener.milestoneAdded (ms);
		}
	}

	synchronized void emitMilestoneHistoryAdded (Bug bug, Identity addedBy,
			Date date, Milestone oldMilestone, Milestone newMilestone) {
		for (ModelModificationListener listener : listeners) {
			listener.milestoneAdded (bug, addedBy, date, oldMilestone, newMilestone);
		}
	}

	synchronized void emitBugGroupAdded (BugGroup grp) {
		for (ModelModificationListener listener : listeners) {
			listener.bugGroupAdded (grp);
		}
	}

	synchronized void emitAssignedToHistoryAdded (Bug bug, Identity addedBy, Date date,
			String identifierAdded, BugGroup groupAdded, Identity identityAdded,
			String identifierRemoved, BugGroup groupRemoved, Identity identityRemoved) {
		for (ModelModificationListener listener : listeners) {
			listener.assignedToAdded (bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityRemoved);
		}
	}

	synchronized void emitQaContactHistoryAdded (Bug bug, Identity addedBy,
			Date date, String identifierAdded, BugGroup groupAdded,
			Identity identityAdded, String identifierRemoved,
			BugGroup groupRemoved, Identity identityRemoved) {
		for (ModelModificationListener listener : listeners) {
			listener.qaContactAdded (bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityRemoved);
		}
	}

	synchronized void emitPlatformAdded (Platform pf) {
		for (ModelModificationListener listener : listeners) {
			listener.platformAdded (pf);
		}
	}

	synchronized void emitBugDeadlineAdded (Bug bug, Date deadline) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDeadlineAdded (bug, deadline);
		}
	}

	synchronized void emitBugDeadlineUpdated (Bug bug, Date deadline) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDeadlineUpdated (bug, deadline);
		}
	}

	synchronized void emitBugDuplicationUpdated (Bug bug, Integer duplication) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDuplicationUpdated (bug, duplication);
		}
	}

	synchronized void emitBugDuplicationAdded (Bug bug, Integer duplication) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDuplicationAdded (bug, duplication);
		}
	}

	synchronized void emitBugQaContactAdded (Bug bug, Identity identity, BugGroup group) {
		for (ModelModificationListener listener : listeners) {
			listener.bugQaContactAdded (bug, identity, group);
		}
	}

	synchronized void emitBugQaContactUpdated (Bug bug, Identity identity, BugGroup group) {
		for (ModelModificationListener listener : listeners) {
			listener.bugQaContactUpdated (bug, identity, group);
		}
	}

	synchronized void emitBugBlocksAdded (Bug bug, Integer[] blocks) {
		for (ModelModificationListener listener : listeners) {
			listener.bugBlocksAdded (bug, blocks);
		}
	}

	synchronized void emitBugBlocksUpdated (Bug bug, Integer[] blocks) {
		for (ModelModificationListener listener : listeners) {
			listener.bugBlocksUpdated (bug, blocks);
		}
	}

	synchronized void emitBugDependsOnAdded (Bug bug, Integer[] dependsOn) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDependsOnAdded (bug, dependsOn);
		}
	}

	synchronized void emitBugDependsOnUpdated (Bug bug, Integer[] dependsOn) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDependsOnUpdated (bug, dependsOn);
		}
	}

	synchronized void emitBugKeywordsAdded (Bug bug, Keyword[] keywords) {
		for (ModelModificationListener listener : listeners) {
			listener.bugKeywordsAdded (bug, keywords);
		}
	}

	synchronized void emitBugKeywordsUpdated (Bug bug, Keyword[] keywords) {
		for (ModelModificationListener listener : listeners) {
			listener.bugKeywordsUpdated (bug, keywords);
		}
	}

	synchronized void emitBugCcUpdated (Bug bug, Identity[] identities) {
		for (ModelModificationListener listener : listeners) {
			listener.bugCcUpdated (bug, identities);
		}
	}

	synchronized void emitBugCcAdded (Bug bug, Identity[] identities) {
		for (ModelModificationListener listener : listeners) {
			listener.bugCcAdded (bug, identities);
		}
	}

	synchronized void emitBugGroupMembershipsAdded (Bug bug, BugGroup[] groups) {
		for (ModelModificationListener listener : listeners) {
			listener.bugGroupMembershipsAdded (bug, groups);
		}
	}

	synchronized void emitBugGroupMembershipsUpdated (Bug bug, BugGroup[] groups) {
		for (ModelModificationListener listener : listeners) {
			listener.bugGroupMembershipsUpdated (bug, groups);
		}
	}

	synchronized void emitBugFlagStatusAdded (BugFlagStatus status) {
		for (ModelModificationListener listener : listeners) {
			listener.bugFlagStatusAdded (status);
		}
	}

	synchronized void emitBugFlagAdded (BugFlag flag) {
		for (ModelModificationListener listener : listeners) {
			listener.bugFlagAdded (flag);
		}
	}

	synchronized void emitBugFlagAssignmentsAdded (Bug bug, BugFlagAssignment[] flags) {
		for (ModelModificationListener listener : listeners) {
			listener.bugFlagAssignmentsAdded (bug, flags);
		}
	}

	synchronized void emitBugFlagAssignmentsUpdated (Bug bug,BugFlagAssignment[] flags) {
		for (ModelModificationListener listener : listeners) {
			listener.bugFlagAssignmentsUpdated (bug, flags);
		}
	}

	synchronized void emitBugSeeAlsoAdded (Bug bug, String[] links) {
		for (ModelModificationListener listener : listeners) {
			listener.bugSeeAlsoAdded (bug, links);
		}
	}

	synchronized void emitBugSeeAlsoUpdated (Bug bug, String[] links) {
		for (ModelModificationListener listener : listeners) {
			listener.bugSeeAlsoUpdated (bug, links);
		}
	}

	synchronized void emitBugClassAdded (BugClass bc) {
		for (ModelModificationListener listener : listeners) {
			listener.bugClassAdded (bc);
		}
	}

	synchronized void emitBugDuplicationCommentAdded (Comment comment, Integer identifier) {
		for (ModelModificationListener listener : listeners) {
			listener.bugDuplicationCommentAdded (comment, identifier);
		}
	}

	synchronized void emitBugAttachmentReviewCommentAdded (Comment comment,
			Attachment attachment) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAttachmentReviewCommentAdded (comment, attachment);
		}
	}

	synchronized void emitAttachmentDetailsAdded (AttachmentDetails ad) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentDetailsAdded (ad);
		}
	}

	synchronized void emitAttachmentDetailsUpdated (AttachmentDetails ad) {
		for (ModelModificationListener listener : listeners) {
			listener.attachmentDetailsUpdated (ad);
		}
	}

	synchronized void emitBugAttachmentFlagAssignmentsAdded (Attachment attachment,
			BugFlagAssignment[] flags) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAttachmentFlagAssignmentsAdded (attachment, flags);
		}
	}

	synchronized void emitBugAttachmentFlagAssignmentsUpdated (Attachment attachment,
			BugFlagAssignment[] flags) {
		for (ModelModificationListener listener : listeners) {
			listener.bugAttachmentFlagAssignmentsUpdated (attachment, flags);
		}
	}

	public void emitBugCommentSentimentAdded (Comment comment,
			Sentiment sentiment) {
		for (ModelModificationListener listener : listeners) {
			listener.bugCommentSentimentAdded (comment, sentiment);
		}
	}

	public void emitSocialStatsAdded (Identity src, Identity dest,
			int quotations, int patchesReviewed, int bugInteractions, int fileInteractions) {
		for (ModelModificationListener listener : listeners) {
			listener.socialStatsAdded (src, dest, quotations, patchesReviewed, bugInteractions, fileInteractions);
		}
	}

	public void emitCommitSentimentAdded (Commit commit, Sentiment sentiment) {
		for (ModelModificationListener listener : listeners) {
			listener.commitSentimentAdded (commit, sentiment);
		}
	}
}