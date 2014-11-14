/* Category.java
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


public class Category implements NamedContainer, Comparable<Category> {
	private Integer id;
	private String name;
	private Dictionary dictionary;

	public Category (Integer id, String name, Dictionary dictionary) {
		assert (name != null);
		assert (dictionary != null);

		this.id = id;
		this.name = name;
		this.dictionary = dictionary;
	}

	@Override
	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
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
		return "Category [id=" + id + ", dictionary=" + dictionary + ", name=" + name
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

	public Dictionary getDictionary () {
		return dictionary;
	}
}
