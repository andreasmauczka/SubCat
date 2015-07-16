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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import at.ac.tuwien.inso.subcat.config.SemanticException;
import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.ResultCallback;
import at.ac.tuwien.inso.subcat.utility.Lemmatizer;


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
			if (config.getWordStats ()) {
				exportWordStats (config, project, commitDictId, bugDictId, settings, formatter, outputPath, vars);				
			} else {
				exportRows (config, project, commitDictId, bugDictId, settings, formatter, outputPath, vars);				
			}
		} catch (ReporterException e) {
			throw e;
		} catch (Exception e) {
			throw new ReporterException ("Exporter: " + e.getMessage (), e);
		} finally {
			formatter.cleanup ();
		}
	}

	private void exportWordStats (final ExporterConfig config, Project project, int commitDictId, int bugDictId, Settings settings, final ReportWriter formatter, String outputPath, Map<String, Object> vars) throws SQLException, Exception {
		formatter.init (project, settings, outputPath);
		model.rawForeach (config.getQuery (), vars, new ResultCallback () {

			@Override
			public void processResult (ResultSet res) throws SemanticException, SQLException, Exception {
				ResultSetMetaData meta = res.getMetaData ();
				String[] titles = new String[meta.getColumnCount ()];
				if (titles.length != 2) {
					throw new SemanticException ("semantic error: invalid column count, expected: (<string>, <string>)", config.getStart (), config.getEnd ());
				}

				if (meta.getColumnType (1) != Types.VARCHAR || meta.getColumnType (2) != Types.VARCHAR) {
					throw new SemanticException ("semantic error: invalid column type, expected: (<string>, <string>), got "
						+ "(<" + meta.getColumnTypeName (1) + ">, <" + meta.getColumnTypeName (2) + ">)",
						config.getStart (), config.getEnd ());					
				}

				Map<String, Map<String, Integer>> data = new HashMap<String, Map<String, Integer>> ();
				Lemmatizer lemmatiser = new Lemmatizer ();

				Set<String> categoryNames = new HashSet<String> ();

				while (res.next ()) {
					String category = res.getString (1);					
					categoryNames.add (category);

					List<String> lemma = lemmatiser.lemmatize (res.getString (2));
					
					for (String word : lemma) {
						Map<String, Integer> counter = data.get (word);
						if (counter == null) {
							counter = new HashMap<String, Integer> ();
							data.put (word, counter);
						}

						Integer wordCount = counter.get (category);
						if (wordCount == null) {
							wordCount = 0;
						}

						counter.put (category, wordCount + 1);
					}
				}


				String[] header = new String[categoryNames.size () + 1];
				header[0] = "word";

				int i = 1;
				for (String catName : categoryNames) {
					header[i] = catName;
					i++;
				}

				formatter.writeHeader (header);
				
				for (Entry<String, Map<String, Integer>>  entry : data.entrySet ()) {
					Map<String, Integer> scores = entry.getValue ();
					String[] row = new String[header.length];

					row[0] = entry.getKey ();
					i = 1;
					for (String cat : categoryNames) {
						Integer score = scores.get (cat);
						if (score == null) {
							score = 0;
						}
						row[i] = score.toString ();
						i++;
						
					}

					formatter.writeSet (row);
				}
				
				formatter.writeFooter (header);
			}
		});		
	}

	private void exportRows (ExporterConfig config, Project project, int commitDictId, int bugDictId, Settings settings, final ReportWriter formatter, String outputPath, Map<String, Object> vars) throws SQLException, Exception {
		formatter.init (project, settings, outputPath);
		model.rawForeach (config.getQuery (), vars, new ResultCallback () {

			@Override
			public void processResult (ResultSet res) throws SemanticException, SQLException, Exception {
				ResultSetMetaData meta = res.getMetaData ();
				String[] titles = new String[meta.getColumnCount ()];
				for (int i = 0; i < titles.length ; i++) {
					titles[i] = meta.getColumnLabel (i + 1);
				}


				formatter.writeHeader (titles);
				
				while (res.next ()) {
					String[] data = new String[titles.length];
					for (int i = 0; i < data.length ; i++) {
						data[i] = res.getString (i + 1);
					}
					
					formatter.writeSet (data);
				}

				formatter.writeFooter (titles);
			}
		});
	}
	
	public static void main (String[] args) {
		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("p", "project", true, "The project ID to process");
		options.addOption ("P", "list-projects", false, "List all registered projects");
		options.addOption ("C", "config", true, "A configuration file including reports");
		options.addOption ("F", "list-formats", false, "List all supported output formats");
		options.addOption ("f", "format", true, "Output format");
		options.addOption ("R", "list-reports", false, "List all report types");
		options.addOption ("r", "report", true, "Report type");
		options.addOption ("o", "output", true, "Output path");
		options.addOption ("c", "commit-dictionary", true, "The commit dictionary ID to use");
		options.addOption ("b", "bug-dictionary", true, "The bug dictionary ID to use");
		options.addOption ("D", "list-dictionaries", false, "List all dictionaries");
		options.addOption ("v", "verbose", false, "Show details");

		
		at.ac.tuwien.inso.subcat.utility.Reporter errReporter = new at.ac.tuwien.inso.subcat.utility.Reporter (false);
		Settings settings = new Settings ();
		boolean verbose = false;
		ModelPool pool = null;
		Model model = null;

		CommandLineParser parser = new PosixParser ();
		
		try {
			CommandLine cmd = parser.parse (options, args);
			verbose = cmd.hasOption ("verbose");
			
			
			if (cmd.hasOption ("help")) {
				HelpFormatter formatter = new HelpFormatter ();
				formatter.printHelp ("postprocessor", options);
				return ;
			}

			if (cmd.hasOption ("db") == false) {
				errReporter.error ("reporter", "Option --db is required");
				errReporter.printSummary ();
				return ;
			}

			if (cmd.hasOption ("config") == false) {
				errReporter.error ("reporter", "Option --config is required");
				errReporter.printSummary ();
				return ;
			}

			Configuration config = new Configuration ();
			Parser configParser = new Parser ();
			try {
				configParser.parse (config, new File (cmd.getOptionValue ("config")));
			} catch (IOException e) {
				errReporter.error ("reporter", "Could not read configuration file: " + e.getMessage ());
				errReporter.printSummary ();
				return ;
			} catch (ParserException e) {
				errReporter.error ("reporter", "Could not parse configuration file: " + e.getMessage ());
				errReporter.printSummary ();
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
				errReporter.error ("reporter", "Invalid database file path");
				errReporter.printSummary ();
				return ;
			}
			
			pool = new ModelPool (cmd.getOptionValue ("db"), 2);
			pool.setPrintTemplates (verbose);
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
				errReporter.error ("reporter", "Option --project is required");
				errReporter.printSummary ();
				return ;
			} else {
				try {
					projId = Integer.parseInt (cmd.getOptionValue ("project"));
				} catch (NumberFormatException e) {
					errReporter.error ("reporter", "Invalid project ID");
					errReporter.printSummary ();
					return ;
				}
			}

			if (cmd.hasOption ("output") == false) {
				errReporter.error ("reporter", "Option --output is required");
				errReporter.printSummary ();
				return ;
			}

			String outputPath = cmd.getOptionValue ("output");
			model = pool.getModel ();
			Project project = model.getProject (projId);

			if (project == null) {
				errReporter.error ("reporter", "Invalid project ID");
				errReporter.printSummary ();
				return ;
			}

			if (cmd.hasOption ("list-dictionaries")) {
				List<at.ac.tuwien.inso.subcat.model.Dictionary> dictionaries = model.getDictionaries (project);
				for (at.ac.tuwien.inso.subcat.model.Dictionary dict : dictionaries) {
					System.out.println ("  (" + dict.getId () + ") " + " " + dict.getContext () + " " + dict.getName ());
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
						errReporter.error ("reporter", "Invalid bug dictionary ID");
						errReporter.printSummary ();
						return ;
					}
				} catch (NumberFormatException e) {
					errReporter.error ("reporter", "Invalid bug dictionary ID");
					errReporter.printSummary ();
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
						errReporter.error ("reporter", "Invalid commit dictionary ID");
						errReporter.printSummary ();
						return ;
					}
				} catch (NumberFormatException e) {
					errReporter.error ("reporter", "Invalid commit dictionary ID");
					errReporter.printSummary ();
					return ;
				}
			}

			if (cmd.hasOption ("format") == false) {
				errReporter.error ("reporter", "Option --format is required");
				errReporter.printSummary ();
				return ;
			}

			Reporter exporter = new Reporter (model);
			ReportWriter writer = null;
			try {
				int id = Integer.parseInt (cmd.getOptionValue ("format"));
				if (id < 1 || id > exporter.getWriters ().size ()) {
					errReporter.error ("reporter", "Invalid output format");
					errReporter.printSummary ();
					return ;
				}
				
				writer = exporter.getWriters ().get (id - 1);
			} catch (NumberFormatException e) {
				errReporter.error ("reporter", "Invalid output format");
				errReporter.printSummary ();
				return ;
			}

			ExporterConfig exporterConfig = null;
			if (cmd.hasOption ("report") == false) {
				errReporter.error ("reporter", "Option --report is required");
				errReporter.printSummary ();
				return ;
			} else {
				try {
					int id = Integer.parseInt (cmd.getOptionValue ("report"));
					if (id < 1 || id > config.getExporterConfigs ().size ()) {
						errReporter.error ("reporter", "Invalid reporter ID");
						errReporter.printSummary ();
						return ;
					}
					
					exporterConfig = config.getExporterConfigs ().get (id -1);
				} catch (NumberFormatException e) {
					errReporter.error ("reporter", "Invalid reporter ID");
					errReporter.printSummary ();
					return ;
				}
			}

			exporter.export (exporterConfig, project, commitDictId, bugDictId, settings, writer, outputPath);
		} catch (ParseException e) {
			errReporter.error ("reporter", "Parsing failed: " + e.getMessage ());
			if (verbose == true) {
				e.printStackTrace ();
			}
		} catch (ClassNotFoundException e) {
			errReporter.error ("reporter", "Failed to create a database connection: " + e.getMessage ());
			if (verbose == true) {
				e.printStackTrace ();
			}
		} catch (SQLException e) {
			errReporter.error ("reporter", "Failed to create a database connection: " + e.getMessage ());
			if (verbose == true) {
				e.printStackTrace ();
			}
		} catch (ReporterException e) {
			errReporter.error ("reporter", "Reporter Error: " + e.getMessage ());
			if (verbose == true) {
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

		errReporter.printSummary ();
	}
}
