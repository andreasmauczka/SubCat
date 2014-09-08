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

package at.ac.tuwien.inso.subcat.bugzilla;

import java.util.Date;


public class BugzillaComment {
	private int id;
	private int bugId;
	// not supporte by all versions
	private Integer attachmentId;
	private String text;
	private String creator;
	private Date time;

	public BugzillaComment (int id, int bugId, Integer attachmentId, String text,
			String creator, Date time) {

		assert (creator != null);
		assert (time != null);
		assert (text != null);

		this.id = id;
		this.bugId = bugId;
		this.attachmentId = attachmentId;
		this.text = text;
		this.creator = creator;
		this.time = time;
	}

	public String toString () {
		return "[Comment id="
			+ id
			+ " creator="
			+ creator
			+ " attachmentId="
			+ attachmentId
			+ "]";
	}
	
	public int getId () {
		return id;
	}

	public int getBugId () {
		return bugId;
	}

	public Integer getAttachmentId () {
		return attachmentId;
	}

	public String getText () {
		return text;
	}

	public String getCreator () {
		return creator;
	}

	public Date getTime () {
		return time;
	}
}
