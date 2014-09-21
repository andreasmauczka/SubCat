package at.ac.tuwien.inso.subcat.config;

public class ExporterConfig extends ConfigNode {
	private String name;
	private Query query;

	public ExporterConfig (String name, Query query, SourcePos start, SourcePos end) {
		super (start, end);

		assert (name != null);
		assert (query != null);

		this.name = name;
		this.query = query;
	}
	
	public String getName () {
		return name;
	}
	
	public Query getQuery () {
		return query;
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
