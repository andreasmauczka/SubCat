/* ManagedFile.java
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

package at.ac.tuwien.inso.subcat.model;

public class ManagedFile {
	private Integer id;
	private Project project;
	private String name;
	private int touched;
	private int linesAdded;
	private int linesRemoved;
	private int chunksChanged;

	public ManagedFile (Integer id, Project project, String name,
			Commit deletionCommit, int touched, int linesAdded,
			int linesRemoved, int chunksChanged) {
		assert (project != null);
		assert (name != null);
		assert (touched > 0);
		assert (linesAdded >= 0);
		assert (linesRemoved >= 0);
		assert (chunksChanged > 0);

		this.id = id;
		this.project = project;
		this.name = name;
		this.touched = touched;
		this.linesAdded = linesAdded;
		this.linesRemoved = linesRemoved;
		this.chunksChanged = chunksChanged;
	}
	
	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}
	
	public Project getProject () {
		return project;
	}

	public String getName () {
		return name;
	}

	public int getTouched () {
		return touched;
	}

	public int getLinesAdded () {
		return linesAdded;
	}

	public int getLinesRemoved () {
		return linesRemoved;
	}

	public int getChunksChanged () {
		return chunksChanged;
	}
}
