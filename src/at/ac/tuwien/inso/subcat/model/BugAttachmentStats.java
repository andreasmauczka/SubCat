package at.ac.tuwien.inso.subcat.model;


public class BugAttachmentStats {
	private int attId;
	private int attIdentifier;
	private int attObsCnt;
	private int attHistCnt;
	private int attStatHistCnt;

	public BugAttachmentStats (int attId, int attIdentifier, int attObsCnt, int attStatHistCnt, int attHistCnt) {
		this.attId = attId;
		this.attIdentifier = attIdentifier;
		this.attObsCnt = attObsCnt;
		this.attHistCnt = attHistCnt;
		this.attStatHistCnt = attStatHistCnt;
	}

	public int getId () {
		return attId;
	}

	public int getIdentifier () {
		return attIdentifier;
	}
	
	public int getObsoleteCount () {
		return attObsCnt;
	}

	public int getHistoryCount () {
		return attHistCnt;
	}

	public int getStatusHistoryCount () {
		return attStatHistCnt;
	}
}
