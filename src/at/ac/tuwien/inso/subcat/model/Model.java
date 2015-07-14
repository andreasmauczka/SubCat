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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
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
import at.ac.tuwien.inso.subcat.utility.sentiment.SentenceSentiment;
import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;
import at.ac.tuwien.inso.subcat.utility.sentiment.SentimentBlock;


public class Model {
	public static final String FLAG_SRC_FILE_STATS = "SRC_FILE_STATS";
	public static final String FLAG_SRC_LINE_STATS = "SRC_LINE_STATS";
	public static final String FLAG_BUG_COMMENTS = "BUG_COMMENTS";
	public static final String FLAG_BUG_HISTORY = "BUG_HISTORY";
	public static final String FLAG_BUG_ATTACHMENTS = "BUG_ATTACHMENTS";
	public static final String FLAG_BUG_INFO = "BUG_INFO";
	public static final String FLAG_SRC_INFO = "SRC_INFO";
	public static final String FLAG_COMMIT_CATEGORIES = "COMMIT_CATEGORIES";
	public static final String FLAG_BUG_CATEGORIES = "BUG_CATEGORIES";

	public static final String CONTEXT_SRC = "src";
	public static final String CONTEXT_BUG = "bug";

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
	//
	// Table Creation:
	//


	// Note: Dates are not supported by Sqlite

	private static final String PROJECT_TABLE =
		"CREATE TABLE IF NOT EXISTS Projects ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "lastBugDate	TEXT								        ,"
		+ "bugTracker   TEXT										,"
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

	private static final String SELECTED_USER_TABLE =
		"CREATE TEMP TABLE IF NOT EXISTS SelectedUsers ("
		+ "id			INTEGER	PRIMARY KEY					NOT NULL,"
		+ "FOREIGN KEY(id) REFERENCES Users (id)"
		+ ")";
	
	private static final String IDENTITY_TABLE =
		"CREATE TABLE IF NOT EXISTS Identities ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "identifier	INTEGER								        ,"
		+ "context		VARCHAR(3)							NOT NULL,"
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

	private static final String VERSION_TABLE =
		"CREATE TABLE IF NOT EXISTS Versions ("
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

	// TODO: rename to Severities
	private static final String SEVERITY_TABLE =
		"CREATE TABLE IF NOT EXISTS Severity ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String RESOLUTION_TABLE =
		"CREATE TABLE IF NOT EXISTS Resolutions ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String OPERATING_SYSTEM_TABLE =
		"CREATE TABLE IF NOT EXISTS OperatingSystems ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	// TODO: Add isObsolete
	private static final String BUG_TABLE =
		"CREATE TABLE IF NOT EXISTS Bugs ("
		+ "id				INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "identifier		INTEGER								NOT NULL,"
		+ "identity			INT											,"
		+ "component		INT									NOT NULL,"
		+ "title			TEXT								NOT NULL,"
		+ "creation			TEXT								NOT NULL,"
		+ "lastChange		TEXT								NOT NULL,"
		+ "priority			INT									NOT NULL,"
		+ "severity			INT									NOT NULL,"
		+ "resolution		INT									NOT NULL,"
		+ "operatingSystem	INT									NOT NULL,"
		+ "version			INT									NOT NULL,"
		+ "comments			INT									NOT NULL DEFAULT 0,"
		+ "curStat			INT									NOT NULL,"
		+ "FOREIGN KEY(priority) REFERENCES Priorities (id),"
		+ "FOREIGN KEY(severity) REFERENCES Severity (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(component) REFERENCES Components (id),"
		+ "FOREIGN KEY(resolution) REFERENCES Resolutions (id),"
		+ "FOREIGN KEY(version) REFERENCES Versions (id),"
		+ "FOREIGN KEY(operatingSystem) REFERENCES OperatingSystems (id),"
		+ "FOREIGN KEY(curStat) REFERENCES Status (id)"
		+ ")";

	private static final String VERSION_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS VersionHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "version		INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(version) REFERENCES Versions (id)"
		+ ")";

	private static final String RESOLUTION_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS ResolutionHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "resolution	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(resolution) REFERENCES Resolutions (id)"
		+ ")";

	private static final String OPERATING_SYSTEM_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS OperatingSystemHistory ("
		+ "id					INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug					INT									NOT NULL,"
		+ "addedBy				INT									NOT NULL,"
		+ "date					TEXT								NOT NULL,"
		+ "oldOperatingSystem	INT									NOT NULL,"
		+ "newOperatingSystem	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldOperatingSystem) REFERENCES OperatingSystems (id)"
		+ "FOREIGN KEY(newOperatingSystem) REFERENCES OperatingSystems (id)"
		+ ")";

	private static final String PRIORITY_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS PriorityHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "priority		INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(priority) REFERENCES Priorities (id)"
		+ ")";

	private static final String SEVERITY_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS SeverityHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "severity		INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(severity) REFERENCES Severity (id)"
		+ ")";

	private static final String STATUS_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS StatusHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "status		INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(status) REFERENCES Status (id)"
		+ ")";

	private static final String CONFIRMED_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS ConfirmedHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "removed		BOOLEAN								NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id)"
		+ ")";

	private static final String BUG_ALIASES =
		"CREATE TABLE IF NOT EXISTS BugAliases ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "alias		TEXT								NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id)"
		+ ")";

	private static final String BUG_CATEGORIES_TABLE =
		"CREATE TABLE IF NOT EXISTS BugCategories ("
		+ "bug			INT									NOT NULL,"
		+ "category		INT									NOT NULL,"
		+ "FOREIGN KEY(category) REFERENCES Categories (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";
	
	private static final String ATTACHMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Attachments ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "identifier	TEXT								NOT NULL,"
		+ "comment		INTEGER								NOT NULL,"
		+ "FOREIGN KEY(comment) REFERENCES Comments (id)"
		+ ")";

	private static final String ATTACHMENT_ISOBSOLETE_TABLE =
		"CREATE TABLE IF NOT EXISTS ObsoleteAttachments ("
		+ "id			INTEGER PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "attachment	INTEGER								NOT NULL,"
		+ "identity		INTEGER								NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "isObsolete	INTEGER								NOT NULL,"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id)"
		+ ")";

	private static final String ATTACHMENT_STATUS_TABLE =
		"CREATE TABLE IF NOT EXISTS AttachmentStatus ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String ATTACHMENT_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS AttachmentHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "attachment	INT									NOT NULL,"
		+ "status		INT									NOT NULL,"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(status) REFERENCES AttachmentStatus (id)"
		+ ")";

	private static final String COMMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Comments ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "pos          INTEGER                             NOT NULL,"
		+ "creation		TEXT								NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "content		TEXT								NOT NULL,"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "UNIQUE (bug, pos)"
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

	private static final String CC_TABLE =
		"CREATE TABLE IF NOT EXISTS BugCc ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "cc			INT									        ,"
		+ "ccMail		String								NOT NULL,"
		+ "removed		BOOLEAN								NOT NULL,"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(cc) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";

	private static final String BUG_BLOCKS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugBlocks ("
		+ "id				INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "date				TEXT								NOT NULL,"
		+ "bug				INT									NOT NULL,"
		+ "blocks			INT									        ,"
		+ "blocksIdentifier	INT									NOT NULL,"
		+ "addedBy			INT									NOT NULL,"
		+ "removed			BOOLEAN								NOT NULL,"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(blocks) REFERENCES Bugs (id)"
		+ ")";

	private static final String CATEGORY_TABLE =
		"CREATE TABLE IF NOT EXISTS Categories ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "dictionary	INT									NOT NULL,"		
		+ "FOREIGN KEY(dictionary) REFERENCES Dictionary (id)"
		+ ")";

	// TODO: Rename t Dictionaries
	private static final String DICTIONARY_TABLE =
		"CREATE TABLE IF NOT EXISTS Dictionary ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "context		VARCHAR(3)							NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (id, name)"
		+ ")";

	private static final String COMMIT_TABLE =
		"CREATE TABLE IF NOT EXISTS Commits ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "identifier	TEXT								NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "author		INT									NOT NULL,"
		+ "committer	INT									NOT NULL,"	// TODO: Rename to Pusher
		+ "date			TEXT								NOT NULL,"
		+ "title		TEXT								NOT NULL,"
		+ "changedFiles INT									NOT NULL,"
		+ "linesAdded	INT									NOT NULL,"
		+ "linesRemoved	INT									NOT NULL,"
		+ "UNIQUE (identifier, project),"
		+ "FOREIGN KEY(committer) REFERENCES Identities (id),"
		+ "FOREIGN KEY(author) REFERENCES Identities (id),"
		+ "FOREIGN KEY(project) REFERENCES Projects (id)"
		+ ")";

	private static final String COMMIT_CATEGORIES_TABLE =
		"CREATE TABLE IF NOT EXISTS CommitCategories ("
		+ "commitId		INT									NOT NULL,"
		+ "category		INT									NOT NULL,"
		+ "FOREIGN KEY(category) REFERENCES Categories (id),"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id),"
		+ "PRIMARY KEY (commitId, category)"
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
	
