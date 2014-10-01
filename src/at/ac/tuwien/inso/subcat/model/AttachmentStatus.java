

package at.ac.tuwien.inso.subcat.model;


public class AttachmentStatus implements NamedContainer, Comparable<AttachmentStatus> {
	private Integer id;
	private Project project;
	private String name;

	public AttachmentStatus (Integer id, Project project, String name) {
		assert (project != null);
		assert (name != null);

		this.id = id;
		this.project = project;
		this.name = name;
	}

	@Override
	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	@Override
	public Project getProject () {
		return project;
	}

	public void setProject (Project project) {
		assert (project != null);

		this.project = project;
	}

	@Override
	public String getName () {
		return name;
	}

	public void setName (String name) {
		assert (name != null);

		this.name = name;
	}

	@Override
	public String toString () {
		return "AttachmentStatus [id=" + id + ", project=" + project + ", name=" + name
				+ "]";
	}

	public boolean equals (AttachmentStatus obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}


	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Status) {
			return equals ((Status) obj);
		}
			
		return super.equals (obj);
	}

	@Override
	public int compareTo (AttachmentStatus stat) {
		if (id == stat.id) {
			return 0;
		}

		return name.compareTo (stat.name);
	}
}
