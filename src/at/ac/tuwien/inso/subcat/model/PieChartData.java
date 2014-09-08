/* PieChartData.java
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

import java.util.SortedMap;
import java.util.TreeMap;


public class PieChartData {
	private String name;
	private boolean showTotal;

	private SortedMap<String, Integer> data;
	private int total;


	public PieChartData (String name, boolean showTotal) {
		assert (name != null);

		this.name = name;
		this.showTotal = showTotal;
		this.data = new TreeMap<String, Integer> ();
	}
	
	public void setFraction (String name, int value) {
		assert (name != null);
		assert (value >= 0 && value <= 100);
		
		data.put (name, value);
		total += value;
	}
	
	public String getName () {
		return name;
	}

	public SortedMap<String, Integer> getData () {
		return data;
	}

	public boolean getShowTotal () {
		return showTotal;
	}
	
	public int getTotal () {
		return total;
	}
}
