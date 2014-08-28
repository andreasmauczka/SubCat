/* TrendChartConfig.java
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

package at.ac.tuwien.inso.hurrier.config;

import java.util.LinkedList;
import java.util.List;


public class TrendChartConfig extends ConfigNode implements OptionListOwner {
	private String name;
	private Query query;

	private List<TrendChartPlotConfig> dropDowns;
	private OptionListConfig optionList;
	
	public TrendChartConfig (String name, SourcePos start, SourcePos end) {
		super (start, end);

		assert (name != null);

		this.name = name;
		dropDowns = new LinkedList<TrendChartPlotConfig> ();
	}

	public String getName () {
		return name;
	}

	public Query getQuery () {
		return query;
	}
	
	public List<TrendChartPlotConfig> getDropDownConfigs () {
		return dropDowns;
	}
	
	public OptionListConfig getOptionList () {
		return optionList;
	}
	
	public void addDropDown (TrendChartPlotConfig dropDown) {
		assert (dropDown != null);

		dropDowns.add (dropDown);
	}

	@Override
	public void setOptionList (OptionListConfig config) {
		assert (config != null);

		optionList = config;
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);
		
		visitor.visitTrendChartConfig (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);

		for (DropDownConfig config : dropDowns) {
			config.accept (visitor);
		}
		
		if (optionList != null) {
			optionList.accept (visitor);
		}
	}
}