/* PostProcessorTask.java
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

package at.ac.tuwien.inso.subcat.postprocessor;

import java.util.List;

import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Commit;


public abstract class PostProcessorTask {
	public static final long NONE = (1 << 0);
	public static final long BEGIN = (1 << 1);
	public static final long COMMIT = (1 << 2); 
	public static final long BUG = (1 << 3);
	public static final long END = (1 << 5);

	public final long flags;

	public static boolean isValidFlag (long val) {
		return (val & ~NONE
					& ~BEGIN
					& ~COMMIT
					& ~BUG
					& ~END
				) == 0;
	}

	public PostProcessorTask (long flags) {
		assert (isValidFlag (flags));
		assert (flags != NONE);

		this.flags = flags;
	}

	public void begin (PostProcessor procesor) throws PostProcessorException {
	}
	
	public void commit (PostProcessor procesor, Commit commit) throws PostProcessorException {
	}
	
	public void bug (PostProcessor procesor, Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
	}
	
	public void end (PostProcessor procesor) throws PostProcessorException {
	}
}
