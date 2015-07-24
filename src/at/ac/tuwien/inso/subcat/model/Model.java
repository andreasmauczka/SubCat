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
	public static final String FLAG_BUG_ATTACHMENT_DETAILS = "BUG_ATTACHMENT_DETAILS";

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

	private static final String SOCIAL_STATS_TABLE =
		"CREATE TABLE IF NOT EXISTS SocialStats ("
		+ "src             INTEGER     NOT NULL,"
		+ "dest            INTEGER     NOT NULL,"
		+ "quotations      INTEGER     NOT NULL,"
		+ "patchesReviewed INTEGER     NOT NULL,"
		+ "PRIMARY KEY (src, dest),"
		+ "FOREIGN KEY(src) REFERENCES Identities (id),"
		+ "FOREIGN KEY(dest) REFERENCES Identities (id)"
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

	private static final String BUG_GROUP_TABLE =
		"CREATE TABLE IF NOT EXISTS BugGroups ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String MILESTONE_TABLE =
		"CREATE TABLE IF NOT EXISTS Milestones ("
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

	private static final String BUG_CLASS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugClasses ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String BUG_FLAG_STATUS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugFlagStatuses ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String BUG_FLAG_TABLE =
		"CREATE TABLE IF NOT EXISTS BugFlags ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "identifier	INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "typeId		INT									NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, identifier),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String BUG_FLAG_ASSIGNMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugFlagAssignments ("
		+ "bug				INT			NOT NULL,"
		+ "flag				INT			NOT NULL,"
		+ "creationDate		TEXT		NOT NULL,"
		+ "modificationDate	TEXT		NOT NULL,"
		+ "status			INT			NOT NULL,"
		+ "setter			INT			NOT NULL,"
		+ "requestee		INT					,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(flag) REFERENCES BugFlags (id),"
		+ "FOREIGN KEY(status) REFERENCES BugFlagStatuses (id),"
		+ "FOREIGN KEY(setter) REFERENCES Identities (id),"
		+ "FOREIGN KEY(requestee) REFERENCES Identities (id),"
		+ "PRIMARY KEY (bug, flag)"
		+ ")";

	private static final String BUG_ATTACHMENT_FLAG_ASSIGNMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugAttachmentFlagAssignments ("
		+ "attachment		INT			NOT NULL,"
		+ "flag				INT			NOT NULL,"
		+ "creationDate		TEXT		NOT NULL,"
		+ "modificationDate	TEXT		NOT NULL,"
		+ "status			INT			NOT NULL,"
		+ "setter			INT			NOT NULL,"
		+ "requestee		INT					,"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id),"
		+ "FOREIGN KEY(flag) REFERENCES BugFlags (id),"
		+ "FOREIGN KEY(status) REFERENCES BugFlagStatuses (id),"
		+ "FOREIGN KEY(setter) REFERENCES Identities (id),"
		+ "FOREIGN KEY(requestee) REFERENCES Identities (id),"
		+ "PRIMARY KEY (attachment, flag)"
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

	private static final String PLATFORMS_TABLE =
		"CREATE TABLE IF NOT EXISTS Platforms ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (project, name)"
		+ ")";

	private static final String KEYWORD_TABLE =
		"CREATE TABLE IF NOT EXISTS Keywords ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "project		INT									NOT NULL,"
		+ "name			TEXT								NOT NULL,"
		+ "FOREIGN KEY(project) REFERENCES Projects (id),"
		+ "UNIQUE (name)"
		+ ")";

	private static final String KEYWORD_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS KeywordHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "keyword		INT									NOT NULL,"
		+ "removed		BOOL								NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(keyword) REFERENCES Keywords (id)"
		+ ")";

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
		+ "platform			INT									NOT NULL,"
		+ "version			INT									NOT NULL,"
		+ "targetMilestone	INT									NOT NULL,"
		+ "comments			INT									NOT NULL DEFAULT 0,"
		+ "status			INT									NOT NULL,"
		+ "classification	INT											,"
		+ "isOpen			INT									NOT NULL,"
		+ "FOREIGN KEY(priority) REFERENCES Priorities (id),"
		+ "FOREIGN KEY(severity) REFERENCES Severity (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(component) REFERENCES Components (id),"
		+ "FOREIGN KEY(resolution) REFERENCES Resolutions (id),"
		+ "FOREIGN KEY(version) REFERENCES Versions (id),"
		+ "FOREIGN KEY(targetMilestone) REFERENCES Milestones (id),"
		+ "FOREIGN KEY(operatingSystem) REFERENCES OperatingSystems (id),"
		+ "FOREIGN KEY(platform) REFERENCES Platforms (id),"
		+ "FOREIGN KEY(status) REFERENCES Status (id),"
		+ "FOREIGN KEY(classification) REFERENCES BugClasses (id)"
		+ ")";

	private static final String BUG_KEYWORDS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugKeywords ("
		+ "bug			INT				NOT NULL,"
		+ "keyword		INT						,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(keyword) REFERENCES Keywords (id),"
		+ "PRIMARY KEY (bug, keyword)"
		+ ")";

	private static final String BUG_DEADLINE_TABLE =
		"CREATE TABLE IF NOT EXISTS BugDeadlines ("
		+ "bug			INT PRIMARY KEY						NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";

	private static final String BUG_DUPLICATION_TABLE =
		"CREATE TABLE IF NOT EXISTS BugDuplications ("
		+ "bug						INT		NOT NULL,"
		+ "duplicationIdentifier	INT		NOT NULL,"
		+ "duplication				INT				,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(duplication) REFERENCES Bugs (id),"
		+ "PRIMARY KEY (bug, duplicationIdentifier)"
		+ ")";

	private static final String BUG_QA_CONTACT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugQaContacts ("
		+ "bug			INT PRIMARY KEY					NOT NULL,"
		+ "identity		INT										,"
		+ "bugGroup		INT										,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bugGroup) REFERENCES BugGroups (id)"
		+ ")";

	private static final String BUG_BLOCKS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugBlocks ("
		+ "bug			INT				NOT NULL,"
		+ "identifier	INT				NOT NULL,"
		+ "blocks		INT						,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(blocks) REFERENCES Bugs (id),"
		+ "PRIMARY KEY (bug, identifier)"
		+ ")";

	private static final String BUG_CC_TABLE =
		"CREATE TABLE IF NOT EXISTS BugCc ("
		+ "bug			INT				NOT NULL,"
		+ "identity		INT				NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "PRIMARY KEY (bug, identity)"
		+ ")";

	private static final String BUG_SEE_ALSO_TABLE =
		"CREATE TABLE IF NOT EXISTS BugSeeAlso ("
		+ "bug		INT				NOT NULL,"
		+ "link		TEXT			NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "PRIMARY KEY (bug, link)"
		+ ")";

	private static final String BUG_GROUPS_TABLE =
		"CREATE TABLE IF NOT EXISTS BugGroupMemberships ("
		+ "bug			INT				NOT NULL,"
		+ "bugGroup		INT				NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(bugGroup) REFERENCES BugGroups (id),"
		+ "PRIMARY KEY (bug, bugGroup)"
		+ ")";

	private static final String BUG_DEPENDS_ON_TABLE =
		"CREATE TABLE IF NOT EXISTS BugDependsOn ("
		+ "bug			INT				NOT NULL,"
		+ "identifier	INT				NOT NULL,"
		+ "dependsOn	INT						,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(dependsOn) REFERENCES Bugs (id),"
		+ "PRIMARY KEY (bug, identifier)"
		+ ")";

	private static final String VERSION_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS VersionHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldVersion	INT									NOT NULL,"
		+ "newVersion	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldVersion) REFERENCES Versions (id),"
		+ "FOREIGN KEY(newVersion) REFERENCES Versions (id)"
		+ ")";

	private static final String ASSIGNED_TO_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS AssignedToHistory ("
		+ "id					INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug					INT									NOT NULL,"
		+ "addedBy				INT									NOT NULL,"
		+ "date					TEXT								NOT NULL,"
		+ "identifierAdded		TEXT										,"
		+ "groupAdded			INT											,"
		+ "identityAdded		INT											,"
		+ "identifierRemoved	TEXT										,"
		+ "groupRemoved			INT											,"
		+ "identityRemoved		INT											,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(identityAdded) REFERENCES Identities (id),"
		+ "FOREIGN KEY(groupAdded) REFERENCES BugGroups (id),"
		+ "FOREIGN KEY(identityRemoved) REFERENCES Identities (id),"
		+ "FOREIGN KEY(groupRemoved) REFERENCES BugGroups (id)"
		+ ")";

	private static final String QA_CONTACT_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS QaContactHistory ("
		+ "id					INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug					INT									NOT NULL,"
		+ "addedBy				INT									NOT NULL,"
		+ "date					TEXT								NOT NULL,"
		+ "identifierAdded		TEXT										,"
		+ "groupAdded			INT											,"
		+ "identityAdded		INT											,"
		+ "identifierRemoved	TEXT										,"
		+ "groupRemoved			INT											,"
		+ "identityRemoved		INT											,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(identityAdded) REFERENCES Identities (id),"
		+ "FOREIGN KEY(groupAdded) REFERENCES BugGroups (id),"
		+ "FOREIGN KEY(identityRemoved) REFERENCES Identities (id),"
		+ "FOREIGN KEY(groupRemoved) REFERENCES BugGroups (id)"
		+ ")";

	private static final String MILESTONE_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS MilestoneHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldMilestone	INT									NOT NULL,"
		+ "newMilestone	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldMilestone) REFERENCES Milestones (id),"
		+ "FOREIGN KEY(newMilestone) REFERENCES Milestones (id)"
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
		+ "FOREIGN KEY(oldOperatingSystem) REFERENCES OperatingSystems (id),"
		+ "FOREIGN KEY(newOperatingSystem) REFERENCES OperatingSystems (id)"
		+ ")";

	private static final String PRIORITY_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS PriorityHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldPriority	INT									NOT NULL,"
		+ "newPriority	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldPriority) REFERENCES Priorities (id),"
		+ "FOREIGN KEY(newPriority) REFERENCES Priorities (id)"
		+ ")";

	private static final String SEVERITY_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS SeverityHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldSeverity	INT									NOT NULL,"
		+ "newSeverity	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldSeverity) REFERENCES Severity (id),"
		+ "FOREIGN KEY(newSeverity) REFERENCES Severity (id)"
		+ ")";

	private static final String STATUS_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS StatusHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "addedBy		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldStatus	INT									NOT NULL,"
		+ "newStatus	INT									NOT NULL,"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldStatus) REFERENCES Status (id),"
		+ "FOREIGN KEY(newStatus) REFERENCES Status (id)"
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

	private static final String ATTACHMENT_DETAIL_TABLE =
		"CREATE TABLE IF NOT EXISTS AttachmentDetails ("
		+ "attachment			INT		PRIMARY KEY	NOT NULL,"
		+ "data					BLOB						,"
		+ "attCreationTime		TEXT				NOT NULL,"
		+ "lastchangeTime		TEXT				NOT NULL,"
		+ "fileName				TEXT				NOT NULL,"
		+ "summary				TEXT				NOT NULL,"
		+ "isPrivate			BOOL				NOT NULL,"
		+ "isObsolete			BOOL				NOT NULL,"
		+ "isPatch				BOOL				NOT NULL,"
		+ "creator				INT					NOT NULL,"
		+ "contentType			TEXT				NOT NULL,"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id),"
		+ "FOREIGN KEY(creator) REFERENCES Identities (id)"
		+ ")";

	private static final String ATTACHMENT_ISOBSOLETE_TABLE =
		"CREATE TABLE IF NOT EXISTS ObsoleteAttachments ("
		+ "id			INTEGER PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "attachment	INTEGER								NOT NULL,"
		+ "identity		INTEGER								NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldValue		INTEGER								NOT NULL,"
		+ "newValue		INTEGER								NOT NULL,"
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

	private static final String ATTACHMENT_STATUS_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS AttachmentStatusHistory ("
		+ "id			INTEGER	PRIMARY KEY AUTOINCREMENT	NOT NULL,"
		+ "attachment	INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "oldStatus	INT									NOT NULL,"
		+ "newStatus	INT									NOT NULL,"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id),"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(oldStatus) REFERENCES AttachmentStatus (id),"
		+ "FOREIGN KEY(newStatus) REFERENCES AttachmentStatus (id)"
		+ ")";

	private static final String ATTACHMENT_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS AttachmentHistory ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "attachment	INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "field		TEXT								NOT NULL,"
		+ "oldValue		TEXT										,"
		+ "newValue		TEXT										,"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id)"
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

	private static final String BUG_DUPLICATION_COMMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugDuplicationComments ("
		+ "comment		INTEGER		NOT NULL,"
		+ "identifier	INTEGER		NOT NULL,"
		+ "duplication	INTEGER				,"
		+ "FOREIGN KEY(comment) REFERENCES Comments (id),"
		+ "FOREIGN KEY(duplication) REFERENCES Bugs (id)"
		+ ")";

	private static final String BUG_ATTACHMENT_REVIEW_COMMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugAttachmentReviewComments ("
		+ "comment		INTEGER		NOT NULL,"
		+ "attachment	INTEGER		NOT NULL,"
		+ "FOREIGN KEY(comment) REFERENCES Comments (id),"
		+ "FOREIGN KEY(attachment) REFERENCES Attachments (id)"
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
		"CREATE TABLE IF NOT EXISTS BugHistory ("
		+ "id			INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "bug			INT									NOT NULL,"
		+ "identity		INT									NOT NULL,"
		+ "date			TEXT								NOT NULL,"
		+ "field		TEXT								NOT NULL,"
		+ "oldValue		TEXT										,"
		+ "newValue		TEXT										,"
		+ "FOREIGN KEY(identity) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id)"
		+ ")";

	private static final String CC_TABLE =
		"CREATE TABLE IF NOT EXISTS BugCcHistory ("
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

	private static final String BUG_BLOCKS_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS BugBlocksHistory ("
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

	private static final String BUG_DEPENDENCY_HISTORY_TABLE =
		"CREATE TABLE IF NOT EXISTS BugDependencyHistory ("
		+ "id					INTEGER	PRIMARY KEY	AUTOINCREMENT	NOT NULL,"
		+ "date					TEXT								NOT NULL,"
		+ "bug					INT									NOT NULL,"
		+ "depends				INT									        ,"
		+ "dependsIdentifier	INT									NOT NULL,"
		+ "addedBy				INT									NOT NULL,"
		+ "removed				BOOLEAN								NOT NULL,"
		+ "FOREIGN KEY(addedBy) REFERENCES Identities (id),"
		+ "FOREIGN KEY(bug) REFERENCES Bugs (id),"
		+ "FOREIGN KEY(depends) REFERENCES Bugs (id)"
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
		+ "FOREIGN KEY(sentimentId) REFERENCES Sentiment (id)"
		+ ")";

	private static final String SENTIMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS Sentiment ("
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
		+ "sentences				INTEGER		NOT NULL"
		+ ")";

	private static final String BUG_COMMENT_SENTIMENT_TABLE =
		"CREATE TABLE IF NOT EXISTS BugCommentSentiment ("
		+ "sentimentId	INTEGER		NOT NULL,"
		+ "commentId	INTEGER		NOT NULL,"
		+ "FOREIGN KEY(sentimentId) REFERENCES Sentiment (id),"
		+ "FOREIGN KEY(commentId) REFERENCES Comment (id),"
		+ "PRIMARY KEY (sentimentId, commentId)"
		+ ")";

	
	//
	// Triggers:
	//

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
		+ " (SELECT COUNT() FROM BugHistory WHERE BugHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM Attachments, Comments WHERE Attachments.comment = Comments.id AND Comments.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugCcHistory WHERE BugCcHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugBlocksHistory WHERE BugBlocksHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugAliases WHERE BugAliases.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM SeverityHistory WHERE SeverityHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM PriorityHistory WHERE PriorityHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM StatusHistory WHERE StatusHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM ResolutionHistory WHERE ResolutionHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM ConfirmedHistory WHERE ConfirmedHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM VersionHistory WHERE VersionHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM OperatingSystemHistory WHERE OperatingSystemHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM BugDependencyHistory WHERE BugDependencyHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM KeywordHistory WHERE KeywordHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM MilestoneHistory WHERE MilestoneHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM AssignedToHistory WHERE AssignedToHistory.bug = Bugs.id),"
		+ " (SELECT COUNT() FROM QaContactHistory WHERE QaContactHistory.bug = Bugs.id)"
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
		+ " (SELECT COUNT() FROM AttachmentStatusHistory WHERE AttachmentStatusHistory.attachment = Attachments.id),"
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
		+ " Bugs.status, "
		+ " Identity.context		AS aiContext, "
		+ " Identity.identifier,"
		+ " Bugs.resolution,"
		+ " Bugs.lastChange,"
		+ " Bugs.version,"
		+ " Bugs.operatingSystem,"
		+ " Bugs.platform,"
		+ " Bugs.targetMilestone,"
		+ " Bugs.classification,"
		+ " Bugs.isOpen "
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
		+ " Bugs.version,"
		+ " Versions.name,"
		+ " OperatingSystems.id,"
		+ " OperatingSystems.name,"
		+ " Bugs.status, "
		+ " Status.name,"
		+ " Bugs.platform,"
		+ " Platforms.name,"
		+ " Bugs.targetMilestone,"
		+ " Milestones.name,"
		+ " Bugs.classification,"
		+ " BugClasses.name,"
		+ " Bugs.isOpen "
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
		+ "JOIN Status"
		+ " ON Status.id = Bugs.status "
		+ "JOIN Versions"
		+ " ON Versions.id = Bugs.version "
		+ "JOIN Resolutions"
		+ " ON Resolutions.id = Bugs.resolution "
		+ "JOIN OperatingSystems"
		+ " ON OperatingSystems.id = Bugs.operatingSystem "
		+ "JOIN Platforms"
		+ " ON Platforms.id = Bugs.platform "
		+ "JOIN Milestones"
		+ " ON Milestones.id = Bugs.targetMilestone "
		+ "LEFT JOIN BugClasses "
		+ " ON BugClasses.id = Bugs.classification "
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

	private static final String SELECT_ALL_BUG_FLAGS_STATES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " BugFlagStatuses "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_BUG_FLAGS =
		"SELECT"
		+ " id,"
		+ " identifier,"
		+ " name,"
		+ " typeId "
		+ "FROM"
		+ " BugFlags "
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

	private static final String SELECT_ALL_PLATFORMS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Platforms "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_KEYWORDS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Keywords "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_BUG_CLASSES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " BugClasses "
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

	private static final String SELECT_ALL_BUG_GROUPS =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " BugGroups "
		+ "WHERE"
		+ " project = ?";

	private static final String SELECT_ALL_MILESTONES =
		"SELECT"
		+ " id,"
		+ " name "
		+ "FROM"
		+ " Milestones "
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

	private static final String SELECT_SENTIMENT_STATES =
		"SELECT "
		+ "(SELECT count(*) FROM Comments, Bugs, Components, BugCommentSentiment WHERE Comments.bug = Bugs.id AND Bugs.component = Components.id AND BugCommentSentiment.commentId = Comments.id AND project = ?)";

	private static final String SELECT_FULL_HISTORY =
		"SELECT"
		+ " BugHistory.id,"
		+ " Identity.id,"
		+ " Identity.name,"
		+ " Identity.mail,"
		+ " Users.id,"
		+ " Users.name,"
		+ " BugHistory.date,"
		+ " BugHistory.field,"
		+ " BugHistory.oldValue,"
		+ " BugHistory.newValue,"
		+ " Identity.context,"
		+ " Identity.identifier "
		+ "FROM"
		+ " BugHistory "
		+ "LEFT JOIN Identities Identity"
		+ " ON BugHistory.identity = Identity.id "
		+ "LEFT JOIN Users "
		+ " ON Users.id = Identity.user "
		+ "WHERE"
		+ " BugHistory.bug = ? "
		+ "ORDER BY"
		+ " BugHistory.id";

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

	// TODO: rename to ObsoleteAttachmentsHistory
	private static final String OBSOLETE_ATTACHMENT_INSERTION =
		"INSERT INTO ObsoleteAttachments "
		+ "(attachment, identity, date, oldValue, newValue) "
		+ "VALUES (?, ?, ?, ?, ?);";

	private static final String ATTACHMENT_DETAIL_INSERTION =
		"INSERT INTO AttachmentDetails "
		+ "(attachment, data, attCreationTime, lastchangeTime, fileName, summary, isPrivate, isObsolete, isPatch, creator, contentType) "
		+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

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

	private static final String ATTACHMENT_DETAIL_UPDATE =
		"Update AttachmentDetails SET "
		+ " data = ?,"
		+ " attCreationTime = ?,"
		+ " lastchangeTime = ?,"
		+ " fileName = ?,"
		+ " summary = ?,"
		+ " isPrivate = ?,"
		+ " isObsolete = ?,"
		+ " isPatch = ?,"
		+ " creator = ?,"
		+ " contentType = ?"
		+ "WHERE"
		+ " attachment = ?";
	
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

	private static final String INSERT_OR_REPLACE_SOCIAL_STATS =
		"INSERT OR REPLACE INTO SocialStats (src, dest, quotations, patchesReviewed)"
		+ "VALUES ("
		+ " ?,"
		+ " ?,"
		+ " COALESCE((SELECT quotations FROM SocialStats WHERE src = ? AND dest = ?), 0) + ?,"
		+ " COALESCE((SELECT patchesReviewed FROM SocialStats WHERE src = ? AND dest = ?), 0) + ?"
		+ ")";
	
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

	private static final String BUG_GROUP_INSERTION =
		"INSERT INTO BugGroups"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String MILESTONE_INSERTION =
		"INSERT INTO Milestones"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String CATEGORY_INSERTION =
		"INSERT INTO Categories"
		+ "(name, dictionary)"
		+ "VALUES (?,?)";

	private static final String BUG_INSERTION =
		"INSERT INTO Bugs "
		+ "(identifier, identity, component, title, creation, priority, severity, status, resolution, lastChange, version, operatingSystem, platform, targetMilestone, classification, isOpen)"
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String BUG_CC_INSERTION =
		"INSERT INTO BugCc"
		+ "(bug, identity)"
		+ "VALUES (?,?)";

	private static final String BUG_SEE_ALSO_INSERTION =
		"INSERT INTO BugSeeAlso"
		+ "(bug, link)"
		+ "VALUES (?,?)";

	private static final String BUG_GROUP_MEMBERSHIP_INSERTION =
		"INSERT INTO BugGroupMemberships"
		+ "(bug, bugGroup)"
		+ "VALUES (?,?)";

	private static final String BUG_DEADLINE_INSERTION =
		"INSERT INTO BugDeadlines"
		+ "(bug, date)"
		+ "VALUES (?, ?)";

	private static final String BUG_DUPLICATION_INSERTION =
		"INSERT INTO BugDuplications"
		+ "(bug, duplicationIdentifier)"
		+ "VALUES (?, ?)";

	private static final String BUG_QA_CONTACT_INSERTION =
		"INSERT INTO BugQaContacts"
		+ "(bug, identity, bugGroup)"
		+ "VALUES (?, ?, ?)";
		
	private static final String BUG_UPDATE =
		"UPDATE Bugs SET"
		+ " identifier = ?,"
		+ " identity = ?,"
		+ " component = ?,"
		+ " title = ?,"
		+ " creation = ?,"
		+ " priority = ?,"
		+ " severity = ?,"
		+ " status = ?,"
		+ " resolution = ?,"
		+ " lastChange = ?,"
		+ " version = ?,"
		+ " operatingSystem = ?,"
		+ " platform = ?,"
		+ " targetMilestone = ?,"
		+ " classification = ?"
		+ "WHERE"
		+ " id = ?";
	
	private static final String BUG_ALIAS_INSERTION =
		"INSERT INTO BugAliases"
		+ "(bug, addedBy, date, alias)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String PRIORITY_HISTORY_INSERTION =
		"INSERT INTO PriorityHistory"
		+ "(bug, addedBy, date, oldPriority, newPriority)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String ASSIGNED_TO_HISTORY_INSERTION =
		"INSERT INTO AssignedToHistory"
		+ "(bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityRemoved)"
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String QA_CONTACT_HISTORY_INSERTION =
		"INSERT INTO QaContactHistory"
		+ "(bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityRemoved)"
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String VERSION_HISTORY_INSERTION =
		"INSERT INTO VersionHistory"
		+ "(bug, addedBy, date, oldVersion, newVersion)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String MILESTONE_HISTORY_INSERTION =
		"INSERT INTO MilestoneHistory"
		+ "(bug, addedBy, date, oldMilestone, newMilestone)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String RESOLUTION_HISTORY_INSERTION =
		"INSERT INTO ResolutionHistory"
		+ "(bug, addedBy, date, resolution)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String BUG_FLAG_ASSIGNMENT_INSERTION =
		"INSERT INTO BugFlagAssignments"
		+ "(bug, flag, creationDate, modificationDate, status, setter, requestee)"
		+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

	private static final String BUG_ATTACHMENT_FLAG_ASSIGNMENT_INSERTION =
		"INSERT INTO BugAttachmentFlagAssignments"
		+ "(attachment, flag, creationDate, modificationDate, status, setter, requestee)"
		+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	private static final String CONFIRMED_HISTORY_INSERTION =
		"INSERT INTO ConfirmedHistory"
		+ "(bug, addedBy, date, removed)"
		+ "VALUES (?, ?, ?, ?)";

	private static final String SEVERITY_HISTORY_INSERTION =
		"INSERT INTO SeverityHistory"
		+ "(bug, addedBy, date, oldSeverity, newSeverity)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String OPERATING_SYSTEM_HISTORY_INSERTION =
		"INSERT INTO OperatingSystemHistory"
		+ "(bug, addedBy, date, oldOperatingSystem, newOperatingSystem)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String KEYWORD_HISTORY_INSERTION =
		"INSERT INTO KeywordHistory"
		+ "(bug, addedBy, date, keyword, removed)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String STATUS_HISTORY_INSERTION =
		"INSERT INTO StatusHistory"
		+ "(bug, addedBy, date, oldStatus, newStatus)"
		+ "VALUES (?, ?, ?, ?, ?)";
	
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

	private static final String ATTACHMENT_STATUS_INSERTION
		= "INSERT INTO AttachmentStatus"
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String ATTACHMENT_STATUS_HISTORY_INSERTION =
		"INSERT INTO AttachmentStatusHistory"
		+ "(attachment, identity, date, oldStatus, newStatus)"
		+ "VALUES (?, ?, ?, ?, ?)";

	private static final String COMMENT_INSERTION =
		"INSERT INTO Comments"
		+ "(bug, pos, creation, identity, content)"
		+ "VALUES (?,?,?,?,?)";	
	
	private static final String STATUS_INSERTION =
		"INSERT INTO Status"
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String BUG_HISTORY_INSERTION =
		"INSERT INTO BugHistory"
		+ "(bug, identity, date, field, oldValue, newValue)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String ATTACHMENT_HISTORY_INSERTION =
		"INSERT INTO AttachmentHistory"
		+ "(attachment, identity, date, field, oldValue, newValue)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_CC_HISTORY_INSERTION =
		"INSERT INTO BugCcHistory"
		+ "(bug, date, addedBy, cc, ccMail, removed)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_BLOCKS_HISTORY_INSERTION =
		"INSERT INTO BugBlocksHistory"
		+ "(bug, date, addedBy, blocks, blocksIdentifier, removed)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_BLOCKS_INSERTION =
		"INSERT INTO BugBlocks"
		+ "(bug, identifier, blocks)"
		+ "VALUES (?,?,?)";

	private static final String BUG_DEPENDS_ON_INSERTION =
		"INSERT INTO BugDependsOn"
		+ "(bug, identifier, dependsOn)"
		+ "VALUES (?,?,?)";

	private static final String BUG_KEYWORD_INSERTION =
		"INSERT INTO BugKeywords"
		+ "(bug, keyword)"
		+ "VALUES (?,?)";
	
	private static final String BUG_BLOCKS_HISTORY_RESOLVE_BUGS =
		"UPDATE"
		+ " BugBlocksHistory "
		+ "SET "
		+ " blocks = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugBlocksHistory.blocksIdentifier AND Components.project = ?) "
		+ "WHERE"
		+ " blocks IS NULL"
		+ " AND bug in (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_DUPLICATION_COMMENT_RESOLVE_BUGS =
		"UPDATE"
		+ " BugDuplicationComments "
		+ "SET "
		+ " duplication = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugDuplicationComments.identifier AND Components.project = ?) "
		+ "WHERE"
		+ " duplication IS NULL"
		+ " AND comment in (SELECT Comments.id FROM Comments, Bugs, Components WHERE Comments.bug = Bugs.id AND Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_BLOCKS_RESOLVE_BUGS =
		"UPDATE"
		+ " BugBlocks "
		+ "SET "
		+ " blocks = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugBlocks.identifier AND Components.project = ?) "
		+ "WHERE"
		+ " blocks IS NULL"
		+ " AND bug in (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_DEPENDS_ON_RESOLVE_BUGS =
		"UPDATE"
		+ " BugDependsOn "
		+ "SET "
		+ " dependsOn = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugDependsOn.identifier AND Components.project = ?) "
		+ "WHERE"
		+ " dependsOn IS NULL"
		+ " AND bug in (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_DUPLICATIONS_RESOLVE_BUGS =
		"UPDATE"
		+ " BugDuplications "
		+ "SET "
		+ " duplication = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugDuplications.duplicationIdentifier AND Components.project = ?) "
		+ "WHERE"
		+ " duplication IS NULL"
		+ " AND bug in (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Components.project = ?)";

	private static final String BUG_DEPENDENCY_HISTORY_INSERTION =
		"INSERT INTO BugDependencyHistory"
		+ "(bug, date, addedBy, depends, dependsIdentifier, removed)"
		+ "VALUES (?,?,?,?,?,?)";

	private static final String BUG_DEPENDENCY_HISTORY_RESOLVE_BUGS =
		"UPDATE"
		+ " BugDependencyHistory "
		+ "SET "
		+ " depends = (SELECT Bugs.id FROM Bugs, Components WHERE Bugs.component = Components.id AND Bugs.identifier = BugDependencyHistory.dependsIdentifier AND Components.project = ?) "
		+ "WHERE"
		+ " depends IS NULL"
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

	private static final String BUG_DUPLICATION_COMMENT_INSERTION =
		"INSERT INTO BugDuplicationComments "
		+ "(comment, identifier)"
		+ "VALUES (?,?)";

	private static final String BUG_ATTACHMENT_REVIEW_COMMENT_INSERTION =
		"INSERT INTO BugAttachmentReviewComments "
		+ "(comment, attachment)"
		+ "VALUES (?,?)";
	
	private static final String BUG_CLASS_INSERTION =
		"INSERT INTO BugClasses "
		+ "(project, name)"
		+ "VALUES (?,?)";
	
	private static final String BUG_FLAG_STATUS_INSERTION =
		"INSERT INTO BugFlagStatuses "
		+ "(project, name)"
		+ "VALUES (?,?)";

	private static final String BUG_FLAG_INSERTION =
		"INSERT INTO BugFlags "
		+ "(project, identifier, name, typeId)"
		+ "VALUES (?,?,?,?)";

	private static final String RESOLUTION_INSERTION =
		"INSERT INTO Resolutions "
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String OPERATING_SYSTEM_INSERTION =
		"INSERT INTO OperatingSystems "
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String PLATFORM_INSERTION =
		"INSERT INTO Platforms "
		+ "(project, name)"
		+ "VALUES (?, ?)";

	private static final String KEYWORD_INSERTION =
		"INSERT INTO Keywords "
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
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?"
		+ ")";

	private static final String SENTIMENT_INSERTION =
		"INSERT INTO Sentiment ("
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
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?,?,?,?,"
		+ "?,?"
		+ ")";

	private static final String BUG_COMMENT_SENTIMENT_INSERTION =
		"INSERT INTO BugCommentSentiment"
		+ "(sentimentId, commentId)"
		+ "VALUES (?, ?)";

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
	
	private static final String DELETE_BUG_DEADLINE =
		"DELETE FROM BugDeadlines WHERE bug = ?";
	
	private static final String DELETE_BUG_BLOCKS =
		"DELETE FROM BugBlocks WHERE bug = ?";

	private static final String DELETE_BUG_CC =
		"DELETE FROM BugCc WHERE bug = ?";

	private static final String DELETE_BUG_SEE_ALSO =
		"DELETE FROM BugSeeAlso WHERE bug = ?";

	private static final String DELETE_BUG_GROUP_MEMBERSHIPS =
		"DELETE FROM BugGroupMemberships WHERE bug = ?";

	private static final String DELETE_BUG_FLAG_ASSIGNMENTS =
		"DELETE FROM BugFlagAssignments WHERE bug = ?";

	private static final String DELETE_BUG_ATTACHMENT_FLAG_ASSIGNMENTS =
		"DELETE FROM BugAttachmentFlagAssignments WHERE attachment = ?";

	private static final String DELETE_BUG_KEYWORDS =
		"DELETE FROM BugKeywords WHERE bug = ?";

	private static final String DELETE_BUG_DEPENDS_ON =
		"DELETE FROM BugDependsOn WHERE bug = ?";

	private static final String DELETE_BUG_DUPLICATIONS =
		"DELETE FROM BugDuplications WHERE bug = ?";

	private static final String DELETE_BUG_QA_CONTACTS =
		"DELETE FROM BugQaContacts WHERE bug = ?";
	
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

	public void setAttachmentIsObsolete (Attachment attachment, Identity identity, Date date, boolean oldValue, boolean newValue) throws SQLException {
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
		stmt.setBoolean (4, oldValue);
		stmt.setBoolean (5, newValue);
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAttachmentIsObsoleteAdded (attachment, identity, date, oldValue, newValue);
	}


	public AttachmentDetails addAttachmentDetails (Attachment attachment, byte[] attData,
			Date attCreationTime, Date attLastchangeTime, String attFileName,
			String attSummary, Boolean attIsPrivate, Boolean attIsObsolete,
			Boolean attIsPatch, Identity attCreator, String attContentType) throws SQLException {

		AttachmentDetails ad = new AttachmentDetails (attachment, attData,
				attCreationTime, attLastchangeTime, attFileName,
				attSummary, attIsPrivate, attIsObsolete,
				attIsPatch, attCreator, attContentType);

		add (ad);
		return ad;
	}

	public void add (AttachmentDetails ad) throws SQLException {
		assert (conn != null);
		assert (ad != null);
		assert (ad.getAttachment ().getId () != null);
		assert (ad.getCreator ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_DETAIL_INSERTION);
		stmt.setInt (1, ad.getAttachment ().getId ());
		stmt.setBytes (2, ad.getData ());
		resSetDate (stmt, 3, ad.getCreationTime ());
		resSetDate (stmt, 4, ad.getLastChangeTime ());
		stmt.setString (5, ad.getFileName ());
		stmt.setString (6, ad.getSummary ());
		stmt.setBoolean (7, ad.getIsPrivate ());
		stmt.setBoolean (8, ad.getIsObsolete ());
		stmt.setBoolean (9, ad.getIsPatch ());
		stmt.setInt (10, ad.getCreator ().getId ());
		stmt.setString (11, ad.getContentType ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAttachmentDetailsAdded (ad);
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

	public void updateAttachmentDetail (AttachmentDetails ad) throws SQLException {
		assert (conn != null);
		assert (ad != null);
		assert (ad.getAttachment ().getId () != null);
		assert (ad.getCreator ().getId () != null);
		
		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_DETAIL_UPDATE);
		stmt.setBytes (1, ad.getData ());
		resSetDate (stmt, 2, ad.getCreationTime ());
		resSetDate (stmt, 3, ad.getLastChangeTime ());
		stmt.setString (4, ad.getFileName ());
		stmt.setString (5, ad.getSummary ());
		stmt.setBoolean (6, ad.getIsPrivate ());
		stmt.setBoolean (7, ad.getIsObsolete ());
		stmt.setBoolean (8, ad.getIsPatch ());
		stmt.setInt (9, ad.getCreator ().getId ());
		stmt.setString (10, ad.getContentType ());
		stmt.setInt (11, ad.getAttachment ().getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAttachmentDetailsUpdated (ad);
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
	
	public void addSocialStats (Identity src, Identity dest, int quotations, int patchesReviewed) throws SQLException {
		assert (src != null && src.getId () != null);
		assert (dest != null && dest.getId () != null);
		assert (quotations >= 0);
		assert (patchesReviewed >= 0);

		PreparedStatement stmt = conn.prepareStatement (INSERT_OR_REPLACE_SOCIAL_STATS);
		stmt.setInt (1, src.getId ());
		
		stmt.setInt (2, dest.getId ());

		stmt.setInt (3, src.getId ());
		stmt.setInt (4, dest.getId ());
		stmt.setInt (5, quotations);

		stmt.setInt (6, src.getId ());
		stmt.setInt (7, dest.getId ());
		stmt.setInt (8, patchesReviewed);
		
		stmt.executeUpdate();
		stmt.close ();

		pool.emitSocialStatsAdded (src, dest, quotations);
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

	public void addBugDuplicationComment (Comment comment, Integer identifier) throws SQLException {
		assert (conn != null);
		assert (comment != null);
		assert (comment.getId () != null);
		assert (identifier != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DUPLICATION_COMMENT_INSERTION);
		stmt.setInt (1, comment.getId ());
		stmt.setInt (2, identifier);		
		stmt.executeUpdate();
		stmt.close ();		

		pool.emitBugDuplicationCommentAdded (comment, identifier);
	}

	public void addBugAttachmentReviewComment (Comment comment, Attachment attachment) throws SQLException {
		assert (conn != null);
		assert (comment != null);
		assert (comment.getId () != null);
		assert (attachment != null);
		assert (attachment.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_ATTACHMENT_REVIEW_COMMENT_INSERTION);
		stmt.setInt (1, comment.getId ());
		stmt.setInt (2, attachment.getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugAttachmentReviewCommentAdded (comment, attachment);
	}

	public BugClass addBugClass (Project project, String name) throws SQLException {
		BugClass bc = new BugClass (null, project, name);
		add (bc);
		return bc;		
	}

	public void add (BugClass bc) throws SQLException {
		assert (conn != null);
		assert (bc != null);
		Project project = bc.getProject ();
		assert (project.getId () != null);
		assert (bc.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CLASS_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, bc.getName ());
		stmt.executeUpdate();

		bc.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitBugClassAdded (bc);
	}
	
	public BugFlagStatus addBugFlagStatus (Project project, String name) throws SQLException {
		BugFlagStatus status = new BugFlagStatus (null, project, name);
		add (status);
		return status;
	}

	public void add (BugFlagStatus status) throws SQLException {
		assert (conn != null);
		assert (status != null);
		Project project = status.getProject ();
		assert (project.getId () != null);
		assert (status.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (BUG_FLAG_STATUS_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, status.getName ());
		stmt.executeUpdate();

		status.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitBugFlagStatusAdded (status);
	}

	public BugFlag addBugFlag (Project proj, Integer identifier, String name, Integer typeId) throws SQLException {
		BugFlag flag = new BugFlag (null, proj, identifier, name, typeId);
		add (flag);
		return flag;
	}
	
	public void add (BugFlag flag) throws SQLException {
		assert (conn != null);
		assert (flag != null);
		Project project = flag.getProject ();
		assert (project.getId () != null);
		assert (flag.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (BUG_FLAG_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setInt (2, flag.getIdentifier ());
		stmt.setString (3, flag.getName ());
		stmt.setInt (4, flag.getTypeId ());
		stmt.executeUpdate();

		flag.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitBugFlagAdded (flag);
	}

	public void addBugFlagAssignments (Bug bug, BugFlagAssignment[] flags) throws SQLException {
		if (flags == null) {
			return ;
		}

		_addBugFlagAssignments (bug, flags);
		pool.emitBugFlagAssignmentsAdded (bug, flags);
	}

	public void updateBugFlagAssignments (Bug bug, BugFlagAssignment[] flags) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (flags != null);

		_removeBugFlagAssignments (bug);
		if (flags != null) {
			_addBugFlagAssignments (bug, flags);
		}		

		pool.emitBugFlagAssignmentsUpdated (bug, flags);
	}

	private void _addBugFlagAssignments (Bug bug, BugFlagAssignment[] flags) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (flags != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_FLAG_ASSIGNMENT_INSERTION);

		for (BugFlagAssignment flag : flags) {
			assert (flag != null);
			assert (flag.getFlag () != null);
			assert (flag.getStatus () != null);
			assert (flag.getSetter () != null);
			assert (flag.getFlag ().getId () != null);
			assert (flag.getStatus ().getId () != null);
			assert (flag.getSetter ().getId () != null);

			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, flag.getFlag ().getId ());
			resSetDate (stmt, 3, flag.getCreationDate ());
			resSetDate (stmt, 4, flag.getModificationDate ());
			stmt.setInt (5, flag.getStatus ().getId ());
			stmt.setInt (6, flag.getSetter ().getId ());
			if (flag.getRequestee () != null) {
				stmt.setInt (7, flag.getRequestee ().getId ());
			} else {
				stmt.setNull (7, Types.INTEGER);
			}

			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugFlagAssignments (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_FLAG_ASSIGNMENTS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}
	
	public void addBugAttachmentFlagAssignments (Attachment attachment, BugFlagAssignment[] flags) throws SQLException {
		if (flags == null) {
			return ;
		}

		_addBugAttachmentFlagAssignments (attachment, flags);
		pool.emitBugAttachmentFlagAssignmentsAdded (attachment, flags);
	}

	public void updateBugAttachmentFlagAssignments (Attachment attachment, BugFlagAssignment[] flags) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);
		assert (flags != null);

		_removeBugAttachmentFlagAssignments (attachment);
		if (flags != null) {
			_addBugAttachmentFlagAssignments (attachment, flags);
		}		

		pool.emitBugAttachmentFlagAssignmentsUpdated (attachment, flags);
	}

	private void _addBugAttachmentFlagAssignments (Attachment attachment, BugFlagAssignment[] flags) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);
		assert (flags != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_ATTACHMENT_FLAG_ASSIGNMENT_INSERTION);

		for (BugFlagAssignment flag : flags) {
			assert (flag != null);
			assert (flag.getFlag () != null);
			assert (flag.getStatus () != null);
			assert (flag.getSetter () != null);
			assert (flag.getFlag ().getId () != null);
			assert (flag.getStatus ().getId () != null);
			assert (flag.getSetter ().getId () != null);

			stmt.setInt (1, attachment.getId ());
			stmt.setInt (2, flag.getFlag ().getId ());
			resSetDate (stmt, 3, flag.getCreationDate ());
			resSetDate (stmt, 4, flag.getModificationDate ());
			stmt.setInt (5, flag.getStatus ().getId ());
			stmt.setInt (6, flag.getSetter ().getId ());
			if (flag.getRequestee () != null) {
				stmt.setInt (7, flag.getRequestee ().getId ());
			} else {
				stmt.setNull (7, Types.INTEGER);
			}

			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugAttachmentFlagAssignments (Attachment attachment) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_ATTACHMENT_FLAG_ASSIGNMENTS);

		stmt.setInt (1, attachment.getId ());
		stmt.executeUpdate();
		stmt.close ();
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
	
	public Platform addPlatform (Project project, String name) throws SQLException {
		Platform pf = new Platform (null, project, name);
		add (pf);
		return pf;
	}

	public void add (Platform pf) throws SQLException {
		assert (conn != null);
		assert (pf != null);
		Project project = pf.getProject ();
		assert (project.getId () != null);
		assert (pf.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (PLATFORM_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, pf.getName ());
		stmt.executeUpdate();

		pf.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitPlatformAdded (pf);
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

	public Keyword addKeyword (Project project, String name) throws SQLException {
		Keyword kw = new Keyword (null, project, name);
		add (kw);
		return kw;
	}

	public void add (Keyword keyword) throws SQLException {
		assert (conn != null);
		assert (keyword != null);
		Project project = keyword.getProject ();
		assert (project.getId () != null);
		assert (keyword.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (KEYWORD_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, keyword.getName ());
		stmt.executeUpdate();

		keyword.setId (getLastInsertedId (stmt));

		stmt.close ();		

		pool.emitKeywordAdded (keyword);
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

	public BugGroup addBugGroup (Project project, String name) throws SQLException {
		BugGroup grp = new BugGroup (null, project, name);
		add (grp);
		return grp;
	}

	public void add (BugGroup grp) throws SQLException {
		assert (conn != null);
		assert (grp != null);
		Project project = grp.getProject ();
		assert (project.getId () != null);
		assert (grp.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (BUG_GROUP_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, grp.getName ());
		stmt.executeUpdate();

		grp.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitBugGroupAdded (grp);
	}

	public Milestone addMilestone (Project project, String name) throws SQLException {
		Milestone ms = new Milestone (null, project, name);
		add (ms);
		return ms;
	}

	public void add (Milestone ms) throws SQLException {
		assert (conn != null);
		assert (ms != null);
		Project project = ms.getProject ();
		assert (project.getId () != null);
		assert (ms.getId () == null);

		PreparedStatement stmt = conn.prepareStatement (MILESTONE_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, project.getId ());
		stmt.setString(2, ms.getName ());
		stmt.executeUpdate();

		ms.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitMilestoneAdded (ms);
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
	
	public AttachmentStatusHistory addAttachmentStatusHistory (Attachment attachment, Identity identity, Date date, AttachmentStatus oldStatus, AttachmentStatus newStatus) throws SQLException {
		AttachmentStatusHistory histo = new AttachmentStatusHistory (null, attachment, identity, date, oldStatus, newStatus);
		add (histo);
		return histo;
	}

	public void add (AttachmentStatusHistory history) throws SQLException {
		assert (conn != null);
		assert (history != null);
		assert (history.getAttachment () != null);
		assert (history.getOldStatus () != null);
		assert (history.getNewStatus () != null);
		assert (history.getIdentity () != null);
		assert (history.getAttachment ().getId () != null);
		assert (history.getOldStatus ().getId () != null);
		assert (history.getNewStatus ().getId () != null);
		assert (history.getIdentity ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_STATUS_HISTORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, history.getAttachment ().getId ());
		stmt.setInt (2, history.getIdentity ().getId ());
		resSetDate (stmt, 3, history.getDate ());
		stmt.setInt (4, history.getOldStatus ().getId ());
		stmt.setInt (5, history.getNewStatus ().getId ());
		stmt.executeUpdate();

		history.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitAttachmentStatusHistoryAdded (history);
	}

	public Bug addBug (Integer identifier, Identity identity, Component component, String title, 
			Date creation, Date lastChange, Priority priority, Severity severity, Status status, 
			Resolution resolution, Version version, Milestone targetMilestone, 
			OperatingSystem operatingSystem, Platform platform, BugClass classification,
			Boolean isOpen) throws SQLException
	{
		Bug bug = new Bug (null, identifier, identity, component, title, creation, lastChange, priority, severity, status, resolution, version, targetMilestone, operatingSystem, platform, classification, isOpen);
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
		assert (bug.getStatus ().getId () != null);
		assert (bug.getResolution () != null);
		assert (bug.getResolution ().getId () != null);
		assert (bug.getVersion () != null);
		assert (bug.getVersion ().getId () != null);
		assert (bug.getOperatingSystem () != null);
		assert (bug.getOperatingSystem ().getId () != null);
		assert (bug.getPlatform () != null);
		assert (bug.getPlatform ().getId () != null);
		assert (bug.getTargetMilestone () != null);
		assert (bug.getTargetMilestone ().getId () != null);

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
		stmt.setInt (8, bug.getStatus ().getId ());
		stmt.setInt (9, bug.getResolution ().getId ());
		resSetDate (stmt, 10, bug.getLastChange ());
		stmt.setInt (11, bug.getVersion ().getId ());
		stmt.setInt (12, bug.getOperatingSystem ().getId ());
		stmt.setInt (13, bug.getPlatform ().getId ());
		stmt.setInt (14, bug.getTargetMilestone ().getId ());
		if (bug.getClassification () != null) {
			stmt.setInt (15, bug.getClassification ().getId ());
		} else {
			stmt.setNull (15, Types.INTEGER);
		}
		stmt.setBoolean (16, bug.getIsOpen ());
		stmt.executeUpdate();

		bug.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitBugAdded (bug);
	}

	public void addBugDeadline (Bug bug, Date deadline) throws SQLException {
		if (deadline == null) {
			return ;
		}

		_addBugDeadline (bug, deadline);
		pool.emitBugDeadlineAdded (bug, deadline);
	}

	private void _addBugDeadline (Bug bug, Date deadline) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (deadline != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DEADLINE_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getId ());
		resSetDate (stmt, 2, deadline);
		stmt.executeUpdate();
		stmt.close ();
	}

	private void _addBugGroupMemberships (Bug bug, BugGroup[] groups) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (groups != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_GROUP_MEMBERSHIP_INSERTION);

		for (BugGroup grp : groups) {
			assert (grp != null);
			assert (grp.getId () != null);

			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, grp.getId ());
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugGroupMemberships (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_GROUP_MEMBERSHIPS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugGroupMemberships (Bug bug, BugGroup[] groups) throws SQLException {
		if (groups == null) {
			return ;
		}

		_addBugGroupMemberships (bug, groups);
		pool.emitBugGroupMembershipsAdded (bug, groups);
	}

	public void updateBugGroupMemberships (Bug bug, BugGroup[] groups) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (groups != null);

		_removeBugGroupMemberships (bug);
		if (groups != null) {
			_addBugGroupMemberships (bug, groups);
		}		

		pool.emitBugGroupMembershipsUpdated (bug, groups);
	}

	private void _addBugSeeAlso (Bug bug, String[] links) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (links != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_SEE_ALSO_INSERTION);

		for (String link : links) {
			assert (link != null);

			stmt.setInt (1, bug.getId ());
			stmt.setString (2, link);
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugSeeAlso (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_SEE_ALSO);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugSeeAlso (Bug bug, String[] links) throws SQLException {
		if (links == null) {
			return ;
		}

		_addBugSeeAlso (bug, links);
		pool.emitBugSeeAlsoAdded (bug, links);
	}

	public void updateBugSeeAlso (Bug bug, String[] links) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (links != null);

		_removeBugSeeAlso (bug);
		if (links != null) {
			_addBugSeeAlso (bug, links);
		}		

		pool.emitBugSeeAlsoUpdated (bug, links);
	}

	private void _addBugCc (Bug bug, Identity[] identities) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (identities != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CC_INSERTION);

		for (Identity id : identities) {
			assert (id != null);
			assert (id.getId () != null);

			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, id.getId ());
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugCc (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_CC);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugCc (Bug bug, Identity[] identities) throws SQLException {
		if (identities == null) {
			return ;
		}

		_addBugCc (bug, identities);
		pool.emitBugCcAdded (bug, identities);
	}

	public void updateBugCc (Bug bug, Identity[] identities) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (identities != null);

		_removeBugCc (bug);
		if (identities != null) {
			_addBugCc (bug, identities);
		}		

		pool.emitBugCcUpdated (bug, identities);
	}

	private void _addBugKeywords (Bug bug, Keyword[] keywords) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (keywords != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_KEYWORD_INSERTION);

		for (Keyword kw : keywords) {
			assert (kw != null);
			assert (kw.getId () != null);

			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, kw.getId ());
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugKeywords (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_KEYWORDS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugKeywords (Bug bug, Keyword[] keywords) throws SQLException {
		if (keywords == null) {
			return ;
		}

		_addBugKeywords (bug, keywords);
		pool.emitBugKeywordsAdded (bug, keywords);
	}

	public void updateBugKeywords (Bug bug, Keyword[] keywords) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (keywords != null);

		_removeBugKeywords (bug);
		if (keywords != null) {
			_addBugKeywords (bug, keywords);
		}		

		pool.emitBugKeywordsUpdated (bug, keywords);
	}

	private void _addBugDependsOn (Bug bug, Integer[] dependsOn) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (dependsOn != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DEPENDS_ON_INSERTION);

		for (Integer id : dependsOn) {
			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, id);
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugDependsOn (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_DEPENDS_ON);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugDependsOn (Bug bug, Integer[] dependsOn) throws SQLException {
		if (dependsOn == null) {
			return ;
		}

		_addBugDependsOn (bug, dependsOn);
		pool.emitBugDependsOnAdded (bug, dependsOn);
	}

	public void updateBugDependsOn (Bug bug, Integer[] dependsOn) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (dependsOn != null);

		_removeBugDependsOn (bug);
		if (dependsOn != null) {
			_addBugDependsOn (bug, dependsOn);
		}		

		pool.emitBugDependsOnUpdated (bug, dependsOn);
	}

	private void _addBugDuplication (Bug bug, Integer duplication) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (duplication != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DUPLICATION_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, duplication);
		stmt.executeUpdate();
		stmt.close ();
	}

	private void _removeBugDuplication (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_DUPLICATIONS,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugDuplication (Bug bug, Integer duplication) throws SQLException {
		if (duplication == null) {
			return ;
		}

		_addBugDuplication (bug, duplication);
		pool.emitBugDuplicationAdded (bug, duplication);
	}

	public void updateBugDuplication (Bug bug, Integer duplication) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		_removeBugDuplication (bug);
		if (duplication != null) {
			_addBugDuplication (bug, duplication);
		}		

		pool.emitBugDuplicationUpdated (bug, duplication);
	}

	private void _removeBugDeadline (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_DEADLINE,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void updateBugDeadline (Bug bug, Date deadline) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		_removeBugDeadline (bug);
		if (deadline != null) {
			_addBugDeadline (bug, deadline);
		}		

		pool.emitBugDeadlineUpdated (bug, deadline);
	}
	
	private void _addBugBlocks (Bug bug, Integer[] blocks) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (blocks != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_BLOCKS_INSERTION);

		for (Integer id : blocks) {
			stmt.setInt (1, bug.getId ());
			stmt.setInt (2, id);
			stmt.executeUpdate ();
		}

		stmt.close ();
	}

	private void _removeBugBlocks (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_BLOCKS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}

	public void addBugBlocks (Bug bug, Integer[] blocks) throws SQLException {
		if (blocks == null) {
			return ;
		}

		_addBugBlocks (bug, blocks);
		pool.emitBugBlocksAdded (bug, blocks);
	}

	public void updateBugBlocks (Bug bug, Integer[] blocks) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		_removeBugBlocks (bug);
		if (blocks != null) {
			_addBugBlocks (bug, blocks);
		}		

		pool.emitBugBlocksUpdated (bug, blocks);
	}

	public void updateBug (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (bug.getIdentifier () != null);
		assert (bug.getComponent () != null);
		assert (bug.getPriority () != null);
		assert (bug.getSeverity() != null);
		assert (bug.getStatus () != null);
		assert (bug.getResolution () != null);
		assert (bug.getLastChange () != null);
		assert (bug.getVersion () != null);
		assert (bug.getOperatingSystem () != null);
		assert (bug.getPlatform () != null);
		assert (bug.getTargetMilestone () != null);
		assert (bug.getIdentity () == null || bug.getIdentity ().getId () != null);
		assert (bug.getComponent ().getId () != null);
		assert (bug.getPriority ().getId () != null);
		assert (bug.getSeverity().getId () != null);
		assert (bug.getStatus ().getId () != null);
		assert (bug.getResolution ().getId () != null);
		assert (bug.getVersion ().getId () != null);
		assert (bug.getOperatingSystem ().getId () != null);
		assert (bug.getPlatform ().getId () != null);
		assert (bug.getTargetMilestone ().getId () != null);
		assert (bug.getClassification () == null || bug.getClassification ().getId () != null);

		
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
		stmt.setInt (8, bug.getStatus ().getId ());
		stmt.setInt (9, bug.getResolution ().getId ());
		resSetDate (stmt, 10, bug.getLastChange ());
		stmt.setInt (11, bug.getVersion ().getId ());
		stmt.setInt (12, bug.getOperatingSystem ().getId ());
		stmt.setInt (13, bug.getPlatform ().getId ());
		stmt.setInt (14, bug.getTargetMilestone ().getId ());
		if (bug.getClassification () != null) {
			stmt.setInt (15, bug.getClassification ().getId ());
		} else {
			stmt.setInt (15, Types.INTEGER);
		}
		stmt.setInt (16, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
		
		pool.emitBugUpdated (bug);
	}

	public void addBugQaContact (Bug bug, Identity identity, BugGroup group) throws SQLException {
		if (identity == null && group == null) {
			return ;
		}

		_addBugQaContact (bug, identity, group);
		pool.emitBugQaContactAdded (bug, identity, group);
	}

	public void updateBugQaContact (Bug bug, Identity identity, BugGroup group) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (identity != null || group != null);
		assert (identity == null || identity.getId () != null);
		assert (group == null || group.getId () != null);

		_removeBugQaContact (bug);
		if (identity != null || group != null) {
			_addBugQaContact (bug, identity, group);
		}

		pool.emitBugQaContactUpdated (bug, identity, group);
	}

	private void _removeBugQaContact (Bug bug) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (DELETE_BUG_QA_CONTACTS);

		stmt.setInt (1, bug.getId ());
		stmt.executeUpdate();
		stmt.close ();
	}
	
	private void _addBugQaContact (Bug bug, Identity identity, BugGroup group) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (identity != null || group != null);
		assert (identity == null || identity.getId () != null);
		assert (group == null || group.getId () != null);
		
		PreparedStatement stmt = conn.prepareStatement (BUG_QA_CONTACT_INSERTION);

		stmt.setInt (1, bug.getId ());
		if (identity != null) {
			stmt.setInt (2, identity.getId ());
		} else {
			stmt.setNull (2, Types.INTEGER);
		}

		if (group != null) {
			stmt.setInt (3, group.getId ());
		} else {
			stmt.setNull (3, Types.INTEGER);
		}

		stmt.executeUpdate();
		stmt.close ();
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

	public void addPriorityHistory (Bug bug, Identity addedBy, Date date, Priority oldPriority, Priority newPriority) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldPriority != null);
		assert (oldPriority.getId () != null);
		assert (newPriority != null);
		assert (newPriority.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (PRIORITY_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldPriority.getId ());
		stmt.setInt (5, newPriority.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitPriorityHistoryAdded (bug, addedBy, date, oldPriority, newPriority);
	}

	public void addAssigendToHistory (Bug bug, Identity addedBy, Date date, String identifierAdded, BugGroup groupAdded, Identity identityAdded, String identifierRemoved, BugGroup groupRemoved, Identity identityRemoved) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (groupAdded == null || groupAdded.getId () != null);
		assert (identityAdded == null || identityAdded.getId () != null);
		assert (groupRemoved == null || groupRemoved.getId () != null);
		assert (identityRemoved == null || identityRemoved.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (ASSIGNED_TO_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);

		stmt.setString (4, identifierAdded);
		if (groupAdded != null) {
			stmt.setInt (5, groupAdded.getId ());			
		} else {
			stmt.setNull (5, Types.INTEGER);
		}
		if (identityAdded != null) {
			stmt.setInt (6, identityAdded.getId ());			
		} else {
			stmt.setNull (6, Types.INTEGER);
		}

		stmt.setString (7, identifierRemoved);
		if (groupRemoved != null) {
			stmt.setInt (8, groupRemoved.getId ());			
		} else {
			stmt.setNull (8, Types.INTEGER);
		}
		if (identityRemoved != null) {
			stmt.setInt (9, identityRemoved.getId ());			
		} else {
			stmt.setNull (9, Types.INTEGER);
		}
		
		stmt.executeUpdate();
		stmt.close ();

		pool.emitAssignedToHistoryAdded (bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityAdded);
	}	

	public void addQaContactHistory (Bug bug, Identity addedBy, Date date, String identifierAdded, BugGroup groupAdded, Identity identityAdded, String identifierRemoved, BugGroup groupRemoved, Identity identityRemoved) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (groupAdded == null || groupAdded.getId () != null);
		assert (identityAdded == null || identityAdded.getId () != null);
		assert (groupRemoved == null || groupRemoved.getId () != null);
		assert (identityRemoved == null || identityRemoved.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (QA_CONTACT_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);

		stmt.setString (4, identifierAdded);
		if (groupAdded != null) {
			stmt.setInt (5, groupAdded.getId ());			
		} else {
			stmt.setNull (5, Types.INTEGER);
		}
		if (identityAdded != null) {
			stmt.setInt (6, identityAdded.getId ());			
		} else {
			stmt.setNull (6, Types.INTEGER);
		}

		stmt.setString (7, identifierRemoved);
		if (groupRemoved != null) {
			stmt.setInt (8, groupRemoved.getId ());			
		} else {
			stmt.setNull (8, Types.INTEGER);
		}
		if (identityRemoved != null) {
			stmt.setInt (9, identityRemoved.getId ());			
		} else {
			stmt.setNull (9, Types.INTEGER);
		}
		
		stmt.executeUpdate();
		stmt.close ();

		pool.emitQaContactHistoryAdded (bug, addedBy, date, identifierAdded, groupAdded, identityAdded, identifierRemoved, groupRemoved, identityAdded);
	}	

	public void addVersionHistory (Bug bug, Identity addedBy, Date date, Version oldVersion, Version newVersion) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldVersion != null);
		assert (oldVersion.getId () != null);
		assert (newVersion != null);
		assert (newVersion.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (VERSION_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldVersion.getId ());
		stmt.setInt (5, newVersion.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitVersionHistoryAdded (bug, addedBy, date, oldVersion, newVersion);
	}

	public void addMilestoneHistory (Bug bug, Identity addedBy, Date date, Milestone oldMilestone, Milestone newMilestone) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldMilestone != null);
		assert (oldMilestone.getId () != null);
		assert (newMilestone != null);
		assert (newMilestone.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (MILESTONE_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldMilestone.getId ());
		stmt.setInt (5, newMilestone.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitMilestoneHistoryAdded (bug, addedBy, date, oldMilestone, newMilestone);
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

	public void addSeverityHistory (Bug bug, Identity addedBy, Date date, Severity oldSeverity, Severity newSeverity) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldSeverity != null);
		assert (oldSeverity.getId () != null);
		assert (newSeverity != null);
		assert (newSeverity.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SEVERITY_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldSeverity.getId ());
		stmt.setInt (5, newSeverity.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitSeverityHistoryAdded (bug, addedBy, date, oldSeverity, newSeverity);
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

	public void addKeywordHistory (Bug bug, Identity addedBy, Date date, Keyword keyword, boolean removed) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (keyword != null);
		assert (keyword.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (KEYWORD_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, keyword.getId ());
		stmt.setBoolean (5, removed);

		stmt.executeUpdate();
		stmt.close ();

		pool.emitKeywordHistoryAdded (bug, addedBy, date, keyword, removed);
	}

	public void addStatusHistory (Bug bug, Identity addedBy, Date date, Status oldStatus, Status newStatus) throws SQLException {
		assert (bug != null);
		assert (bug.getId () != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (date != null);
		assert (oldStatus != null);
		assert (oldStatus.getId () != null);
		assert (newStatus != null);
		assert (newStatus.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (STATUS_HISTORY_INSERTION);
		stmt.setInt (1, bug.getId ());
		stmt.setInt (2, addedBy.getId ());
		resSetDate (stmt, 3, date);
		stmt.setInt (4, oldStatus.getId ());
		stmt.setInt (5, newStatus.getId ());

		stmt.executeUpdate();
		stmt.close ();

		pool.emitStatusHistoryAdded (bug, addedBy, date, oldStatus, newStatus);
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
	
	public BugHistory addBugHistory (Bug bug, Identity identity, Date date, String fieldName, String oldValue, String newValue) throws SQLException {
		BugHistory history = new BugHistory (null, bug, identity, date, fieldName, oldValue, newValue);
		add (history);
		return history;
	}

	public void add (BugHistory history) throws SQLException {
		assert (conn != null);
		assert (history != null);
		assert (history.getId () == null);
		assert (history.getBug ().getId () != null);
		assert (history.getIdentity ().getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_HISTORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, history.getBug ().getId ());
		stmt.setInt (2, history.getIdentity ().getId ());
		resSetDate (stmt, 3, history.getDate ());
		stmt.setString (4, history.getFieldName ());
		stmt.setString (5, history.getOldValue ());
		stmt.setString (6, history.getNewValue ());
		stmt.executeUpdate();

		history.setId (getLastInsertedId (stmt));

		stmt.close ();

		pool.emitBugHistoryAdded (history);
	}

	public void addAttachmentHistory (Attachment attachment, Identity identity, Date date, String fieldName, String oldValue, String newValue) throws SQLException {
		assert (conn != null);
		assert (attachment != null);
		assert (attachment.getId () != null);
		assert (identity != null);
		assert (identity.getId () != null);
		assert (date != null);
		assert (fieldName != null);

		PreparedStatement stmt = conn.prepareStatement (ATTACHMENT_HISTORY_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, attachment.getId ());
		stmt.setInt (2, identity.getId ());
		resSetDate (stmt, 3, date);
		stmt.setString (4, fieldName);
		stmt.setString (5, oldValue);
		stmt.setString (6, newValue);
		stmt.executeUpdate();

		stmt.close ();

		pool.emitAttachmentHistoryAdded (attachment, identity, date, fieldName, oldValue, newValue);
	}

	public void addBugCcHistory (Bug bug, Date date, Identity addedBy, Identity cc, String ccMail, boolean removed) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (date != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (cc == null || cc.getId () != null);
		assert (ccMail != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_CC_HISTORY_INSERTION);
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

		pool.emitBugCcHistoryAdded (bug, date, addedBy, cc, ccMail, removed);
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

		PreparedStatement stmt = conn.prepareStatement (BUG_BLOCKS_HISTORY_INSERTION);
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

	public void addBugDependency (Bug bug, Date date, Identity addedBy, Bug blocks, int bugIdentifier, boolean removed) throws SQLException {
		assert (conn != null);
		assert (bug != null);
		assert (bug.getId () != null);
		assert (date != null);
		assert (addedBy != null);
		assert (addedBy.getId () != null);
		assert (blocks == null || blocks.getId () != null);
		assert (bugIdentifier > 0);

		PreparedStatement stmt = conn.prepareStatement (BUG_DEPENDENCY_HISTORY_INSERTION);
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

	public void resolveBugBlocksHistoryBugs (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_BLOCKS_HISTORY_RESOLVE_BUGS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, project.getId ());

		stmt.executeUpdate();
		stmt.close ();
	}

	public void resolveDuplicationCommentyBugs (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DUPLICATION_COMMENT_RESOLVE_BUGS);
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

	public void resolveBugDependsOnBugs (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DEPENDS_ON_RESOLVE_BUGS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, project.getId ());

		stmt.executeUpdate();
		stmt.close ();
	}

	public void resolveBugDuplicationsBugs (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DUPLICATIONS_RESOLVE_BUGS);
		stmt.setInt (1, project.getId ());
		stmt.setInt (2, project.getId ());

		stmt.executeUpdate();
		stmt.close ();
	}

	public void resolveBugDependencyHistory (Project project) throws SQLException {
		assert (conn != null);
		assert (project != null);
		assert (project.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_DEPENDENCY_HISTORY_RESOLVE_BUGS);
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
	
	private void addSentimentBlock (int parentId, int blockPos, SentimentBlock sentiment) throws SQLException {
		assert (blockPos >= 0);
		assert (parentId >= 0);
		assert (sentiment != null);

		PreparedStatement stmt = conn.prepareStatement (SENTIMENT_BLOCK_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, parentId);
		stmt.setInt (2, blockPos);


		stmt.setInt (3, sentiment.getNegativeCount ());
		stmt.setInt (4, sentiment.getSomewhatNegativeCount ());
		stmt.setInt (5, sentiment.getNeutralCount ());
		stmt.setInt (6, sentiment.getSomewhatPositiveCount ());
		stmt.setInt (7, sentiment.getPositiveCount ());

		stmt.setDouble (8, sentiment.getNegativeMean ());
		stmt.setDouble (9, sentiment.getSomewhatNegativeMean ());
		stmt.setDouble (10, sentiment.getNeutralMean ());
		stmt.setDouble (11, sentiment.getSomewhatPositiveMean ());
		stmt.setDouble (12, sentiment.getPositiveMean ());

		stmt.setDouble (13, sentiment.getNegativeWMean ());
		stmt.setDouble (14, sentiment.getSomewhatNegativeWMean ());
		stmt.setDouble (15, sentiment.getNeutralWMean ());
		stmt.setDouble (16, sentiment.getSomewhatPositiveWMean ());
		stmt.setDouble (17, sentiment.getPositiveWMean ());

		stmt.setInt (18, sentiment.getWordCount ());
		stmt.setInt (19, sentiment.getSentenceCount ());
		stmt.executeUpdate();

		int newId = getLastInsertedId (stmt);
		stmt.close ();

		addSentenceSentiments (newId, sentiment.getSentenceSentiments ());
	}
	
	public void addSentiment (Sentiment sentiment) throws SQLException {
		assert (sentiment != null);

		PreparedStatement stmt = conn.prepareStatement (SENTIMENT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, sentiment.getNegativeCount ());
		stmt.setInt (2, sentiment.getSomewhatNegativeCount ());
		stmt.setInt (3, sentiment.getNeutralCount ());
		stmt.setInt (4, sentiment.getSomewhatPositiveCount ());
		stmt.setInt (5, sentiment.getPositiveCount ());

		stmt.setDouble (6, sentiment.getNegativeMean ());
		stmt.setDouble (7, sentiment.getSomewhatNegativeMean ());
		stmt.setDouble (8, sentiment.getNeutralMean ());
		stmt.setDouble (9, sentiment.getSomewhatPositiveMean ());
		stmt.setDouble (10, sentiment.getPositiveMean ());

		stmt.setDouble (11, sentiment.getNegativeWMean ());
		stmt.setDouble (12, sentiment.getSomewhatNegativeWMean ());
		stmt.setDouble (13, sentiment.getNeutralWMean ());
		stmt.setDouble (14, sentiment.getSomewhatPositiveWMean ());
		stmt.setDouble (15, sentiment.getPositiveWMean ());

		stmt.setInt (16, sentiment.getWordCount ());
		stmt.setInt (17, sentiment.getSentenceCount ());
		stmt.executeUpdate();

		int newId = getLastInsertedId (stmt);
		sentiment.setId (newId);
		stmt.close ();

		int i = 0;
		for (SentimentBlock block : sentiment.getBlocks ()) {
			addSentimentBlock (newId, i, block);
			i++;
		}
		
		pool.emitSentimentAdded (sentiment);
	}

	public void addBugCommentSentiment (Comment comment, Sentiment sentiment) throws SQLException {
		assert (comment != null);
		assert (comment.getId () != null);
		assert (sentiment != null);
		assert (sentiment.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (BUG_COMMENT_SENTIMENT_INSERTION,
				Statement.RETURN_GENERATED_KEYS);

		stmt.setInt (1, sentiment.getId ());
		stmt.setInt (2, comment.getId ());
		stmt.executeUpdate();
		stmt.close ();

		pool.emitBugCommentSentimentAdded (comment, sentiment);
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
		int dependsCnt = res.getInt (15);
		int keywordCnt = res.getInt (16);
		int milestoneCnt = res.getInt (17);
		int assignedToCnt = res.getInt (18);
		int qaContactCnt = res.getInt (19);


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
			int attStatHistCnt = res.getInt (4);
			int attHistCnt = res.getInt (5);

			// BugAttachmentStats (int attId, int attIdentifier, int attObsCnt, int statHistCnt, int attHistCnt)
			attStats.put (attIdentifier, new BugAttachmentStats (attId, attIdentifier, attObsCnt, attStatHistCnt, attHistCnt));
		}

		return new BugStats (bugId, cmntCnt, histCnt, attCnt, ccCnt, blocksCnt, aliasCnt, severityHistoryCnt, priorityCnt,
				statusCnt, resolutionCnt, confirmedCnt, versionHistoCnt, operatingSystemCnt, dependsCnt, keywordCnt,
				milestoneCnt, assignedToCnt, qaContactCnt, attStats);
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
						stmt.close ();
						res.close ();
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
					stmt.close ();
					res.close ();
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
		Map<Integer, Milestone> milestones = getMilestones (proj);
		Map<Integer, Platform> platforms = getPlatforms (proj);
		Map<Integer, OperatingSystem> operatingSystems = getOperatingSystems (proj);
		Map<Integer, Status> statuses = getStatuses (proj);
		Map<Integer, BugClass> classifications = getBugClasses (proj);


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
			Milestone milestone = milestones.get (res.getInt (22));
			OperatingSystem operatingSystem = operatingSystems.get (res.getInt (20));
			Platform platform = platforms.get (res.getInt (21));
			Status status = statuses.get (res.getInt (14));
			BugClass classification = classifications.get (res.getInt (23));
			boolean isOpen = res.getBoolean (24);

			Bug bug = new Bug (id, identifier, identity, component,
				title, creation, lastChange, priority, severity, status, resolution,
				version, milestone, operatingSystem, platform, classification,
				isOpen);

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

			int statId = res.getInt (25);
			String statName = res.getString (26);
			Status status = new Status (statId, proj, statName);

			int platId = res.getInt (27);
			String platName = res.getString (28);
			Platform platform = new Platform (platId, proj, platName);

			int mileId = res.getInt (29);
			String mileName = res.getString (30);
			Milestone milestone = new Milestone (mileId, proj, mileName);
			
			BugClass classification = null;
			int classificationId = res.getInt (31);
			if (!res.wasNull ()) {
				String classificationName = res.getString (32);
				classification = new BugClass (classificationId, proj, classificationName);
			}

			boolean isOpen = res.getBoolean (33);

			return new Bug (id, identifier, identity, component,
				title, creation, lastChange, priority, severity,
				status, resolution, version, milestone, 
				operatingSystem, platform, classification,
				isOpen);
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

	public Map<Integer, BugGroup> getBugGroups (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_GROUPS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, BugGroup> groups = new HashMap<Integer, BugGroup> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugGroup grp = new BugGroup (res.getInt (1), proj, res.getString (2));
			groups.put (grp.getId (), grp);
		}
	
		return groups;		
	}

	public Map<Integer, Milestone> getMilestones (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_MILESTONES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, Milestone> milestones = new HashMap<Integer, Milestone> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Milestone ms = new Milestone (res.getInt (1), proj, res.getString (2));
			milestones.put (ms.getId (), ms);
		}

		return milestones;		
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

	public Map<String, Platform> getPlatformsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PLATFORMS);
		stmt.setInt (1, proj.getId ());

		// Collect data:
		HashMap<String, Platform> platforms = new HashMap<String, Platform> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Platform pf = new Platform (res.getInt (1), proj, res.getString (2));
			platforms.put (pf.getName (), pf);
		}
	
		return platforms;
	}

	public Map<String, Keyword> getKeywordsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_KEYWORDS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Keyword> kwds = new HashMap<String, Keyword> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Keyword kw = new Keyword (res.getInt (1), proj, res.getString (2));
			kwds.put (kw.getName (), kw);
		}
	
		return kwds;
	}

	public Map<String, BugClass> getBugClassesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_BUG_CLASSES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, BugClass> classes = new HashMap<String, BugClass> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugClass cl = new BugClass (res.getInt (1), proj, res.getString (2));
			classes.put (cl.getName (), cl);
		}
	
		return classes;
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

	public Map<String, BugGroup> getBugGroupsByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_GROUPS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, BugGroup> groups = new HashMap<String, BugGroup> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugGroup grp = new BugGroup (res.getInt (1), proj, res.getString (2));
			groups.put (grp.getName (), grp);
		}
	
		return groups;
	}

	public Map<String, Milestone> getMilestonesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_MILESTONES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, Milestone> milestones = new HashMap<String, Milestone> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Milestone ms = new Milestone (res.getInt (1), proj, res.getString (2));
			milestones.put (ms.getName (), ms);
		}
	
		return milestones;
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

	public Map<Integer, BugFlagStatus> getBugFlagStates (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_FLAGS_STATES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, BugFlagStatus> states = new HashMap<Integer, BugFlagStatus> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugFlagStatus state = new BugFlagStatus (res.getInt (1), proj, res.getString (2));
			states.put (state.getId (), state);
		}
	
		return states;
	}

	public Map<Integer, BugFlag> getBugFlags (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_FLAGS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, BugFlag> flags = new HashMap<Integer, BugFlag> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Integer id  = res.getInt (1);
			Integer identifier = res.getInt (2);
			String name = res.getString (3);
			Integer typeId = res.getInt (4);

			
			BugFlag flag = new BugFlag (id, proj, identifier, name, typeId);
			flags.put (flag.getId (), flag);
		}
	
		return flags;
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

	public Map<Integer, Platform> getPlatforms (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_PLATFORMS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, Platform> platforms = new HashMap<Integer, Platform> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Platform pf = new Platform (res.getInt (1), proj, res.getString (2));
			platforms.put (pf.getId (), pf);
		}
	
		return platforms;
	}

	public Map<Integer, Keyword> getKeywords (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_KEYWORDS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, Keyword> kwds = new HashMap<Integer, Keyword> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Keyword kw = new Keyword (res.getInt (1), proj, res.getString (2));
			kwds.put (kw.getId (), kw);
		}
	
		return kwds;
	}

	public Map<Integer, BugClass> getBugClasses (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_BUG_CLASSES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, BugClass> classes = new HashMap<Integer, BugClass> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugClass cl = new BugClass (res.getInt (1), proj, res.getString (2));
			classes.put (cl.getId (), cl);
		}
	
		return classes;
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

	public Map<String, BugFlagStatus> getBugFlagStatesByName (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_FLAGS_STATES);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<String, BugFlagStatus> states = new HashMap<String, BugFlagStatus> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			BugFlagStatus state = new BugFlagStatus (res.getInt (1), proj, res.getString (2));
			states.put (state.getName (), state);
		}
	
		return states;
	}

	public Map<Integer, BugFlag> getBugFlagsByIdentifier (Project proj) throws SQLException {
		assert (conn != null);
		assert (proj != null);
		assert (proj.getId () != null);

		// Statement:
		PreparedStatement stmt = conn.prepareStatement (SELECT_ALL_BUG_FLAGS);
		stmt.setInt (1, proj.getId ());
	
		// Collect data:
		HashMap<Integer, BugFlag> flags = new HashMap<Integer, BugFlag> ();
		ResultSet res = stmt.executeQuery ();
		while (res.next ()) {
			Integer id  = res.getInt (1);
			Integer identifier = res.getInt (2);
			String name = res.getString (3);
			Integer typeId = res.getInt (4);
			
			BugFlag flag = new BugFlag (id, proj, identifier, name, typeId);
			flags.put (flag.getIdentifier (), flag);
		}
	
		return flags;
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
			User user = userFromResult (res, proj, 5, 6);
			Identity identity = identityFromResult (res, user, 12, 2, 11, 3, 4);
			Date creation = resGetDate (res, 7);
			String field = res.getString (8);
			String oldValue = res.getString (9);
			String newValue = res.getString (10);

			BugHistory entry = new BugHistory (id, bug, identity, creation, field, oldValue, newValue);
			history.add (entry);
		}
	
		return history;
	}

	public int getSentimentState (Project proj) throws SQLException {
		assert (proj != null);
		assert (proj.getId () != null);

		PreparedStatement stmt = conn.prepareStatement (SELECT_SENTIMENT_STATES);
		stmt.setInt (1, proj.getId ());
		ResultSet res = stmt.executeQuery ();

		int commentCnt = 0;
		if (res.next ()) {
			commentCnt = res.getInt (1);
		}
	
		res.close ();
		stmt.close ();
		return commentCnt;
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
		stmt.executeUpdate (PLATFORMS_TABLE);
		stmt.executeUpdate (MILESTONE_TABLE);
		stmt.executeUpdate (OPERATING_SYSTEM_TABLE);
		stmt.executeUpdate (KEYWORD_TABLE);
		stmt.executeUpdate (BUG_FLAG_STATUS_TABLE);
		stmt.executeUpdate (BUG_FLAG_TABLE);
		stmt.executeUpdate (BUG_GROUP_TABLE);
		stmt.executeUpdate (BUG_TABLE);
		stmt.executeUpdate (BUG_FLAG_ASSIGNMENT_TABLE);
		stmt.executeUpdate (BUG_GROUPS_TABLE);
		stmt.executeUpdate (BUG_CC_TABLE);
		stmt.executeUpdate (BUG_KEYWORDS_TABLE);
		stmt.executeUpdate (ASSIGNED_TO_HISTORY_TABLE);
		stmt.executeUpdate (QA_CONTACT_HISTORY_TABLE);
		stmt.executeUpdate (PRIORITY_HISTORY_TABLE);
		stmt.executeUpdate (SEVERITY_HISTORY_TABLE);
		stmt.executeUpdate (RESOLUTION_HISTORY_TABLE);
		stmt.executeUpdate (CONFIRMED_HISTORY_TABLE);
		stmt.executeUpdate (VERSION_HISTORY_TABLE);
		stmt.executeUpdate (MILESTONE_HISTORY_TABLE);
		stmt.executeUpdate (OPERATING_SYSTEM_HISTORY_TABLE);
		stmt.executeUpdate (KEYWORD_HISTORY_TABLE);
		stmt.executeUpdate (BUG_ALIASES);
		stmt.executeUpdate (BUG_DEADLINE_TABLE);
		stmt.executeUpdate (BUG_DUPLICATION_TABLE);
		stmt.executeUpdate (BUG_QA_CONTACT_TABLE);
		stmt.executeUpdate (BUG_SEE_ALSO_TABLE);
		stmt.executeUpdate (BUG_CLASS_TABLE);
		stmt.executeUpdate (COMMENT_TABLE);
		stmt.executeUpdate (BUG_DUPLICATION_COMMENT_TABLE);
		stmt.executeUpdate (STATUS_TABLE);
		stmt.executeUpdate (BUG_HISTORY_TABLE);
		stmt.executeUpdate (STATUS_HISTORY_TABLE);
		stmt.executeUpdate (CC_TABLE);
		stmt.executeUpdate (BUG_BLOCKS_HISTORY_TABLE);
		stmt.executeUpdate (BUG_BLOCKS_TABLE);
		stmt.executeUpdate (BUG_DEPENDS_ON_TABLE);
		stmt.executeUpdate (BUG_DEPENDENCY_HISTORY_TABLE);
		stmt.executeUpdate (CATEGORY_TABLE);
		stmt.executeUpdate (COMMIT_TABLE);
		stmt.executeUpdate (BUGFIX_COMMIT_TABLE);
		stmt.executeUpdate (FILE_TABLE);
		stmt.executeUpdate (FILE_RENAMES_TABLE);
		stmt.executeUpdate (FILE_CHANGES_TABLE);
		stmt.executeUpdate (FILE_DELETION_TABLE);
		stmt.executeUpdate (FILE_COPY_TABLE);
		stmt.executeUpdate (BUG_COMMENT_COUNT_UPDATE_TRIGGER);
		stmt.executeUpdate (PROJECT_FLAG_TABLE);
		stmt.executeUpdate (ATTACHMENT_TABLE);
		stmt.executeUpdate (ATTACHMENT_STATUS_TABLE);
		stmt.executeUpdate (ATTACHMENT_STATUS_HISTORY_TABLE);
		stmt.executeUpdate (ATTACHMENT_HISTORY_TABLE);
		stmt.executeUpdate (ATTACHMENT_DETAIL_TABLE);
		stmt.executeUpdate (BUG_ATTACHMENT_FLAG_ASSIGNMENT_TABLE);
		stmt.executeUpdate (BUG_ATTACHMENT_REVIEW_COMMENT_TABLE);
		stmt.executeUpdate (BUG_CATEGORIES_TABLE);
		stmt.executeUpdate (COMMIT_CATEGORIES_TABLE);
		stmt.executeUpdate (DICTIONARY_TABLE);
		stmt.executeUpdate (SENTENCE_SENTIMENT_TABLE);
		stmt.executeUpdate (BUG_COMMENT_SENTIMENT_TABLE);
		stmt.executeUpdate (BLOCK_SENTIMENT_TABLE);
		stmt.executeUpdate (SENTIMENT_TABLE);
		stmt.executeUpdate (SELECTED_USER_TABLE);
		stmt.executeUpdate (ATTACHMENT_ISOBSOLETE_TABLE);
		stmt.executeUpdate (SOCIAL_STATS_TABLE);
		stmt.close ();
	}
}
