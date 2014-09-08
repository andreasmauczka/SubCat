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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;

public abstract class Miner {
	public enum MinerType {
		SOURCE,
		BUG
	}

	public static interface MetaData {
		public abstract MinerType getType ();
		
		public abstract String name ();
		
		public abstract boolean is (Settings settings);
		
		public abstract Miner create (Settings settings, Project project, Model model);
	}

	
	private List<MinerListener> listeners = new LinkedList<MinerListener> ();
	
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


	//
	// Listener Helper:
	//

	protected synchronized void triggerStart () {
		for (MinerListener listener : listeners) {
			listener.start (this);
		}
	}

	protected synchronized void triggerEnd () {
		for (MinerListener listener : listeners) {
			listener.end (this);
		}
	}

	protected synchronized void triggerStop () {
		for (MinerListener listener : listeners) {
			listener.stop (this);
		}
	}
}

