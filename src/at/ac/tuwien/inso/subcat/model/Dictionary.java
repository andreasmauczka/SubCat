package at.ac.tuwien.inso.subcat.model;

public class Dictionary {
	private Integer id;
	private String name;
	private Project project;

	
	public Dictionary (Integer id, String name, Project project) {
		assert (name != null);
		assert (project != null);
		
		this.id = id;
		this.name = name;
		this.project = project;
	}


	public Integer getId () {
		return id;
	}


	public void setId (Integer id) {
		this.id = id;
	}


	public String getName () {
		return name;
	}


	public Project getProject () {
		return project;
	}


}
