/* History.java
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

package at.ac.tuwien.inso.subcat.bugzilla;

import java.util.Date;


public class BugzillaHistory {
	private Date when;
	private String who;
	private BugzillaChange[] changes;

	public BugzillaHistory (Date when, String who, BugzillaChange[] changes) {
		assert (when != null);
		assert (who != null);
		assert (changes != null);
		
		this.when = when;
		this.who = who;
		this.changes = changes;
	}

	public Date getWhen () {
		return when;
	}

	public String getWho () {
		return who;
	}

	public BugzillaChange[] getChanges () {
		return changes;
	}

	public String toString () {
		return "[History when="
			+ when
			+ " who="
			+ who
			+ " changes="
			+ changes.length
			+ "]";
	}
}
