/* Comment.java
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


public class Comment {
	private Integer id;
	private Bug bug;
	private Identity identity;
	private int wordCount;

	public Comment (Integer id, Bug bug, Identity identity, int wordCount) {
		assert (bug != null);
		assert (identity != null);
		assert (wordCount >= 0);

		this.id = id;
		this.bug = bug;
		this.identity = identity;
		this.wordCount = wordCount;
	}

	public Integer getId () {
		return id;
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

	public Identity getIdentity () {
		return identity;
	}

	public void setIdentity (Identity identity) {
		assert (identity != null);

		this.identity = identity;
	}

	public int getWordCount () {
		return wordCount;
	}

	public void setWordCount (int wordCount) {
		assert (wordCount >= 0);

		this.wordCount = wordCount;
	}

	@Override
	public String toString() {
		return "Comment [id=" + id + ", bug=" + bug + ", identity=" + identity
				+ ", wordCount=" + wordCount + "]";
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
