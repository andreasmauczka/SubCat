/* DistributionChartController.java
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
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.jfree.chart.JFreeChart;

import at.ac.tuwien.inso.subcat.config.DistributionChartConfig;
import at.ac.tuwien.inso.subcat.config.Query;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.model.DistributionChartConfigData;
import at.ac.tuwien.inso.subcat.model.DistributionChartData;
import at.ac.tuwien.inso.subcat.model.DropDownData;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.ui.events.DistributionChartListener;
import at.ac.tuwien.inso.subcat.ui.widgets.DistributionChart;


public class DistributionChartController extends ChartController implements DistributionChartListener {
	private DistributionChart view;
	
	public DistributionChartController (Model model, List<String> flags, DistributionChart view, DistributionChartConfig config, ViewController viewController) {
		super (model, viewController);

		assert (model != null);
		assert (view != null);
		assert (config != null);
		assert (viewController != null);

		this.view = view;

		Calendar now = Calendar.getInstance();
		int endYear = now.get (Calendar.YEAR);
		now.setTime (viewController.getProject ().getDate ());
		int startYear = now.get(Calendar.YEAR);

		view.setYearRange (startYear, endYear);

		Map<String, Object> vars = viewController.getVariables ();

		try {
			DistributionChartConfigData configData =  model.getDistributionChartData (config, vars);
			view.addConfiguration (configData, flags);

			for (DistributionChart.ChartIdentifier identifier : view.getIdentifiers ()) {
				drawLine (identifier);
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void timeSelectionChanged () {
		for (DistributionChart.ChartIdentifier identifier : view.getIdentifiers ()) {
			try {
				view.removeData (identifier);
				drawLine (identifier);
			} catch (SemanticException e) {
				// TODO
				e.printStackTrace ();				
			} catch (SQLException e) {
				// TODO 
				e.printStackTrace();
			}
		}
	}

	@Override
	public void chartSaveRequest (String path) {
		// Already handled
	}

	@Override
	public void chartSelectionChanged () {
		DistributionChart.SelectedChart chartType = view.getSelectedChart ();
		JFreeChart chart = view.createChart (chartType);
		view.setChart (chart);
	}

	@Override
	public void optionChanged (DistributionChart.ChartIdentifier identifier) {
		assert (identifier != null);

		try {
			view.removeData (identifier);
			drawLine (identifier);
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace ();				
		} catch (SQLException e) {
			// TODO
			e.printStackTrace();
		}
	}
	
	private void drawLine (DistributionChart.ChartIdentifier identifier) throws SemanticException, SQLException {
		assert (identifier != null);

		Map<String, Object> vars = getVariables (identifier);
		Query dataQuery = identifier.getSelectedAttributeConfig ()
			.getQuery ();

		DistributionChartData data = model.getDistributionChartData (dataQuery, vars);
		view.addData (identifier, data);
	}

	private Map<String, Object> getVariables (DistributionChart.ChartIdentifier identifier) {
		assert (identifier != null);

		Map<String, Object> map = viewController.getVariables ();
		map.put ("year", view.getSelectedYear ());

		for (Map.Entry<DropDownData, Integer> entry : identifier.getSelectedFilterConfigs ().entrySet()) {
			String name = entry.getKey ().getConfig ().getVariableName ();
			Integer value = entry.getKey ().getData ().get (entry.getValue ()).id;

			map.put (name, value);
		}
		return map;
	}
}
