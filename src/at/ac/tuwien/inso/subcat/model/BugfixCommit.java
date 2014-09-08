/* BugfixCommit.java
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

package at.ac.tuwien.inso.subcat.model;


public class BugfixCommit {
	private Commit commit;
	private Bug bug;

	public BugfixCommit (Commit commit, Bug bug) {
		assert (commit != null);
		assert (bug != null);
	
		this.commit = commit;
		this.bug = bug;
	}

	public Commit getCommit () {
		return commit;
	}

	public void setCommit (Commit commit) {
		assert (commit != null);

		this.commit = commit;
	}

	public Bug getBug () {
		return bug;
	}

	public void setBug (Bug bug) {
		assert (bug != null);

		this.bug = bug;
	}

	@Override
	public String toString () {
		return "BugfixCommit [commit=" + commit + ", bug=" + bug + "]";
	}

	public boolean equals (BugfixCommit obj) {
		return commit.equals (obj.commit) && bug.equals (obj.bug);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof BugfixCommit) {
			return equals ((BugfixCommit) obj);
		}
		
		return super.equals (obj);
	}
}
