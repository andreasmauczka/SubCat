/* TrendViewController.java
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
import java.util.LinkedList;
import java.util.Map;

import org.jfree.chart.JFreeChart;

import at.ac.tuwien.inso.subcat.config.DropDownConfig;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.config.TrendChartConfig;
import at.ac.tuwien.inso.subcat.config.TrendChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.TrendChartPlotConfig;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.TrendChartConfigData;
import at.ac.tuwien.inso.subcat.model.TrendChartData;
import at.ac.tuwien.inso.subcat.ui.events.TrendViewListener;
import at.ac.tuwien.inso.subcat.ui.widgets.TrendView;
import at.ac.tuwien.inso.subcat.ui.widgets.TrendView.ChartIdentifier;


public class TrendViewController extends ChartController implements TrendViewListener {
	private TrendView view;


	public TrendViewController (Model model, TrendView view, TrendChartGroupConfig groupConfig, ViewController viewController) {
		super (model, viewController);

		assert (model != null);
		assert (view != null);
		assert (groupConfig != null);
		assert (viewController != null);
		
		this.view = view;


		Calendar now = Calendar.getInstance();
		int endYear = now.get (Calendar.YEAR);
		now.setTime (viewController.getProject ().getDate ());
		int startYear = now.get (Calendar.YEAR);

		view.setYearRange (startYear, endYear);

		Map<String, Object> vars = viewController.getVariables ();

		try {
			for (TrendChartConfig trendConf : groupConfig.getTrendChartConfigs ()) {
				TrendChartConfigData trendConfData;
					trendConfData = model.getChartGroupConfigData (trendConf, vars);
				view.addConfiguration (trendConfData);
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO
			e.printStackTrace();
		}
	}
	
	@Override
	public void timeSelectionChanged () {
		LinkedList<ChartIdentifier> identifiers = new LinkedList<ChartIdentifier> (view.getSelectedIdentifiers());
		for (ChartIdentifier identifier : identifiers) {
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
		TrendView.SelectedChart chartType = view.getSelectedChart ();
		JFreeChart chart = view.createChart (chartType);
		view.setChart (chart);
	}

	@Override
	public void optionSelected (ChartIdentifier identifier, boolean checked) {
		assert (identifier != null);

		try {
			if (checked == false) {
				view.removeData (identifier);
			} else {
				drawLine (identifier);
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO
			e.printStackTrace();
		}
	}
	
	@Override
	public void comboChanged (DropDownConfig config) {
		assert (config != null);

		try {
			LinkedList<ChartIdentifier> identifiers = new LinkedList<ChartIdentifier> (view.getSelectedIdentifiers());
			for (ChartIdentifier identifier : identifiers) {
				if (((DropDownConfig) identifier.getTopConfig ()) == config) {
					view.removeData(identifier);
					drawLine (identifier);
				}
			}
		} catch (SemanticException e) {
			// TODO
			e.printStackTrace ();
		} catch (SQLException e) {
			// TODO
			e.printStackTrace();
		}
	}

	private void drawLine (ChartIdentifier identifier) throws SemanticException, SQLException {
		assert (identifier != null);

		Map<String, Object> vars = getVariables (identifier);
		TrendChartPlotConfig dataQuery = (TrendChartPlotConfig) identifier.getDropDownData ().getConfig ();
		TrendChartData data = model.getTrendChartData (dataQuery, vars);
		view.addData (identifier, data);
	}

	private Map<String, Object> getVariables (ChartIdentifier identifier) {
		assert (identifier != null);

		Map<String, Object> vars = viewController.getVariables ();
		vars.put ("year", view.getSelectedYear ());
		vars.put (identifier.getLeftConfig ().getVariableName (), identifier.getLeftId ());
		vars.put (identifier.getTopConfig ().getVariableName (), identifier.getTopId ());
		return vars;
	}
}
