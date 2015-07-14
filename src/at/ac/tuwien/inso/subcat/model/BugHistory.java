/* BugHistory.java
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


public class BugHistory {
	private Integer id;
	private Bug bug;
	private Identity identity;
	private Date date;
	private String field;
	private String oldValue;
	private String newValue;

	public BugHistory (Integer id, Bug bug, Identity identity, Date date,
			String field, String oldValue, String newValue) {
		assert (bug != null);
		assert (identity != null);
		assert (date != null);
		assert (field != null);

		this.id = id;
		this.bug = bug;
		this.identity = identity;
		this.date = date;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
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

	public Date getDate () {
		return date;
	}

	public void setDate (Date date) {
		assert (date != null);

		this.date = date;
	}

	public String getFieldName () {
		return field;
	}

	public void setFieldName (String field) {
		assert (field != null);

		this.field = field;
	}

	public String getOldValue () {
		return oldValue;
	}

	public void setOldValue (String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue () {
		return newValue;
	}

	public void setNewValue (String newValue) {
		this.newValue = newValue;
	}

	@Override
	public String toString () {
		return "BugHistory [id=" + id + ", bug=" + bug
			+ ", identity=" + identity + ", date=" + date + "]";
	}

	public boolean equals (BugHistory obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof BugHistory) {
			return equals ((BugHistory) obj);
		}
		
		return super.equals (obj);
	}
}
