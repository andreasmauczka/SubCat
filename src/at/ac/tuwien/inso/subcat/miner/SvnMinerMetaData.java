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

import at.ac.tuwien.inso.subcat.miner.Miner.MetaData;
import at.ac.tuwien.inso.subcat.miner.Miner.MinerType;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.XmlReader;


public class SvnMinerMetaData implements MetaData {
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
	public boolean is (Settings settings) {
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
	public Miner create (Settings settings, Project project, Model model) {
		return new SvnMiner (settings, project, model);
	}
}
