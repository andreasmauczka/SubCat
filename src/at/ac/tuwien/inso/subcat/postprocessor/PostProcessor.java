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

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.ObjectCallback;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.classifier.Dictionary;
import at.ac.tuwien.inso.subcat.utility.classifier.DictionaryParser;

public class PostProcessor {
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

	public ModelPool getModelPool () {
		return pool;
	}

	public Settings getSettings () {
		return settings;
	}
	
	public Project getProject () {
		return proj;
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
								emitCommit (item);
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

	private void emitCommit (Commit commit) throws PostProcessorException {
		for (PostProcessorTask task : commitTasks) {
			if (stopped){
				break;
			}

			task.commit (this, commit);
		}
	}

	private void emitBug (Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		for (PostProcessorTask task : bugTasks) {
			if (stopped){
				break;
			}

			task.bug (this, bug, history, comments);
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
		try {
			Settings settings = new Settings ();
			
			DictionaryParser dp = new DictionaryParser ();
			Dictionary swansonDict = dp.parseFile ("dictionary.xml");
			settings.srcDictionaries.add (swansonDict);
			settings.bugDictionaries.add (swansonDict);

			ModelPool pool = new ModelPool ("my-test.db");

			Model model = pool.getModel ();
			Project proj = model.getProjects ().get (0);
			model.close ();

			PostProcessor processor = new PostProcessor (proj, pool, settings);
			processor.register (new ClassificationTask ());
			processor.process ();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

