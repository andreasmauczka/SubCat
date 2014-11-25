package at.ac.tuwien.inso.subcat.model;

public class Dictionary {
	private Integer id;
	private String name;
	private Project project;
	private String context;
	
	public Dictionary (Integer id, String name, String context, Project project) {
		assert (name != null);
		assert (project != null);
		
		this.id = id;
		this.name = name;
		this.project = project;
		this.context = context;
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


	public String getContext () {
		return context;
	}


}
