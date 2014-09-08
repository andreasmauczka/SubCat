/* User.java
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

package at.ac.tuwien.inso.subcat.model;


public class User {
	private Integer id;
	private Project project;
	private String name;

	public User (Integer id, Project project, String name) {
		assert (project != null);
		assert (name != null);
		
		this.id = id;
		this.project = project;
		this.name = name;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Project getProject () {
		return project;
	}

	public void setProject (Project project) {
		assert (project != null);
		
		this.project = project;
	}

	public String getName () {
		assert (name != null);

		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	@Override
	public String toString () {
		return "User [id=" + id + ", project=" + project + ", name=" + name
				+ "]";
	}

	public boolean equals (User obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof User) {
			return equals ((User) obj);
		}
		
		return super.equals (obj);
	}
}
