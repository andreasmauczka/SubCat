package at.ac.tuwien.inso.subcat.config;

import java.util.LinkedList;
import java.util.List;

public abstract class OptionalConfigNode extends ConfigNode {
	private List<Requires> requirements;

	public OptionalConfigNode (SourcePos start, SourcePos end) {
		super (start, end);

		this.requirements = new LinkedList<Requires> ();
	}

	public List<Requires> getRequirements () {
		return requirements;
	}

	public void setRequirements (List<Requires> req) {
		assert (req != null);
		requirements = req;
	}

	public boolean show (List<String> flags) {
		for (Requires requires : requirements) {
			if (!requires.evaluate (flags)) {
				return false;
			}
		}

		return true;
	}
	
	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		for (Requires req : requirements) {
			req.accept (visitor);
		}

		super.acceptChildren (visitor);
	}
}
