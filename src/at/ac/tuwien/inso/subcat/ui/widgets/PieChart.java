/* PieChartView.java
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

package at.ac.tuwien.inso.subcat.ui.widgets;

import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import at.ac.tuwien.inso.subcat.model.PieChartData;


public class PieChart extends Composite {

	//
	// Helper:
	//
	
	/**
	 * A single status segment
	 */
	private class StateItem extends Composite {
		private Label lblTitle;
	
		public StateItem(Composite parent, int style, String title, Map<String, Integer> stats, int total, boolean showTotal) {
			super(parent, style);
	
			assert (title != null);
			assert (stats != null);

			// UI:
	
			setLayout(new GridLayout (3, false));
			
			lblTitle = new Label (this, SWT.NONE);
			Helper.setLabelStyle (lblTitle, SWT.BOLD);
			lblTitle.setLayoutData(new GridData (SWT.LEFT, SWT.CENTER, true, false, 3, 1));
			lblTitle.setText(title);
			Helper.separator (this, 3);
			new Label(this, SWT.NONE);

			
			Composite composite = new Composite (this, SWT.NONE);
			composite.setLayout (new GridLayout (4, false));
			composite.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));

			DefaultPieDataset dataset = new DefaultPieDataset ();

			org.eclipse.swt.graphics.Color background = parent.getBackground ();
			Paint backgroundPaint =  new Color (background.getRed (), background.getGreen (), background.getBlue ());

			JFreeChart pieChart = ChartFactory.createPieChart (null, dataset, false, true, false);
			pieChart.setBackgroundPaint(backgroundPaint);
	
			PiePlot plot = (PiePlot) pieChart.getPlot ();
			// Hide labels:
			plot.setLabelGenerator (null);
			// Background colour:
			plot.setBackgroundPaint (backgroundPaint);
			// Remove Boarder:
		    plot.setOutlineVisible (false);
			
			plot.setNoDataMessage ("No data available");
			plot.setCircular (true);
	
			
			ChartComposite chartComposite = new ChartComposite (this, SWT.NONE, pieChart);
			chartComposite.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, false, 1, 1));
			chartComposite.setLayout (new FillLayout (SWT.HORIZONTAL));
	
	
			// Data:
			DrawingSupplier drawingSupplier = plot.getDrawingSupplier ();
	
			for (Entry<String, Integer> set : stats.entrySet ()) {
				String entryTitle = set.getKey ();
				Integer val = set.getValue();
	
				dataset.setValue (entryTitle, val);
	
				java.awt.Color colour = (java.awt.Color) drawingSupplier.getNextPaint ();
				plot.setSectionPaint (entryTitle, colour);
	
				Label lblCol = new Label(composite, SWT.NONE);
				lblCol.setForeground(new org.eclipse.swt.graphics.Color (null, colour.getRed (), colour.getGreen (), colour.getBlue ()));
				lblCol.setText ("■");
	
				Label lblCat = new Label (composite, SWT.NONE);
				lblCat.setText (entryTitle);
				
				Label lblVal = new Label (composite, SWT.NONE);
				lblVal.setLayoutData (new GridData (SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
				lblVal.setText (val.toString ());
				
				
				String percStr = "-";
				if (total > 0) {
					DecimalFormat df = new DecimalFormat ("0.00");
					double perc = (val / (double) total) * 100;
					percStr = df.format (perc) + "%";
				}

				Label lblPerc = new Label (composite, SWT.NONE);
				lblPerc.setText (percStr);

				GridData spacedGridData = new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1);
				lblPerc.setLayoutData(spacedGridData);
				spacedGridData.horizontalIndent = 10;
			}

			
			if (showTotal) {
				Helper.separator (composite, 4);
	
				Label lblCol = new Label (composite, SWT.NONE);
				lblCol.setText ("■");
	
				Label lblCat = new Label (composite, SWT.NONE);
				Helper.setLabelStyle (lblCat, SWT.ITALIC);
				lblCat.setText ("Total:");
	
				Label lblVal = new Label (composite, SWT.NONE);
				lblVal.setLayoutData (new GridData (SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
				lblVal.setText (new Integer (total).toString ());

				Label lblPerc = new Label (composite, SWT.NONE);
				if (total > 0) {
					lblPerc.setText ("100.00%");
				} else {
					lblPerc.setText ("-");
				}

				GridData spacedGridData = new GridData (SWT.FILL, SWT.FILL, false, false, 1, 1);
				lblPerc.setLayoutData (spacedGridData);
				spacedGridData.horizontalIndent = 10;
			}
		}
	}

	

	//
	// Public API:
	//
	
	public PieChart (Composite parent, int style) {
		super (parent, style);

		setLayout (new GridLayout (2, true));
	}

	public void add (PieChartData data) {
		StateItem item = new StateItem (this, 0, data.getName (), data.getData (), data.getTotal (), data.getShowTotal ()); // TODO: showTotal
		item.setLayoutData(new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));
	}
	
	
	//
	// Test Method:
	//

/*
	public static void main (String[] args) {
		// Test data:
		Project project = new Project (1, new Date (), "domain", "product", "revision");
		
		HashMap<Category, Integer> stats = new HashMap<Category, Integer> ();
		stats.put(new Category (null, project, "Cat1"), 10);
		stats.put(new Category (null, project, "Cat2"), 20);
		stats.put(new Category (null, project, "Cat3"), 30);
		
		
		// Gui:
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());
	
		StateView view = new StateView (shell, SWT.NONE);
		view.add("Fooo 1", stats, true);
		view.add("Fooo 2", stats, true);
		view.add("Fooo 3", stats, true);
		
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) {
				display.sleep ();
			}
		}
		
		display.dispose();
	}
*/
}
