/* ViewController.java
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

package at.ac.tuwien.inso.hurrier.ui.controller;

import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.inso.hurrier.model.Project;


public class ViewController {
	private Project project;
	
	public ViewController (Project project) {
		assert (project != null);
		assert (project.getId () != null);

		this.project = project;
	}

	public Project getProject () {
		return project;
	}

	public Map<String, Object> getVariables () {
		HashMap<String, Object> map = new HashMap<String, Object> ();
		map.put ("project", project.getId ());
		return map;
	}
}
