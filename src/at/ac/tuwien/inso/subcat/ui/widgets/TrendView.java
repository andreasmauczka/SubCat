/* TrendView.java
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


import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import at.ac.tuwien.inso.subcat.config.DropDownConfig;
import at.ac.tuwien.inso.subcat.config.OptionListConfig;
import at.ac.tuwien.inso.subcat.config.TrendChartPlotConfig;
import at.ac.tuwien.inso.subcat.model.DropDownData;
import at.ac.tuwien.inso.subcat.model.OptionListConfigData;
import at.ac.tuwien.inso.subcat.model.TrendChartConfigData;
import at.ac.tuwien.inso.subcat.model.TrendChartData;
import at.ac.tuwien.inso.subcat.ui.events.TrendViewListener;


public class TrendView extends Composite {
	private LinkedList<TrendViewListener> listeners
		= new LinkedList<TrendViewListener> ();
	
	private TimeChartControlPanel<SelectedChart> timeChart;
	private CategoryPlot trendPlot;
	private DefaultCategoryDataset dataset;
	private CategoryPlot sizePlot;
	private CombinedDomainCategoryPlot plot;
	private DrawingSupplier drawingSupplier;
	private ScrolledComposite scrolledComposite;
	private Composite optionComposite;

	private int boxWeight;
	private SelectionListener boxListener;
	private SelectionListener comboListener;
	
	public enum SelectedChart {
		LINE,
		BAR,
		BOTH
	}

	public static class ChartIdentifier implements Comparable<ChartIdentifier> {
		private final TrendChartPlotConfig topConfig;
		private final OptionListConfig leftConfig;

		private final int left;
		private final Combo top;
		private final Integer weight;
		private Paint paint;
	
		public ChartIdentifier (TrendChartPlotConfig topConfig, Combo top, OptionListConfig leftConfig, int left, int weight) {
			assert (top != null);
			assert (topConfig != null);
			assert (leftConfig != null);

			this.topConfig = topConfig;
			this.leftConfig = leftConfig;
			this.top = top;
			this.left = left;
			this.weight = weight;
		}

		@Override
		public int compareTo (ChartIdentifier o) {
			assert (o != null);

			return weight.compareTo (o.weight);
		}

		public DropDownData getDropDownData () {
			DropDownData dropData = (DropDownData) top.getData ();
			assert (dropData != null);
			return dropData;
		}

		public int getLeftId () {
			return left;
		}
		
		public int getTopId () {
			return getDropDownData ().getData().get(top.getSelectionIndex ()).id;
		}

		public Paint getPaint () {
			return paint;
		}

		public DropDownConfig getTopConfig () {
			return topConfig;
		}

		public OptionListConfig getLeftConfig () {
			return leftConfig;
		}
	}


	//
	// GUI:
	//

	public TrendView (Composite parent, int style) {
		super (parent, style);
		initCharts ();

		setLayout (new FillLayout ());

		SashForm sashForm = new SashForm (this, SWT.VERTICAL);

		timeChart = new TimeChartControlPanel<SelectedChart> (sashForm, SWT.BORDER);
		timeChart.addChartSelectionEntry ("Trend View", SelectedChart.LINE);
		timeChart.addChartSelectionEntry ("Bar View", SelectedChart.BAR);
		timeChart.addChartSelectionEntry ("Both", SelectedChart.BOTH);

		JFreeChart chart = createChart (SelectedChart.LINE);
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

	
	    // Signals:
		boxListener = new SelectionListener () {

			@Override
			public void widgetSelected (SelectionEvent e) {
				Button button = (Button) e.getSource();
				ChartIdentifier boxData = (ChartIdentifier) button.getData();
				assert (boxData != null);

				boolean checked = button.getSelection();


				if (boxData.paint == null) {
					boxData.paint = drawingSupplier.getNextPaint();
				}

				if (checked) {
					java.awt.Color paintColor = (java.awt.Color) boxData.paint;
					Color color = new Color (null, paintColor.getRed (), paintColor.getGreen(), paintColor.getBlue());
					button.setBackground (color);
				} else {
					button.setBackground(null);
				}


				for (TrendViewListener listener : listeners) {
					listener.optionSelected (boxData, checked);
				}
			}

			@Override
			public void widgetDefaultSelected (SelectionEvent e) {
			}
		};

		comboListener = new SelectionListener () {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo combo = (Combo) e.getSource ();
				DropDownData data = (DropDownData) combo.getData ();
				assert (data != null);
				DropDownConfig config = data.getConfig ();

				for (TrendViewListener listener : listeners) {
					listener.comboChanged (config);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private void initCharts () {
		dataset = new DefaultCategoryDataset ();

		// XY:s
		NumberAxis rangeAxis1 = new NumberAxis ("Values");
		rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits ());
		LineAndShapeRenderer renderer1 = new LineAndShapeRenderer ();
		renderer1.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator ());
		trendPlot = new CategoryPlot (dataset, null, rangeAxis1, renderer1);
		trendPlot.setDomainGridlinesVisible (true);

		// Box:
		NumberAxis rangeAxis2 = new NumberAxis ("Values");
		rangeAxis2.setStandardTickUnits (NumberAxis.createIntegerTickUnits ());
		BarRenderer renderer2 = new BarRenderer ();
		renderer2.setBaseToolTipGenerator (new StandardCategoryToolTipGenerator ());
		sizePlot = new CategoryPlot (dataset, null, rangeAxis2, renderer2);
		sizePlot.setDomainGridlinesVisible (true);
	}
	
	public JFreeChart createChart (SelectedChart chart) {
		assert (chart != null);

		CategoryAxis domainAxis = new CategoryAxis ("Date");
		plot = new CombinedDomainCategoryPlot (domainAxis);
		if (drawingSupplier == null) {
			drawingSupplier = plot.getDrawingSupplier ();
		} else {
			plot.setDrawingSupplier (drawingSupplier);
		}

		
		switch (chart) {
		case LINE:
			plot.add (trendPlot, 1);
			break;
			
		case BAR:
			plot.add (sizePlot, 1);
			break;
			
		case BOTH:
			plot.add (trendPlot, 1);
			plot.add (sizePlot, 2);
			break;

		default:
			assert (false);
		}


		JFreeChart result = new JFreeChart(
			null,
			null,
			plot,
			true);

		result.removeLegend ();

		return result;
    }

	private void fixChartColours () {
		for (int i = 0; i < dataset.getRowCount() ; i++) {
			sizePlot.getRenderer().setSeriesPaint(i, ((ChartIdentifier) dataset.getRowKey(i)).paint);
			trendPlot.getRenderer().setSeriesPaint(i, ((ChartIdentifier) dataset.getRowKey(i)).paint);
		}
	}

	public void addConfiguration (TrendChartConfigData config) {
		assert (config != null);

		LinkedList<Combo> combos = new LinkedList<Combo> ();
		
		// Title Row:
		Label lblGrpTitle = new Label (optionComposite, SWT.NONE);
		lblGrpTitle.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Helper.setLabelStyle (lblGrpTitle, SWT.BOLD);
		lblGrpTitle.setText (config.getName ());

		Composite topOptions = new Composite (optionComposite, SWT.NONE);
		topOptions.setLayoutData (new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		topOptions.setLayout (new GridLayout (config.getDropDowns ().size(), true));

		for (DropDownData dropData : config.getDropDowns ()) {
			Combo comboDropDown = new Combo (topOptions, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
			comboDropDown.setLayoutData (new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
			comboDropDown.setData (dropData);
			combos.add (comboDropDown);

			for (DropDownData.Pair data : dropData.getData()) {
				comboDropDown.add (data.name);
			}

			comboDropDown.select (0);

			comboDropDown.addSelectionListener(this.comboListener);
		}


		// Separator:
		Helper.separator (optionComposite, 3);		

	
		// Left Option Labels:
		new Label (optionComposite, SWT.NONE);

		Composite leftOptions = new Composite (optionComposite, SWT.NONE);
		leftOptions.setLayoutData (new GridData (SWT.FILL, SWT.CENTER, false, false, 1, 1));

		leftOptions.setLayout (new GridLayout (1, true));
		for (OptionListConfigData.Pair pair : config.getOptionList ().getData ()) {
			Label lblOpt = new Label (leftOptions, SWT.NONE);
			lblOpt.setText (pair.name);
		}


		// Check Boxes:
		Composite selectionComposite = new Composite (optionComposite, SWT.NONE);
		selectionComposite.setLayoutData(new GridData (SWT.FILL, SWT.CENTER, false, false, 1, 1));
		selectionComposite.setLayout (new GridLayout (combos.size (), true));


		OptionListConfig leftConfig = config.getOptionList().getConfig ();
		int x = 0;

		for (Combo combo : combos) {
			TrendChartPlotConfig topConfig = (TrendChartPlotConfig) config.getDropDowns().get (x).getConfig ();

			for (OptionListConfigData.Pair pair : config.getOptionList ().getData ()) {
				Button button = new Button (selectionComposite, SWT.CHECK);
				button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
				button.setData (new ChartIdentifier (topConfig, combo, leftConfig, pair.id, boxWeight++));
				button.addSelectionListener (boxListener);
			}

			x++;
		}


		// Scrolling area size update:
	    scrolledComposite.setMinSize (optionComposite.computeSize (SWT.DEFAULT, SWT.DEFAULT));
	}

	
	//
	// API:
	//
	
	public void setYearRange (int start, int end) {
		timeChart.setYearRange (start, end);
	}

	@SuppressWarnings("unchecked")
	public List<ChartIdentifier> getSelectedIdentifiers () {
		return (List<ChartIdentifier>) dataset.getRowKeys();
	}

	public List<ChartIdentifier> getSelectedIdentifiers (DropDownConfig config) {
		assert (config != null);
		
		LinkedList<ChartIdentifier> result = new LinkedList<ChartIdentifier> ();
		List<ChartIdentifier> selection = getSelectedIdentifiers ();

		for (ChartIdentifier id : selection) {
			if (id.getTopConfig () == config) {
				result.add (id);
			}
		}

		return result;
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

	//
	// Data:
	//
	
	public void addData (ChartIdentifier identifier, TrendChartData data) {
		assert (identifier != null);
		assert (data != null);

		for (int i = 0; i < 12; i++) {
			dataset.addValue(data.getData ()[i], identifier, TimeChartControlPanel.MONTHS[i]);
		}

		fixChartColours ();
	}

	public void removeData (ChartIdentifier chart) {
		assert (chart != null);

		dataset.removeRow (chart);

		fixChartColours ();
	}
	
	public void removeData (Collection<ChartIdentifier> charts) {
		assert (charts != null);

		for (ChartIdentifier id : charts) {
			removeData (id);
		}
	}
	

	//
	// Listeners:
	//
	
	public void addTrendViewListener (TrendViewListener listener) {
		assert (listener != null);

		this.timeChart.addPanelListener (listener);
		this.listeners.add (listener);
	}
	
	public void removeTrendViewListener (TrendViewListener listener) {
		assert (listener != null);

		this.timeChart.removePanelListener (listener);
		this.listeners.remove (listener);
	}


	//
	// Test Method:
	//
	
	/*
	public static void main (String[] args) {
		// Option Configuration:
		Project proj = new Project (null, new Date (), "My Project", "", "");
		LinkedList<NamedContainer> rows = new LinkedList<NamedContainer> ();
		rows.add (new Category (1, proj, "Foo"));
		rows.add (new Category (2, proj, "Bar"));
		rows.add (new Category (3, proj, "Baz"));

		// UI:
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());

		TrendView view = new TrendView (shell, SWT.NONE);

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
