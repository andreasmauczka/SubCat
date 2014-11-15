/* MinerRunner.java
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

package at.ac.tuwien.inso.subcat.miner;

import java.sql.SQLException;
import java.util.Date;
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

import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;


public class MinerRunner {
	private static LinkedList<MetaData> registeredMiner;

	private List<MinerListener> listeners = new LinkedList<MinerListener> ();
	private LinkedList<RunnableMiner> miners = new LinkedList<RunnableMiner> ();
	
	private MinerException storedException;

	private static void init () {
		if (registeredMiner == null) {
			registeredMiner = new LinkedList<MetaData> ();
			registeredMiner.add (new BugzillaMinerMetaData ());
			registeredMiner.add (new GitMinerMetaData ());
			registeredMiner.add (new SvnMinerMetaData ());
		}
	}
	
	private MinerRunner (ModelPool pool, Settings settings) throws MinerException {
		assert (settings != null);

		init ();

		Project project;
		Model model = null;
		try {
			model = pool.getModel ();
			project = model.addProject (new Date (), settings.bugTrackerName, settings.bugProductName, null);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		} finally {
			if (model != null) {
				model.close ();
				model = null;
			}
		}


		LinkedList<Miner.MinerType> foundMinerTypes = new LinkedList<Miner.MinerType> ();
		
		for (MetaData meta : registeredMiner) {
			if (foundMinerTypes.contains (meta.getType ())) {
				continue ;
			}

			if (meta.is (settings)) {
				Miner miner = meta.create (settings, project, pool);
				miner.addListener (listeners);
				this.miners.add (new RunnableMiner (this, miner));
				foundMinerTypes.add (meta.getType ());
			}
		}

		if (settings.bugRepository != null || settings.bugProductName != null) {
			if (!foundMinerTypes.contains (Miner.MinerType.BUG)) {
				throw new MinerException ("No fitting bug repository miner found.");
			}
		}
		if (settings.srcLocalPath != null) {
			if (!foundMinerTypes.contains (Miner.MinerType.SOURCE)) {
				throw new MinerException ("No fitting source repository miner found.");
			}
		}
	}

	public void run () throws MinerException {
		for (RunnableMiner thread : miners) {
			thread.start ();
		}

		for (RunnableMiner thread : miners) {
			try {
				thread.join ();
			} catch (InterruptedException e) {
			}
		}

		if (storedException != null) {
			throw storedException;
		}		
	}

	synchronized void stop (MinerException e) {
		if (storedException == null) {
			storedException = e;
		}

		for (RunnableMiner miner : this.miners) {
			miner.interrupt ();
		}
	}
	
	public synchronized void addListener (MinerListener listener) {
		assert (listener != null);

		listeners.add (listener);

		for (RunnableMiner miner : miners) {
			miner.getMiner ().addListener (listener);
		}
	}

	public synchronized void removeListener (MinerListener listener) {
		assert (listener != null);

		listeners.remove (listener);
		
		for (RunnableMiner miner : miners) {
			miner.getMiner ().removeListener (listener);
		}
	}

	public static void main (String[] args) {
		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("v", "verbose", false, "Show details");

		options.addOption (null, "bug-repo", true, "Bug Repository URL");
		options.addOption (null, "bug-product", true, "Bug Product Name");
		options.addOption (null, "bug-tracker", true, "Bug Tracker name (e.g. bugzilla)");
		options.addOption (null, "bug-account", true, "Bug account name");
		options.addOption (null, "bug-passwd", true, "Bug account password");
		options.addOption (null, "bug-enable-untrusted", false, "Accept untrusted certificates");
		options.addOption (null, "bug-threads", true, "Thread count used in bug miners");
		options.addOption (null, "bug-cooldown-time", true, "Bug cooldown time");

		options.addOption (null, "src-path", true, "Local source repository path");
		options.addOption (null, "src-remote", true, "Remote address");
		options.addOption (null, "src-passwd", true, "Source repository account password");
		options.addOption (null, "src-account", true, "Source repository account name");

		
		Settings settings = new Settings ();
		ModelPool pool = null;

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
		
			pool = new ModelPool (cmd.getOptionValue ("db"), 2);

			settings.bugRepository = cmd.getOptionValue ("bug-repo");
			settings.bugProductName = cmd.getOptionValue ("bug-product");
			settings.bugTrackerName = cmd.getOptionValue ("bug-tracker");

			
			if (settings.bugRepository != null && (settings.bugProductName == null || settings.bugTrackerName == null)) {
				System.err.println ("--bug-repo should only be used in combination with --bug-product and --bug-tracker");
				return ;
			} else if (settings.bugProductName != null && (settings.bugRepository == null || settings.bugTrackerName == null)) {
				System.err.println ("--bug-product should only be used in combination with --bug-repo and --bug-tracker");
				return ;
			} else if (settings.bugTrackerName != null && (settings.bugRepository == null || settings.bugProductName == null)) {
				System.err.println ("--bug-tracker should only be used in combination with --bug-repo and --bug-product");
				return ;					
			}


			settings.bugLoginUser = cmd.getOptionValue ("bug-account");
			settings.bugLoginPw = cmd.getOptionValue ("bug-passwd");

			if (settings.bugLoginPw == null || settings.bugLoginUser == null) {
				if (settings.bugLoginPw != null) {
					System.err.println ("--bug-passwd should only be used in combination with --bug-account");
					return ;
				} else if (settings.bugLoginUser != null) {
					System.err.println ("--bug-account should only be used in combination with --bug-passwd");
					return ;
				}
			}

			if (settings.bugLoginUser != null && settings.bugRepository == null) {
				System.err.println ("--bug-account should only be used in combination with --bug-repo");
				return ;
			}

			if (cmd.hasOption ("bug-enable-untrusted")) {
				settings.bugEnableUntrustedCertificates = true;
			}

			if (cmd.hasOption ("bug-threads")) {
				try {
					settings.bugThreads = Integer.parseInt (cmd.getOptionValue ("bug-threads"));
				} catch (Exception e) {
					System.err.println ("--bug-threads: Invalid parameter type");
					return ;
				}
			}

			if (cmd.hasOption ("bug-cooldown-time")) {
				try {
					settings.bugCooldownTime = Integer.parseInt (cmd.getOptionValue ("bug-cooldown-time"));
				} catch (Exception e) {
					System.err.println ("--bug-cooldown-time: Invalid parameter type");
					return ;
				}
			}
			

			settings.srcLocalPath = cmd.getOptionValue ("src-path");
			settings.srcRemote = cmd.getOptionValue ("src-remote");
			settings.srcRemotePw = cmd.getOptionValue ("src-passwd");
			settings.srcRemoteUser = cmd.getOptionValue ("src-account");


			if (settings.srcRemotePw == null || settings.srcRemoteUser == null) {
				if (settings.srcRemotePw != null) {
					System.err.println ("--src-passwd should only be used in combination with --src-account");
					return ;
				} else if (settings.srcRemoteUser != null) {
					System.err.println ("--src-account should only be used in combination with --src-passwd");
					return ;
				}
			}

			if (settings.srcRemoteUser != null && settings.bugRepository == null) {
				System.err.println ("--src-account should only be used in combination with --src-remote");
				return ;
			}

			
			MinerRunner runner = new MinerRunner (pool, settings);
			if (cmd.hasOption ("verbose")) {
				runner.addListener (new MinerListener () {
					private Map<Miner, Integer> totals = new HashMap<Miner, Integer> ();
					
					@Override
					public void start (Miner miner) {
					}
		
					@Override
					public void end (Miner miner) {
					}
		
					@Override
					public void stop (Miner miner) {
					}

					@Override
					public void tasksTotal (Miner miner, int count) {
						totals.put (miner, count);
					}

					@Override
					public void tasksProcessed (Miner miner, int processed) {
						Integer total = totals.get (miner);
						System.out.println (miner.getName () + ":\t" + processed + "/" + ((total == null)? "?" : total));
					}
				});
			}
			runner.run ();
		} catch (ParseException e) {
			System.err.println ("Parsing failed: " + e.getMessage ());
		} catch (ClassNotFoundException e) {
			System.err.println ("Failed to create a database connection: " + e.getMessage ());
		} catch (SQLException e) {
			System.err.println ("Failed to create a database connection: " + e.getMessage ());
		} catch (MinerException e) {
			System.err.println ("Mining Error: " + e.getMessage ());
		} finally {
			if (pool != null) {
				pool.close ();
			}
		}
	}
}
