package at.ac.tuwien.inso.subcat.model;

import java.util.Date;

public class AttachmentStatusHistory {
	private Integer id;
	private Attachment attachment;
	private Identity identity;
	private Date date;
	private AttachmentStatus oldStatus;
	private AttachmentStatus newStatus;

	public AttachmentStatusHistory (Integer id, Attachment attachment, Identity identity, Date date, AttachmentStatus oldStatus, AttachmentStatus newStatus) {
		assert (attachment != null);
		assert (identity != null);
		assert (date != null);
		assert (oldStatus != null);
		assert (newStatus != null);

		this.id = id;
		this.attachment = attachment;
		this.identity = identity;
		this.date = date;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
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

	public AttachmentStatus getOldStatus () {
		return oldStatus;
	}

	public AttachmentStatus getNewStatus () {
		return newStatus;
	}

	public Attachment getAttachment () {
		return attachment;
	}

	public Date getDate () {
		return date;
	}
}
