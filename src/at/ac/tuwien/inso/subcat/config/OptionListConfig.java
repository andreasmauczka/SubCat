/* OptionListConfig.java
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

package at.ac.tuwien.inso.subcat.config;


public class OptionListConfig extends ConfigNode {
	private Query query;
	private String variableName;

	public OptionListConfig (String variableName, Query query, SourcePos start, SourcePos end) {
		super (start, end);
		
		assert (variableName != null);
		assert (query != null);
		
		this.variableName = variableName;
		this.query = query;
	}

	public Query getQuery () {
		return query;
	}
	
	public String getVariableName () {
		return variableName;
	}
	
	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitOptionListConfig (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);

		if (query != null) {
			query.accept (visitor);
		}
	}
}
