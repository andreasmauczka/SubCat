/* Settings.java
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

import java.util.HashMap;
import java.util.Map;


public class Settings {

	//
	// Bug Repository:
	//

	public String bugRepository = null;
	public String bugProductName = null;
	public String bugLoginUser = null;
	public String bugLoginPw = null;
	public String bugTrackerName = null;
	public boolean bugEnableUntrustedCertificates = false;
	public int bugThreads = 1;
	public long bugCooldownTime = 0;
	public Map<String, Object> bugSpecificParams
		= new HashMap<String, Object> ();

	public boolean bugGetParameter (String name, boolean defaultValue) {
		return getParameter (bugSpecificParams, name, defaultValue);
	}

	public int bugGetParameter (String name, int defaultValue) {
		return getParameter (bugSpecificParams, name, defaultValue);
	}


	//
	// Source Repository:
	//
	
	public String srcLocalPath = null;
	public String srcRemote = "";
	public String srcRemotePw = null;
	public String srcRemoteUser = null;
	public Map<String, Object> srcSpecificParams
		= new HashMap<String, Object> ();

	public boolean srcGetParameter (String name, boolean defaultValue) {
		return getParameter (srcSpecificParams, name, defaultValue);
	}

	public int srcGetParameter (String name, int defaultValue) {
		return getParameter (srcSpecificParams, name, defaultValue);
	}

	
	//
	// Helper:
	//
	
	private boolean getParameter (Map<String, Object> params, String name, boolean defaultValue) {
		Object obj = params.get (name);
		if (obj == null) {
			return defaultValue;
		}

		assert (obj instanceof Boolean);
		return (Boolean) obj;
	}

	private int getParameter (Map<String, Object> params, String name, int defaultValue) {
		Object obj = params.get (name);
		if (obj == null) {
			return defaultValue;
		}

		assert (obj instanceof Integer);
		return (Integer) obj;		
	}
}
