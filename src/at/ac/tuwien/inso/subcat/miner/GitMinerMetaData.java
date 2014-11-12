/* GitMinerMetaData.java
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

import at.ac.tuwien.inso.subcat.miner.MetaData;
import at.ac.tuwien.inso.subcat.miner.Miner.MinerType;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;


public class GitMinerMetaData extends MetaData {
	private final static String name = "GIT";
	
	@Override
	public MinerType getType () {
		return MinerType.SOURCE;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public boolean is (Settings settings) {
		assert (settings !=null);

		if (settings.srcLocalPath == null) {
			return false;
		}

		return Miner.inRepository (settings.srcLocalPath, ".git");
	}

	@Override
	public Miner create (Settings settings, Project project, ModelPool pool) {
		return new GitMiner (settings, project, pool);
	}

	@Override
	public boolean checkSpecificParams (Map<String, Object> params, Map<String, String> errors) {
		assert (params != null);
		assert (errors != null);

		for (Map.Entry<String, Object> entry : params.entrySet ()) {
			String name = entry.getKey ();
			Object val = entry.getValue ();

			if (name.equalsIgnoreCase ("process-diffs")) {
				assertBoolean (name, val, errors);
			} else if (name.equalsIgnoreCase ("start-ref")) {
				assertString (name, val, errors);
			} else {
				errors.put (name, "unknown parameter");
			}
		}
		
		return true;
	}

	@Override
	public Map<String, ParamType> getSpecificParams () {
		Map<String, ParamType> params = new HashMap<String, ParamType> ();
		params.put ("process-diffs", ParamType.BOOLEAN);
		params.put ("start-ref", ParamType.STRING);
		return params;
	}

}
