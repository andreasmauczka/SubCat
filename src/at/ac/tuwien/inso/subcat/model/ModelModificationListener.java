/* ModelModificationListener.java
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

import java.util.Date;

import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;

public interface ModelModificationListener {

	public void projectAdded (Project project);

	public void projectUpdated (Project project);

	public void userAdded (User user);

	public void identityAdded (Identity identity);

	public void interactionAdded (Interaction relation);

	public void severityAdded (Severity severity);

	public void priorityAdded (Priority priority);

	public void categoryAdded (Category category);

	public void componentAdded (Component component);

	public void bugAdded (Bug bug);

	public void bugUpdated (Bug bug);

	public void bugHistoryAdded (BugHistory history);

	public void commentAdded (Comment cmnt);

	public void statusAdded (Status status);

	public void commitAdded (Commit commit);

	public void bugfixCommitAdded (BugfixCommit bugfix);

	public void fileChangeAdded (FileChange change);

	public void fileRenameAdded (FileRename rename);

	public void managedFileAdded (ManagedFile file);

	public void fileDeletedAdded (FileDeletion deletion);

	public void managedFileCopyAdded (ManagedFileCopy copy);

	public void attachmentAdded (Attachment attachment);

	public void attachmentUpdated (Attachment attachment);

	public void attachmentStatusAdded (AttachmentStatus status);

	public void attachmentStatusHistoryAdded (AttachmentStatusHistory history);

	public void bugCategoryAdded (Bug bug, Category category);

	public void commitCategoryAdded (Commit commit, Category category);

	public void commitDictionaryAdded (Dictionary dict);

	public void attachmentReplacementAdded (Attachment oldAtt,
			Attachment newAtt);

	public void sentimentAdded (Comment comment, Sentiment<Identity> sentiment);

	public void attachmentIsObsoleteAdded (Attachment attachment,
			Identity identity, Date date, boolean oldValue, boolean newValue);

	public void bugCcHistoryAdded (Bug bug, Date date, Identity addedBy,
			Identity cc, String ccMail, boolean removed);

	public void bugBlocksAdded (Bug bug, Date date, Identity addedBy,
			boolean removed);

	public void bugAliasAdded (Bug bug, Identity addedBy, Date date,
			String alias);

	public void severityHistoryAdded (Bug bug, Identity addedBy, Date date,
			Severity oldSeverity, Severity newSeverity);

	public void priorityHistoryAdded (Bug bug, Identity addedBy, Date date,
			Priority oldPriority, Priority newPriority);

	public void statusHistoryAdded (Bug bug, Identity addedBy, Date date,
			Status oldStatus, Status newStatus);

	public void resolutionAdded (Resolution resolution);

	public void resolutionHistoryAdded (Bug bug, Identity addedBy, Date date,
			Resolution resolution);

	public void confiremdHistoryAdded (Bug bug, Identity addedBy, Date date,
			boolean removed);

	public void versionAdded (Version version);

	public void versionHistoryAdded (Bug bug, Identity addedBy, Date date,
			Version version, Version newVersion);

	public void operatingSystemAdded (OperatingSystem os);

	public void operatingSystemHistoryAdded (Bug bug, Identity addedBy,
			Date date, OperatingSystem oldOs, OperatingSystem newOs);

	public void attachmentHistoryAdded (Attachment attachment,
			Identity addedBy, Date date, String fieldName, String oldValue,
			String newValue);

	public void keywordAdded (Keyword keyword);

	public void keywordHistoryAdded (Bug bug, Identity addedBy, Date date,
			Keyword keyword, boolean removed);

	public void milestoneAdded (Milestone ms);

	public void milestoneAdded (Bug bug, Identity addedBy, Date date,
			Milestone oldMilestone, Milestone newMilestone);

	public void bugGroupAdded (BugGroup grp);

	public void assignedToAdded (Bug bug, Identity addedBy, Date date,
			String identifierAdded, BugGroup groupAdded, Identity identityAdded,
			String identifierRemoved, BugGroup groupRemoved, Identity identityRemoved);

	public void qaContactAdded (Bug bug, Identity addedBy, Date date,
			String identifierAdded, BugGroup groupAdded,
			Identity identityAdded, String identifierRemoved,
			BugGroup groupRemoved, Identity identityRemoved);

	public void platformAdded (Platform pf);

	public void bugDeadlineAdded (Bug bug, Date deadline);

	public void bugDeadlineUpdated (Bug bug, Date deadline);

	public void bugDuplicationUpdated (Bug bug, Integer duplication);

	public void bugDuplicationAdded (Bug bug, Integer duplication);

	public void bugQaContactAdded (Bug bug, Identity identity, BugGroup group);

	public void bugQaContactUpdated (Bug bug, Identity identity, BugGroup group);

	public void bugBlocksAdded (Bug bug, Integer[] blocks);

	public void bugBlocksUpdated (Bug bug, Integer[] blocks);

	public void bugDependsOnAdded (Bug bug, Integer[] dependsOn);

	public void bugDependsOnUpdated (Bug bug, Integer[] dependsOn);

	public void bugKeywordsAdded (Bug bug, Keyword[] keywords);

	public void bugKeywordsUpdated (Bug bug, Keyword[] keywords);

	public void bugCcAdded (Bug bug, Identity[] identities);

	public void bugCcUpdated (Bug bug, Identity[] identities);

	public void bugGroupMembershipsAdded (Bug bug, BugGroup[] groups);

	public void bugGroupMembershipsUpdated (Bug bug, BugGroup[] groups);

	public void bugFlagStatusAdded (BugFlagStatus status);

	public void bugFlagAdded (BugFlag flag);

	public void bugFlagAssignmentsAdded (Bug bug, BugFlagAssignment[] flags);

	public void bugFlagAssignmentsUpdated (Bug bug, BugFlagAssignment[] flags);

	public void bugSeeAlsoUpdated (Bug bug, String[] links);

	public void bugSeeAlsoAdded (Bug bug, String[] links);

	public void bugClassAdded (BugClass bc);

	public void bugDuplicationCommentAdded (Comment comment, Integer identifier);

	public void bugAttachmentReviewCommentAdded (Comment comment,
			Attachment attachment);

	public void attachmentDetailsAdded (AttachmentDetails ad);

	public void attachmentDetailsUpdated (AttachmentDetails ad);

	public void bugAttachmentFlagAssignmentsAdded (Attachment attachment,
			BugFlagAssignment[] flags);

	public void bugAttachmentFlagAssignmentsUpdated (Attachment attachment,
			BugFlagAssignment[] flags);
}
