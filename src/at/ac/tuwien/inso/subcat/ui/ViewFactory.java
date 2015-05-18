/* ViewFactory.java
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

package at.ac.tuwien.inso.subcat.ui;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import at.ac.tuwien.inso.subcat.config.ChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.ConfigVisitor;
import at.ac.tuwien.inso.subcat.config.Configuration;
import at.ac.tuwien.inso.subcat.config.DistributionChartConfig;
import at.ac.tuwien.inso.subcat.config.Parser;
import at.ac.tuwien.inso.subcat.config.ParserException;
import at.ac.tuwien.inso.subcat.config.PieChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.ProjectViewConfig;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.config.TeamViewConfig;
import at.ac.tuwien.inso.subcat.config.TrendChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.UserViewConfig;
import at.ac.tuwien.inso.subcat.config.ViewConfig;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.ui.controller.DistributionChartController;
import at.ac.tuwien.inso.subcat.ui.controller.PieChartController;
import at.ac.tuwien.inso.subcat.ui.controller.TrendChartController;
import at.ac.tuwien.inso.subcat.ui.controller.ViewController;
import at.ac.tuwien.inso.subcat.ui.events.UserListListener;
import at.ac.tuwien.inso.subcat.ui.widgets.DistributionChart;
import at.ac.tuwien.inso.subcat.ui.widgets.PieChart;
import at.ac.tuwien.inso.subcat.ui.widgets.TrendChart;
import at.ac.tuwien.inso.subcat.ui.widgets.UserList;
import at.ac.tuwien.inso.subcat.utility.Reporter;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;


public class ViewFactory {
	private Configuration config;
	private Model model;
	private List<String> flags;

	private class ViewBuilder extends ConfigVisitor {
		private TabFolder folder;
		private Project project;

		private ViewController viewController;


		// Used to pass exceptions to builder ()
		private SemanticException semanticException;

		
		public ViewBuilder () {
		}

		public ViewController build (ViewConfig config, TabFolder folder, Project project, HashMap<String, Object> vars) throws SemanticException, SQLException {
			assert (project != null);
			assert (config != null);
			assert (folder != null);

			// TODO: ViewConfig-visitors to select the right
			//   viewController
			viewController = new ViewController (project, vars);
			
			this.project = project;
			this.folder = folder;
			
			for (ChartGroupConfig groupConf : config.getChartGroups ()) {
				groupConf.accept (this);
	
				if (semanticException != null) {
					SemanticException e = this.semanticException;
					this.semanticException = null;
					throw e;
				}
			}

			this.folder = null;
			return viewController;
		}
		
		@Override
		public void visitPieChartGroupConfig (PieChartGroupConfig groupConfig) {
			assert (project != null);
			assert (folder != null);

			if (!groupConfig.show (flags)) {
				return ;
			}
			
			TabItem page = new TabItem (folder, SWT.NONE);
			page.setText (groupConfig.getName ());

			ScrolledComposite scroll = new ScrolledComposite (folder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			scroll.setLayout (new FillLayout ());

			PieChart view = new PieChart (scroll, SWT.NONE);
			PieChartController pieController = new PieChartController (model, view, groupConfig, flags, viewController);
			viewController.addChartController (pieController);

			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			page.setControl (scroll);

			scroll.setContent (view);
			scroll.setMinSize (view.computeSize (SWT.DEFAULT, SWT.DEFAULT));
		}

		@Override
		public void visitTrendChartGroupConfig (TrendChartGroupConfig groupConfig) {			
			assert (project != null);
			assert (folder != null);

			if (!groupConfig.show (flags)) {
				return ;
			}
			
			TabItem page = new TabItem (folder, SWT.NONE);
			page.setText (groupConfig.getName ());

			TrendChart view = new TrendChart (folder, SWT.NONE);
			TrendChartController trendController = new TrendChartController (model, flags, view, groupConfig, viewController);
			view.addTrendViewListener (trendController);
			viewController.addChartController (trendController);
			page.setControl (view);
		}

		@Override
		public void visitDistributionChartConfig (DistributionChartConfig config) {
			assert (project != null);
			assert (folder != null);

			if (!config.show (flags)) {
				return ;
			}
	
			TabItem page = new TabItem (folder, SWT.NONE);
			page.setText (config.getName ());

			DistributionChart view = new DistributionChart (folder, SWT.NONE);
			DistributionChartController distController = new DistributionChartController (model, flags, view, config, viewController);
			view.addDistributionViewListener (distController);
			viewController.addChartController (distController);
			page.setControl (view);
		}
	}


	public ViewFactory (Model model, Configuration config) {
		this.model = model;
		this.config = config;
	}

	public Composite createUserViewComposite (Composite parent, int style, Project proj, int commitDictId, int bugDictId, UserViewConfig config) throws SemanticException, SQLException {
		assert (config != null);
		assert (proj != null);
		assert (proj.getId () != null);

		this.flags = model.getFlags (proj);

		SashForm sash = new SashForm (parent, SWT.HORIZONTAL);
		sash.setLayout (new FillLayout ());
		final UserList users = new UserList (sash, SWT.NONE, model.getUsers (proj), false);

		TabFolder folder = new TabFolder (sash, SWT.NONE);
		model.updateUserSelection (new ArrayList<Integer> ());
		final HashMap<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", proj.getId ());
		vars.put ("commitDict", commitDictId);
		vars.put ("bugDict", bugDictId);
		vars.put ("user", -1);					

		ViewBuilder builder = new ViewBuilder ();
		final ViewController viewController = builder.build (config, folder, proj, vars);

		users.addUserListListener (new UserListListener () {
			@Override
			public void selectionChanged () {
				List<Integer> selectedUsers = users.getSelectedIDs ();
				assert (selectedUsers.size () <= 1);
				if (selectedUsers.size () > 0) {
					vars.put ("user", selectedUsers.get (0));
				} else {
					vars.put ("user", -1);					
				}
				viewController.variableChanged ();
			}
		});
		
		return sash;
	}

	public Composite createTeamViewComposite (Composite parent, int style, Project proj, int commitDictId, int bugDictId, TeamViewConfig config) throws SemanticException, SQLException {
		assert (config != null);
		assert (proj != null);
		assert (proj.getId () != null);

		this.flags = model.getFlags (proj);

		SashForm sash = new SashForm (parent, SWT.HORIZONTAL);
		sash.setLayout (new FillLayout ());
		final UserList users = new UserList (sash, SWT.NONE, model.getUsers (proj), true);

		TabFolder folder = new TabFolder (sash, SWT.NONE);
		model.updateUserSelection (new ArrayList<Integer> ());
		final HashMap<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", proj.getId ());
		vars.put ("commitDict", commitDictId);
		vars.put ("bugDict", bugDictId);

		ViewBuilder builder = new ViewBuilder ();
		final ViewController viewController = builder.build (config, folder, proj, vars);

		users.addUserListListener (new UserListListener () {
			@Override
			public void selectionChanged () {
				try {
					List<Integer> selectedUsers = users.getSelectedIDs ();
					model.updateUserSelection (selectedUsers);
					viewController.variableChanged ();
					if (model.getPrintTemplates ()) {
						System.out.println ("Selected Users:");
						for (Integer id : model.getUserSelection ()) {
							System.out.println (" - " + id);
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
					assert (false);
				}
			}
		});
		
		return sash;
	}

	public Composite createProjectViewComposite (Composite parent, int style, Project proj, int commitDictId, int bugDictId, ProjectViewConfig config) throws SemanticException, SQLException {
		assert (config != null);
		assert (proj != null);
		assert (proj.getId () != null);

		this.flags = model.getFlags (proj);

		Composite composite = new Composite (parent, style);
		composite.setLayout (new FillLayout ());
		
		TabFolder folder = new TabFolder (composite, SWT.NONE);
		HashMap<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", proj.getId ());
		vars.put ("commitDict", commitDictId);
		vars.put ("bugDict", bugDictId);

		ViewBuilder builder = new ViewBuilder ();
		builder.build (config, folder, proj, vars);

		return composite;
	}
	
	public Configuration getConfiguration () {
		return config;
	}

	
	//
	// Test Method:
	//

	public static void main (String[] args) {
		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("p", "project", true, "The project ID to process");
		options.addOption ("P", "list-projects", false, "List all registered projects");
		options.addOption ("C", "config", true, "A configuration file including reports");
		options.addOption ("c", "commit-dictionary", true, "The commit dictionary ID to use");
		options.addOption ("b", "bug-dictionary", true, "The bug dictionary ID to use");
		options.addOption ("D", "list-dictionaries", false, "List all dictionaries");
		options.addOption ("e", "db-extension", true, "Sqlite extension");
		options.addOption ("v", "verbose", false, "Show details");

		Reporter reporter = new Reporter ();
		String[] extensions = new String[0];
		ModelPool pool = null;
		Model model = null;

		CommandLineParser parser = new PosixParser ();
		boolean printDetails = false;
		
		try {
			CommandLine cmd = parser.parse (options, args);
			printDetails = cmd.hasOption ("verbose");

			if (cmd.hasOption ("help")) {
				HelpFormatter formatter = new HelpFormatter ();
				formatter.printHelp ("postprocessor", options);
				return ;
			}

			if (cmd.hasOption ("db-extension")) {
				extensions = cmd.getOptionValues ("db-extension");
			}
			
			if (cmd.hasOption ("db") == false) {
				reporter.error ("explorer", "Option --db is required");
				reporter.printSummary ();
				return ;
			}

			if (cmd.hasOption ("config") == false) {
				reporter.error ("explorer", "Option --config is required");
				reporter.printSummary ();
				return ;
			}

			Configuration config = new Configuration ();
			Parser configParser = new Parser ();
			try {
				configParser.parse (config, new File (cmd.getOptionValue ("config")));
			} catch (IOException e) {
				reporter.error ("explorer", "Could not read configuration file: " + e.getMessage ());
				reporter.printSummary ();
				return ;
			} catch (ParserException e) {
				reporter.error ("explorer", "Could not parse configuration file: " + e.getMessage ());
				reporter.printSummary ();
				return ;
			}
			
			
			File dbf = new File (cmd.getOptionValue ("db"));
			if (dbf.exists() == false || dbf.isFile () == false) {
				reporter.error ("explorer", "Invalid database file path");
				reporter.printSummary ();
				return ;
			}
			
			pool = new ModelPool (cmd.getOptionValue ("db"), 2, extensions);
			pool.setPrintTemplates (printDetails);
			model = pool.getModel ();

			if (cmd.hasOption ("list-projects")) {
				for (Project proj : model.getProjects ()) {
					System.out.println ("  " + proj.getId () + ": " + proj.getDate ());
				}

				return ;
			}
			
			Integer projId = null;
			if (cmd.hasOption ("project") == false) {
				reporter.error ("explorer", "Option --project is required");
				reporter.printSummary ();
				return ;
			} else {
				try {
					projId = Integer.parseInt(cmd.getOptionValue ("project"));
				} catch (NumberFormatException e) {
					reporter.error ("explorer", "Invalid project ID");
					reporter.printSummary ();
					return ;
				}
			}


			Project project = model.getProject (projId);
			if (project == null) {
				reporter.error ("explorer", "Invalid project ID");
				reporter.printSummary ();
				return ;
			}

			if (cmd.hasOption ("list-dictionaries")) {
				List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
				for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
					System.out.println ("  (" + dict.getId () + ") " + dict.getContext () + " " + dict.getName ());
				}
				return ;
			}
			
			int bugDictId = -1;
			if (cmd.hasOption ("bug-dictionary")) {
				try {
					bugDictId = Integer.parseInt(cmd.getOptionValue ("bug-dictionary"));
					List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
					boolean valid = false;

					for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
						if (dict.getId () == bugDictId) {
							valid = true;
							break;
						}
					}

					if (valid == false) {
						reporter.error ("explorer", "Invalid bug dictionary ID");
						reporter.printSummary ();
						return ;
					}
				} catch (NumberFormatException e) {
					reporter.error ("explorer", "Invalid bug dictionary ID");
					reporter.printSummary ();
					return ;
				}
			} else {
				List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
				for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
					if (dict.getContext ().equals ("bug")) {
						bugDictId = dict.getId ();
						break;
					}
				}
			}

			int commitDictId = -1;
			if (cmd.hasOption ("commit-dictionary")) {
				try {
					commitDictId = Integer.parseInt(cmd.getOptionValue ("commit-dictionary"));
					List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
					boolean valid = false;

					for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
						if (dict.getId () == commitDictId) {
							valid = true;
							break;
						}
					}
					
					if (valid == false) {
						reporter.error ("explorer", "Invalid commit dictionary ID");
						reporter.printSummary ();
						return ;
					}
				} catch (NumberFormatException e) {
					reporter.error ("explorer", "Invalid commit dictionary ID");
					reporter.printSummary ();
					return ;
				}
			} else {
				List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
				for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
					if (dict.getContext ().equals ("src")) {
						commitDictId = dict.getId ();
						break;
					}
				}
			}
			

			// UI:
			Display display = new Display ();
			final Shell shell = new Shell (display);
			final StackLayout stack = new StackLayout ();
			shell.setLayout (stack);

			Composite _projectControl = null;
			Composite _teamControl = null;
			Composite _userControl = null;

			if (config.getProjectViewConfig () != null) {
				ViewFactory factory = new ViewFactory (model, config);
				_projectControl = factory.createProjectViewComposite (shell, SWT.NONE, project, commitDictId, bugDictId, config.getProjectViewConfig ());
			}

			if (config.getTeamViewConfig () != null) {
				ViewFactory factory = new ViewFactory (model, config);
				_teamControl = factory.createTeamViewComposite (shell, SWT.NONE, project, commitDictId, bugDictId, config.getTeamViewConfig ());
			}

			if (config.getUserViewConfig () != null) {
				ViewFactory factory = new ViewFactory (model, config);
				_userControl = factory.createUserViewComposite (shell, SWT.NONE, project, commitDictId, bugDictId, config.getUserViewConfig ());
			}
			
			final Composite projectControl = _projectControl;
			final Composite teamControl = _teamControl;
			final Composite userControl = _userControl;
			
			// Menu:
			Menu menu = new Menu(shell, SWT.BAR);
			shell.setMenuBar(menu);
			
			MenuItem mntmNewSubmenu = new MenuItem (menu, SWT.CASCADE);
			mntmNewSubmenu.setText ("Views");

			Menu viewMenu = new Menu (shell, SWT.DROP_DOWN);
			mntmNewSubmenu.setMenu (viewMenu);

			Composite defaultView = null;

			if (projectControl != null) {
				MenuItem projectRadio = new MenuItem(viewMenu, SWT.RADIO);
				projectRadio.setText("Project View");
				projectRadio.addSelectionListener (new SelectionListener () {
					@Override
					public void widgetDefaultSelected (SelectionEvent arg0) {
						stack.topControl = projectControl;
						shell.layout ();
					}
	
					@Override
					public void widgetSelected (SelectionEvent arg0) {
						stack.topControl = projectControl;
						shell.layout ();
					}
				});

				defaultView = projectControl;
				projectRadio.setSelection (true);
			}

			if (teamControl != null) {
				MenuItem teamRadio = new MenuItem(viewMenu, SWT.RADIO);
				teamRadio.setText("Team View");
				teamRadio.addSelectionListener (new SelectionListener () {
					@Override
					public void widgetDefaultSelected (SelectionEvent arg0) {
						stack.topControl = teamControl;
						shell.layout ();
					}
	
					@Override
					public void widgetSelected (SelectionEvent arg0) {
						stack.topControl = teamControl;
						shell.layout ();
					}
				});

				if (defaultView == null) {
					defaultView = teamControl;
					teamRadio.setSelection (true);
				}
			}

			if (userControl != null) {
				MenuItem userRadio = new MenuItem(viewMenu, SWT.RADIO);
				userRadio.setText("User View");
				userRadio.addSelectionListener (new SelectionListener () {
					@Override
					public void widgetDefaultSelected (SelectionEvent arg0) {
						stack.topControl = userControl;
						shell.layout ();
					}
	
					@Override
					public void widgetSelected (SelectionEvent arg0) {
						stack.topControl = userControl;
						shell.layout ();
					}
				});

				if (defaultView == null) {
					defaultView = userControl;
					userRadio.setSelection (true);
				}
			}

			stack.topControl = defaultView;

			
			// Display:
			shell.pack();
			shell.open ();

			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) {
					display.sleep ();
				}
			}
			display.dispose();
			
			
			/*/ UI:
			Display display = new Display ();

			Shell teamShell = new Shell (display, SWT.CLOSE);
			teamShell.setLayout (new FillLayout ());

			Shell projectShell = new Shell (display, SWT.CLOSE);
			projectShell.setLayout (new FillLayout ());

			projectShell.setText ("Project View");
			teamShell.setText ("Team View");

			if (config.getTeamViewConfig () != null) {
				ViewFactory factory = new ViewFactory (model, config);
				factory.createTeamViewComposite (teamShell, SWT.NONE, project, commitDictId, bugDictId, config.getTeamViewConfig ());

				teamShell.pack();
				teamShell.open ();
			} else {
				teamShell.dispose ();
			}

			if (config.getProjectViewConfig () != null) {
				ViewFactory factory = new ViewFactory (model, config);
				factory.createProjectViewComposite (projectShell, SWT.NONE, project, commitDictId, bugDictId, config.getProjectViewConfig ());

				projectShell.pack();
				projectShell.open ();
			} else {
				projectShell.dispose ();
			}

			teamShell.addListener (SWT.CLOSE, new Listener () {
				@Override
				public void handleEvent (Event event) {
					teamShell.dispose ();
				}
			});

			projectShell.addListener (SWT.CLOSE, new Listener () {
				@Override
				public void handleEvent (Event event) {
					projectShell.dispose ();
				}
			});

	        while (!display.isDisposed ()) {
	        	if (teamShell.isDisposed () && projectShell.isDisposed ()) {
	        		break;
	        	}
				if (!display.readAndDispatch ()) {
					display.sleep ();
				}
			}

			display.dispose();
			*/
		} catch (ParseException e) {
			reporter.error ("explorer", "Parsing failed: " + e.getMessage ());
			if (printDetails == true) {
				e.printStackTrace ();
			}
		} catch (ClassNotFoundException e) {
			reporter.error ("explorer", "Failed to create a database connection: " + e.getMessage ());
			if (printDetails == true) {
				e.printStackTrace ();
			}
		} catch (SQLException e) {
			reporter.error ("explorer", "Failed to create a database connection: " + e.getMessage ());
			if (printDetails == true) {
				e.printStackTrace ();
			}
		} catch (SemanticException e) {
			reporter.error ("explorer", "Semantic Error: " + e.getMessage ());
			if (printDetails == true) {
				e.printStackTrace ();
			}
		} finally {
			if (model != null) {
				model.close ();
			}
			if (pool != null) {
				pool.close ();
			}
		}

		reporter.printSummary ();
	}
}
