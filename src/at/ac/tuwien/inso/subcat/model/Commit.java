/* Commit.java
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


public class Commit {
	private Integer id;
	private Project project;
	private Identity author;
	private Identity committer;
	private Date date;
	private String title;
	private int linesAdded;
	private int linesRemoved;
	private int changedFiles;
	private Category category;

	public Commit (Integer id, Project project, Identity author,
			Identity committer, Date date, String title, int changedFiles,
			int linesAdded, int linesRemoved, Category category) {
		assert (project != null);
		assert (author != null);
		assert (committer != null);
		assert (date != null);
		assert (title != null);
		assert (linesAdded >= 0);
		assert (linesRemoved >= 0);
		assert (changedFiles >= 0);
		
		this.id = id;
		this.project = project;
		this.author = author;
		this.committer = committer;
		this.date = date;
		this.title = title;
		this.linesAdded = linesAdded;
		this.linesRemoved = linesRemoved;
		this.category = category;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Project getProject () {
		assert (project != null);

		return project;
	}

	public void setProject (Project project) {
		this.project = project;
	}

	public Identity getAuthor () {
		return author;
	}

	public void setAuthor (Identity author) {
		assert (author != null);

		this.author = author;
	}

	public Identity getCommitter () {
		return committer;
	}

	public void setCommitter (Identity committer) {
		assert (committer != null);

		this.committer = committer;
	}

	public Date getDate () {
		assert (date != null);

		return date;
	}

	public void setDate (Date date) {
		this.date = date;
	}

	public String getTitle () {
		return title;
	}

	public void setTitle (String title) {
		assert (title != null);

		this.title = title;
	}

	public int getLinesAdded () {
		return linesAdded;
	}

	public void setLinesAdded (int linesAdded) {
		assert (linesAdded >= 0);

		this.linesAdded = linesAdded;
	}

	public int getChangedFiles () {
		return changedFiles;
	}
	
	public int getLinesRemoved () {
		return linesRemoved;
	}

	public void setLinesRemoved (int linesRemoved) {
		assert (linesRemoved >= 0);

		this.linesRemoved = linesRemoved;
	}

	public Category getCategory () {
		return category;
	}

	public void setCategory (Category category) {
		this.category = category;
	}

	@Override
	public String toString () {
		return "Commit [id=" + id + ", project=" + project + ", author=" + author
				+ ", committer=" + committer + ", date=" + date + ", title="
				+ title + ", linesAdded=" + linesAdded + ", linesRemoved="
				+ linesRemoved + ", category=" + category + "]";
	}

	public boolean equals (Commit obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Commit) {
			return equals ((Commit) obj);
		}
		
		return super.equals (obj);
	}
}
