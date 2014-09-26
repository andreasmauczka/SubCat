package at.ac.tuwien.inso.subcat.model;

public class ManagedFileCopy {
	private Commit commit;
	private ManagedFile file;
	private ManagedFile original;
	
	public ManagedFileCopy (ManagedFile file, Commit commit, ManagedFile original) {
		assert (file != null);
		assert (commit != null);
		assert (original != null);

		this.file = file;
		this.commit = commit;
		this.original = original;
	}
	
	public Commit getCommit () {
		return commit;
	}

	public ManagedFile getFile () {
		return file;
	}

	public ManagedFile getOriginal () {
		return original;
	}
}
