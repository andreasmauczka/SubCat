/* MetaData.java
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

import java.util.Map;

import at.ac.tuwien.inso.subcat.miner.Miner.MinerType;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;


public abstract class MetaData {
	public enum ParamType {
		INTEGER,
		BOOLEAN,
		STRING
	}

	public abstract MinerType getType ();
	
	public abstract String name ();
	
	public abstract boolean is (Settings settings);
	
	public abstract Miner create (Settings settings, Project project, ModelPool pool);

	public abstract boolean checkSpecificParams (Map<String, Object> params, Map<String, String> errors);

	public abstract Map<String, ParamType> getSpecificParams ();

	protected boolean assertString (String name, Object obj, Map<String, String> errors) {
		assert (name != null);
		assert (errors != null);
		assert (obj != null);

		if (obj instanceof String == false) {
			errors.put (name, "invalid type, expected string");
			return false;
		}

		return true;
	}

	protected boolean assertBoolean (String name, Object obj, Map<String, String> errors) {
		assert (name != null);
		assert (errors != null);
		assert (obj != null);

		if (obj instanceof Boolean == false) {
			errors.put (name, "invalid type, expected boolean");
			return false;
		}

		return true;
	}
	
	protected boolean assertInteger (String name, Object obj, Map<String, String> errors) {
		return assertInteger (name, obj, Integer.MIN_VALUE, Integer.MAX_VALUE, errors);
	}

	protected boolean assertInteger (String name, Object obj, int min, int max, Map<String, String> errors) {
		assert (name != null);
		assert (obj != null);
		assert (errors != null);
		assert (max >= min);
		
		if (obj instanceof Integer == false) {
			errors.put (name, "invalid type, expected int");
			return false;
		}

		int val = (Integer) obj;
		
		if (val < min) {
			errors.put (name, "invalid value, name < " + min);			
			return false;
		}

		if (val > max) {
			errors.put (name, "invalid value, name > " + max);			
			return false;
		}

		return true;
	}
}
