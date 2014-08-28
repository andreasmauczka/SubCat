/* OptionListConfigData.java
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

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.inso.hurrier.config.OptionListConfig;


public class OptionListConfigData {
	private List<Pair> data;
	private OptionListConfig config;

	public static class Pair {
		public final String name;
		public final int id;

		public Pair (int id, String name) {
			assert (name != null);
			
			this.id = id;
			this.name = name;
		}
	}


	public OptionListConfigData (OptionListConfig config) {
		assert (config != null);
		
		this.config = config;
		data = new ArrayList<Pair> ();
	}

	public void add (int id, String name) {
		assert (name != null);
		
		data.add (new Pair (id, name));
	}

	public List<Pair> getData () {
		return data;
	}

	public OptionListConfig getConfig () {
		return config;
	}
}