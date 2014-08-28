/* BugzillaMinerMetaData.java
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

import at.ac.tuwien.inso.hurrier.miner.Miner.MinerType;
import at.ac.tuwien.inso.hurrier.model.Model;


public class BugzillaMinerMetaData implements Miner.MetaData {
	private static final String name = "Bugzilla";

	@Override
	public MinerType getType () {
		return Miner.MinerType.BUG;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public boolean is (Settings settings) {
		if (settings.bugRepository == null || settings.bugProductName == null || settings.bugTrackerName == null) {
			return false;
		}

		return name.equals (settings.bugTrackerName);
	}

	@Override
	public Miner create (Settings settings, Model model) {
		return new BugzillaMiner (settings, model);
	}
}
