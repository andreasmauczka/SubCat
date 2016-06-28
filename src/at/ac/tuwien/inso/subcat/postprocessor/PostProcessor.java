/* PostProcessor.java
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

package at.ac.tuwien.inso.subcat.postprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Collection;
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

import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.FileChange;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Model.Stats;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.ObjectCallback;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.Reporter;
import at.ac.tuwien.inso.subcat.utility.XmlReaderException;
import at.ac.tuwien.inso.subcat.utility.classifier.Dictionary;
import at.ac.tuwien.inso.subcat.utility.classifier.DictionaryParser;
import at.ac.tuwien.inso.subcat.utility.phonetic.HashFunc;


public class PostProcessor {
	private List<PostProcessorListener> listener = new LinkedList<PostProcessorListener> ();
	
	private List<PostProcessorTask> beginTasks;
	private List<PostProcessorTask> commitTasks;
	private List<PostProcessorTask> bugTasks;
	private List<PostProcessorTask> endTasks;

	private ModelPool pool;
	private Settings settings;
	private Project proj;

	private PostProcessorException exception = null;
	private boolean stopped = false;
	

	public PostProcessor (Project proj, ModelPool pool, Settings settings) {
		beginTasks = new LinkedList<PostProcessorTask> ();
		commitTasks = new LinkedList<PostProcessorTask> ();
		bugTasks = new LinkedList<PostProcessorTask> ();
		endTasks = new LinkedList<PostProcessorTask> ();

		this.pool = pool;
		this.settings = settings;
		this.proj = proj;
	}

	public void addListener (PostProcessorListener listener) {
		assert (listener != null);
		
		this.listener.add(listener);
	}

	public void removeListener (PostProcessorListener listener) {
		assert (listener != null);

		this.listener.remove (listener);
	}
	
	public ModelPool getModelPool () {
		return pool;
	}

	public Settings getSettings () {
		return settings;
	}
	
	public Project getProject () {
		return proj;
	}

	public void register (Collection<PostProcessorTask> collection) {
		for (PostProcessorTask task : collection) {
			register (task);
		}
	}

	public void register (PostProcessorTask task) {
		assert (task != null);
		
		if ((task.flags & PostProcessorTask.BEGIN) > 0) {
			beginTasks.add (task);
		}

		if ((task.flags & PostProcessorTask.COMMIT) > 0) {
			commitTasks.add (task);
		}

		if ((task.flags & PostProcessorTask.BUG) > 0) {
			bugTasks.add (task);
		}

		if ((task.flags & PostProcessorTask.END) > 0) {
			endTasks.add (task);
		}
	}

	public void process () throws PostProcessorException {
		stopped = false;

		Runnable runnable = new Runnable () {
			@Override
			public void run () {
				
				Model model = null;
				try {
					model = pool.getModel ();
					emitBegin ();

					if (commitTasks.size () > 0) {
						model.foreachCommit (proj, new ObjectCallback<Commit> () {
							@Override
							public boolean processResult (Commit item) throws SQLException, Exception {								
								Model model2 = pool.getModel ();
								List<FileChange> changes = model2.getFileChanges (item);
								model2.close ();
								emitCommit (item, changes);
								return !stopped;
							}				
						});
					}

					if (bugTasks.size () > 0) {
						model.foreachBug (proj, new ObjectCallback<Bug> () {
							@Override
							public boolean processResult (Bug bug) throws SQLException, Exception {
								Model model2 = pool.getModel ();
								List<BugHistory> history = model2.getBugHistory (proj, bug);
								List<Comment> comments = model2.getComments (proj, bug);
								model2.close ();
								emitBug (bug, history, comments);
								return !stopped;
							}
						});
					}

					emitEnd ();
				} catch (PostProcessorException e) {
					exception = e;
				} catch (SQLException e) {
					exception = new PostProcessorException ("SQL-Error: " + e.getMessage (), e);
				} catch (Exception e) {
					exception = new PostProcessorException ("Unexpected Error: " + e.getMessage (), e);
				} finally {
					if (model != null) {
						model.close ();
					}
				}
			}
		};

		runnable.run ();
		stopped = false;

		if (exception != null) {
			PostProcessorException e = exception;
			exception = null;
			throw e;
		}
	}

	private void emitBegin () throws PostProcessorException {
		for (PostProcessorTask task : beginTasks) {
			if (stopped){
				break;
			}

			task.begin (this);
		}
	}

	private void emitCommit (Commit commit, List<FileChange> changes) throws PostProcessorException {
		for (PostProcessorTask task : commitTasks) {
			if (stopped){
				break;
			}

			task.commit (this, commit, changes);
		}

		for (PostProcessorListener l : listener) {
			l.commit (this);
		}
	}

	private void emitBug (Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		for (PostProcessorTask task : bugTasks) {
			if (stopped){
				break;
			}

			task.bug (this, bug, history, comments);
		}

		for (PostProcessorListener l : listener) {
			l.bug (this);
		}
	}

	private void emitEnd () throws PostProcessorException {
		for (PostProcessorTask task : endTasks) {
			if (stopped){
				break;
			}

			task.end (this);
		}
	}
	
	public void stop () throws PostProcessorException {
		stopped = true;
	}

	public static void main (String[] args) {
		Map<String, PostProcessorTask> steps = new HashMap<String, PostProcessorTask> ();
		PostProcessorTask _step = new ClassificationTask ();
		steps.put (_step.getName (), _step);
		CommentAnalyserTask commentAnalysisStep = new CommentAnalyserTask ();
		steps.put (commentAnalysisStep.getName (), commentAnalysisStep);
		AccountInterlinkingTask interlinkingTask = new AccountInterlinkingTask ();
		steps.put (interlinkingTask.getName (), interlinkingTask);
		_step = new CommitBugInterlinkingTask ();
		steps.put (_step.getName (), _step);

		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("v", "verbose", false, "Show details");
		options.addOption ("p", "project", true, "The project ID to process");
		options.addOption ("P", "list-projects", false, "List all registered projects");
		options.addOption ("S", "list-processor-steps", false, "List all registered processor steps");
		options.addOption ("s", "processor-step", true, "A processor step name");
		options.addOption ("c", "commit-dictionary", true, "Path to a classification dictionary for commit message classification"); 
		options.addOption ("b", "bug-dictionary", true, "Path to a classification dictionary for bug classification"); 
		options.addOption ("m", "smart-matching", true, "Smart user matching configuration. Syntax: <method>:<distance>");
		options.addOption ("M", "list-matching-methods", false, "List smart matching methods");

		final Reporter reporter = new Reporter (true);
		reporter.startTimer ();

		Settings settings = new Settings ();
		ModelPool pool = null;

		boolean printTraces = false;
		CommandLineParser parser = new PosixParser ();
		
		try {
			CommandLine cmd = parser.parse (options, args);
			printTraces = cmd.hasOption ("verbose");

			if (cmd.hasOption ("help")) {
				HelpFormatter formatter = new HelpFormatter ();
				formatter.printHelp ("postprocessor", options);
				return ;
			}

			if (cmd.hasOption ("list-processor-steps")) {
				for (String proj : steps.keySet ()) {
					System.out.println ("  " + proj);
				}
				return ;
			}

			if (cmd.hasOption ("list-matching-methods")) {
				for (String method : HashFunc.getHashFuncNames ()) {
					System.out.println ("  " + method);
				}
				return ;
			}
			
			if (cmd.hasOption ("db") == false) {
				reporter.error ("post-processor", "Option --db is required");
				reporter.printSummary ();
				return ;
			}

			File dbf = new File (cmd.getOptionValue ("db"));
			if (dbf.exists() == false || dbf.isFile () == false) {
				reporter.error ("post-processor", "Invalid database file path");
				reporter.printSummary ();
				return ;
			}
			
			pool = new ModelPool (cmd.getOptionValue ("db"), 2);

			if (cmd.hasOption ("list-projects")) {
				Model model = pool.getModel ();

				for (Project proj : model.getProjects ()) {
					System.out.println ("  " + proj.getId () + ": " + proj.getDate ());
				}

				model.close ();
				return ;
			}

			Integer projId = null;
			if (cmd.hasOption ("project") == false) {
				reporter.error ("post-processor", "Option --project is required");
				reporter.printSummary ();
				return ;
			} else {
				try {
					projId = Integer.parseInt(cmd.getOptionValue ("project"));
				} catch (NumberFormatException e) {
					reporter.error ("post-processor", "Invalid project ID");
					reporter.printSummary ();
					return ;
				}
			}

			Model model = pool.getModel ();
			Project project = model.getProject (projId);
			model.close ();


			
			if (project == null) {
				reporter.error ("post-processor", "Invalid project ID");
				reporter.printSummary ();
				return ;
			}
			

			if (cmd.hasOption ("bug-dictionary")) {
					DictionaryParser dp = new DictionaryParser ();

					for (String path : cmd.getOptionValues ("bug-dictionary")) {
						try {
							Dictionary dict = dp.parseFile (path);
							settings.bugDictionaries.add (dict);
						} catch (FileNotFoundException e) {
							reporter.error ("post-processor", "File  not found: "  + path +  ": " + e.getMessage ());					
							reporter.printSummary ();
							return ;
						} catch (XmlReaderException e) {
							reporter.error ("post-processor", "XML Error: " + path + ": " + e.getMessage ());					
							reporter.printSummary ();
							return ;
						}
					}
			}

			if (cmd.hasOption ("commit-dictionary")) {
				DictionaryParser dp = new DictionaryParser ();

				for (String path : cmd.getOptionValues ("commit-dictionary")) {
					try {
						Dictionary dict = dp.parseFile (path);
						settings.srcDictionaries.add (dict);
					} catch (FileNotFoundException e) {
						reporter.error ("post-processor", "File  not found: "  + path +  ": " + e.getMessage ());					
						reporter.printSummary ();
						return ;
					} catch (XmlReaderException e) {
						reporter.error ("post-processor", "XML Error: " + path + ": " + e.getMessage ());					
						reporter.printSummary ();
						return ;
					}
				}
			}

			if (cmd.hasOption ("smart-matching")) {
				String str = cmd.getOptionValue ("smart-matching");
				String[] parts = str.split (":");
				if (parts.length != 2) {
					reporter.error ("post-processor", "Unexpected smart-matching format");
					reporter.printSummary ();
					return ;
				}

				HashFunc func = HashFunc.getHashFunc (parts[0]);
				if (func == null) {
					reporter.error ("post-processor", "Unknown smart matching hash function");
					reporter.printSummary ();
					return ;
				}

				int dist = -1;
				try {
					dist = Integer.parseInt (parts[1]);
				} catch (NumberFormatException e) {
					dist = -1;
				}

				if (dist < 0) {
					reporter.error ("post-processor", "Invalid smart matching edist distance");
					reporter.printSummary ();
					return ;
				}

				interlinkingTask.setDistance (dist);
				interlinkingTask.setHashFunc (func);
			}

			PostProcessor processor = new PostProcessor (project, pool, settings);
			if (cmd.hasOption ("processor-step")) {
				for (String stepName : cmd.getOptionValues ("processor-step")) {
					PostProcessorTask step = steps.get (stepName);
					if (step == null) {
						reporter.error ("post-processor", "Unknown processor step: '" + stepName + "'");
						reporter.printSummary ();
						return ;
					}

					processor.register (step);
				}
			} else {
				processor.register (steps.values ());
			}

			if (printTraces == true) {
				model = pool.getModel ();
				final Stats stats = model.getStats (project);
				model.close ();

				processor.addListener(new PostProcessorListener () {
					private int commitCount = 0;
					private int bugCount = 0;

					@Override
					public void commit(PostProcessor proc) {
						commitCount++;
						reporter.note ("post-processor", "status: Commit " + commitCount + "/" + stats.commitCount);
					}

					@Override
					public void bug(PostProcessor proc) {
						bugCount++;
						reporter.note ("post-processor", "status: Bug " + bugCount + "/" + stats.bugCount);
					}
				});
			}

			processor.process ();
		} catch (ParseException e) {
			reporter.error ("post-processor", "Parsing failed: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (ClassNotFoundException e) {
			reporter.error ("post-processor", "Failed to create a database connection: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (SQLException e) {
			reporter.error ("post-processor", "Failed to create a database connection: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (PostProcessorException e) {
			reporter.error ("post-processor", "Post-Processor Error: " + e.getMessage ());			
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} finally {
			if (pool != null) {
				pool.close ();
			}
		}

		reporter.printSummary (true);
	}
}

