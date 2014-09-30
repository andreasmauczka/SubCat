package at.ac.tuwien.inso.subcat.config;

import java.util.List;
import java.util.Set;

public class Requires extends ConfigNode {
	private Set<String> requirements;
	
	public Requires (SourcePos start, SourcePos end) {
		super (start, end);
	}

	public void setRequirements (Set<String> flags) {
		assert (flags != null);

		this.requirements = flags;
	}

	public boolean evaluate (String flag) {
		return requirements.contains (flag);
	}

	public boolean evaluate (List<String> flags) {
		assert (flags != null);

		for (String flag : flags) {
			if (evaluate (flag)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);
		
		visitor.visitRequires (this);
	}
}
