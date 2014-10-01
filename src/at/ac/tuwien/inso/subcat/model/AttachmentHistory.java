package at.ac.tuwien.inso.subcat.model;

import java.util.Date;

public class AttachmentHistory {
	private Integer id;
	private Identity identity;
	private AttachmentStatus status;
	private Attachment attachment;
	private Date date;

	public AttachmentHistory (Integer id, Identity identity, AttachmentStatus status,
			Attachment attachment, Date date) {
		assert (identity != null);
		assert (status != null);
		assert (attachment != null);
		assert (date != null);

		this.id = id;
		this.identity = identity;
		this.status = status;
		this.attachment = attachment;
		this.date = date;
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

	public AttachmentStatus getStatus () {
		return status;
	}

	public Attachment getAttachment () {
		return attachment;
	}

	public Date getDate () {
		return date;
	}
}
