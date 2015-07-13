package at.ac.tuwien.inso.subcat.model;

import java.util.Map;


public class BugStats {
	private Map<Integer, BugAttachmentStats> attStats;
	private int id;
	private int cmntCnt;
	private int histCnt;
	private int attCnt;
	private int ccCnt;
	private int blocksCnt;
	private int aliasCnt;
	private int severityHistoryCnt;


	public BugStats (int id, int cmntCnt, int histCnt, int attCnt, int ccCnt, int blocksCnt, int aliasCnt, int severityHistoryCnt, Map<Integer, BugAttachmentStats> attStats) {
		this.id = id;
		this.cmntCnt = cmntCnt;
		this.histCnt = histCnt;
		this.attCnt = attCnt;
		this.attStats = attStats;
		this.ccCnt = ccCnt;
		this.blocksCnt = blocksCnt;
		this.aliasCnt = aliasCnt;
		this.severityHistoryCnt = severityHistoryCnt;
	}
	
	public Map<Integer, BugAttachmentStats> getAttachmentStatsByIdentifier () {
		return attStats;
	}

	public int getId () {
		return id;
	}
	
	public int getCommentCount () {
		return cmntCnt;
	}

	public int getHistoryCount () {
		return histCnt;
	}

	public int getAttachmentCont () {
		return attCnt;
	}

	public int getCcCount () {
		return ccCnt;
	}

	public int getBlocksCount () {
		return blocksCnt;
	}

	public int getAliasCount () {
		return aliasCnt;
	}
	
	public int getSeverityHistoryCount () {
		return severityHistoryCnt;
	}
}
