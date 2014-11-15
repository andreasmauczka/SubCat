/* Miner.java
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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public abstract class Miner {
	public enum MinerType {
		SOURCE,
		BUG
	}
	
	private List<MinerListener> listeners = new LinkedList<MinerListener> ();
	private int processed = 0;
	
	public void removeListener (MinerListener listener) {
		listeners.remove (listener);
	}

	public void addListener (MinerListener listener) {
		assert (listener != null);

		listeners.add (listener);
	}

	public void addListener (Collection<MinerListener> listeners) {
		this.listeners.addAll (listeners);
	}
	
	public abstract void run () throws MinerException;

	public abstract void stop ();

	public abstract String getName ();

	//
	// Listener Helper:
	//

	protected synchronized void emitStart () {
		for (MinerListener listener : listeners) {
			listener.start (this);
		}
	}

	protected synchronized void emitEnd () {
		for (MinerListener listener : listeners) {
			listener.end (this);
		}
	}

	protected synchronized void emitStop () {
		for (MinerListener listener : listeners) {
			listener.stop (this);
		}
	}

	protected synchronized void emitTasksTotal (int total) {
		for (MinerListener listener : listeners) {
			listener.tasksTotal (this, total);
		}		
	}

	protected synchronized void emitTasksProcessed (int newlyProcessed) {
		processed += newlyProcessed;
		
		for (MinerListener listener : listeners) {
			listener.tasksProcessed (this, processed);
		}
	}
	

	//
	// Helper:
	//

	public static boolean inRepository (String filePath, String hiddenDirName) {
		File dir = new File (filePath);
		if (!dir.exists() || !dir.isDirectory()) {
			return false;
		}
	
		dir = dir.getAbsoluteFile ();
	
		do {
			File girDir = new File (dir, hiddenDirName);
			if (girDir.exists() && girDir.isDirectory()) {
				return true;
			}
			
			dir = dir.getParentFile ();
		} while (dir != null);
		
		return false;
	}
}

