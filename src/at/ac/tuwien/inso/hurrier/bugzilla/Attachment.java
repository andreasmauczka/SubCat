/* Attachment.java
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

package at.ac.tuwien.inso.hurrier.bugzilla;

import java.util.Date;


public class Attachment {
	private String data;
	private Date creationTime;
	private Date lastChangeTime;
	private int id;
	private int bugId;
	private String fileName;
	private String summary;
	private Boolean isPrivate;
	private Boolean isObsolete;
	private Boolean isPatch;
	private String creator;
	private String contentType;
	
	public Attachment (int id, int bug_id, Date creationTime,
			Date lastChangeTime,  String fileName, String contentType,
			String summary, String creator, Boolean isPrivate, Boolean isObsolete,
			Boolean isPatch, String data) {

		assert (creationTime != null);
		assert (lastChangeTime != null);
		assert (fileName != null);
		assert (contentType != null);
		assert (creator != null);
		assert (data != null);

		this.data = data;
		this.creationTime = creationTime;
		this.lastChangeTime = lastChangeTime;
		this.id = id;
		this.bugId = bug_id;
		this.fileName = fileName;
		this.summary = summary;
		this.isPrivate = isPrivate;
		this.isObsolete = isObsolete;
		this.isPatch = isPatch;
		this.creator = creator;
		this.contentType = contentType;
	}

	public String getData () {
		return data;
	}

	public Date getCreationTime () {
		return creationTime;
	}

	public Date getLastChange_time () {
		return lastChangeTime;
	}

	public int getId () {
		return id;
	}

	public int getBugId () {
		return bugId;
	}

	public String getFileName () {
		return fileName;
	}

	public String getSummary () {
		return summary;
	}

	public Boolean getIsPrivate () {
		return isPrivate;
	}

	public Boolean getIsObsolete () {
		return isObsolete;
	}

	public Boolean getIsPatch () {
		return isPatch;
	}

	public String getCreator () {
		return creator;
	}

	public String getContentType () {
		return contentType;
	}
}
