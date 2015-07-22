/* BugzillaFLag.java
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

package at.ac.tuwien.inso.subcat.bugzilla;

import java.util.Date;


public class BugzillaFlag {
	private int id;
	private String name;
	private int typeId;
	private Date creationDate;
	private Date modificationDate;
	private String status;
	private String setter;
	private String requestee;

	
	public BugzillaFlag(int id, String name, int typeId, Date creationDate,
			Date modificationDate, String status, String setter,
			String requestee) {
		this.id = id;
		this.name = name;
		this.typeId = typeId;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.status = status;
		this.setter = setter;
		this.requestee = requestee;
	}


	public int getId () {
		return id;
	}

	public String getName () {
		return name;
	}

	public int getTypeId () {
		return typeId;
	}

	public Date getCreationDate () {
		return creationDate;
	}

	public Date getModificationDate () {
		return modificationDate;
	}

	public String getStatus () {
		return status;
	}

	public String getSetter () {
		return setter;
	}

	public String getRequestee () {
		return requestee;
	}
}
