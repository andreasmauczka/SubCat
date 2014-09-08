/* DistributionChartOptionConfigData.java
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

import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.subcat.config.DistributionAttributesConfig;
import at.ac.tuwien.inso.subcat.config.DistributionChartOptionConfig;


public class DistributionChartOptionConfigData {
	private String name;
	private List<DropDownData> filter;
	private DistributionAttributesConfig attributes;
	private DistributionChartOptionConfig config;

	public DistributionChartOptionConfigData (String name, DistributionAttributesConfig attData, DistributionChartOptionConfig config) {
		assert (name != null);
		assert (attData != null);
		assert (config != null);
		
		this.name = name;
		this.attributes = attData;
		this.filter = new LinkedList<DropDownData> ();
		this.config = config;
	}
	
	public String getName () {
		return name;
	}

	public List<DropDownData> getFilter () {
		return filter;
	}
	
	public DistributionAttributesConfig getAttributes () {
		return attributes;
	}

	public void addFilter (DropDownData data) {
		assert (data != null);

		filter.add (data);
	}

	public DistributionChartOptionConfig getConfig() {
		return config;
	}
}
