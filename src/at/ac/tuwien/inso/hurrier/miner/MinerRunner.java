/* MinerRunner.java
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

package at.ac.tuwien.inso.hurrier.miner;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.hurrier.model.Model;
import at.ac.tuwien.inso.hurrier.model.Project;


public class MinerRunner {
	private static LinkedList<Miner.MetaData> registeredMiner;

	private List<MinerListener> listeners = new LinkedList<MinerListener> ();
	private LinkedList<RunnableMiner> miners = new LinkedList<RunnableMiner> ();

	private Settings settings;
	
	private MinerException storedException;

	private static void init () {
		if (registeredMiner == null) {
			registeredMiner = new LinkedList<Miner.MetaData> ();
			registeredMiner.add (new BugzillaMinerMetaData ());
			registeredMiner.add (new GitMinerMetaData ());
			//registeredMiner.add (new SvnMinerMetaData ());
		}
	}
	
	private MinerRunner (Settings settings) throws MinerException {
		assert (settings != null);

		this.settings = settings;
		init ();

		Model model;
		Project project;

		try {
			model = new Model ("my-test.db");
		} catch (ClassNotFoundException e) {
			throw new MinerException ("Unknown JDBC-Driver: " + e.getMessage (), e);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		}

		try {
			project = model.addProject (new Date (), settings.bugTrackerName, settings.bugProductName, null);
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		}


		LinkedList<Miner.MinerType> foundMinerTypes = new LinkedList<Miner.MinerType> ();
		
		for (Miner.MetaData meta : registeredMiner) {
			if (foundMinerTypes.contains (meta.getType ())) {
				continue ;
			}

			if (meta.is (settings)) {
				Miner miner = meta.create (settings, project, model);
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
		Settings settings = new Settings ();

		/* Bugzilla Test:
		settings.bugRepository = "https://bugzilla.gnome.org";
		settings.bugProductName = "valadoc";
		settings.bugTrackerName = "Bugzilla";
		settings.bugEnableUntrustedCertificates = true;
		settings.bugThreads = 1;
		/* GIT Test: * /
		settings.srcLocalPath = System.getProperty("user.dir");
		*/
		/* Svn Test: * /
		settings.srcLocalPath = "/home/mog/student-project-netcracker-read-only";
		settings.srcRemote = "http://student-project-netcracker.googlecode.com/svn/trunk/";
		*/
		try {
			MinerRunner runner = new MinerRunner (settings);
			runner.addListener (new MinerListener () {
				@Override
				public void start (Miner miners) {
					System.out.println ("START");
				}
	
				@Override
				public void end (Miner miner) {
					System.out.println ("END");
				}
	
				@Override
				public void stop (Miner miner) {
					System.out.println ("STOP");
				}
			});
			runner.run ();
		} catch (MinerException e) {
			System.out.println ("Error: " + e.getMessage ());
			e.printStackTrace();
		}
	}
}

