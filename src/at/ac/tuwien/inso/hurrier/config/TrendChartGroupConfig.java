/* TrendChartGroupConfig.java
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


public class TrendChartGroupConfig extends ChartGroupConfig {
	private List<TrendChartConfig> configs;
	
	public TrendChartGroupConfig (String name, SourcePos start, SourcePos end) {
		super (name, start, end);
		
		configs = new LinkedList<TrendChartConfig> ();
	}

	public List<TrendChartConfig> getTrendChartConfigs () {
		return configs;
	}
	
	public void addChart(TrendChartConfig chart) {
		assert (chart != null);

		configs.add (chart);
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitTrendChartGroupConfig (this);
	}
	
	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);

		for (TrendChartConfig config : configs) {
			config.accept (visitor);
		}
	}
}