	private static final String BUGFIX_COMMIT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugfixCommit ("
		+ "bug			INTEGER								NOT NULL,"
		+ "commitId		INTEGER								NOT NULL,"
		+ "FOREIGN KEY(commitId) REFERENCES Commits (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "PRIMARY KEY(bug, commitId)"
		+ ")";

	private static final String SENTENCE_SENTIMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS SentenceSentiment ("
		+ "groupId				INTEGER		NOT NULL,"
		+ "pos					INTEGER		NOT NULL,"
		+ "negative				DOUBLE		NOT NULL,"
		+ "somewhatNegative		DOUBLE		NOT NULL,"
		+ "neutral				DOUBLE		NOT NULL,"
		+ "somewhatPositive		DOUBLE		NOT NULL,"
		+ "positive				DOUBLE		NOT NULL,"
		+ "wordCount			INTEGER		NOT NULL,"
		+ "PRIMARY KEY(groupId, pos),"
		+ "FOREIGN KEY(groupId) REFERENCES BlockSentiment (id)"
		+ ")";

	private static final String BLOCK_SENTIMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BlockSentiment ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"

		+ "sentimentId				INTEGER		NOT NULL,"
		+ "pos						INTEGER		NOT NULL,"

		+ "target					INTEGER,"
		
		+ "negative					INTEGER		NOT NULL,"
		+ "somewhatNegative			INTEGER		NOT NULL,"
		+ "neutral					INTEGER		NOT NULL,"
		+ "somewhatPositive			INTEGER		NOT NULL,"
		+ "positive					INTEGER		NOT NULL,"

		+ "negativeMean				DOUBLE		NOT NULL,"
		+ "somewhatNegativeMean		DOUBLE		NOT NULL,"
		+ "neutralMean				DOUBLE		NOT NULL,"
		+ "somewhatPositiveMean		DOUBLE		NOT NULL,"
		+ "positiveMean				DOUBLE		NOT NULL,"

		+ "negativeWMean			DOUBLE		NOT NULL,"
		+ "somewhatNegativeWMean	DOUBLE		NOT NULL,"
		+ "neutralWMean				DOUBLE		NOT NULL,"
		+ "somewhatPositiveWMean	DOUBLE		NOT NULL,"
		+ "positiveWMean			DOUBLE		NOT NULL,"
		
		+ "wordCount				INTEGER		NOT NULL,"
		+ "sentenceCount			INTEGER		NOT NULL,"

		//+ "PRIMARY KEY(sentimentId, pos),"
		+ "FOREIGN KEY(sentimentId) REFERENCES Sentiment (id),"
		+ "FOREIGN KEY(target) REFERENCES Identities (id)"
		+ ")";

	private static final String SENTIMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Sentiment ("
		+ "commentId				PRIMARY KEY	NOT NULL,"

		+ "negative					INTEGER		NOT NULL,"
		+ "somewhatNegative			INTEGER		NOT NULL,"
		+ "neutral					INTEGER		NOT NULL,"
		+ "somewhatPositive			INTEGER		NOT NULL,"
		+ "positive					INTEGER		NOT NULL,"

		+ "negativeMean				DOUBLE		NOT NULL,"
		+ "somewhatNegativeMean		DOUBLE		NOT NULL,"
		+ "neutralMean				DOUBLE		NOT NULL,"
		+ "somewhatPositiveMean		DOUBLE		NOT NULL,"
		+ "positiveMean				DOUBLE		NOT NULL,"

		+ "negativeWMean			DOUBLE		NOT NULL,"
		+ "somewhatNegativeWMean	DOUBLE		NOT NULL,"
		+ "neutralWMean				DOUBLE		NOT NULL,"
		+ "somewhatPositiveWMean	DOUBLE		NOT NULL,"
		+ "positiveWMean			DOUBLE		NOT NULL,"

		+ "wordCount				INTEGER		NOT NULL,"
		+ "sentences				INTEGER		NOT NULL,"
		+ "FOREIGN KEY(commentId) REFERENCES Comment (id)"
		+ ")";
	
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

	private static final String SELECT_BUG_STATS =	
		"SELECT "
		+ " Bugs.id,"
		+ " (SELECT COUNT() FROM Comments WHERE Comments.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugHistories WHERE BugHistories.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM Attachments, Comments WHERE Attachments.comment = Comments.id AND Comments.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugCc WHERE BugCc.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugBlocks WHERE BugBlocks.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugAliases WHERE BugAliases.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM SeverityHistory WHERE SeverityHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM PriorityHistory WHERE PriorityHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM StatusHistory WHERE StatusHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM ResolutionHistory WHERE ResolutionHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM ConfirmedHistory WHERE ConfirmedHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM VersionHistory WHERE VersionHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM OperatingSystemHistory WHERE OperatingSystemHistory.bug = Bugs.id)"
		+ "FROM "
		+ " Bugs, Components "
		+ "WHERE "
		+ " Components.id = Bugs.component "
		+ " AND Bugs.identifier = ? "
		+ " AND Components.project = ? ";

	private static final String SELECT_ATTACHMENT_STATS =
		"SELECT"
		+ " Attachments.id,"
		+ " Attachments.identifier,"
		+ " (SELECT COUNT() FROM ObsoleteAttachments WHERE ObsoleteAttachments.attachment = Attachments.id),"
		+ " (SELECT COUNT() FROM AttachmentHistory WHERE AttachmentHistory.attachment = Attachments.id) "
		+ "FROM "
		+ " Bugs, Attachments, Comments "
		+ "WHERE "
		+ " Attachments.comment = Comments.id"
		+ " AND Comments.bug = Bugs.id"
		+ " AND Bugs.id = ?"
		+ "GROUP BY "
		+ " Attachments.id "
		+ "ORDER BY "
		+ " Attachments.id";
	
	private static final String SELECT_START_DATE =
		"SELECT"
		+ " min(date) "
		+ "FROM "
		+ " Commits "
		+ "WHERE "
		+ " project = ?";

	
	private static final String SELECT_ALL_COMMITS =
		"SELECT"
		+ " Commits.id					AS cId,"
		+ " Commits.date				AS cDate,"
		+ " Commits.title				AS cTitle,"
		+ " Commits.linesAdded			AS cLinesAdded,"
		+ " Commits.linesRemoved		AS cLinesRemoved,"
		+ " AuthorIdentity.id			AS aiId,"
		+ " AuthorIdentity.name			AS aiName,"
		+ " AuthorIdentity.mail			AS aiMail,"
		+ " AuthorUser.id				AS auId,"
		+ " AuthorUser.name				AS auName,"
		+ " CommitterIdentity.id		AS ciId,"
		+ " CommitterIdentity.name		AS ciName,"
		+ " CommitterIdentity.mail		AS ciMail,"
		+ " CommitterUser.id			AS cuId,"
		+ " CommitterUser.name			AS cuName,"
		+ " Commits.changedFiles, "
		+ " AuthorIdentity.context		AS aiId,"
		+ " CommitterIdentity.context	AS ciId,"
		+ " Commits.identifier, "
		+ " AuthorIdentity.identifier, "
		+ " CommitterIdentity.identifier "
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
		+ " Bugs.identifier,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiMail,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Bugs.component,"
		+ " Bugs.title,"
		+ " Bugs.creation,"
		+ " Bugs.priority,"
		+ " Bugs.severity,"
		+ " Bugs.comments,"
		+ " Bugs.curStat, "
		+ " Identity.context		AS aiContext, "
		+ " Identity.identifier,"
		+ " Bugs.resolution,"
		+ " Bugs.lastChange,"
		+ " Bugs.version,"
		+ " Bugs.operatingSystem "
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


	private static final String SELECT_BUG = 
		"SELECT"
		+ " Bugs.id,"
		+ " Bugs.identifier,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiMail,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Bugs.component,"
		+ " Bugs.title,"
		+ " Bugs.creation,"
		+ " Bugs.priority,"
		+ " Bugs.severity,"
		+ " Identity.context		AS aiContext,"
		+ " Components.name,"
		+ " Priorities.name			AS pName,"
		+ " Severity.name			AS sName, "
		+ " Identity.identifier,"
		+ " Resolutions.id, "
		+ " Resolutions.name,"
		+ " Bugs.lastChange,"
		+ " Versions.version,"
		+ " Versions.name,"
		+ " OperatingSystem.version,"
		+ " OperatingSystem.name "
		+ "FROM"
		+ " Bugs "
		+ "LEFT JOIN Identities Identity"
		+ " ON Bugs.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "JOIN Components"
		+ " ON Components.id = Bugs.component "
		+ "JOIN Priorities"
		+ " ON Priorities.id = Bugs.priority "
		+ "JOIN Severity"
		+ " ON Severity.id = Bugs.severity "
		+ "JOIN Versions"
		+ " ON Versions.id = Bugs.version "
		+ "JOIN Resolutions"
		+ " ON Resolutions.id = Bugs.resolution "
		+ "JOIN OperatingSystem"
		+ " ON OperatingSystem.id = Bugs.operatingSystem "
		+ "WHERE"
		+ " Components.project = ?"
		+ " AND Bugs.identifier = ?";
	
	private static final String SELECT_ALL_CATEGORIES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Categories "
		+ "WHERE"
		+ " dictionary = ?";

	private static final String SELECT_ALL_SEVERITIES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Severity "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_OPERATING_SYSTEMS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " OperatingSystems "
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

	private static final String SELECT_ALL_RESOLUTIONS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Resolutions "
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

	private static final String SELECT_ALL_VERSIONS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Versions "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_PROJECTS =
		"SELECT"
		+ " id,"
		+ " date,"
		+ " domain,"
		+ " product,"
		+ " revision, "
		+ " lastBugDate,"
		+ " bugTracker "
		+ "FROM"
		+ " Projects";

	private static final String SELECT_PROJECT =
		"SELECT"
		+ " id,"
		+ " date,"
		+ " domain,"
		+ " product,"
		+ " revision, "
		+ " lastBugDate,"
		+ " bugTracker "
		+ "FROM"
		+ " Projects "
		+ "WHERE "
		+ " id = ?";

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
		+ " Comments.pos,"
		+ " Comments.creation,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiMail,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Comments.content, "
		+ " Identity.context		AS aiContext,"
		+ " Identity.identifier "
		+ "FROM"
		+ " Comments "
		+ "LEFT JOIN Identities Identity"
		+ " ON Comments.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "WHERE"
		+ " Comments.bug = ?"
		+ "ORDER BY"
		+ " Comments.pos";

	private static final String SELECT_COMMENT_BY_INDEX =
		"SELECT"
		+ " Comments.id,"
		+ " Comments.creation,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiMail,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " Comments.content, "
		+ " Identity.context		AS aiContext,"
		+ " Identity.identifier "
		+ "FROM"
		+ " Comments "
		+ "LEFT JOIN Identities Identity"
		+ " ON Comments.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "WHERE"
		+ " Comments.bug = ?"
		+ " AND Comments.pos = ?";

	private static final String SELECT_FULL_HISTORY =
		"SELECT"
		+ " BugHistories.id,"
		+ " Status.id,"
		+ " Status.name,"
		+ " Identity.id				AS aiId,"
		+ " Identity.name			AS aiName,"
		+ " Identity.mail			AS aiMail,"
		+ " Users.id				AS auId,"
		+ " Users.name				AS auName,"
		+ " BugHistories.date, "
		+ " Identity.context		AS iContext."
		+ " Identity.identifier "
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

	private static final String SELECT_DICTIONARIES =
		"SELECT "
		+ " id, "
		+ " name, "
		+ " context "
		+ "FROM "
		+ " Dictionary "
		+ "WHERE "
		+ " project = ?";

	private static final String SELECT_ALL_IDENTITIES_CONTEXT =
			"SELECT "
			+ " u.id, "
			+ " u.name, "
			+ " i.id, "
			+ " i.context, "
			+ " i.mail, "
			+ " i.name, "
			+ " i.user,"
			+ " i.identifier "
			+ "FROM "
			+ " Users u, Identities i "
			+ "WHERE "
			+ " u.id = i.user "
			+ " AND u.project = ? "
			+ " AND i.context = ? ";

	private static final String SELECT_IDENTITY_BY_IDENTIFIER =
			"SELECT "
			+ " u.id, "
			+ " u.name, "
			+ " i.id, "
			+ " i.context, "
			+ " i.mail, "
			+ " i.name, "
			+ " i.user,"
			+ " i.identifier "
			+ "FROM "
			+ " Users u, Identities i "
			+ "WHERE "
			+ " u.id = i.user "
			+ " AND u.project = ? "
			+ " AND i.context = ? "
			+ " AND i.identifier = ?";

	private static final String SELECT_ALL_IDENTITIES =
		"SELECT "
		+ " u.id, "
		+ " u.name, "
		+ " i.id, "
		+ " i.context, "
		+ " i.mail, "
		+ " i.name, "
		+ " i.user,"
		+ " i.identifier "
		+ "FROM "
		+ " Users u, Identities i "
		+ "WHERE "
		+ " u.id = i.user "
		+ " AND u.project = ? ";

	private static final String SELECT_ALL_USERS =
		"SELECT "
		+ " u.id, "
		+ " u.name "
		+ "FROM "
		+ " Users u "
		+ "WHERE "
		+ " u.project = ? ";

	private static final String SELECT_SELECTED_USERS =
		"SELECT "
		+ " id "
		+ "FROM "
		+ " SelectedUsers";

	private static final String SELECT_ATTACHMENT_IDENTITY =
		"SELECT "
		+ " u.id,"
		+ " u.name,"
		+ " i.id,"
		+ " i.context,"
		+ " i.name,"
		+ " i.mail, "
		+ " i.identifier "
		+ "FROM "
		+ " Attachments a,"
		+ " Comments c,"
		+ " Identities i,"
		+ " Users u "
		+ "WHERE "
		+ " a.comment = c.id "
		+ " AND c.identity = i.id "
		+ " AND i.user = u.id "
		+ " AND u.project = ?"
		+ " AND a.identifier = ?";

	private static final String SELECT_COUNTS =
		"SELECT "
		+ "(SELECT COUNT(*) FROM Commits WHERE project = ?),"
		+ "(SELECT COUNT(*) FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String SELECT_ATTACHMENT_STATES =
		"SELECT "
		+ " id,"
		+ " name "
		+ "FROM "
		+ " AttachmentStatus "
		+ "WHERE"
		+ " project = ?";
	
	private static final String LOAD_EXTENSION =
		"SELECT load_extension (?)";

	
	//
	// Insertions:
	//

	private static final String OBSOLETE_ATTACHMENT_INSERTION =
		"INSERT INTO ObsoleteAttachments "
		+ "(attachment, identity, date, isObsolete) "
		+ "VALUES (?, ?, ?, ?);";

	private static final String BUG_CATEGORY_INSERTION =
		"INSERT INTO BugCategories "
		+ "(bug, category) "
		+ "VALUES (?, ?)";

	private static final String COMMIT_CATEGORY_INSERTION =
		"INSERT INTO CommitCategories "
		+ "(commitId, category) "
		+ "VALUES (?, ?)";

	private static final String DICTIONARY_INSERT =
		"INSERT INTO Dictionary "
		+ "(name, project, context) "
		+ "VALUES (?, ?, ?)";
	
	private static final String PROJECT_INSERTION =
		"INSERT INTO Projects"
		+ "(date, domain, product, revision, defaultStatusId, lastBugDate, bugTracker)"
		+ "VALUES (?,?,?,?,?,?,?)";
	
	private static final String PROJECT_UPDATE =
		"UPDATE Projects SET "
		+ " date = ?,"
		+ " domain = ?,"
		+ " product = ?,"
		+ " revision = ?,"
		+ " lastBugDate = ?,"
		+ " bugTracker = ? "
		+ "WHERE "
		+ " id = ?";
	
	private static final String PROJECT_FLAG_INSERTION =
			"INSERT INTO ProjectFlags"
			+ "(project, flag)"
			+ "VALUES (?,?)";
	
	private static final String USER_INSERTION =
		"INSERT INTO Users"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String SELECTED_USER_INSERTION =
		"INSERT INTO SelectedUsers"
		+ "(id) "
		+ "VALUES (?)";
	
	private static final String IDENTITY_INSERTION =
		"INSERT INTO Identities"
		+ "(mail, name, user, context, identifier)"
		+ "VALUES (?,?,?,?,?)";

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

	private static final String VERSION_INSERTION =
		"INSERT INTO Versions"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String CATEGORY_INSERTION =
		"INSERT INTO Categories"
		+ "(name, dictionary)"
		+ "VALUES (?,?)";

	private static final String BUG_INSERTION =
		"INSERT INTO Bugs "
		+ "(identifier, identity, component, title, creation, priority, severity, curStat, resolution, lastChange, version, operatingSystem)"
		+ "SELECT ?, ?, ?, ?, ?, ?, ?, defaultStatusId, ?, ?, ?, ? "
		+ "FROM Projects WHERE id=?";

	private static final String BUG_UPDATE =
		"UPDATE Bugs SET"
		+ " identifier = ?,"
		+ " identity = ?,"
		+ " component = ?,"
		+ " title = ?,"
		+ " creation = ?,"
		+ " priority = ?,"
		+ " severity = ?,"
		+ " curStat = ?"
		+ "WHERE"
		+ " id = ?";

	private static final String BUG_ALIAS_INSERTION =
		"INSERT INTO BugAliases"
		+ "(bug, addedBy, date, alias)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String PRIORITY_HISTORY_INSERTION =
		"INSERT INTO PriorityHistory"
		+ "(bug, addedBy, date, priority)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String VERSION_HISTORY_INSERTION =
		"INSERT INTO VersionHistory"
		+ "(bug, addedBy, date, version)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String RESOLUTION_HISTORY_INSERTION =
		"INSERT INTO ResolutionHistory"
		+ "(bug, addedBy, date, resolution)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String CONFIRMED_HISTORY_INSERTION =
		"INSERT INTO ConfirmedHistory"
		+ "(bug, addedBy, date, removed)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String SEVERITY_HISTORY_INSERTION =
		"INSERT INTO SeverityHistory"
		+ "(bug, addedBy, date, severity)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String OPERATING_SYSTEM_HISTORY_INSERTION =
		"INSERT INTO OperatingSystemHistory"
		+ "(bug, addedBy, date, oldOperatingSystem, newOperatingSystem)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String STATUS_HISTORY_INSERTION =
		"INSERT INTO StatusHistory"
		+ "(bug, addedBy, date, status)"
		+ "VALUES (?, ?, ?, ?)";
	
	private static final String ATTACHMENT_INSERTION =
		"INSERT INTO Attachments"
		+ "(identifier, comment)"
		+ "VALUES (?, ?)";

	private static final String ATTACHMENT_UPDATE =
		"UPDATE Attachments SET "
		+ " identifier = ?,"
		+ " comment = ? "
		+ "WHERE"
		+ " id = ?";

	/*
	private static final String ATTACHMENT_REPLACEMENT_INSERTION =
		"INSERT INTO AttachmentReplacements"
		+ "(old, new)"
		+ "VALUES (?, ?)";
	*/
	
	private static final String ATTACHMENT_STATUS_INSERTION
		= "INSERT INTO AttachmentStatus"
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String ATTACHMENT_HISTORY_INSERTION =
		"INSERT INTO AttachmentHistory"
		+ "(identity, attachment, status, date)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String COMMENT_INSERTION =
		"INSERT INTO Comments"
		+ "(bug, pos, creation, identity, content)"
		+ "VALUES (?,?,?,?,?)";	
	
	private static final String STATUS_INSERTION =
		"INSERT INTO Status"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String BUG_HISTORY_INSERTION =
		"INSERT INTO BugHistories"
		+ "(bug, status, identity, date)"
		+ "VALUES (?,?,?,?)";

	private static final String BUG_CC_INSERTION =
		"INSERT INTO BugCc"
		+ "(bug, date, addedBy, cc, ccMail, removed)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_CC_RESOLVE_IDENTITIES =
		"UPDATE "
		+ " BugCc "
		+ "SET "
		+ " cc = (SELECT Identities.id FROM Identities, Users WHERE Identities.context = 'bug' AND Identities.user = Users.id AND mail = ccMail AND Users.project = ?) "
		+ "WHERE "
		+ " cc IS NULL "
		+ "AND bug IN (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_BLOCKS_INSERTION =
		"INSERT INTO BugBlocks"
		+ "(bug, date, addedBy, blocks, blocksIdentifier, removed)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_BLOCKS_RESOLVE_BUGS =
		"UPDATE"
		+ " BugBlocks "
		+ "SET "
		+ " blocks = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugBlocks.blocksIdentifier AND Components.project = ?) "
		+ "WHERE"
		+ " blocks IS NULL"
		+ " AND bug in (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String COMMIT_INSERTION =
		"INSERT INTO Commits"
		+ "(project, author, committer, date, title, linesAdded, linesRemoved, changedFiles, identifier)"
		+ "VALUES (?,?,?,?,?,?,?,?,?)";

	private static final String BUGFIX_COMMIT_INSERTION =
		"INSERT INTO BugfixCommit"
		+ "(bug, commitId)"
		+ "VALUES (?,?)";

	private static final String SEVERITY_INSERTION =
		"INSERT INTO Severity "
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String RESOLUTION_INSERTION =
		"INSERT INTO Resolutions "
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String OPERATING_SYSTEM_INSERTION =
		"INSERT INTO OperatingSystems "
		+ "(project, name)"
		+ "VALUES (?, ?)";

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

	private static final String SENTIMENT_BLOCK_INSERTION =
		"INSERT INTO BlockSentiment ("
		+ "sentimentId,"
		+ "pos,"

		+ "target,"

		+ "negative,"
		+ "somewhatNegative,"
		+ "neutral,"
		+ "somewhatPositive,"
		+ "positive,"

		+ "negativeMean,"
		+ "somewhatNegativeMean,"
		+ "neutralMean,"
		+ "somewhatPositiveMean,"
		+ "positiveMean,"

		+ "negativeWMean,"
		+ "somewhatNegativeWMean,"
		+ "neutralWMean,"
		+ "somewhatPositiveWMean,"
		+ "positiveWMean,"

		+ "wordCount,"
		+ "sentenceCount"
		+ ") VALUES ("
		+ "?,?,"
		+ "?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?"
		+ ")";

	private static final String SENTIMENT_INSERTION =
		"INSERT INTO Sentiment ("
		+ "commentId,"

		+ "negative,"
		+ "somewhatNegative,"
		+ "neutral,"
		+ "somewhatPositive,"
		+ "positive,"

		+ "negativeMean,"
		+ "somewhatNegativeMean,"
		+ "neutralMean,"
		+ "somewhatPositiveMean,"
		+ "positiveMean,"

		+ "negativeWMean,"
		+ "somewhatNegativeWMean,"
		+ "neutralWMean,"
		+ "somewhatPositiveWMean,"
		+ "positiveWMean,"

		+ "wordCount,"
		+ "sentences"
		+ ") VALUES ("
		+ "?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?"
		+ ")";

	private static final String SENTIMENT_SENTENCE_INSERTION =
		"INSERT INTO SentenceSentiment ("
		+ "groupId,"
		+ "pos,"			

		+ "negative,"
		+ "somewhatNegative,"
		+ "neutral,"
		+ "somewhatPositive,"
		+ "positive,"

		+ "wordCount"
		+ ") VALUES ("
		+ "?,?,"
		+ "?,?,?,?,?,"
		+ "?"
		+ ")";
	

	private static final String UPDATE_DEFAULT_STATUS =
		"UPDATE Projects SET defaultStatusId = ? WHERE id = ?";

	private static final String UPDATE_FILE_NAME =
		"UPDATE Files SET name = ? WHERE id = ?";

	
	private static final String DELETE_USERS =
		"DELETE FROM Users WHERE project = ?";

	private static final String DELETE_SELECTED_USERS =
		"DELETE FROM SelectedUsers";
	
	private static final String DELETE_SENTENCE_SENTIMENTS =
		"DELETE FROM SentenceSentiment "
		+ "WHERE groupId IN "
		+ "(SELECT b.id FROM BlockSentiment b WHERE b.sentimentId IN "
		+ "(SELECT cmnt.id FROM Comments cmnt, Bugs bg, Components cmp "
		+ "WHERE cmnt.bug = bg.id AND bg.component = cmp.id AND cmp.project = ?))";

	private static final String DELETE_BLOCK_SENTIMENTS =
		"DELETE FROM BlockSentiment "
		+ "WHERE sentimentId IN "
		+ "(SELECT cmnt.id FROM Comments cmnt, Bugs bg, Components cmp "
		+ "WHERE cmnt.bug = bg.id AND bg.component = cmp.id AND cmp.project = ?)";

	private static final String DELETE_SENTIMENTS =
		"DELETE FROM Sentiment "
		+ "WHERE commentId IN "
		+ "(SELECT cmnt.id FROM Comments cmnt, Bugs bg, Components cmp "
		+ "WHERE cmnt.bug = bg.id AND bg.component = cmp.id AND cmp.project = ?)";

	private static final String DELETE_BUGFIX_COMMITS =
		"DELETE FROM BugfixCommit WHERE commitId IN (SELECT id FROM Commits c WHERE project = ?)";

	
	public static class Stats {
		public final int commitCount;
		public final int bugCount;

		public Stats (int commitCount, int bugCount) {
			this.commitCount = commitCount;
			this.bugCount = bugCount;
		}
	}


	private boolean printTemplates = false;
	private ModelPool pool;
	private Connection conn;

	
	//
	// Creation & Destruction:
	//
	
	public Model (ModelPool pool, Connection conn, String[] extensions) throws SQLException {
		assert (extensions != null);
		
		this.pool = pool;
		this.conn = conn;

		createTables ();

		// Load drivers:
		PreparedStatement stmt = conn.prepareStatement (LOAD_EXTENSION);
		try {
			for (String ext : extensions) {
				stmt.setString (1, ext);
				stmt.execute ();
			}
		} finally {
			stmt.close ();
		}
	}
	
	public Model (ModelPool pool, Connection conn) throws SQLException {
		this (pool, conn, new String[0]);
	}

	public void setPrintTemplates (boolean printTemplates) {
		this.printTemplates = printTemplates;
	}

	public boolean getPrintTemplates () {
		return this.printTemplates;
	}
	
	public synchronized boolean close () {
		try {
			if (conn != null) {
				pool.pushConnection (conn);
				this.conn = null;
			}
			return true;
		} catch (SQLException e) {
			this.conn = null;
			return false;
		}
	}

	public synchronized void begin () throws SQLException {
		conn.setAutoCommit (false);
	}

	public synchronized void commit () throws SQLException {
		conn.commit ();
		conn.setAutoCommit (true);
	}

	public synchronized void rollback () throws SQLException {
		conn.rollback ();
		conn.setAutoCommit (true);
	}


	//
	// Data Insertion API:
	//
	
	public Stats getStats (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SELECT_COUNTS);
		stmt.setInt (1, proj.getId ());
		stmt.setInt (2, proj.getId ());

		ResultSet res = stmt.executeQuery ();
		res.next ();

		int commitCount = res.getInt (1);
		int bugCount = res.getInt (2);
		stmt.close ();

		return new Stats (commitCount, bugCount);
	}

	public void setAttachmentIsObsolete (Attachment attachment, Identity identity, Date date, boolean value) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);
		assert (identity != null);
		assert (identity.getId () != null);
		assert (date != null);

		PreparedStatement stmt = conn.prepareStatement (OBSOLETE_ATTACHMENT_INSERTION);
		stmt.setInt (1, attachment.getId ());
		stmt.setInt (2, identity.getId ());
		resSetDate (stmt, 3, date);
		stmt.setBoolean (4, value);
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAttachmentIsObsoleteAdded (attachment, identity, date, value);
	}
	
