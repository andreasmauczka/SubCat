/* Bug.java
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

import java.util.Arrays;
import java.util.Date;

public class BugzillaBug {
	private int id;
	private String alias;
	// since 3.4
	private String assignedTo;
	// since ???
	private String[] ccs;
	// since 3.4
	private String component;
	private Date creationTime;
	// since 3.4
	private Integer dup;
	// since 3.4
	private boolean isOpen;
	private Date lastChangeTime;
	// since ???
	private String opSys;
	private String platform;
	// since 3.4
	private String priority;
	// since 3.4
	private String product;
	// since 3.4
	private String resolution;
	// since 3.4
	private String severity;
	// since 3.4
	private String status;
	private String summary;
	// since ???
	private String version;
	private String targetMilestone;
	private Date deadline;
	private String qaContact;
	private Integer[] blocks;
	private Integer[] dependsOn;


	public BugzillaBug (int id, String alias, String assignedTo,
			String[] ccs, String component,
			Date creationTime, Integer dup, boolean isOpen,
			Date lastChangeTime, String opSys, String platform,
			String priority, String product,
			String resolution, String severity, String status, String summary,
			String version, String targetMilestone, Date deadline,
			String qaContact, Integer[] blocks, Integer[] dependsOn)
	{
		assert (alias != null);
		assert (component != null);
		assert (creationTime != null);
		assert (lastChangeTime != null);
		assert (priority != null);
		assert (product != null);
		assert (resolution != null);
		assert (severity != null);
		assert (status != null);
		assert (summary != null);
		assert (platform != null);
		assert (targetMilestone != null);

		if (ccs == null) {
			ccs = new String[0];
		}
		if (blocks == null) {
			blocks = new Integer[0];
		}
		if (dependsOn == null) {
			dependsOn = new Integer[0];
		}

		this.id = id;
		this.alias = alias;
		this.assignedTo = assignedTo;
		this.ccs = ccs;
		this.component = component;
		this.creationTime = creationTime;
		this.dup = dup;
		this.isOpen = isOpen;
		this.lastChangeTime = lastChangeTime;
		this.opSys = opSys;
		this.platform = platform;
		this.priority = priority;
		this.product = product;
		this.resolution = resolution;
		this.severity = severity;
		this.status = status;
		this.summary = summary;
		this.version = version;
		this.targetMilestone = targetMilestone;
		this.deadline = deadline;
		this.qaContact = qaContact;
		this.blocks = blocks;
		this.dependsOn = dependsOn;
	}


	public int getId () {
		return id;
	}

	public String getAlias () {
		return alias;
	}

	public String getAssignedTo () {
		return assignedTo;
	}

	public String[] getCcs () {
		return ccs;
	}

	public String getComponent () {
		return component;
	}

	public Date getCreationTime () {
		return creationTime;
	}

	public Integer getDup () {
		return dup;
	}

	public boolean isOpen () {
		return isOpen;
	}

	public Date getLastChangeTime () {
		return lastChangeTime;
	}

	public String getOpSys () {
		return opSys;
	}

	public String getPlatform () {
		return platform;
	}

	public String getPriority () {
		return priority;
	}

	public String getProduct () {
		return product;
	}

	public String getResolution () {
		return resolution;
	}

	public String getSeverity () {
		return severity;
	}

	public String getStatus () {
		return status;
	}

	public String getSummary () {
		return summary;
	}

	public String getVersion () {
		return version;
	}

	public String getTargetMilestone () {
		return targetMilestone;
	}

	public Date getDeadline () {
		return deadline;
	}

	public String getQaContact () {
		return qaContact;
	}
	
	public Integer[] getBlocks () {
		return blocks;
	}

	public Integer[] getDependsOn () {
		return dependsOn;
	}

	@Override
	public String toString () {
		return "Bug [id=" + id + ", alias=" + alias + ", assignedTo="
				+ assignedTo + ", ccs=" + Arrays.toString(ccs) + ", component="
				+ component + ", creationTime=" + creationTime + ", dup="
				+ dup + ", isOpen=" + isOpen + ", lastChangeTime="
				+ lastChangeTime + ", opSys=" + opSys + ", priority="
				+ priority + ", product=" + product + ", resolution="
				+ resolution + ", severity=" + severity + ", status=" + status
				+ ", summary=" + summary + ", version=" + version + "]";
	}
}
