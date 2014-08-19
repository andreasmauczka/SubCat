/* DistributionView.java
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

package at.ac.tuwien.inso.hurrier.ui.widgets;

import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import at.ac.tuwien.inso.hurrier.config.DistributionAttributeConfig;
import at.ac.tuwien.inso.hurrier.config.DistributionAttributesConfig;
import at.ac.tuwien.inso.hurrier.config.DistributionChartOptionConfig;
import at.ac.tuwien.inso.hurrier.model.DistributionChartConfigData;
import at.ac.tuwien.inso.hurrier.model.DistributionChartData;
import at.ac.tuwien.inso.hurrier.model.DistributionChartOptionConfigData;
import at.ac.tuwien.inso.hurrier.model.DropDownData;
import at.ac.tuwien.inso.hurrier.ui.events.DistributionViewListener;


public class DistributionView extends Composite {
	private TimeChartControlPanel<SelectedChart> timeChart;
	private ScrolledComposite scrolledComposite;
	private Composite optionComposite;
	private LinkedList<DistributionViewListener> listeners;
	private LinkedList<ChartIdentifier> identifiers;

	private DrawingSupplier drawingSupplier;
	private CategoryPlot sizePlot;
	private CategoryPlot boxPlot;
	private DefaultBoxAndWhiskerCategoryDataset boxDataSet;
	private DefaultCategoryDataset sizeDataSet;
	private CombinedDomainCategoryPlot plot;

	private SelectionListener configSelectionListener;

	private int optionCount = 0;


	public enum SelectedChart {
		SIZE,
		DISTRIBUTION,
		BOTH
	}

	public static class ChartIdentifier implements Comparable<ChartIdentifier> {
		private Paint paint;
		private DistributionChartOptionConfig config;
		private LinkedList<Combo> filter;
		private Combo attributes;
		private Integer weight;
		

		public ChartIdentifier (DistributionChartOptionConfig config, Integer weight) {
			assert (config != null);
			assert (weight != null);
			
			this.filter = new LinkedList<Combo> ();
			this.config = config;
			this.weight = weight;
		}

		public DistributionChartOptionConfig getDistributionChartOptionConfig () {
			return config;
		}

		public DistributionAttributeConfig getSelectedAttributeConfig () {
			int selectionIndex = attributes.getSelectionIndex ();
			DistributionAttributesConfig config = (DistributionAttributesConfig) attributes.getData ();
			return config.getData ().get(selectionIndex);
		}

		public Map<DropDownData, Integer> getSelectedFilterConfigs () {
			HashMap<DropDownData, Integer> map = new HashMap<DropDownData, Integer> ();
			
			for (Combo combo : filter) {
				DropDownData data = (DropDownData) combo.getData ();
				int selection = combo.getSelectionIndex ();

				map.put (data, selection);
			}
			
			return map;
		}
		
		@Override
		public int compareTo (ChartIdentifier o) {
			assert (o != null);

			return weight.compareTo (o.weight);
		}
	}
	
	public DistributionView (Composite parent, int style) {
		super (parent, style);

		listeners = new LinkedList<DistributionViewListener> ();
		identifiers = new LinkedList<ChartIdentifier> ();
		initCharts ();
		
		setLayout (new FillLayout ());

		SashForm sashForm = new SashForm (this, SWT.VERTICAL);

		timeChart = new TimeChartControlPanel<SelectedChart> (sashForm, SWT.BORDER);
		timeChart.addChartSelectionEntry ("Distribution View", SelectedChart.DISTRIBUTION);
		timeChart.addChartSelectionEntry ("Bar View", SelectedChart.SIZE);
		timeChart.addChartSelectionEntry ("Both", SelectedChart.BOTH);

		JFreeChart chart = createChart (SelectedChart.DISTRIBUTION);
		setChart (chart);

		
		scrolledComposite = new ScrolledComposite (sashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrolledComposite.setLayout (new FillLayout ());

		optionComposite = new Composite (scrolledComposite, SWT.NONE);
		optionComposite.setLayout (new GridLayout (3, false));

	    scrolledComposite.setContent (optionComposite);
	    scrolledComposite.setMinSize (optionComposite.computeSize (SWT.DEFAULT, SWT.DEFAULT));

	    scrolledComposite.setExpandVertical (true);
		scrolledComposite.setExpandHorizontal (true);
	    scrolledComposite.setAlwaysShowScrollBars (false);

	
	    configSelectionListener = new SelectionListener () {

			@Override
			public void widgetSelected (SelectionEvent e) {
				Combo combo = (Combo) e.getSource ();
				ChartIdentifier identifier = (ChartIdentifier) combo.getData ("identifier");
				assert (identifier != null);

				for (DistributionViewListener listener : listeners) {
					listener.optionChanged (identifier);
				}
			}

			@Override
			public void widgetDefaultSelected (SelectionEvent e) {
			}
	    };
	}

	private void initCharts () {
		boxDataSet = new DefaultBoxAndWhiskerCategoryDataset ();
		sizeDataSet = new DefaultCategoryDataset ();

		// Box:
		NumberAxis yAxis = new NumberAxis ("Values");
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer ();
		renderer.setBaseToolTipGenerator (new StandardCategoryToolTipGenerator ());
		
		boxPlot = new CategoryPlot (boxDataSet, null, yAxis, renderer);
		drawingSupplier = boxPlot.getDrawingSupplier ();

		
		// Bar.
		NumberAxis rangeAxis2 = new NumberAxis ("Values");
		rangeAxis2.setStandardTickUnits (NumberAxis.createIntegerTickUnits ());
		BarRenderer renderer2 = new BarRenderer ();
		renderer2.setBaseToolTipGenerator (new StandardCategoryToolTipGenerator ());
		sizePlot = new CategoryPlot (sizeDataSet, null, rangeAxis2, renderer2);
		sizePlot.setDomainGridlinesVisible (true);
	}
	
	public void addConfiguration (DistributionChartConfigData config) {
		assert (config != null);

		for (DistributionChartOptionConfigData optionConf : config.getOptions ()) {
			addOptionConfiguration (optionConf);
		}
	}

	private void addOptionConfiguration (DistributionChartOptionConfigData config) {
		assert (config != null);
		assert (config.getAttributes() != null);

		if (config.getAttributes().getData().size() == 0) {
			return ;
		}
		
		if (optionCount > 0) {
			Helper.separator(optionComposite, 3);
		}


		ChartIdentifier identifier = new ChartIdentifier (config.getConfig (), optionCount);
		identifiers.add (identifier);

		Label lblTitle = new Label (optionComposite, SWT.NONE);
		lblTitle.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Helper.setLabelStyle (lblTitle, SWT.BOLD);
		lblTitle.setText (config.getName ());

		Iterator<DropDownData> filterIter = config.getFilter ().iterator ();
		if (filterIter.hasNext ()) {
			Combo filterCombo = new Combo (optionComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
			filterCombo.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));
			fillCombo (filterCombo, filterIter.next (), identifier);
			identifier.filter.add (filterCombo);
		} else {
			new Label (optionComposite, SWT.NONE);
		}

		Combo attCombo = new Combo (optionComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fillAttributeCombo (attCombo, config.getAttributes (), identifier);
		attCombo.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));
		identifier.attributes = attCombo;

		while (filterIter.hasNext ()) {
			new Label (optionComposite, SWT.NONE);
			Combo filterCombo = new Combo (optionComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
			filterCombo.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1));
			new Label (optionComposite, SWT.NONE);

			fillCombo (filterCombo, filterIter.next (), identifier);
			identifier.filter.add (filterCombo);
		}

		identifier.paint = drawingSupplier.getNextPaint();
		optionCount++;
	}

	private void fillAttributeCombo(Combo combo, DistributionAttributesConfig attributes, ChartIdentifier identifier) {
		assert (combo != null);
		assert (attributes != null);
		assert (identifier != null);

		combo.setData ("identifier", identifier);
		combo.setData (attributes);

		for (DistributionAttributeConfig item : attributes.getData()) {
			combo.add (item.getName ());
		}

		combo.select (0);
		combo.addSelectionListener (configSelectionListener);
	}

	private void fillCombo (Combo combo, DropDownData data, ChartIdentifier identifier) {
		assert (combo != null);
		assert (data != null);
		assert (identifier != null);

		combo.setData ("identifier", identifier);
		combo.setData (data);
		
		for (DropDownData.Pair item : data.getData()) {
			combo.add (item.name);
		}

		combo.select (0);
		combo.addSelectionListener (configSelectionListener);
	}

	public void addData (ChartIdentifier identifier, DistributionChartData data) {
		assert (identifier != null);
		assert (data != null);

		LinkedList<Double> emptyList = new LinkedList<Double> ();
		
		for (int i = 0; i < 12 ; i++) {
			double mean = data.getMean (i);
			double median = data.getMedian (i);
			double q1 = data.getQ1 (i);
			double q3 = data.getQ3 (i);
			double min = data.getMin (i);
			double max = data.getMax (i);
			double count = data.getCount (i);

			// Box Plot Dataset:
			BoxAndWhiskerItem item = new BoxAndWhiskerItem (mean, median, q1, q3, min, max, null, null, emptyList); 
			boxDataSet.add (item, identifier, TimeChartControlPanel.MONTHS[i]);

			// Bar Dataset:
			sizeDataSet.addValue (count, identifier, TimeChartControlPanel.MONTHS[i]);
		}

		fixChartColours ();
	}

	public void removeData (ChartIdentifier identifier) {
		assert (identifier != null);

		boxDataSet.removeRow (identifier);
		sizeDataSet.removeRow (identifier);
		
		fixChartColours ();
	}

	private void fixChartColours () {
		for (int i = 0; i < boxDataSet.getRowCount() ; i++) {
			boxPlot.getRenderer().setSeriesPaint(i, ((ChartIdentifier) boxDataSet.getRowKey(i)).paint);
			sizePlot.getRenderer().setSeriesPaint(i, ((ChartIdentifier) boxDataSet.getRowKey(i)).paint);
		}
	}

	public void setYearRange(int startYear, int endYear) {
		timeChart.setYearRange (startYear, endYear);
	}

	public Integer getSelectedYear () {
		return timeChart.getSelectedYear();
	}

	public void saveChartAsJPEG (File file) throws IOException {
		timeChart.saveChartAsJPEG (file, 800, 600);
	}

	public void saveChartAsJPEG (File file, int width, int height) throws IOException {
		timeChart.saveChartAsJPEG(file, width, height);
	}

	public void saveChartAsPNG (File file) throws IOException {
		timeChart.saveChartAsJPEG (file, 800, 600);
	}

	public void saveChartAsPNG (File file, int width, int height) throws IOException {
		timeChart.saveChartAsPNG(file, width, height);
	}

	public boolean hasNextChart () {
		return timeChart.hasNextChart();
	}

	public boolean hasPrevChart () {
		return timeChart.hasPrevChart();
	}

	public SelectedChart getSelectedChart () {
		return timeChart.getSelectedChart ();
	}

	public void setChart (JFreeChart chart) {
		timeChart.setChart (chart);
		// Force the view to update the chart:
		chart.clearSubtitles();
	}

	public List<ChartIdentifier> getIdentifiers () {
		return identifiers;
	}

	public JFreeChart createChart (SelectedChart chart) {
		CategoryAxis domainAxis = new CategoryAxis ("Date");
		plot = new CombinedDomainCategoryPlot (domainAxis);
		if (drawingSupplier == null) {
			drawingSupplier = plot.getDrawingSupplier ();
		} else {
			plot.setDrawingSupplier (drawingSupplier);
		}


		switch (chart) {
		case DISTRIBUTION:
			plot.add (boxPlot, 1);
			break;
		
		case SIZE:
			plot.add (sizePlot, 1);
			break;
		
		case BOTH:
			plot.add (boxPlot, 1);
			plot.add (sizePlot, 2);
			break;

		default:
			assert (false);
		}

		JFreeChart result = new JFreeChart (
				null,
				null,
				plot,
				true
			);

		return result;
	}


	//
	// Listeners:
	//
	
	public void addDistributionViewListener (DistributionViewListener listener) {
		assert (listener != null);

		this.timeChart.addPanelListener (listener);
		this.listeners.add (listener);
	}
	
	public void removeDistributionViewListener (DistributionViewListener listener) {
		assert (listener != null);

		this.timeChart.removePanelListener (listener);
		this.listeners.remove (listener);
	}


	//
	// Test Main:
	//
	
	/*
	public static void main (String[] args) {
		// UI:
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());

		DistributionView view = new DistributionView (shell, SWT.NONE);
		view.addOption("Bugs", new String[][]{
				{"Filter 1", "Filter 2"},
				{"Filter 1", "Filter 2"}
			},
			new String[] {"Att1", "Att2", "Att3"});

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
