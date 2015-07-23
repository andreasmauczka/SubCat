/* AttachmentDetails.java
 *
 * Copyright (C) 2015 Florian Brosch
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


public class AttachmentDetails {
	private Attachment attachment;
	private byte[] data;
	private Date creationTime;
	private Date lastChangeTime;
	private String fileName;
	private String summary;
	private Boolean isPrivate;
	private Boolean isObsolete;
	private Boolean isPatch;
	private Identity creator;
	private String contentType;

	public AttachmentDetails(Attachment attachment, byte[] data, Date creationTime,
			Date lastChangeTime, String fileName, String summary,
			Boolean isPrivate, Boolean isObsolete, Boolean isPatch,
			Identity creator, String contentType) {
		this.attachment = attachment;
		this.data = data;
		this.creationTime = creationTime;
		this.lastChangeTime = lastChangeTime;
		this.fileName = fileName;
		this.summary = summary;
		this.isPrivate = isPrivate;
		this.isObsolete = isObsolete;
		this.isPatch = isPatch;
		this.creator = creator;
		this.contentType = contentType;
	}

	public Attachment getAttachment () {
		return attachment;
	}

	public byte[] getData () {
		return data;
	}

	public Date getCreationTime () {
		return creationTime;
	}

	public Date getLastChangeTime () {
		return lastChangeTime;
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

	public Identity getCreator () {
		return creator;
	}

	public String getContentType () {
		return contentType;
	}

}
