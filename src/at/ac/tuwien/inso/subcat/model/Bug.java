/* Bug.java
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


public class Bug {
	private Integer id;
	private Identity identity;
	private Component component;
	private String title;
	private Date creation;
	private Priority priority;
	private Severity severity;
	private Category category;

	public Bug (Integer id, Identity identity, Component component,
			String title, Date creation, Priority priority, Severity severity,
			Category category) {
		assert (identity != null);
		assert (component != null);
		assert (title != null);
		assert (creation != null);
		assert (priority != null);
		//assert (category != null);

		this.id = id;
		this.identity = identity;
		this.component = component;
		this.title = title;
		this.creation = creation;
		this.priority = priority;
		this.category = category;
		this.severity = severity;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Identity getIdentity () {
		return identity;
	}

	public void setIdentity (Identity identity) {
		assert (identity != null);

		this.identity = identity;
	}

	public Component getComponent () {
		return component;
	}

	public void setComponent (Component component) {
		assert (component != null);

		this.component = component;
	}

	public String getTitle () {
		return title;
	}

	public void setTitle (String title) {
		assert (title != null);

		this.title = title;
	}

	public Date getCreation () {
		return creation;
	}

	public void setCreation (Date creation) {
		assert (creation != null);

		this.creation = creation;
	}

	public Priority getPriority () {
		return priority;
	}

	public void setPriority (Priority priority) {
		assert (priority != null);
		
		this.priority = priority;
	}

	public Category getCategory () {
		return category;
	}

	public void setCategory (Category category) {
		assert (category != null);

		this.category = category;
	}

	public boolean equals (Bug obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	public Severity getSeverity () {
		return severity;
	}

	public void setSeverity (Severity severity) {
		this.severity = severity;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Bug) {
			return equals ((Bug) obj);
		}
		
		return super.equals (obj);
	}

	@Override
	public String toString () {
		return "Bug [id=" + id + ", identity=" + identity + ", component="
				+ component + ", title=" + title + ", creation=" + creation
				+ ", priority=" + priority + ", severity=" + severity
				+ ", category=" + category + "]";
	}
}
