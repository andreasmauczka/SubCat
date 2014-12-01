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

public interface ModelModificationListener {

	public void projectAdded (Project project);

	public void userAdded (User user);

	public void identityAdded (Identity identity);

	public void interactionAdded (Interaction relation);

	public void severityAdded (Severity severity);

	public void priorityAdded (Priority priority);

	public void categoryAdded (Category category);

	public void componentAdded (Component component);

	public void bugAdded (Bug bug);

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

	public void attachmentStatusAdded (AttachmentStatus status);

	public void attachmentHistoryAdded (AttachmentHistory history);

	public void bugCategoryAdded (Bug bug, Category category);

	public void commitCategoryAdded (Commit commit, Category category);

	public void commitDictionaryAdded (Dictionary dict);

	public void commitAttachmentReplacementAdded (Attachment oldAtt,
			Attachment newAtt);
}
