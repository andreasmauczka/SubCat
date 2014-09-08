/* PieChartController.java
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

package at.ac.tuwien.inso.subcat.ui.controller;

import java.util.Map;

import at.ac.tuwien.inso.subcat.config.PieChartConfig;
import at.ac.tuwien.inso.subcat.config.PieChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.PieChartData;
import at.ac.tuwien.inso.subcat.ui.widgets.PieChartView;


public class PieChartController extends ChartController {

	public PieChartController(Model model, PieChartView view, PieChartGroupConfig groupConfig, ViewController viewController) {
		super(model, viewController);

		assert (model != null);
		assert (view != null);
		assert (groupConfig != null);
		assert (viewController != null);
		

		try {
			for (PieChartConfig pieConf : groupConfig.getCharts ()) {
				Map<String, Object> vars = getVariables ();
				PieChartData pieData = model.getPieChart (pieConf, vars);
				view.add (pieData);
			}
		} catch (SemanticException e) {
		}
	}

	protected Map<String, Object> getVariables () {
		return viewController.getVariables ();
	}
}
