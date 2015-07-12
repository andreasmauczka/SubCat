/* Comment.java
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


public class Comment {
	private Integer id;
	private int index;
	private Date creation;
	private Bug bug;
	private Identity identity;
	private String content;

	public Comment (Integer id, int index, Bug bug, Date creation, Identity identity, String content) {
		assert (bug != null);
		assert (creation != null);
		assert (identity != null);
		assert (content != null);

		this.id = id;
		this.index = index;
		this.bug = bug;
		this.identity = identity;
		this.content = content;
		this.creation = creation;
	}

	public Integer getId () {
		return id;
	}

	public int getIndex () {
		return index;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Bug getBug () {
		return bug;
	}

	public void setBug (Bug bug) {
		assert (bug != null);

		this.bug = bug;
	}

	public Date getCreationDate () {
		return creation;
	}

	public void setCreationDate (Date creation) {
		assert (creation != null);

		this.creation = creation;
	}

	public Identity getIdentity () {
		return identity;
	}

	public void setIdentity (Identity identity) {
		assert (identity != null);

		this.identity = identity;
	}

	public String getContent () {
		return content;
	}

	public void setContent (String content) {
		assert (content != null);

		this.content = content;
	}

	@Override
	public String toString() {
		return "Comment [id=" + id + ", bug=" + bug + ", identity=" + identity
				+ ", wordCount=" + content.length () + "]";
	}

	public boolean equals (Comment obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Comment) {
			return equals ((Comment) obj);
		}
		
		return super.equals (obj);
	}
}
