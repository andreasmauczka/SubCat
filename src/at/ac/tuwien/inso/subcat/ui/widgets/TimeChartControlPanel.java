/* TimeChartControlPanel.java
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

package at.ac.tuwien.inso.subcat.ui.widgets;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.experimental.chart.swt.ChartComposite;

import at.ac.tuwien.inso.subcat.ui.events.TimeChartControlPanelListener;


public class TimeChartControlPanel<T> extends Composite {

	public static final String[] MONTHS = {
			"Jan",
			"Feb",
			"Mar",
			"Apr",
			"May",
			"Jun",
			"Jul",
			"Aug",
			"Sep",
			"Oct",
			"Nov",
			"Dec"
		};

	private Combo yearSelector;
	private Button btnPrev;
	private Button btnNext;
	private int baseYear;

	private List<T> chartSelectionData = new ArrayList<T> ();
	private Combo chartSelector;

	private ScrolledComposite scrolledComposite;
	private ChartComposite chartComposite;
	// TODO: replace with ChartComposite.getChart ()
	private JFreeChart chart;

	private LinkedList<TimeChartControlPanelListener> listeners
		= new LinkedList<TimeChartControlPanelListener> ();

	
	//
	// Helper:
	//

	private void triggerChartSelected () {
		int index = yearSelector.getSelectionIndex();
		if (index < 0) {
			return ;
		}

		for (TimeChartControlPanelListener listener : listeners) {
			listener.timeSelectionChanged();
		}
	}


	//
	// Public API:
	//
	
	
	// ** Composite Setup:

	public TimeChartControlPanel (Composite parent, int style) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(3, false));
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		gl_composite.verticalSpacing = 0;
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		gl_composite.horizontalSpacing = 0;
		composite.setLayout(gl_composite);



		// Chart, Top Panel:
		new Label (composite, SWT.NONE);;		

		Composite chartTopComposite = new Composite(composite, SWT.NONE);
		GridLayout chartBottomLayout = new GridLayout(3, true);
		chartTopComposite.setLayout(chartBottomLayout);
		chartBottomLayout.verticalSpacing = 0;
		chartBottomLayout.marginWidth = 0;
		chartBottomLayout.marginHeight = 0;
		chartBottomLayout.horizontalSpacing = 0;

		chartTopComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		yearSelector = new Combo (chartTopComposite, SWT.READ_ONLY);

		chartTopComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		chartSelector = new Combo (chartTopComposite, SWT.READ_ONLY);
		chartSelector.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

		chartTopComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		Button saveButton = new Button (chartTopComposite, SWT.NONE);
		saveButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		saveButton.setText("Save");

		new Label (composite, SWT.NONE);


		// Chart Row:
		btnPrev = new Button(composite, SWT.NONE);
		btnPrev.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		btnPrev.setText("<");

		// TODO: drop scrolledComposite 
		scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setLayout(new FillLayout ());

		chartComposite = new ChartComposite(scrolledComposite, SWT.NONE, null);
		chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		chartComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
		scrolledComposite.setContent (chartComposite);
		
		btnNext = new Button(composite, SWT.NONE);
		btnNext.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		btnNext.setText(">");

		// ** Event handling:

		chartSelector.addSelectionListener(new SelectionListener () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (TimeChartControlPanelListener listener : listeners) {
					listener.chartSelectionChanged();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		
		// *** Year Setting:
		yearSelector.addSelectionListener(new SelectionListener () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnPrev.setEnabled(hasPrevChart ());
				btnNext.setEnabled(hasNextChart ());

				triggerChartSelected ();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		btnPrev.addSelectionListener(new SelectionListener () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				yearSelector.select (yearSelector.getSelectionIndex() - 1);
				btnPrev.setEnabled(hasPrevChart ());
				btnNext.setEnabled(hasNextChart ());

				triggerChartSelected ();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		btnNext.addSelectionListener(new SelectionListener () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				yearSelector.select (yearSelector.getSelectionIndex() + 1);
				btnPrev.setEnabled(hasPrevChart ());
				btnNext.setEnabled(hasNextChart ());

				triggerChartSelected ();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});


		saveButton.addSelectionListener(new SelectionListener () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(TimeChartControlPanel.this.getShell(), SWT.SAVE);
				fd.setText("Save Chart");
				String[] filterExt = { "*.png", "*.jpg" };
				fd.setFilterExtensions(filterExt);
				String selectedPath = fd.open();
				if (selectedPath == null) {
					return ;
				}
				
				for (TimeChartControlPanelListener listener : listeners) {
					listener.chartSaveRequest (selectedPath);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	public void addChartSelectionEntry (String title, T data) {
		assert (title != null);
		assert (data != null);
		
		chartSelector.add (title);
		chartSelector.select (0);
		
		chartSelectionData.add (data);
	}
	
	public List<T> getChartSelectionEntries () {
		return chartSelectionData;
	}

	public T getSelectedChart () {
		return chartSelectionData.get (chartSelector.getSelectionIndex());
	}

	public void setChart (JFreeChart chart) {
		assert (chart != null);

		chartComposite.setChart(chart);
		this.chart = chart;
	}
	
	public JFreeChart getChart () {
		return this.chart;
	}

	
	// ** Time Selection:
	
	public void setYearRange (int start, int end) {
		assert (start <= end);

		baseYear = start;

		for (int year = start; year <= end; year++) {
			Integer yearObj = new Integer (year);
			yearSelector.add(yearObj.toString());
		}

		yearSelector.select(end - start);

		btnPrev.setEnabled(hasPrevChart ());
		btnNext.setEnabled(false);
	}

	public Integer getSelectedYear () {
		int index = yearSelector.getSelectionIndex();
		if (index < 0) {
			return -1;
		}

		return new Integer (baseYear + index);
	}
	
	public boolean hasNextChart () {
		return yearSelector.getSelectionIndex() < yearSelector.getItemCount() - 1;
	}

	public boolean hasPrevChart () {
		return yearSelector.getSelectionIndex() > 0;
	}

	
	// ** Save:

	public void saveChartAsJPEG (File file) throws IOException {
		saveChartAsJPEG (file, 800, 600);
	}

	public void saveChartAsJPEG (File file, int width, int height) throws IOException {
		assert (chart != null);

		ChartUtilities.saveChartAsJPEG(file, chart, width, height);
	}

	public void saveChartAsPNG (File file) throws IOException {
		saveChartAsJPEG (file, 800, 600);
	}

	public void saveChartAsPNG (File file, int width, int height) throws IOException {
		assert (chart != null);
		
		ChartUtilities.saveChartAsPNG (file, chart, width, height);
	}

	

	// ** Listener:
	
	public void addPanelListener (TimeChartControlPanelListener listener) {
		assert (listener != null);

		this.listeners.add (listener);
	}
	
	public void removePanelListener (TimeChartControlPanelListener listener) {
		assert (listener != null);

		this.listeners.remove (listener);
	}

	
	//
	// Test Method:
	//

	/*
	public static void main (String[] args) {
		// Data:

		
		// Gui:
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());

	    ScrolledComposite scroll = new ScrolledComposite(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		
		TimeChartControlPanel<Object> view = new TimeChartControlPanel<Object> (scroll, SWT.NONE);
	    scroll.setLayout(new GridLayout(1, true));

	    
		CategoryDataset dataset = new DefaultCategoryDataset ();
		NumberAxis rangeAxis2 = new NumberAxis("Values");
		rangeAxis2.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		BarRenderer renderer2 = new BarRenderer();
		renderer2.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
		CategoryPlot sizePlot = new CategoryPlot(dataset , null, rangeAxis2, renderer2);
		sizePlot.setDomainGridlinesVisible(true);

		JFreeChart chart = new JFreeChart(null, null, sizePlot, true);
		view.setYearRange(2000, 2014);
		view.setChart(chart);

	    scroll.setMinSize(view.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	    scroll.setContent(view);
	    scroll.setExpandVertical(true);
	    scroll.setExpandHorizontal(true);
	    scroll.setAlwaysShowScrollBars(true);

		
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
