package at.ac.tuwien.inso.subcat.config;


public class ExporterConfig extends OptionalConfigNode {
	private String name;
	private Query query;
	private boolean wordStats;

	public ExporterConfig (String name, Query query, SourcePos start, SourcePos end, boolean wordStats) {
		super (start, end);

		assert (name != null);
		assert (query != null);

		this.name = name;
		this.query = query;
		this.wordStats = wordStats;
	}
	
	public String getName () {
		return name;
	}
	
	public Query getQuery () {
		return query;
	}

	public boolean getWordStats () {
		return wordStats;
	}
	
	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitExporterConfig (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);
		
		if (query != null) {
			query.accept (visitor);
		}
	}
}
