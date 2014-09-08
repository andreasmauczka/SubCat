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

package at.ac.tuwien.inso.subcat.bugzilla;


public class BugzillaUser {
	private int id;
	private String realName;
	private String email;
	private String name;

	public BugzillaUser (int id, String realName, String email, String name) {
		super ();

		assert (name != null);
		
		this.id = id;
		this.realName = realName;
		this.email = email;
		this.name = name;
	}

	public String toString () {
		return "[User id="
			+ id
			+ " realName="
			+ realName
			+ " email="
			+ email
			+ " name= "
			+ name
			+ "]";
	}
	
	public int getId () {
		return id;
	}

	public String getRealName () {
		return realName;
	}

	public String getEmail () {
		return email;
	}

	public String getName () {
		return name;
	}
}
