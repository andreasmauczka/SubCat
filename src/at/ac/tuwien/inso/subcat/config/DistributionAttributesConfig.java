/* DistributionAttributesConfig.java
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

import java.util.LinkedList;
import java.util.List;


public class DistributionAttributesConfig extends ConfigNode {
	private List<DistributionAttributeConfig> data;


	public DistributionAttributesConfig (SourcePos start, SourcePos end) {
		super (start, end);

		data = new LinkedList<DistributionAttributeConfig> ();
	}

	public void add (DistributionAttributeConfig config) {
		assert (config != null);

		data.add (config);
	}

	public List<DistributionAttributeConfig> getData () {
		return data;
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitDistributionAttributesConfig (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);

		for (DistributionAttributeConfig item : data) {
			item.accept (visitor);
		}
	}
}
