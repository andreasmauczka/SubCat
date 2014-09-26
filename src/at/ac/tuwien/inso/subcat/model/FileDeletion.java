package at.ac.tuwien.inso.subcat.model;

public class FileDeletion {
	private ManagedFile file;
	private Commit commit;

	public FileDeletion (ManagedFile file, Commit commit) {
		assert (file != null);
		assert (commit != null);
		
		this.file = file;
		this.commit = commit;
	}
	
	public ManagedFile getFile () {
		return file;
	}

	public Commit getCommit () {
		return commit;
	}
}
