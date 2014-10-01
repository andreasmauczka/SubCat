package at.ac.tuwien.inso.subcat.model;

public class Attachment {
	private Comment comment;
	private String identifier;
	private Integer id;

	public Attachment (Integer id, String identifier, Comment comment) {
		assert (comment != null);
		assert (identifier != null);
		
		this.id = id;
		this.comment = comment;
		this.identifier = identifier;
	}
	
	public Comment getComment () {
		return comment;
	}

	public String getIdentifier () {
		return identifier;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}
}
