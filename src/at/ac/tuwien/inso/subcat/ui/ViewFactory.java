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
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import at.ac.tuwien.inso.subcat.config.PieChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.ProjectViewConfig;
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.config.TrendChartGroupConfig;
import at.ac.tuwien.inso.subcat.config.ViewConfig;
import at.ac.tuwien.inso.subcat.model.Category;
import at.ac.tuwien.inso.subcat.model.Component;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Priority;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.Severity;
import at.ac.tuwien.inso.subcat.model.Status;
import at.ac.tuwien.inso.subcat.model.User;
import at.ac.tuwien.inso.subcat.ui.controller.DistributionChartController;
import at.ac.tuwien.inso.subcat.ui.controller.PieChartController;
import at.ac.tuwien.inso.subcat.ui.controller.TrendViewController;
import at.ac.tuwien.inso.subcat.ui.controller.ViewController;
import at.ac.tuwien.inso.subcat.ui.widgets.DistributionView;
import at.ac.tuwien.inso.subcat.ui.widgets.PieChartView;
import at.ac.tuwien.inso.subcat.ui.widgets.TrendView;


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

		public void build (ViewConfig config, TabFolder folder, Project project, HashMap<String, Object> vars) throws SemanticException, SQLException {
			assert (project != null);
			assert (config != null);
			assert (folder != null);

			// TODO: ViewConfig-visitors to select the right
			//   viewController
			viewController = new ViewController (project);
			
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

			PieChartView view = new PieChartView (scroll, SWT.NONE);
			new PieChartController (model, view, groupConfig, flags, viewController);

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

			TrendView view = new TrendView (folder, SWT.NONE);
			view.addTrendViewListener (new TrendViewController (model, flags, view, groupConfig, viewController));
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

			DistributionView view = new DistributionView (folder, SWT.NONE);
			view.addDistributionViewListener (new DistributionChartController (model, flags, view, config, viewController));
			page.setControl (view);
		}
	}


	public ViewFactory (Model model, Configuration config) {
		this.model = model;
		this.config = config;
	}

	public Composite createProjectViewComposite (Composite parent, int style, Project proj, ProjectViewConfig config) throws SemanticException, SQLException {
		assert (config != null);
		assert (proj != null);
		assert (proj.getId () != null);

		this.flags = model.getFlags (proj);

		Composite composite = new Composite (parent, style);
		composite.setLayout (new FillLayout ());
		
		TabFolder folder = new TabFolder (composite, SWT.NONE);
		HashMap<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", proj.getId ());

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
		try {
			// Dummy Model:
			Model model = new Model ("foo.db");
			Project project = model.addProject (new Date (), "", "", "");
			model.addFlag (project, "FOO");
			Component component1 = model.addComponent(project, "Component 1");

			Status status1 = model.addStatus(project, "OPEN");
			model.addStatus(project, "CLOSED");
			model.setDefaultStatus (status1);

			
			User user1 = model.addUser (project, "user 1");
			Identity identity1a = model.addIdentity ("user1@mail.endl", "user1a", user1);
			Identity identity1b = model.addIdentity ("user1@mail.endl", "user1b", user1);
			
			User user2 = model.addUser (project, "user 2");
			Identity identity2a = model.addIdentity ("user2@mail.endl", "user2a", user2);
			model.addIdentity ("user2@mail.endl", "user2b", user2);

			User user3 = model.addUser (project, "user 3");
			Identity identity3a = model.addIdentity ("user3@mail.endl", "user3a", user3);
			model.addIdentity ("user3@mail.endl", "user3b", user3);

			
			Category cat1 = model.addCategory(project, "Corrective");
			Category cat2 = model.addCategory(project, "Adaptive");
			Category cat3 = model.addCategory(project, "Perfective");

			Severity sev1 = model.addSeverity(project, "blocker");
			model.addSeverity(project, "critical");
			model.addSeverity(project, "major");
			model.addSeverity(project, "normal");
			model.addSeverity(project, "minor");
			model.addSeverity(project, "trivial");
			model.addSeverity(project, "enhancment");

			Priority priority1 = model.addPriority (project, "Immediate");
			model.addPriority (project, "Urgent");
			model.addPriority (project, "High");
			model.addPriority (project, "Normal");
			model.addPriority (project, "Low");

			
			model.addCommit(project, identity1a, identity1a, new Date (), "commit 1", 1, 5, 5, cat1);
			model.addCommit(project, identity1a, identity1a, new Date (), "commit 3", 1, 5, 5, cat1);
			model.addCommit(project, identity1a, identity1a, new Date (), "commit 3", 1, 5, 5, cat1);

			model.addCommit(project, identity2a, identity2a, new Date (), "commit 4", 1, 5, 5, cat2);
			model.addCommit(project, identity2a, identity2a, new Date (), "commit 5", 1, 5, 5, cat2);
			model.addCommit(project, identity2a, identity2a, new Date (), "commit 6", 1, 5, 5, cat2);

			model.addCommit(project, identity3a, identity1a, new Date (), "commit 7", 1, 5, 5, cat3);
			model.addCommit(project, identity3a, identity1a, new Date (), "commit 8", 1, 5, 5, cat3);
			model.addCommit(project, identity3a, identity1a, new Date (), "commit 9", 1, 5, 5, cat3);

			
	
			model.addBug("1", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("2", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("3", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("4", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("5", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("6", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("7", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("8", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("9", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			model.addBug("10", identity1b, component1, "bug 1", new Date (), priority1, sev1, cat1);
			
			
			// Configuration:
			Configuration config = new Configuration ();
			Parser parser = new Parser ();
			String content = "\n"
					+ "ProjectView = { \n"

					+ "  DistributionCharts = {\n"
					+ "    Name = \"Distributions\";\n"

					+ "    DistributionOption = {\n"
					+ "      Name = \"Bugs\";\n"

					+ "	     Filter = {\n"
					+ "        VarName = priority;\n"
					+ "	       Query = \"SELECT id, name FROM Priorities WHERE project = \" + project;\n"
					+ "	     };\n"

					+ "      Filter = {\n"
					+ "        VarName = severity;\n"
					+ "        Query = \"SELECT id, name FROM Severity WHERE project = \" + project;\n"
					+ "	     };\n"

					+ "      Filter = {\n"
					+ "        VarName = status;\n"
					+ "        Query = \"SELECT id, name FROM Status WHERE project = \" + project;\n"
					+ "	     };\n"

					+ "      Attributes = {\n"
					+ "        Attribute = {\n"
					+ "          Name = \"Comments\";\n"
					+ "          Query = \"SELECT CAST(strftime('%m', creation) AS INTEGER), avg(comments), median(comments), lower_quartile(comments), upper_quartile(comments), min(comments), max(comments), count (*) FROM Bugs\";\n"
					+ "        };\n"
					+ "        Attribute = {\n"
					+ "          Name = \"Patches\";\n"
					+ "          Query = \"SELECT CAST(strftime('%m', creation) AS INTEGER), avg(comments), median(comments), lower_quartile(comments), upper_quartile(comments), min(comments), max(comments), count (*) FROM Bugs\";\n"
					+ "        };\n"
					+ "      };\n"

					+ "	   };\n"
					+ "	 };\n"
					
					+ "  TrendCharts = {\n"
					+ "    Name = \"Bug Trends\";\n"

					+ "    TrendChart = {\n"
					+ "      Name = \"Group 1\";\n"

					+ "      DropDown = {\n"
					+ "        VarName = categories;\n"
					+ "        Query = \"SELECT id, name FROM Categories\";\n"
					+ "        DataQuery = \"SELECT CAST(strftime('%m', creation) AS INTEGER), COUNT(id)"
					+ "							FROM Bugs"
					+ "							WHERE component IN (SELECT id FROM Components WHERE project = \" + project + \")"
					+ "								AND severity = \" + severity + \""
					+ "								AND category = \" + categories + \""
					+ "								AND CAST(strftime('%Y', creation) AS INTEGER) = \" + year + \""
					+ "							GROUP BY strftime('%m', creation)\";\n"
					+ "      };\n"
					
					+ "      OptionList = {\n"
					+ "        VarName = severity;\n"
					+ "        Query = \"SELECT id, name FROM Severity\";\n"
					+ "      };\n"

					+ "    };\n"
					+ "  };\n"

					
					+ "  PieCharts = {\n"
					+ "    Name = \"Source Overview\";\n"

					+ "    PieChart = {\n"
					+ "       Name = \"Pie 1\";\n"
					+ "       Query = \"SELECT name, (SELECT COUNT () FROM Commits WHERE Commits.category = Categories.id) AS count FROM  Categories WHERE project = \" + project + \" ORDER BY name \";"
					+ "       ShowTotal = true;\n"
					+ "    };\n"

					+ "    PieChart = {\n"
					+ "       Name = \"Pie 2\";\n"
					+ "       Query = \"SELECT name, (SELECT COUNT () FROM Commits WHERE Commits.category = Categories.id) AS count FROM  Categories WHERE project = \" + project + \" ORDER BY name \";"
					+ "       ShowTotal = true;\n"
					+ "    };\n"					

					+ "    PieChart = {\n"
					+ "       Name = \"Pie 3\";\n"
					+ "       Query = \"SELECT name, (SELECT COUNT () FROM Commits WHERE Commits.category = Categories.id) AS count FROM  Categories WHERE project = \" + project + \" ORDER BY name \";"
					+ "       ShowTotal = true;\n"
					+ "    };\n"					

					+ "    PieChart = {\n"
					+ "       Name = \"Pie 4\";\n"
					+ "       Query = \"SELECT name, (SELECT COUNT () FROM Commits WHERE Commits.category = Categories.id) AS count FROM  Categories WHERE project = \" + project + \" ORDER BY name \";"
					+ "       ShowTotal = true;\n"
					+ "    };\n"					

					+ "    PieChart = {\n"
					+ "       Name = \"Pie 5\";\n"
					+ "       Query = \"SELECT name, (SELECT COUNT () FROM Commits WHERE Commits.category = Categories.id) AS count FROM  Categories WHERE project = \" + project + \" ORDER BY name \";"
					+ "       ShowTotal = true;\n"
					+ "    };\n"					

					+ "  };\n"
					+ "};\n"
					+ "\n";

			
			parser.parse (config, new File ("/foo/bar/baz"), content.getBytes());


			// UI:
			Display display = new Display ();
			Shell shell = new Shell (display);
			shell.setLayout (new FillLayout ());

			ViewFactory factory = new ViewFactory (model, config);
			factory.createProjectViewComposite (shell, SWT.NONE, project, config.getProjectViewConfig());

			shell.pack();
			shell.open ();
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) {
					display.sleep ();
				}
			}
			
			display.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
