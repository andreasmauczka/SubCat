/* DistributionChartOptionConfig.java
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

package at.ac.tuwien.inso.subcat.config;

import java.util.LinkedList;
import java.util.List;


public class DistributionChartOptionConfig extends OptionalConfigNode {
	private String name;
	private LinkedList<DropDownConfig> filter;
	private DistributionAttributesConfig attributes;

	public DistributionChartOptionConfig (String name, SourcePos start, SourcePos end) {
		super (start, end);

		assert (name != null);

		filter = new LinkedList<DropDownConfig> ();
		this.name = name;
	}

	public String getName () {
		return name;
	}

	public void addFilter(DropDownConfig filter) {
		assert (filter != null);

		this.filter.add (filter);
	}

	public void setAttributes(DistributionAttributesConfig att) {
		assert (att != null);
		
		this.attributes = att;
	}

	public List<DropDownConfig> getFilter () {
		return this.filter;
	}
	
	public DistributionAttributesConfig getAttributes () {
		return this.attributes;
	}
	
	@Override
	public void accept(ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitDistributionChartOptionConfig (visitor);
	}

	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);
		
		for (DropDownConfig conf : filter) {
			conf.accept (visitor);
		}

		if (attributes != null) {
			attributes.accept (visitor);			
		}
	}
}
