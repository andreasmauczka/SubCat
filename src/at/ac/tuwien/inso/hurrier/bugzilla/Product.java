/* Product.java
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


public class Product {
	private int id;
	private String name;
	private String description;
	//private Component[] components;
	
	Product (int id, String name, String description) {
		
		assert (name != null);
		
		this.id = id;
		this.name = name;
		this.description = description;
	}
	
	public int getId () {
		return id;
	}

	public String getName () {
		return name;
	}

	public String getDescription () {
		return description;
	}	

	public String toString () {
		return "[Product: id="
			+ id
			+ " name="
			+ name
			+ "]";
	}
}
