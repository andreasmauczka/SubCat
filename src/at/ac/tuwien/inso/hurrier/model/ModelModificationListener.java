package at.ac.tuwien.inso.hurrier.model;

public interface ModelModificationListener {

	public void projectAdded (Project project);

	public void userAdded (User user);

	public void identityAdded (Identity identity);

	public void interactionAdded (Interaction relation);

	public void severityAdded (Severity severity);

	public void priorityAdded (Priority priority);

	public void categoryAdded (Category category);

	public void componentAdded (Component component);

	public void bugAdded (Bug bug);

	public void bugHistoryAdded (BugHistory history);

	public void commentAdded (Comment cmnt);

	public void statusAdded (Status status);

	public void commitAdded (Commit commit);

	public void bugfixCommitAdded (BugfixCommit bugfix);

}
