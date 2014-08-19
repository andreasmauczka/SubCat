/* BugHistory.java
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

import java.util.Date;


public class BugHistory {
	private Integer id;
	private Bug bug;
	private Status status;
	private Identity identity;
	private Date date;

	public BugHistory (Integer id, Bug bug, Status status, Identity identity,
			Date date) {
		assert (bug != null);
		assert (status != null);
		assert (identity != null);
		assert (date != null);

		this.id = id;
		this.bug = bug;
		this.status = status;
		this.identity = identity;
		this.date = date;
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

	public Status getStatus () {
		return status;
	}

	public void setStatus (Status status) {
		assert (status != null);

		this.status = status;
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

	@Override
	public String toString () {
		return "BugHistory [id=" + id + ", bug=" + bug + ", status=" + status
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
