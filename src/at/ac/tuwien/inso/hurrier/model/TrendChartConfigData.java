/* TrendChartConfigData.java
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

import java.util.LinkedList;
import java.util.List;


public class TrendChartConfigData {
	private String name;
	private List<DropDownData> dropDowns;
	private OptionListConfigData optionList;
	
	public TrendChartConfigData (String name, OptionListConfigData optionList) {
		assert (name != null);
		assert (optionList != null);

		this.name = name;
		this.optionList = optionList;
		
		dropDowns = new LinkedList<DropDownData> ();
	}
	
	public String getName () {
		return name;
	}
	
	public void addDropDown (DropDownData data) {
		assert (data != null);

		dropDowns.add (data);
	}
	
	public List<DropDownData> getDropDowns () {
		return dropDowns;
	}
	
	public OptionListConfigData getOptionList () {
		return optionList;
	}
}
