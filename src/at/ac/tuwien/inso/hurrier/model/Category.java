/* Category.java
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


public class Category implements NamedContainer, Comparable<Category> {
	private Integer id;
	private Project project;
	private String name;

	public Category (Integer id, Project project, String name) {
		assert (project != null);
		assert (name != null);

		this.id = id;
		this.project = project;
		this.name = name;
	}

	@Override
	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	@Override
	public Project getProject () {
		assert (project != null);

		return project;
	}

	public void setProject (Project project) {
		this.project = project;
	}

	@Override
	public String getName () {
		return name;
	}

	public void setName (String name) {
		assert (name != null);

		this.name = name;
	}

	@Override
	public String toString () {
		return "Category [id=" + id + ", project=" + project + ", name=" + name
				+ "]";
	}

	public boolean equals (Category obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Category) {
			return equals ((Category) obj);
		}
		
		return super.equals (obj);
	}

	@Override
	public int compareTo (Category cat) {
		if (id == cat.id) {
			return 0;
		}

		return name.compareTo (cat.name);
	}
}
