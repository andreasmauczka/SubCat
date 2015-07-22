/* BugFlagAssignment.java
 *
 * Copyright (C) 2015 Florian Brosch
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

import java.util.Date;


public class BugFlagAssignment  {
	private BugFlag flag;
	private Date creationDate;
	private Date modificationDate;
	private BugFlagStatus status;
	private Identity setter;
	private Identity requestee;

	public BugFlagAssignment(BugFlag flag, Date creationDate,
			Date modificationDate, BugFlagStatus status, Identity setter,
			Identity requestee) {
		assert (flag != null);
		assert (creationDate != null);
		assert (modificationDate != null);
		assert (status != null);
		assert (setter != null);

		this.flag = flag;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.status = status;
		this.setter = setter;
		this.requestee = requestee;
	}

	public BugFlag getFlag () {
		return flag;
	}
	public Date getCreationDate () {
		return creationDate;
	}
	public Date getModificationDate () {
		return modificationDate;
	}
	public BugFlagStatus getStatus () {
		return status;
	}
	public Identity getSetter () {
		return setter;
	}
	public Identity getRequestee () {
		return requestee;
	}
}
