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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import at.ac.tuwien.inso.subcat.config.Configuration;
import at.ac.tuwien.inso.subcat.config.ExporterConfig;
import at.ac.tuwien.inso.subcat.config.Parser;
import at.ac.tuwien.inso.subcat.config.ParserException;
import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;


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
	
	public void export (ExporterConfig config, Project project, int commitDictId, int bugDictId, Settings settings, ReportWriter formatter, String outputPath) throws ReporterException {
		assert (project != null);
		assert (project.getId () != null);
		assert (config != null);
		assert (settings != null);
		assert (formatter != null);
		assert (model != null);
		assert (config.getQuery () != null);
		
		Map<String, Object> vars = new HashMap<String, Object> ();
		vars.put ("project", project.getId ());
		vars.put ("commitDict", commitDictId);
		vars.put ("bugDict", bugDictId);

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
		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("p", "project", true, "The project ID to process");
		options.addOption ("l", "list-projects", false, "List all registered projects");
		options.addOption ("C", "config", true, "A configuration file including reports");
		options.addOption ("F", "list-formats", false, "List all supported output formats");
		options.addOption ("f", "format", true, "Output format");
		options.addOption ("R", "list-reports", false, "List all report types");
		options.addOption ("r", "report", true, "Report type");
		options.addOption ("o", "output", true, "Output path");
		options.addOption ("c", "commit-dictionary", true, "The commit dictionary ID to use");
		options.addOption ("b", "bug-dictionary", true, "The bug dictionary ID to use");
		options.addOption ("D", "list-dictionaries", false, "List all dictionaries");

		
		Settings settings = new Settings ();
		ModelPool pool = null;
		Model model = null;

		CommandLineParser parser = new PosixParser ();
		
		try {
			CommandLine cmd = parser.parse (options, args);

			if (cmd.hasOption ("help")) {
				HelpFormatter formatter = new HelpFormatter ();
				formatter.printHelp ("postprocessor", options);
				return ;
			}

			if (cmd.hasOption ("db") == false) {
				System.err.println ("Option --db is required");
				return ;
			}

			if (cmd.hasOption ("config") == false) {
				System.err.println ("Option --config is required");
				return ;
			}

			Configuration config = new Configuration ();
			Parser configParser = new Parser ();
			try {
				configParser.parse (config, new File (cmd.getOptionValue ("config")));
			} catch (IOException e) {
				System.err.println ("Could not read configuration file: " + e.getMessage ());
				return ;
			} catch (ParserException e) {
				System.err.println ("Could not parse configuration file: " + e.getMessage ());
				return ;
			}

			if (cmd.hasOption ("list-reports")) {
				int i = 1;

				for (ExporterConfig exconf : config.getExporterConfigs ()) {
					System.out.println ("  (" + i + ") " + exconf.getName ());
					i++;
				}

				return ;
			}

			
			File dbf = new File (cmd.getOptionValue ("db"));
			if (dbf.exists() == false || dbf.isFile () == false) {
				System.err.println ("Invalid database file path");
				return ;
			}
			
			pool = new ModelPool (cmd.getOptionValue ("db"), 2);
			model = pool.getModel ();

			if (cmd.hasOption ("list-formats")) {
				Reporter exporter = new Reporter (model);
				int i = 1;

				for (ReportWriter formatter : exporter.getWriters ()) {
					System.out.println ("  (" + i + ") " + formatter.getLabel ());
					i++;
				}

				return ;
			}
			
			if (cmd.hasOption ("list-projects")) {
				for (Project proj : model.getProjects ()) {
					System.out.println ("  " + proj.getId () + ": " + proj.getDate ());
				}

				return ;
			}			
			
			Integer projId = null;
			if (cmd.hasOption ("project") == false) {
				System.err.println ("Option --project is required");
				return ;
			} else {
				try {
					projId = Integer.parseInt (cmd.getOptionValue ("project"));
				} catch (NumberFormatException e) {
					System.err.println ("Invalid project ID");
					return ;
				}
			}

			if (cmd.hasOption ("output") == false) {
				System.err.println ("Option --output is required");
				return ;
			}

			String outputPath = cmd.getOptionValue ("output");
			model = pool.getModel ();
			Project project = model.getProject (projId);

			if (project == null) {
				System.err.println ("Invalid project ID");
				return ;
			}

			if (cmd.hasOption ("list-dictionaries")) {
				List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
				for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
					System.out.println ("  (" + dict.getId () + ") " + dict.getName ());
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
						System.err.println ("Invalid bug dictionary ID");
					}
				} catch (NumberFormatException e) {
					System.err.println ("Invalid bug dictionary ID");
					return ;
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
						System.err.println ("Invalid commit dictionary ID");
					}
				} catch (NumberFormatException e) {
					System.err.println ("Invalid commit dictionary ID");
					return ;
				}
			}

			if (cmd.hasOption ("format") == false) {
				System.err.println ("Option --format is required");
				return ;
			}

			Reporter exporter = new Reporter (model);
			ReportWriter writer = null;
			try {
				int id = Integer.parseInt (cmd.getOptionValue ("format"));
				if (id < 1 || id > exporter.getWriters ().size ()) {
					System.err.println ("Invalid output format");
					return ;
				}
				
				writer = exporter.getWriters ().get (id - 1);
			} catch (NumberFormatException e) {
				System.err.println ("Invalid output format");
				return ;
			}

			ExporterConfig exporterConfig = null;
			if (cmd.hasOption ("report") == false) {
				System.err.println ("Option --report is required");
				return ;
			} else {
				try {
					int id = Integer.parseInt (cmd.getOptionValue ("report"));
					if (id < 1 || id > config.getExporterConfigs ().size ()) {
						System.err.println ("Invalid reporter ID");
						return ;
					}
					
					exporterConfig = config.getExporterConfigs ().get (id -1);
				} catch (NumberFormatException e) {
					System.err.println ("Invalid reporter ID");
					return ;
				}
			}

			exporter.export (exporterConfig, project, commitDictId, bugDictId, settings, writer, outputPath);
		} catch (ParseException e) {
			System.err.println ("Parsing failed: " + e.getMessage ());
		} catch (ClassNotFoundException e) {
			System.err.println ("Failed to create a database connection: " + e.getMessage ());
		} catch (SQLException e) {
			System.err.println ("Failed to create a database connection: " + e.getMessage ());
		} catch (ReporterException e) {
			System.err.println ("Reporter Error: " + e.getMessage ());
		} finally {
			if (model != null) {
				model.close ();
			}
			if (pool != null) {
				pool.close ();
			}
		}
	}
}
