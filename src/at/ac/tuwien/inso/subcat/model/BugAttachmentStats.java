package at.ac.tuwien.inso.subcat.model;


public class BugAttachmentStats {
	private int attId;
	private int attIdentifier;
	private int attObsCnt;
	private int attHistCnt;

	public BugAttachmentStats (int attId, int attIdentifier, int attObsCnt, int attHistCnt) {
		this.attId = attId;
		this.attIdentifier = attIdentifier;
		this.attObsCnt = attObsCnt;
		this.attHistCnt = attHistCnt;
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
}
