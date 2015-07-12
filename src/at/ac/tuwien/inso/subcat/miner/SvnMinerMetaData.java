/* SvnMinerMetaData.java
 *
 * Copyright (C) 2014 Florian Brosch
 * Copyright (C) 2014 Andreas Mauczka
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
 *       Andreas Mauczka <andreas.mauczka(at)inso.tuwien.ac.at>
 */


package at.ac.tuwien.inso.subcat.miner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.inso.subcat.miner.Miner.MinerType;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.Reporter;
import at.ac.tuwien.inso.subcat.utility.XmlReader;


public class SvnMinerMetaData extends MetaData {
	private final static String name = "SVN";
	
	@Override
	public MinerType getType () {
		return MinerType.SOURCE;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public boolean is (Project proj, Settings settings) {
		assert (settings !=null);

		if (settings.srcLocalPath == null) {
			return false;
		}

		File dir = new File (settings.srcLocalPath);
		if (dir.isFile ()) {
			return XmlReader.isXml (settings.srcLocalPath, "log");
		}

		return Miner.inRepository (settings.srcLocalPath, ".svn");
	}

	
	@Override
	public Miner create (Settings settings, Project project, ModelPool pool, Reporter reporter) {
		return new SvnMiner (settings, project, pool, reporter);
	}

	@Override
	public boolean checkSpecificParams (Map<String, Object> params, Map<String, String> errors) {
		assert (params != null);
		assert (errors != null);

		if (params.size () != 0) {
			errors.put (null, "Unexpected parameter count");
			return false;
		}
		
		return true;
	}

	@Override
	public Map<String, ParamType> getSpecificParams () {
		return new HashMap<String, ParamType> ();
	}
}
