package at.ac.tuwien.inso.subcat.model;


public class Milestone implements NamedContainer, Comparable<Milestone> {
	private Integer id;
	private Project project;
	private String name;

	public Milestone (Integer id, Project project, String name) {
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
		return "Milestone [id=" + id + ", project=" + project + ", name=" + name
				+ "]";
	}

	public boolean equals (Milestone obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}


	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Milestone) {
			return equals ((Milestone) obj);
		}
		
		return super.equals (obj);
	}

	@Override
	public int compareTo (Milestone stat) {
		if (id == stat.id) {
			return 0;
		}

		return name.compareTo (stat.name);
	}
}
