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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.inso.subcat.utility.classifier.Dictionary;


public class Settings {

	//
	// Bug Repository:
	//

	public List<Dictionary> bugDictionaries = new LinkedList<Dictionary> ();
	public String bugRepository = null;
	public String bugProductName = null;
	public String bugLoginUser = null;
	public String bugLoginPw = null;
	public String bugTrackerName = null;
	public boolean bugEnableUntrustedCertificates = false;
	public boolean bugUpdate = false;
	public int bugThreads = 1;
	public long bugCooldownTime = 0;
	public Map<String, String> bugSpecificParams
		= new HashMap<String, String> ();

	public boolean bugGetParameter (Miner miner, String name, boolean defaultValue) throws ParameterException {
		return getParameter (miner, bugSpecificParams, name, defaultValue);
	}

	public int bugGetParameter (Miner miner, String name, int defaultValue) throws ParameterException {
		return getParameter (miner, bugSpecificParams, name, defaultValue);
	}

	public String bugGetParameter (Miner miner, String name, String defaultValue) {
		return getParameter (miner, bugSpecificParams, name, defaultValue);
	}


	//
	// Source Repository:
	//
	
	public List<Dictionary> srcDictionaries = new LinkedList<Dictionary> ();
	public String srcLocalPath = null;
	public String srcRemote = "";
	public String srcRemotePw = null;
	public String srcRemoteUser = null;
	public Map<String, String> srcSpecificParams
		= new HashMap<String, String> ();

	public boolean srcGetParameter (Miner miner, String name, boolean defaultValue) throws ParameterException {
		return getParameter (miner, srcSpecificParams, name, defaultValue);
	}

	public int srcGetParameter (Miner miner, String name, int defaultValue) throws ParameterException {
		return getParameter (miner, srcSpecificParams, name, defaultValue);
	}

	public String srcGetParameter (Miner miner, String name, String defaultValue) {
		return getParameter (miner, srcSpecificParams, name, defaultValue);
	}

	
	//
	// Helper:
	//
	
	private boolean getParameter (Miner miner, Map<String, String> params, String name, boolean defaultValue) throws ParameterException {
		assert (miner != null);
		assert (params != null);
		assert (name != null);
		
		String value = params.get (name);
		if (value == null) {
			return defaultValue;
		}

		if (value.equalsIgnoreCase ("true")) {
			return true;
		} else if (value.equalsIgnoreCase ("false")) {
			return false;				
		} else {
			throw new ParameterException (miner, "Invalid argument type for '" + name + "', expected boolean");
		}
	}

	private int getParameter (Miner miner, Map<String, String> params, String name, int defaultValue) throws ParameterException {
		assert (miner != null);
		assert (params != null);
		assert (name != null);

		String value = params.get (name);
		if (value == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt (value);
		} catch (NumberFormatException e) {
			throw new ParameterException (miner, "Invalid argument type for '" + name + "', expected integer");
		}
	}

	private String getParameter (Miner miner, Map<String, String> params, String name, String defaultValue) {
		assert (miner != null);
		assert (params != null);
		assert (name != null);

		String value = params.get (name);
		if (value == null) {
			return defaultValue;
		}
		
		return value;
	}
}
