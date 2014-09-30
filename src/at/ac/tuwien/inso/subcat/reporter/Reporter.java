/* Reporter.java
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

package at.ac.tuwien.inso.subcat.reporter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.inso.subcat.config.Configuration;
import at.ac.tuwien.inso.subcat.config.ExporterConfig;
import at.ac.tuwien.inso.subcat.config.Parser;
import at.ac.tuwien.inso.subcat.config.ParserException;
import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Category;
import at.ac.tuwien.inso.subcat.model.Component;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Priority;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.Severity;
import at.ac.tuwien.inso.subcat.model.Status;
import at.ac.tuwien.inso.subcat.model.User;


public class Reporter {
	private List<ReportWriter> formatters;
	private Model model;
	
	public Reporter (Model model) {
		assert (model != null);

		this.model = model;

		// Supported Formats:
		formatters = new LinkedList<ReportWriter> ();
		formatters.add (new CsvRFC4180ReporterWriter ());
		formatters.add (new CsvEuropeanReporterWriter ());
		formatters.add (new XmlReportWriter ());
	}

	public List<ReportWriter> getWriters () {
		return formatters;
	}
	
	public void export (ExporterConfig config, Project project, Settings settings, ReportWriter formatter, String outputPath) throws ReporterException {
		assert (project != null);
		assert (config != null);
		assert (settings != null);
		assert (formatter != null);
		
		Map<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", project.getId ());

		try {
			formatter.init (project, settings, outputPath);
			model.rawForeach (config.getQuery (), vars, formatter);
		} catch (ReporterException e) {
			throw e;
		} catch (Exception e) {
			throw new ReporterException ("Exporter: " + e.getMessage (), e);
		} finally {
			formatter.cleanup ();
		}
	}

	public static void main (String[] args) {
		// Dummy Model:
		try {
			Model model = new Model (":memory:");
			Settings settings = new Settings ();
			Project project = model.addProject (new Date (), "", "", "");
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

		
			// Config:
			Configuration config = new Configuration ();
			Parser parser = new Parser ();
			String content = "\n"
					+ " Reporter = {\n"
					+ "  Name = \"Dummy Report\";\n"
					+ "  Query = \"SELECT * From Bugs\";\n"
					+ " };\n"
					+ "\n";
			
			parser.parse (config, new File ("/foo/bar/baz"), content.getBytes());

			Reporter exporter = new Reporter (model);
			for (ExporterConfig exconf : config.getExporterConfigs ()) {
				for (ReportWriter formatter : exporter.getWriters ()) {
					String fileName = "test-export-" + exconf.getName () + formatter.getLabel () + "-" + formatter.getTypeName ();
					System.out.println ("Export: " + fileName);
					exporter.export (exconf, project, settings, formatter, fileName);
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			e.printStackTrace();
		} catch (ReporterException e) {
			e.printStackTrace();
		}
	}
}
