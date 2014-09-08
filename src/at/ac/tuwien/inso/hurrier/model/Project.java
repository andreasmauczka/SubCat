/* Project.java
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


public class Project {
	private Integer id;
	private Date date;
	private String domain;
	private String product;
	private String revision;

	public Project (Integer id, Date date, String domain, String product,
			String revision) {
		assert (date != null);

		this.id = id;
		this.date = date;
		this.domain = domain;
		this.product = product;
		this.revision = revision;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Date getDate () {
		return date;
	}

	public void setDate (Date date) {
		assert (date != null);

		this.date = date;
	}

	public String getDomain () {
		return domain;
	}

	public void setDomain (String domain) {
		this.domain = domain;
	}

	public String getProduct () {
		return product;
	}

	public void setProduct (String product) {
		this.product = product;
	}

	public String getRevision () {
		return revision;
	}

	public void setRevision (String revision) {
		this.revision = revision;
	}

	@Override
	public String toString () {
		return "Project [id=" + id + ", date=" + date + ", domain=" + domain
				+ ", product=" + product + ", revision=" + revision + "]";
	}

	public boolean equals (Project obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Project) {
			return equals ((Project) obj);
		}
		
		return super.equals (obj);
	}
}