	public void addFlag (Project proj, String flag) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		assert (flag != null);

		PreparedStatement stmt = conn.prepareStatement (PROJECT_FLAG_INSERTION);

		stmt.setInt (1, proj.getId ());
		stmt.setString (2, flag);
		stmt.executeUpdate();
		stmt.close ();
	}

	public List<String> getFlags (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

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
	}

	public Project addProject (Date date, Date lastBugDate, String bugTracker, String domain, String product, String revision) throws SQLException {
		Project proj = new Project (null, date, lastBugDate, bugTracker, domain, product, revision);
		add (proj);
		return proj;
	}
	
	public void setDefaultStatus (Status status) throws SQLException {
		assert (conn != null);
		assert (status != null);
		assert (status.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (UPDATE_DEFAULT_STATUS);

		stmt.setInt (1, status.getId ());
		stmt.setInt (2, status.getProject ().getId ());
		stmt.executeUpdate();
		stmt.close ();
	}
	
	public void add (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (PROJECT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		resSetDate (stmt, 1, project.getDate ());
		stmt.setString (2, project.getDomain ());
		stmt.setString (3, project.getProduct ());
		stmt.setString (4, project.getRevision ());
		resSetDate (stmt, 6, project.getLastBugMiningDate ());
		stmt.setString (7, project.getBugTracker ());
		stmt.executeUpdate();

		project.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitProjectAdded (project);
	}

	public void updateProject (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (PROJECT_UPDATE);
		resSetDate (stmt, 1, project.getDate ());
		stmt.setString (2, project.getDomain ());
		stmt.setString (3, project.getProduct ());
		stmt.setString (4, project.getRevision ());
		resSetDate (stmt, 5, project.getLastBugMiningDate ());
		stmt.setString (6, project.getBugTracker ());
		stmt.setInt (7, project.getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitProjectUpdated (project);
	}

	
	public User addUser (Project project, String name) throws SQLException {
		User user = new User (null, project, name);
		add (user);
		return user;
	}

	public void add (User user) throws SQLException {
		assert (conn != null);
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

		pool.emitUserAdded (user);
	}
	
	public void removeUsers (Project project) throws SQLException {
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_USERS,
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.executeUpdate();
		stmt.close ();		
	}
	
	public void setIdentityUser (Identity id, User user) throws SQLException {
		assert (user != null);
		assert (id != null);
		assert (user.getId () != null);
		assert (id.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (
			"UPDATE Identities SET user = ? WHERE id = ?",
			Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, user.getId ());
		stmt.setInt (2, id.getId ());
		stmt.executeUpdate();
		stmt.close ();		
	}
	
	public Identity addIdentity (Integer identifier, String context, String mail, String name, User user) throws SQLException {
		Identity identity = new Identity (null, identifier, context, mail, name, user);
		add (identity);
		return identity;
	}

	public void add (Identity identity) throws SQLException {
		assert (conn != null);
		assert (identity != null);
		assert (identity.getUser ().getId () != null);
		assert (identity.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (IDENTITY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setString (1, identity.getMail ());
		stmt.setString (2, identity.getName ());
		stmt.setInt (3, identity.getUser ().getId ());
		stmt.setString (4, identity.getContext ());
		if (identity.getIdentifier () != null) {
			stmt.setInt (5, identity.getIdentifier ());
		} else {
			stmt.setNull (5, Types.INTEGER);
		}
		stmt.executeUpdate();

		identity.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitIdentityAdded (identity);
	}
	
	
	public Interaction addInteraction (User from, User to, boolean closed, int quotes, float pos, float neg, Date date) throws SQLException {
		Interaction relation = new Interaction (null, from, to, closed, quotes, pos, neg, date);
		add (relation);
		return relation;
	}

	public void add (Interaction relation) throws SQLException {
		assert (conn != null);
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
		resSetDate (stmt, 6, relation.getDate ());
		stmt.setInt (7, (relation.isClosed ())? 1 : 0);
		stmt.executeUpdate();

		relation.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitInteraction (relation);
	}

	public Severity addSeverity (Project project, String name) throws SQLException {
		Severity sev = new Severity (null, project, name);
		add (sev);
		return sev;
	}

	public void add (Severity severity) throws SQLException {
		assert (conn != null);
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

		pool.emitSeverityAdded (severity);
	}

	public Resolution addResolution (Project project, String name) throws SQLException {
		Resolution res = new Resolution (null, project, name);
		add (res);
		return res;
	}

	public void add (Resolution resolution) throws SQLException {
		assert (conn != null);
		assert (resolution != null);
		Project project = resolution.getProject ();
		assert (project.getId () != null);
		assert (resolution.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (RESOLUTION_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, resolution.getName ());
		stmt.executeUpdate();

		resolution.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitResolutionAdded (resolution);
	}

	public OperatingSystem addOperatingSystem (Project project, String name) throws SQLException {
		OperatingSystem os = new OperatingSystem (null, project, name);
		add (os);
		return os;
	}

	public void add (OperatingSystem os) throws SQLException {
		assert (conn != null);
		assert (os != null);
		Project project = os.getProject ();
		assert (project.getId () != null);
		assert (os.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (OPERATING_SYSTEM_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, os.getName ());
		stmt.executeUpdate();

		os.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitOperatingSystemAdded (os);
	}

	public Priority addPriority (Project project, String name) throws SQLException {
		Priority priority = new Priority (null, project, name);
		add (priority);
		return priority;
	}
	
	public void add (Priority priority) throws SQLException {
		assert (conn != null);
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

		pool.emitPriorityAdded (priority);
	}


	public Category addCategory (String name, Dictionary dictionary) throws SQLException {
		Category category = new Category (null, name, dictionary);
		add (category);
		return category;
	}

	public void add (Category category) throws SQLException {
		assert (conn != null);
		assert (category != null);
		assert (category.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (CATEGORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setString(1, category.getName ());
		stmt.setInt (2, category.getDictionary ().getId ());
		stmt.executeUpdate();

		category.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitCategoryAdded (category);
	}

	public void addBugCategory (Bug bug, Category category) throws SQLException {
		assert (bug != null);
		assert (category != null);
		assert (bug.getId () != null);
		assert (category.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CATEGORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, category.getId ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitBugCategoryAdded (bug, category);
	}

	public void addCommitCategory (Commit commit, Category category) throws SQLException {
		assert (commit != null);
		assert (category != null);
		assert (commit.getId () != null);
		assert (category.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (COMMIT_CATEGORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, commit.getId ());
		stmt.setInt (2, category.getId ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitCommitCategoryAdded (commit, category);
	}

	public Component addComponent (Project project, String name) throws SQLException {
		Component component = new Component (null, project, name);
		add (component);
		return component;
	}

	public void add (Component component) throws SQLException {
		assert (conn != null);
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

		pool.emitComponentAdded (component);
	}

	public Version addVersion (Project project, String name) throws SQLException {
		Version version = new Version (null, project, name);
		add (version);
		return version;
	}

	public void add (Version version) throws SQLException {
		assert (conn != null);
		assert (version != null);
		Project project = version.getProject ();
		assert (project.getId () != null);
		assert (version.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (VERSION_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, version.getName ());
		stmt.executeUpdate();

		version.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitVersionAdded (version);
	}
	
	public Attachment addAttachment (Integer identifier, Comment comment) throws SQLException {
		Attachment att = new Attachment (null, identifier, comment);
		add (att);
		return att;
	}

	public void add (Attachment attachment) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () == null);
		assert (attachment.getIdentifier () != null);
		assert (attachment.getComment () != null);
		assert (attachment.getComment ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, attachment.getIdentifier ());
		stmt.setInt (2, attachment.getComment ().getId ());
		stmt.executeUpdate();

		attachment.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitAttachmentAdded (attachment);
	}

	public void updateAttachment (Attachment attachment) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);
		assert (attachment.getIdentifier () != null);
		assert (attachment.getComment () != null);
		assert (attachment.getComment ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_UPDATE);
		stmt.setInt (1, attachment.getIdentifier ());
		stmt.setInt (2, attachment.getComment ().getId ());
		stmt.setInt (3, attachment.getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAttachmentUpdated (attachment);
	}
	
	/*
	public void addAttachmentReplacement (Attachment oldAtt, Attachment newAtt) throws SQLException {
		assert (oldAtt != null);
		assert (newAtt != null);
		assert (oldAtt.getId () != null);
		assert (newAtt.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_REPLACEMENT_INSERTION);
		stmt.setInt (1, oldAtt.getId ());
		stmt.setInt (2, newAtt.getId ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitAttachmentReplacementAdded (oldAtt, newAtt);
	}
	*/
	
	public AttachmentHistory addAttachmentHistory (Identity identity, AttachmentStatus status, Attachment attachment, Date date) throws SQLException {
		AttachmentHistory histo = new AttachmentHistory (null, identity, status, attachment, date);
		add (histo);
		return histo;
	}

	public void add (AttachmentHistory history) throws SQLException {
		assert (conn != null);
		assert (history != null);
		assert (history.getAttachment () != null);
		assert (history.getStatus () != null);
		assert (history.getIdentity () != null);
		assert (history.getAttachment ().getId () != null);
		assert (history.getStatus ().getId () != null);
		assert (history.getIdentity ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_HISTORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, history.getIdentity ().getId ());
		stmt.setInt (2, history.getAttachment ().getId ());
		stmt.setInt (3, history.getStatus ().getId ());
		resSetDate (stmt, 4, history.getDate ());
		stmt.executeUpdate();

		history.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitAttachmentHistoryAdded (history);
	}

	public Bug addBug (Integer identifier, Identity identity, Component component,
			String title, Date creation, Date lastChange, Priority priority, Severity severity, Resolution resolution, Version version, OperatingSystem operatingSystem) throws SQLException {
		Bug bug = new Bug (null, identifier, identity, component, title, creation, lastChange, priority, severity, resolution, version, operatingSystem);
		add (bug);

		return bug;
	}

	public void add (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () == null);
		assert (bug.getComponent () != null);
		assert (bug.getPriority () != null);
		assert (bug.getSeverity() != null);
		assert (bug.getIdentity () == null || bug.getIdentity ().getId () != null);
		assert (bug.getComponent ().getId () != null);
		assert (bug.getPriority ().getId () != null);
		assert (bug.getSeverity().getId () != null);
		assert (bug.getResolution () != null);
		assert (bug.getResolution ().getId () != null);
		assert (bug.getVersion () != null);
		assert (bug.getVersion ().getId () != null);
		assert (bug.getOperatingSystem () != null);
		assert (bug.getOperatingSystem ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getIdentifier ());
		if (bug.getIdentity () != null) {
			stmt.setInt (2, bug.getIdentity ().getId ());
		} else {
			stmt.setNull (2, Types.INTEGER);
		}
		stmt.setInt (3, bug.getComponent ().getId ());
		stmt.setString (4, bug.getTitle ());
		resSetDate (stmt, 5, bug.getCreation ());
		stmt.setInt (6, bug.getPriority ().getId ());
		stmt.setInt (7, bug.getSeverity ().getId ());
		stmt.setInt (8, bug.getResolution ().getId ());
		resSetDate (stmt, 9, bug.getLastChange ());		
		stmt.setInt (10, bug.getVersion ().getId ());
		stmt.setInt (11, bug.getOperatingSystem ().getId ());
		stmt.setInt (12, bug.getComponent ().getProject ().getId ());
		stmt.executeUpdate();

		bug.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitBugAdded (bug);
	}

	public void updateBug (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (bug.getIdentifier () != null);
		assert (bug.getComponent () != null);
		assert (bug.getPriority () != null);
		assert (bug.getSeverity() != null);
		assert (bug.getIdentity () == null || bug.getIdentity ().getId () != null);
		assert (bug.getComponent ().getId () != null);
		assert (bug.getPriority ().getId () != null);
		assert (bug.getSeverity().getId () != null);
	
		PreparedStatement stmt = conn.prepareStatement (BUG_UPDATE);
		stmt.setInt (1, bug.getIdentifier ());
		if (bug.getIdentity () != null) {
			stmt.setInt (2, bug.getIdentity ().getId ());
		} else {
			stmt.setNull (2, Types.INTEGER);
		}
		stmt.setInt (3, bug.getComponent ().getId ());
		stmt.setString (4, bug.getTitle ());
		resSetDate (stmt, 5, bug.getCreation ());
		stmt.setInt (6, bug.getPriority ().getId ());
		stmt.setInt (7, bug.getSeverity ().getId ());
		stmt.setInt (8, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugUpdated (bug);
	}

	public void addBugAlias (Bug bug, Identity addedBy, Date date, String alias) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (alias != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_ALIAS_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setString (4, alias);

		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugAliasAdded (bug, addedBy, date, alias);
	}

	public void addPriorityHistory (Bug bug, Identity addedBy, Date date, Priority priority) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (priority != null);
		assert (priority.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (PRIORITY_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, priority.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitPriorityHistoryAdded (bug, addedBy, date, priority);
	}

	public void addVersionHistory (Bug bug, Identity addedBy, Date date, Version version) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (version != null);
		assert (version.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (VERSION_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, version.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitVersionHistoryAdded (bug, addedBy, date, version);
	}

	public void addResolutionHistory (Bug bug, Identity addedBy, Date date, Resolution resolution) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (resolution != null);
		assert (resolution.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (RESOLUTION_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, resolution.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitResolutionHistoryAdded (bug, addedBy, date, resolution);
	}

	public void addConfirmedHistory (Bug bug, Identity addedBy, Date date, boolean removed) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);

		PreparedStatement stmt = conn.prepareStatement (CONFIRMED_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setBoolean (4, removed);

		stmt.executeUpdate();
		stmt.close ();

		pool.emitConfirmedHistoryAdded (bug, addedBy, date, removed);
	}

	public void addSeverityHistory (Bug bug, Identity addedBy, Date date, Severity severity) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (severity != null);
		assert (severity.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SEVERITY_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, severity.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitSeverityHistoryAdded (bug, addedBy, date, severity);
	}

	public void addOperatingSystemHistory (Bug bug, Identity addedBy, Date date, OperatingSystem oldOs, OperatingSystem newOs) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldOs != null);
		assert (oldOs.getId () != null);
		assert (newOs != null);
		assert (newOs.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (OPERATING_SYSTEM_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldOs.getId ());
		stmt.setInt (5, newOs.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitOperatingSystemHistoryAdded (bug, addedBy, date, oldOs, newOs);
	}

	public void addStatusHistory (Bug bug, Identity addedBy, Date date, Status status) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (status != null);
		assert (status.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (STATUS_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, status.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitStatusHistoryAdded (bug, addedBy, date, status);
	}

	public Dictionary addDictionary (String name, String context, Project project) throws SQLException {
		Dictionary dict = new Dictionary (null, name, context, project);
		add (dict);
		return dict;
	}

	public void add (Dictionary dict) throws SQLException {
		assert (dict != null);
		assert (dict.getId () == null);
		assert (dict.getProject () != null);
		assert (dict.getProject ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DICTIONARY_INSERT,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setString (1, dict.getName ());
		stmt.setInt (2, dict.getProject ().getId ());
		stmt.setString (3, dict.getContext ());
		stmt.executeUpdate();

		dict.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitDictionaryAdded (dict);
	}
	
	public BugHistory addBugHistory (Bug bug, Status status, Identity identity, Date date) throws SQLException {
		BugHistory history = new BugHistory(null, bug, status, identity, date);
		add (history);
		return history;
	}

	public void add (BugHistory history) throws SQLException {
		assert (conn != null);
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
		resSetDate (stmt, 4, history.getDate ());
		stmt.executeUpdate();

		history.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitBugHistoryAdded (history);
	}

	public void addBugCc (Bug bug, Date date, Identity addedBy, Identity cc, String ccMail, boolean removed) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (date != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (cc == null || cc.getId () != null);
		assert (ccMail != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CC_INSERTION);
		stmt.setInt (1, bug.getId ());
		resSetDate (stmt, 2, date);
		stmt.setInt (3, addedBy.getId ());
		if (cc != null) {
			stmt.setInt (4, cc.getId ());
		} else {
			stmt.setNull (4, Types.INTEGER);
		}
		stmt.setString (5, ccMail);
		stmt.setBoolean (6, removed);
		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugCcAdded (bug, date, addedBy, cc, ccMail, removed);
	}

	public void addBugBlocks (Bug bug, Date date, Identity addedBy, Bug blocks, int bugIdentifier, boolean removed) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (date != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (blocks == null || blocks.getId () != null);
		assert (bugIdentifier > 0);

		PreparedStatement stmt = conn.prepareStatement (BUG_BLOCKS_INSERTION);
		stmt.setInt (1, bug.getId ());
		resSetDate (stmt, 2, date);
		stmt.setInt (3, addedBy.getId ());
		if (blocks != null) {
			stmt.setInt (4, blocks.getId ());
		} else {
			stmt.setNull (4, Types.INTEGER);
		}
		stmt.setInt (5, bugIdentifier);
		stmt.setBoolean (6, removed);
		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugBlocksAdded (bug, date, addedBy, removed);
	}

	public void resolveCcIdentities (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CC_RESOLVE_IDENTITIES);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, project.getId ());

		stmt.executeUpdate();
		stmt.close ();
	}

	public void resolveBugBlocksBugs (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_BLOCKS_RESOLVE_BUGS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, project.getId ());

		stmt.executeUpdate();
		stmt.close ();
	}

	public Comment addComment (int index, Bug bug, Date creation, Identity identity, String content) throws SQLException {
		Comment cmnt = new Comment(null, index, bug, creation, identity, content);
		add (cmnt);
		return cmnt;
	}
	
	public void add (Comment cmnt) throws SQLException {
		assert (conn != null);
		assert (cmnt != null);
		assert (cmnt.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (COMMENT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, cmnt.getBug ().getId ());
		stmt.setInt (2, cmnt.getIndex ());
		resSetDate (stmt, 3, cmnt.getCreationDate ());
		stmt.setInt (4, cmnt.getIdentity ().getId ());
		stmt.setString (5, cmnt.getContent ());
		stmt.executeUpdate();

		cmnt.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitCommentAdded (cmnt);
	}

	public AttachmentStatus addAttachmentStatus (Project project, String name) throws SQLException {
		AttachmentStatus status = new AttachmentStatus (null, project, name);
		add (status);
		return status;
	}	

	public void add (AttachmentStatus status) throws SQLException {
		assert (conn != null);
		assert (status != null);
		Project project = status.getProject ();
		assert (project.getId () != null);
		assert (status.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_STATUS_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, status.getName ());
		stmt.executeUpdate();

		status.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitAttachmentStatusAdded (status);
	}

	public Status addStatus (Project project, String name) throws SQLException {
		Status status = new Status (null, project, name);
		add (status);
		return status;
	}	

	public void add (Status status) throws SQLException {
		assert (conn != null);
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

		pool.emitStatusAdded (status);
	}

		
	public Commit addCommit (String identifier, Project project, Identity author,
			Identity committer, Date date, String title, int changedFiles,
			int changedLines, int linesRemoved)
		throws SQLException
	{
		Commit commit = new Commit (null, identifier, project,
				author, committer, date, title, changedFiles,
				changedLines, linesRemoved);
		add (commit);
		return commit;
	}
	
	public void add (Commit commit) throws SQLException {
		assert (conn != null);
		assert (commit != null);
		Project project = commit.getProject ();
		assert (commit.getId () == null);
		assert (commit.getAuthor ().getId () != null);
		assert (commit.getCommitter ().getId () != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (COMMIT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, commit.getAuthor ().getId ());
		stmt.setInt (3, commit.getCommitter ().getId ());
		resSetDate (stmt, 4, commit.getDate ());
		stmt.setString (5, commit.getTitle ());
		stmt.setInt (6, commit.getLinesAdded ());
		stmt.setInt (7, commit.getLinesRemoved ());
		stmt.setInt (8, commit.getChangedFiles ());
		stmt.setString (9, commit.getIdentifier ());
		stmt.executeUpdate();

		commit.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitCommitAdded (commit);
	}
		
	
	public ManagedFileCopy addIsCopy (ManagedFile copy, Commit commit, ManagedFile original) throws SQLException {
		ManagedFileCopy _copy = new ManagedFileCopy (copy, commit, original);
		add (_copy);
		return _copy;
	}

	public void add (ManagedFileCopy copy) throws SQLException {
		assert (conn != null);
		assert (copy != null);
		assert (copy.getFile ().getId () != null);
		assert (copy.getOriginal ().getId () != null);
		assert (copy.getCommit ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (FILE_COPY_INSERTION);

		stmt.setInt (1, copy.getFile ().getId ());
		stmt.setInt (2, copy.getCommit ().getId ());
		stmt.setInt (3, copy.getOriginal ().getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitManagedFileCopyAdded (copy);
	}

	// TODO: calculate lines-added, lines-removed, chunks-changed
	public ManagedFile addManagedFile (Project project, String name) throws SQLException {
		ManagedFile file = new ManagedFile (null, project, name, null, 1, 0, 0, 1);
		add (file);
		return file;
	}

	public void add (ManagedFile file) throws SQLException {
		assert (conn != null);
		assert (file != null);
		assert (file.getId () == null);
		assert (file .getChunksChanged () == 1);
		assert (file.getLinesRemoved () == 0);
		assert (file.getTouched () == 1);
		assert (file.getProject ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (FILE_INSERTION);

		stmt.setInt (1, file.getProject ().getId ());
		stmt.setString (2, file.getName ());
		stmt.setInt (3, file.getLinesAdded ());
		stmt.executeUpdate();
		stmt.close ();

		file.setId (getLastInsertedId (stmt));

		pool.emitManagedFileAdded (file);
	}

	
	public FileDeletion addFileDeletion (ManagedFile file, Commit commit) throws SQLException {
		FileDeletion deletion = new FileDeletion (file, commit);
		add (deletion);
		return deletion;
	}

	public void add (FileDeletion deletion) throws SQLException {
		assert (conn != null);
		assert (deletion != null);
		assert (deletion.getCommit ().getId () != null);
		assert (deletion.getFile ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (FILE_DELETION_INSERTION);

		stmt.setInt (1, deletion.getFile ().getId ());
		stmt.setInt (2, deletion.getCommit ().getId ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitFileDeletionAdded (deletion);
	}


	public FileRename addFileRename (ManagedFile file, Commit commit, String oldName, String newName) throws SQLException {
		FileRename rename = new FileRename (file, commit, oldName);
		add (rename, newName);
		return rename;
	}

	public void add (FileRename rename, String newName) throws SQLException {
		assert (conn != null);
		assert (rename != null);
		assert (rename.getCommit ().getId () != null);
		assert (rename.getFile ().getId () != null);
		assert (rename.getOldName () != null);

		PreparedStatement stmt = conn.prepareStatement (UPDATE_FILE_NAME);
		stmt.setString (1, newName);
		stmt.setInt (2, rename.getFile ().getId ());
		stmt.executeUpdate();

		stmt.close ();


		stmt = conn.prepareStatement (FILE_RENAME_INSERTION);
		stmt.setInt (1, rename.getFile ().getId ());
		stmt.setInt (2, rename.getCommit ().getId ());
		stmt.setString (3, rename.getOldName ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitFileRenameAdded (rename);
	}

	
	public FileChange addFileChange (Commit commit, ManagedFile file, int linesAdded, int linesRemoved, int emptyLinesAdded, int emptyLinesRemoved, int changedChunks) throws SQLException {
		FileChange change = new FileChange (commit, file, linesAdded, linesRemoved, emptyLinesAdded, emptyLinesRemoved, changedChunks);
		add (change);
		return change;
	}

	public void add (FileChange change) throws SQLException {
		assert (conn != null);
		assert (change != null);
		assert (change.getCommit ().getId () != null);
		assert (change.getFile ().getId () != null);

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


		pool.emitFileChangeAdded (change);
	}
	
	
	public BugfixCommit addBugfixCommit (Commit commit, Bug bug) throws SQLException {
		BugfixCommit bugfix = new BugfixCommit (commit, bug);
		add (bugfix);
		return bugfix;
	}

	public void add (BugfixCommit bugfix) throws SQLException {
		assert (conn != null);
		assert (bugfix != null);

		PreparedStatement stmt = conn.prepareStatement (BUGFIX_COMMIT_INSERTION);

		stmt.setInt (1, bugfix.getBug ().getId ());
		stmt.setInt (2, bugfix.getCommit ().getId ());
		stmt.executeUpdate();

		stmt.close ();

		pool.emitBugfixCommitAdded (bugfix);
	}

	public void removeBugfixCommits (Project project) throws SQLException {
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUGFIX_COMMITS);
		stmt.setInt (1, project.getId ());
		stmt.executeUpdate();
		stmt.close ();		
	}
	
	private void addSentenceSentiments (int blockId, List<SentenceSentiment> sentences) throws SQLException {
		assert (blockId >= 0);
		assert (sentences != null);

		PreparedStatement stmt = conn.prepareStatement (SENTIMENT_SENTENCE_INSERTION,
			Statement.RETURN_GENERATED_KEYS);

		
		int pos = 0;
		for (SentenceSentiment sent : sentences) {
			stmt.setInt (1, blockId);
			stmt.setInt (2, pos);
			stmt.setDouble (3, sent.getNegative ());
			stmt.setDouble (4, sent.getSomewhatNegative ());
			stmt.setDouble (5, sent.getNeutral ());
			stmt.setDouble (6, sent.getSomewhatPositive ());
			stmt.setDouble (7, sent.getPositive ());
			stmt.setInt (8, sent.getWordCount ());
			stmt.execute ();
			pos++;
		}

		stmt.close ();
	}
	
	private void addSentimentBlock (int parentId, int blockPos, SentimentBlock<Identity> sentiment) throws SQLException {
		assert (blockPos >= 0);
		assert (parentId >= 0);
		assert (sentiment != null);

		Identity target = sentiment.getData ();
		assert (target == null || target.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SENTIMENT_BLOCK_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, parentId);
		stmt.setInt (2, blockPos);


		if (target != null) {
			stmt.setInt (3, target.getId ());
		} else {
			stmt.setNull (3, Types.INTEGER);
		}

		stmt.setInt (4, sentiment.getNegativeCount ());
		stmt.setInt (5, sentiment.getSomewhatNegativeCount ());
		stmt.setInt (6, sentiment.getNeutralCount ());
		stmt.setInt (7, sentiment.getSomewhatPositiveCount ());
		stmt.setInt (8, sentiment.getPositiveCount ());

		stmt.setDouble (9, sentiment.getNegativeMean ());
		stmt.setDouble (10, sentiment.getSomewhatNegativeMean ());
		stmt.setDouble (11, sentiment.getNeutralMean ());
		stmt.setDouble (12, sentiment.getSomewhatPositiveMean ());
		stmt.setDouble (13, sentiment.getPositiveMean ());

		stmt.setDouble (14, sentiment.getNegativeWMean ());
		stmt.setDouble (15, sentiment.getSomewhatNegativeWMean ());
		stmt.setDouble (16, sentiment.getNeutralWMean ());
		stmt.setDouble (17, sentiment.getSomewhatPositiveWMean ());
		stmt.setDouble (18, sentiment.getPositiveWMean ());

		stmt.setInt (19, sentiment.getWordCount ());
		stmt.setInt (20, sentiment.getSentenceCount ());
		stmt.executeUpdate();

		int newId = getLastInsertedId (stmt);
		stmt.close ();

		addSentenceSentiments (newId, sentiment.getSentenceSentiments ());
	}
	
	public void addSentiment (Comment comment, Sentiment<Identity> sentiment) throws SQLException {
		assert (comment != null);
		assert (sentiment != null);
		assert (comment.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SENTIMENT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, comment.getId ());

		stmt.setInt (2, sentiment.getNegativeCount ());
		stmt.setInt (3, sentiment.getSomewhatNegativeCount ());
		stmt.setInt (4, sentiment.getNeutralCount ());
		stmt.setInt (5, sentiment.getSomewhatPositiveCount ());
		stmt.setInt (6, sentiment.getPositiveCount ());

		stmt.setDouble (7, sentiment.getNegativeMean ());
		stmt.setDouble (8, sentiment.getSomewhatNegativeMean ());
		stmt.setDouble (9, sentiment.getNeutralMean ());
		stmt.setDouble (10, sentiment.getSomewhatPositiveMean ());
		stmt.setDouble (11, sentiment.getPositiveMean ());

		stmt.setDouble (12, sentiment.getNegativeWMean ());
		stmt.setDouble (13, sentiment.getSomewhatNegativeWMean ());
		stmt.setDouble (14, sentiment.getNeutralWMean ());
		stmt.setDouble (15, sentiment.getSomewhatPositiveWMean ());
		stmt.setDouble (16, sentiment.getPositiveWMean ());

		stmt.setInt (17, sentiment.getWordCount ());
		stmt.setInt (18, sentiment.getSentenceCount ());
		stmt.executeUpdate();

		int newId = getLastInsertedId (stmt);
		stmt.close ();

		int i = 0;
		for (SentimentBlock<Identity> block : sentiment.getBlocks ()) {
			addSentimentBlock (newId, i, block);
			i++;
		}
		
		pool.emitSentimentAdded (comment, sentiment);
	}

	public void removeSentiment (Project project) throws SQLException {
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_SENTENCE_SENTIMENTS);
		stmt.setInt (1, project.getId ());
		stmt.executeUpdate();
		stmt.close ();		


		stmt = conn.prepareStatement (DELETE_BLOCK_SENTIMENTS);
		stmt.setInt (1, project.getId ());
		stmt.executeUpdate();
		stmt.close ();		


		stmt = conn.prepareStatement (DELETE_SENTIMENTS);
		stmt.setInt (1, project.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}


	//
	// Get Data:
	//

	private PreparedStatement buildPreparedStmt (Query queryConfig, Map<String, Object> vars, Connection conn) throws SemanticException, SQLException {
		assert (conn != null);
		assert (queryConfig != null);
		assert (vars != null);

		LinkedList<Query.VariableSegment> paramOrder = new LinkedList<Query.VariableSegment> ();
		String query = queryConfig.getQuery (vars.keySet (), paramOrder);
		if (printTemplates == true) {	
			System.out.println (queryConfig.getStart ().getFile ().getName () + ": " + queryConfig.getStart ().toString () + "-" + queryConfig.getEnd () + ": " + queryConfig.toString ());
			for (Map.Entry<String, Object> var : vars.entrySet ()) {
				System.out.println (" - " + var.getKey () + " = " + var.getValue ());
			}
			System.out.println ();
		}

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
				resSetDate (stmt, i, (Date) val);
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
		assert (conn != null);
		assert (query != null);
		assert (vars != null);
		
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
				resSetDate (stmt, i, (Date) val);
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

	public BugStats getBugStats (Project proj, int identifier) throws SQLException {
		assert (proj != null);
		assert (conn != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_BUG_STATS);
		stmt.setInt (1, identifier);
		stmt.setInt (2, proj.getId ());

		// Collect bug stats:
		ResultSet res = stmt.executeQuery ();
		if (!res.next ()) {
			return null;
		}

		int bugId = res.getInt (1);
		int cmntCnt = res.getInt (2);
		int histCnt = res.getInt (3);
		int attCnt  = res.getInt (4);
		int ccCnt  = res.getInt (5);
		int blocksCnt  = res.getInt (6);
		int aliasCnt = res.getInt (7);
		int severityHistoryCnt = res.getInt (8);
		int priorityCnt = res.getInt (9);
		int statusCnt = res.getInt (10);
		int resolutionCnt = res.getInt (11);
		int confirmedCnt = res.getInt (12);
		int versionHistoCnt = res.getInt (13);
		int operatingSystemCnt = res.getInt (14);

		
		// Statement:
		stmt = conn.prepareStatement (SELECT_ATTACHMENT_STATS);
		stmt.setInt (1, bugId);

		// Collect attachment stats:
		res = stmt.executeQuery ();
		HashMap<Integer, BugAttachmentStats> attStats = new HashMap<Integer, BugAttachmentStats> ();
		while (res.next ()) {
			int attId = res.getInt (1);
			int attIdentifier = res.getInt (2);
			int attObsCnt = res.getInt (3); 
			int attHistCnt = res.getInt (4);
			attStats.put (attIdentifier, new BugAttachmentStats (attId, attIdentifier, attObsCnt, attHistCnt));
		}

		return new BugStats (bugId, cmntCnt, histCnt, attCnt, ccCnt, blocksCnt, aliasCnt, severityHistoryCnt, priorityCnt,
				statusCnt, resolutionCnt, confirmedCnt, versionHistoCnt, operatingSystemCnt, attStats);
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
		assert (conn != null);
		assert (trendChartPlotConfig != null);
		assert (vars != null);

		Query queryConfig = trendChartPlotConfig.getDataQuery ();
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
		assert (conn != null);
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
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
	}
	
	private OptionListConfigData getOptionListData (OptionListConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (conn != null);
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
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
	}
	
	public PieChartData getPieChart (PieChartConfig config, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (conn != null);
		assert (config != null);
		assert (vars != null);

		Query queryConfig = config.getQuery();
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
	}
	
	public DistributionChartData getDistributionChartData (Query query, Map<String, Object> vars) throws SemanticException, SQLException {
		assert (conn != null);
		assert (query != null);
		assert (vars != null);


		double[][] data = new double[12][7];

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
		
		return new DistributionChartData (data);
	}

	public void foreachCommit (Project proj, ObjectCallback<Commit> callback) throws SQLException, Exception {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		assert (callback != null);

		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMMITS);
		stmt.setInt (1, proj.getId ());
	
		// Execution:
		ResultSet res = stmt.executeQuery ();
		boolean do_next = true;
		while (res.next () && do_next) {
			Integer id = res.getInt (1);
			Date date = resGetDate (res, 2);
			String title = res.getString (3);
			Integer linesAdded = res.getInt (4);
			Integer linesRemoved = res.getInt (5);
			User authorUser = userFromResult (res, proj, 9, 10);
			Identity author = identityFromResult (res, authorUser, 20, 6, 17, 7, 8);
			User committerUser = userFromResult (res, proj, 14, 15);
			Identity committer = identityFromResult (res, committerUser, 21, 11, 18, 12, 13);
			Integer changedFiles = res.getInt (14);
			String identifier = res.getString (19);
		
			Commit commit = new Commit (id, identifier, proj, author, committer, date, title, changedFiles, linesAdded, linesRemoved);
			do_next = callback.processResult (commit);
		}
	
		res.close ();
	}

	public void foreachBug (Project proj, ObjectCallback<Bug> callback) throws SQLException, Exception {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		assert (callback != null);

		// Cache some values:
		Map<Integer, Severity> severities = getSeverities (proj);
		//Map<Integer, Status> statuses = getStatuses (proj);
		Map<Integer, Priority> priorities = getPriorities (proj);
		Map<Integer, Component> components = getComponents (proj);
		Map<Integer, Resolution> resolutions = getResolutions (proj);
		Map<Integer, Version> versions = getVersions (proj);
		Map<Integer, OperatingSystem> operatingSystems = getOperatingSystems (proj);

		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUGS);
		stmt.setInt (1, proj.getId ());
	
		// Execution:
		ResultSet res = stmt.executeQuery ();
		boolean do_next = true;
		while (res.next () && do_next) {
			User user = null;
			Identity identity = null;

			Integer id = res.getInt (1);
			Integer identifier = res.getInt (2);
			if (res.getInt (3) != 0) {
				user = userFromResult (res, proj, 6, 7);
				identity = identityFromResult (res, user, 16, 3, 15, 4, 5);
			}
			Component component = components.get (res.getInt (8));
			String title = res.getString (9);
			Date creation = resGetDate (res, 10);
			Priority priority = priorities.get (res.getInt (11));
			Severity severity = severities.get (res.getInt (12));
			Resolution resolution = resolutions.get (res.getInt (17));
			Date lastChange = resGetDate (res, 18);
			Version version = versions.get (res.getInt (19));
			OperatingSystem operatingSystem = operatingSystems.get (res.getInt (20));

			Bug bug = new Bug (id, identifier, identity, component,
				title, creation, lastChange, priority, severity, resolution,
				version, operatingSystem);

			do_next = callback.processResult (bug);
		}
	
		res.close ();
	}

	public Bug getBug (Project proj, Integer identifier) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		assert (identifier != null);

		PreparedStatement stmt = conn.prepareStatement (SELECT_BUG);
		stmt.setInt (1, proj.getId ());
		stmt.setInt (2, identifier);
		

		// Execution:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			int id = res.getInt (1);
			String title = res.getString (8);
			Date creation = resGetDate (res, 9);

			User user = null;
			Identity identity = null;
			if (res.getInt (2) != 0) {
				user = userFromResult (res, proj, 5, 6);
				identity = identityFromResult (res, user, 17, 2, 12, 3, 4);
			}

			int compId = res.getInt (7);
			String compName = res.getString (13);
			Component component = new Component (compId, proj, compName);

			int prioId = res.getInt (10);
			String prioName = res.getString (14);
			Priority priority = new Priority (prioId, proj, prioName);

			int sevId = res.getInt (11);
			String sevName = res.getString (15);
			Severity severity = new Severity (sevId, proj, sevName);

			int resId = res.getInt (18);
			String resName = res.getString (19);
			Resolution resolution = new Resolution (resId, proj, resName);

			Date lastChange = resGetDate (res, 20);

			int versId = res.getInt (21);
			String versName = res.getString (22);
			Version version = new Version (versId, proj, versName);

			int osId = res.getInt (23);
			String osName = res.getString (24);
			OperatingSystem operatingSystem = new OperatingSystem (osId, proj, osName);

			return new Bug (id, identifier, identity, component,
				title, creation, lastChange, priority, severity,
				resolution, version, operatingSystem);
		}

		return null;
	}
	
	private User userFromResult (ResultSet res, Project proj, int idCol, int nameCol) throws SQLException {
		Integer id = res.getInt (idCol);
		String name = res.getString (nameCol);
		return new User (id, proj, name);
	}

	private Identity identityFromResult (ResultSet res, User user, int identifierCol, int idCol, int contextCol, int nameCol, int mailCol) throws SQLException {
		Integer id = res.getInt (idCol);
		Integer identifier = res.getInt (identifierCol);
		String mail = res.getString (mailCol);
		String name = res.getString (nameCol);
		String context = res.getString (contextCol);
		return new Identity (id, identifier, context, mail, name, user);
	}

	public void rawForeach (Query query, Map<String, Object> vars, ResultCallback callback) throws SQLException, Exception {
		assert (conn != null);
		assert (query != null);
		assert (vars != null);
		assert (callback != null);

		PreparedStatement stmt = buildPreparedStmt (query, vars, conn);
	
		// Execution:
		ResultSet res = stmt.executeQuery ();
		callback.processResult (res);
	}

	public void rawForeach (String query, Map<String, Object> vars, ResultCallback callback) throws SQLException, Exception {
		assert (conn != null);
		assert (query != null);
		assert (callback != null);

		PreparedStatement stmt = buildPreparedStmt (query, vars, conn);
		
		// Execution:
		ResultSet res = stmt.executeQuery ();
		callback.processResult (res);
	}

	public Map<Integer, Category> getCategories (Dictionary dictionary) throws SQLException {
		assert (conn != null);
		assert (dictionary != null);
		assert (dictionary.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_CATEGORIES);
		stmt.setInt (1, dictionary.getId ());
	
		// Collect data:
		HashMap<Integer, Category> categories = new HashMap<Integer, Category> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Category category = new Category (res.getInt (1), res.getString (2), dictionary);
			categories.put (category.getId (), category);
		}
	
		return categories;
	}

	public Map<Integer, Component> getComponents (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

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
	}

	public Map<Integer, Resolution> getResolutions (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_RESOLUTIONS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, Resolution> resolutions = new HashMap<Integer, Resolution> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Resolution category = new Resolution (res.getInt (1), proj, res.getString (2));
			resolutions.put (category.getId (), category);
		}
	
		return resolutions;		
	}

	public Map<Integer, Version> getVersions (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_VERSIONS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, Version> versions = new HashMap<Integer, Version> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Version version = new Version (res.getInt (1), proj, res.getString (2));
			versions.put (version.getId (), version);
		}
	
		return versions;		
	}
	
	public Map<String, Component> getComponentsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMPONENTS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Component> components = new HashMap<String, Component> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Component category = new Component (res.getInt (1), proj, res.getString (2));
			components.put (category.getName (), category);
		}
	
		return components;
	}

	public Map<String, OperatingSystem> getOperatingSystemsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_OPERATING_SYSTEMS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, OperatingSystem> opsys = new HashMap<String, OperatingSystem> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			OperatingSystem os = new OperatingSystem (res.getInt (1), proj, res.getString (2));
			opsys.put (os.getName (), os);
		}
	
		return opsys;
	}

	public Map<String, Version> getVersionsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_VERSIONS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Version> versions = new HashMap<String, Version> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Version version = new Version (res.getInt (1), proj, res.getString (2));
			versions.put (version.getName (), version);
		}
	
		return versions;
	}

	public Map<String, AttachmentStatus> getAttachmentStatusByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ATTACHMENT_STATES);
		stmt.setInt (1, proj.getId ());

		// Collect data:
		HashMap<String, AttachmentStatus> stats = new HashMap<String, AttachmentStatus> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			AttachmentStatus stat = new AttachmentStatus (res.getInt (1), proj, res.getString (2));
			stats.put (stat.getName (), stat);
		}
	
		return stats;
	} 

	public Map<Integer, Severity> getSeverities (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

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
	}

	public Map<Integer, OperatingSystem> getOperatingSystems (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_OPERATING_SYSTEMS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, OperatingSystem> opsys = new HashMap<Integer, OperatingSystem> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			OperatingSystem os = new OperatingSystem (res.getInt (1), proj, res.getString (2));
			opsys.put (os.getId (), os);
		}
	
		return opsys;
	}
	
	public Map<String, Severity> getSeveritiesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_SEVERITIES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Severity> severities = new HashMap<String, Severity> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Severity severity = new Severity (res.getInt (1), proj, res.getString (2));
			severities.put (severity.getName (), severity);
		}
	
		return severities;
	}

	public Map<Integer, Status> getStatuses (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

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
	}

	public Map<String, Status> getStatusesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_STATUSES);
		stmt.setInt (1, proj.getId ());

		// Collect data:
		HashMap<String, Status> statuses = new HashMap<String, Status> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Status status = new Status (res.getInt (1), proj, res.getString (2));
			statuses.put (status.getName (), status);
		}
	
		return statuses;
	}

	public Map<Integer, Priority> getPriorities (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

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
	}

	public Map<String, Priority> getPrioritiesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PRIORITIES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Priority> priorities = new HashMap<String, Priority> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Priority priority = new Priority (res.getInt (1), proj, res.getString (2));
			priorities.put (priority.getName (), priority);
		}
	
		return priorities;
	}

	public Map<String, Resolution> getResolutionsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_RESOLUTIONS);
		stmt.setInt (1, proj.getId ());

		// Collect data:
		HashMap<String, Resolution> resolutions = new HashMap<String, Resolution> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Resolution resolution = new Resolution (res.getInt (1), proj, res.getString (2));
			resolutions.put (resolution.getName (), resolution);
		}
	
		return resolutions;
	}

	public List<BugHistory> getBugHistory (Project proj, Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

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
			Identity identity = identityFromResult (res, user, 11, 4, 10, 5, 6);
			Date creation = resGetDate (res, 9);

			BugHistory entry = new BugHistory (id, bug, status, identity, creation);
			history.add (entry);
		}
	
		return history;
	}
	
	public List<Comment> getComments (Project proj, Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_COMMENTS);
		stmt.setInt (1, bug.getId ());
	
		// Collect data:
		LinkedList<Comment> comments = new LinkedList<Comment> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Integer id = res.getInt (1);
			Integer index = res.getInt (2);
			Date creation = resGetDate (res, 3);
			User user = userFromResult (res, proj, 7, 8);
			Identity identity = identityFromResult (res, user, 11, 4, 10, 5, 6);
			String content = res.getString (9);

			Comment comment = new Comment (id, index, bug, creation, identity, content);
			comments.add (comment);
		}
	
		return comments;
	}

	public Comment getCommentByIndex (Project proj, Bug bug, int index) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (index >= 0);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_COMMENT_BY_INDEX);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, index);

		// Collect data:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			Integer id = res.getInt (1);
			Date creation = resGetDate (res, 2);
			User user = userFromResult (res, proj, 6, 7);
			Identity identity = identityFromResult (res, user, 10, 3, 9, 4, 5);
			String content = res.getString (8);

			return new Comment (id, index, bug, creation, identity, content);
		}

		return null;
	}

	public Collection<Identity> getIdentities (Project proj, String context) throws SQLException {
		assert (conn != null);
		assert (context != null);
		assert (proj != null);
		assert (proj.getId () != null);
		
		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_IDENTITIES_CONTEXT);
		stmt.setInt (1, proj.getId ());
		stmt.setString (2, context);
	
		// Collect data:
		LinkedList<Identity> identities = new LinkedList<Identity> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			User user = userFromResult (res, proj, 1, 2);
			Identity identity = identityFromResult (res, user, 8, 3, 4, 6, 5);
			identities.add (identity);
		}
	
		return identities;
	}

	public Identity getIdentityByIdentifier (Project proj, int identifier, String context) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		assert (context != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_IDENTITY_BY_IDENTIFIER);
		stmt.setInt (1, proj.getId ());
		stmt.setString (2, context);
		stmt.setInt (3, identifier);
		
		// Collect data:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			User user = userFromResult (res, proj, 1, 2);
			return identityFromResult (res, user, 8, 3, 4, 6, 5);
		}
	
		return null;
	}
	
	public Identity getIdentityForAttachmentIdentifier (Project proj, String patchId) throws SQLException {
		assert (patchId != null);
		assert (proj != null);
		assert (proj.getId () != null);

		
		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ATTACHMENT_IDENTITY);
		stmt.setInt (1, proj.getId ());
		stmt.setString (2, patchId);

		
		// Collect data:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			User user = userFromResult (res, proj, 1, 2);
			Identity identity = identityFromResult (res, user, 7, 3, 4, 5, 6);
			return identity;
		}
	
		return null;
	}
	
	public Collection<Identity> getIdentities (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		
		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_IDENTITIES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		LinkedList<Identity> identities = new LinkedList<Identity> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			User user = userFromResult (res, proj, 1, 2);
			Identity identity = identityFromResult (res, user, 8, 3, 4, 6, 5);
			identities.add (identity);
		}
	
		return identities;
	}

	public List<User> getUsers (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		
		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_USERS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		LinkedList<User> identities = new LinkedList<User> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			User user = userFromResult (res, proj, 1, 2);
			identities.add (user);
		}
	
		return identities;
	}

	public Project getProject (int id) throws SQLException {
		assert (conn != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_PROJECT);
		stmt.setInt (1, id);
	
		// Collect data:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			Date date = resGetDate (res, 2);
			String domain = res.getString (3);
			String product = res.getString (4);
			String revision = res.getString (5);
			Date lastBugDate = resGetDate (res, 6);
			String bugTracker = res.getString (7);
	
			Project proj = new Project (id, date, lastBugDate, bugTracker, domain, product, revision);
			return proj;
		}

		return null;
	}
	
	public List<Project> getProjects () throws SQLException {
		assert (conn != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PROJECTS);
	
		// Collect data:
		LinkedList<Project> projects = new LinkedList<Project> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Integer id = res.getInt (1);
			Date date = resGetDate (res, 2);
			String domain = res.getString (3);
			String product = res.getString (4);
			String revision = res.getString (5);
			Date lastBugDate = resGetDate (res, 6);
			String bugTracker = res.getString (7);
			
			Project proj = new Project (id, date, lastBugDate, bugTracker, domain, product, revision);
			projects.add (proj);
		}
	
		return projects;
	}

	public List<Dictionary> getDictionaries (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_DICTIONARIES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		LinkedList<Dictionary> dictionaries = new LinkedList<Dictionary> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Integer id = res.getInt (1);
			String name = res.getString (2);
			String context = res.getString (3);
	
			Dictionary dict = new Dictionary (id, name, context, proj);
			dictionaries.add (dict);
		}
	
		return dictionaries;
	}

	public Date getStartDate (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);
		
		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_START_DATE);
		stmt.setInt (1, proj.getId ());

		
		// Collect data:
		ResultSet res = stmt.executeQuery ();
		if (res.next ()) {
			return resGetDate (res, 1);
		}
	
		return new Date ();
	}

	public List<Integer> getUserSelection () throws SQLException {
		assert (conn != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_SELECTED_USERS);

		// Collect data:
		LinkedList<Integer> users = new LinkedList<Integer> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			users.add (res.getInt (1));
		}
		
		return users;
	}

	public void updateUserSelection (List<Integer> selectedUsers) throws SQLException {
		assert (conn != null);
		assert (selectedUsers != null);

		// Drop old selections:
		PreparedStatement stmt = conn.prepareStatement (DELETE_SELECTED_USERS);
		stmt.executeUpdate();
		stmt.close ();
		
		// Insert new selections:
		stmt = conn.prepareStatement (SELECTED_USER_INSERTION);
		for (Integer id : selectedUsers) {
			stmt.setInt (1, id);
			stmt.executeUpdate();
		}

		stmt.close ();
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

	private Date resGetDate (ResultSet res, int pos) throws SQLException {
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
	    try {
			return formatter.parse (res.getString (pos));
		} catch (ParseException e) {
			// TODO
			return null;
		}
	}

	private void resSetDate (PreparedStatement stmt, int pos, Date date) throws SQLException {
		if (date != null) {
			stmt.setString (pos, dateFormat.format (date));
		} else {
			stmt.setNull (pos, Types.VARCHAR);
		}
	}
	
	private void createTables () throws SQLException {
		assert (conn != null);

		Statement stmt = conn.createStatement();
		stmt.executeUpdate (PROJECT_TABLE);
		stmt.executeUpdate (USER_TABLE);
		stmt.executeUpdate (IDENTITY_TABLE);
		stmt.executeUpdate (INTERACTION_TABLE);
		stmt.executeUpdate (COMPONENT_TABLE);
		stmt.executeUpdate (PRIORITY_TABLE);
		stmt.executeUpdate (RESOLUTION_TABLE);
		stmt.executeUpdate (SEVERITY_TABLE);
		stmt.executeUpdate (VERSION_TABLE);
		stmt.executeUpdate (OPERATING_SYSTEM_TABLE);
		stmt.executeUpdate (BUG_TABLE);
		stmt.executeUpdate (PRIORITY_HISTORY_TABLE);
		stmt.executeUpdate (SEVERITY_HISTORY_TABLE);
		stmt.executeUpdate (RESOLUTION_HISTORY_TABLE);
		stmt.executeUpdate (CONFIRMED_HISTORY_TABLE);
		stmt.executeUpdate (VERSION_HISTORY_TABLE);
		stmt.executeUpdate (OPERATING_SYSTEM_HISTORY_TABLE);
		stmt.executeUpdate (BUG_ALIASES);
		stmt.executeUpdate (COMMENT_TABLE);
		stmt.executeUpdate (STATUS_TABLE);
		stmt.executeUpdate (BUG_HISTORY_TABLE);
		stmt.executeUpdate (STATUS_HISTORY_TABLE);
		stmt.executeUpdate (CC_TABLE);
		stmt.executeUpdate (BUG_BLOCKS_TABLE);
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
		stmt.executeUpdate (ATTACHMENT_TABLE);
		stmt.executeUpdate (ATTACHMENT_STATUS_TABLE);
		stmt.executeUpdate (ATTACHMENT_HISTORY_TABLE);
		stmt.executeUpdate (BUG_CATEGORIES_TABLE);
		stmt.executeUpdate (COMMIT_CATEGORIES_TABLE);
		stmt.executeUpdate (DICTIONARY_TABLE);
		//stmt.executeUpdate (ATTACHMENT_REPLACEMENT_TABLE);
		stmt.executeUpdate (SENTENCE_SENTIMENT_TABLE);
		stmt.executeUpdate (BLOCK_SENTIMENT_TABLE);
		stmt.executeUpdate (SENTIMENT_TABLE);
		stmt.executeUpdate (SELECTED_USER_TABLE);
		stmt.executeUpdate (ATTACHMENT_ISOBSOLETE_TABLE);
		stmt.close ();
	}
}
