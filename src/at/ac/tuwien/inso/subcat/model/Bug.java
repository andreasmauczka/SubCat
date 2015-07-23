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

package at.ac.tuwien.inso.subcat.model;

import java.util.Date;


public class Bug {
	private Integer id;
	private Identity identity;
	private Component component;
	private String title;
	private Date creation;
	private Date lastChange;
	private Priority priority;
	private Severity severity;
	private Integer identifier;
	private Resolution resolution;
	private Version version;
	private Milestone milestone;
	private OperatingSystem os;
	private Platform platform;
	private Status status;
	private BugClass classification;


	public Bug (Integer id, Integer identifier, Identity identity, Component component,
			String title, Date creation, Date lastChange, Priority priority, Severity severity,
			Status status, Resolution resolution, Version version, Milestone milestone,
			OperatingSystem os, Platform platform, BugClass classification) {
		assert (component != null);
		assert (title != null);
		assert (creation != null);
		assert (priority != null);
		assert (resolution != null);
		assert (lastChange != null);
		assert (version != null);
		assert (os != null);
		assert (platform != null);
		assert (status != null);
		assert (milestone != null);
		assert (severity != null);
		
		this.id = id;
		this.identifier = identifier;
		this.identity = identity;
		this.component = component;
		this.title = title;
		this.creation = creation;
		this.priority = priority;
		this.severity = severity;
		this.status = status;
		this.resolution = resolution;
		this.lastChange = lastChange;
		this.version = version;
		this.os = os;
		this.platform = platform;
		this.milestone = milestone;
		this.classification = classification;
	}

	public Integer getIdentifier () {
		return identifier;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Identity getIdentity () {
		return identity;
	}

	public void setIdentity (Identity identity) {
		this.identity = identity;
	}

	public Component getComponent () {
		return component;
	}

	public void setComponent (Component component) {
		assert (component != null);

		this.component = component;
	}

	public String getTitle () {
		return title;
	}

	public void setTitle (String title) {
		assert (title != null);

		this.title = title;
	}

	public Date getCreation () {
		return creation;
	}

	public void setCreation (Date creation) {
		assert (creation != null);

		this.creation = creation;
	}

	public Date getLastChange () {
		return lastChange;
	}

	public void setLastChange (Date lastChange) {
		assert (lastChange != null);

		this.lastChange = lastChange;
	}
	
	public Priority getPriority () {
		return priority;
	}

	public void setPriority (Priority priority) {
		assert (priority != null);
		
		this.priority = priority;
	}
	
	public Version getVersion () {
		return version;
	}

	public void setVersion (Version version) {
		assert (version != null);
		
		this.version = version;
	}
	
	public Milestone getTargetMilestone () {
		return milestone;
	}

	public void setMilestone (Milestone milestone) {
		assert (milestone != null);
		
		this.milestone = milestone;
	}

	public boolean equals (Bug obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	public Severity getSeverity () {
		return severity;
	}

	public void setSeverity (Severity severity) {
		this.severity = severity;
	}

	public Status getStatus () {
		return status;
	}

	public void setStatus (Status status) {
		this.status = status;
	}

	public void setResolution (Resolution resolution) {
		this.resolution = resolution;
	}

	public OperatingSystem getOperatingSystem () {
		return os;
	}
	
	public Platform getPlatform () {
		return platform;
	}

	public BugClass getClassification () {
		return classification;
	}
	
	public void setOperatingSystem (OperatingSystem os) {
		assert (os != null);

		this.os = os;
	}

	public Resolution getResolution () {
		return resolution;
	}
	
	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Bug) {
			return equals ((Bug) obj);
		}
		
		return super.equals (obj);
	}

	@Override
	public String toString () {
		return "Bug [id=" + id + ", identity=" + identity + ", component="
				+ component + ", title=" + title + ", creation=" + creation
				+ ", priority=" + priority + ", severity=" + severity + "]";
	}
}
