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
	private int priorityCnt;
	private int statusCnt;
	private int resolutionCnt;
	private int confirmedCnt;
	private int versionHistoCnt;
	private int operatingSystemCnt;
	private int dependsCnt;
	private int keywordCnt;
	private int milestoneCnt;
	private int assignedToCnt;
	private int qaContactCnt;


	public BugStats (int id, int cmntCnt, int histCnt, int attCnt, int ccCnt, int blocksCnt, int aliasCnt, int severityHistoryCnt, 
			int priorityCnt, int statusCnt, int resolutionCnt, int confirmedCnt, int versionHistoCnt, int operatingSystemCnt,
			int dependsCnt, int keywordCnt, int milestoneCnt, int assignedToCnt, int qaContactCnt,
			Map<Integer, BugAttachmentStats> attStats) {
		this.id = id;
		this.cmntCnt = cmntCnt;
		this.histCnt = histCnt;
		this.attCnt = attCnt;
		this.attStats = attStats;
		this.ccCnt = ccCnt;
		this.blocksCnt = blocksCnt;
		this.aliasCnt = aliasCnt;
		this.severityHistoryCnt = severityHistoryCnt;
		this.priorityCnt = priorityCnt;
		this.statusCnt = statusCnt;
		this.resolutionCnt = resolutionCnt;
		this.confirmedCnt = confirmedCnt;
		this.versionHistoCnt = versionHistoCnt;
		this.operatingSystemCnt = operatingSystemCnt;
		this.dependsCnt = dependsCnt;
		this.keywordCnt = keywordCnt;
		this.milestoneCnt = milestoneCnt;
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

	public int getPriorityHistoryCount () {
		return priorityCnt;
	}

	public int getStatusHistoryCount () {
		return statusCnt;
	}

	public int getResolutionHistoryCount () {
		return resolutionCnt;
	}

	public int getConfirmedHistoryCount () {
		return confirmedCnt;
	}

	public int getVersionHistoryCount () {
		return versionHistoCnt;
	}

	public int getOperatingSystemHistoryCount () {
		return operatingSystemCnt;
	}

	public int getDependsCount () {
		return dependsCnt;
	}

	public int getKeywordCount () {
		return keywordCnt;
	}

	public int getMilestoneCount () {
		return milestoneCnt;
	}

	public int getAssignedToCount () {
		return assignedToCnt;
	}

	public int getQaContactCount () {
		return qaContactCnt;
	}
}
