/* DistributionChartController.java
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

package at.ac.tuwien.inso.hurrier.ui.controller;

import java.util.Calendar;
import java.util.Map;

import org.jfree.chart.JFreeChart;

import at.ac.tuwien.inso.hurrier.config.DistributionChartConfig;
import at.ac.tuwien.inso.hurrier.config.Query;
import at.ac.tuwien.inso.hurrier.config.SemanticException;
import at.ac.tuwien.inso.hurrier.model.DistributionChartConfigData;
import at.ac.tuwien.inso.hurrier.model.DistributionChartData;
import at.ac.tuwien.inso.hurrier.model.DropDownData;
import at.ac.tuwien.inso.hurrier.model.Model;
import at.ac.tuwien.inso.hurrier.ui.events.DistributionViewListener;
import at.ac.tuwien.inso.hurrier.ui.widgets.DistributionView;


public class DistributionChartController extends ChartController implements DistributionViewListener {
	private DistributionView view;
	
	public DistributionChartController (Model model, DistributionView view, DistributionChartConfig config, ViewController viewController) {
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
			view.addConfiguration (configData);

			for (DistributionView.ChartIdentifier identifier : view.getIdentifiers ()) {
				drawLine (identifier);
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace();
		}

	}

	@Override
	public void timeSelectionChanged () {
		for (DistributionView.ChartIdentifier identifier : view.getIdentifiers ()) {
			try {
				view.removeData (identifier);
				drawLine (identifier);
			} catch (SemanticException e) {
				// TODO
				e.printStackTrace ();				
			}
		}
	}

	@Override
	public void chartSaveRequest (String path) {
		// Already handled
	}

	@Override
	public void chartSelectionChanged () {
		DistributionView.SelectedChart chartType = view.getSelectedChart ();
		JFreeChart chart = view.createChart (chartType);
		view.setChart (chart);
	}

	@Override
	public void optionChanged (DistributionView.ChartIdentifier identifier) {
		assert (identifier != null);

		try {
			view.removeData (identifier);
			drawLine (identifier);
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace ();				
		}
	}
	
	private void drawLine (DistributionView.ChartIdentifier identifier) throws SemanticException {
		assert (identifier != null);

		Map<String, Object> vars = getVariables (identifier);
		Query dataQuery = identifier.getSelectedAttributeConfig ()
			.getQuery ();

		DistributionChartData data = model.getDistributionChartData (dataQuery, vars);
		view.addData (identifier, data);
	}

	private Map<String, Object> getVariables (DistributionView.ChartIdentifier identifier) {
		assert (identifier != null);

		Map<String, Object> map = viewController.getVariables ();
		for (Map.Entry<DropDownData, Integer> entry : identifier.getSelectedFilterConfigs ().entrySet()) {
			String name = entry.getKey ().getConfig ().getVariableName ();
			Integer value = entry.getValue ();

			map.put (name, value);
		}
		return map;
	}
}