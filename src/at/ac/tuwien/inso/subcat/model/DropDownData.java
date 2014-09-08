/* DropDownData.java
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

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.inso.subcat.config.DropDownConfig;


public class DropDownData {
	private DropDownConfig config;
	private List<Pair> data;

	public static class Pair {
		public final String name;
		public final int id;

		public Pair (int id, String name) {
			assert (name != null);
			
			this.id = id;
			this.name = name;
		}
	}

	public DropDownData (DropDownConfig config) {
		assert (config != null);
		
		this.config = config;
		data = new ArrayList<Pair> ();
	}
	
	public void add (int id, String name) {
		assert (name != null);
		
		data.add (new Pair (id, name));
	}

	public DropDownConfig getConfig () {
		return config;
	}
	
	public List<Pair> getData () {
		return data;
	}
}
