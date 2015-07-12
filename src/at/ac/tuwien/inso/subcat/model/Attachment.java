package at.ac.tuwien.inso.subcat.model;

public class Attachment {
	private Comment comment;
	private Integer identifier;
	private Integer id;

	public Attachment (Integer id, Integer identifier, Comment comment) {
		assert (comment != null);
		assert (identifier != null);
		
		this.id = id;
		this.comment = comment;
		this.identifier = identifier;
	}
	
	public Comment getComment () {
		return comment;
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
}
