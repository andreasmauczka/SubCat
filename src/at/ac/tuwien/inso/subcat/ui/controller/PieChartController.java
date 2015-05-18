/* PieChartController.java
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

package at.ac.tuwien.inso.subcat.ui.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.inso.subcat.config.PieChartConfig;
import at.ac.tuwien.inso.subcat.config.PieChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.PieChartData;
import at.ac.tuwien.inso.subcat.ui.widgets.PieChart;


public class PieChartController extends ChartController {
	private PieChartGroupConfig groupConfig;
	private List<String> flags;
	private PieChart view;
	private Model model;
	
	public PieChartController(Model model, PieChart view, PieChartGroupConfig groupConfig, List<String> flags, ViewController viewController) {
		super(model, viewController);

		assert (model != null);
		assert (view != null);
		assert (groupConfig != null);
		assert (viewController != null);

		this.groupConfig = groupConfig;
		this.model = model;
		this.flags = flags;
		this.view = view;

		redraw ();
	}

	private void redraw () {
		try {
			view.clear ();

			for (PieChartConfig pieConf : groupConfig.getCharts ()) {
				if (pieConf.show (flags)) {
					Map<String, Object> vars = getVariables ();
					PieChartData pieData = model.getPieChart (pieConf, vars);
					view.add (pieData);
				}
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO 
			e.printStackTrace();
		}
	}

	protected Map<String, Object> getVariables () {
		return viewController.getVariables ();
	}

	@Override
	public void viewVariableChanged () {
		redraw ();
	}
}
