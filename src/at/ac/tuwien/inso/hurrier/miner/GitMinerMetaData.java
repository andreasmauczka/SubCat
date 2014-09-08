package at.ac.tuwien.inso.hurrier.miner;

import java.io.File;

import at.ac.tuwien.inso.hurrier.miner.Miner.MetaData;
import at.ac.tuwien.inso.hurrier.miner.Miner.MinerType;
import at.ac.tuwien.inso.hurrier.model.Model;
import at.ac.tuwien.inso.hurrier.model.Project;

public class GitMinerMetaData implements MetaData {
	private final static String name = "GIT";
	
	@Override
	public MinerType getType () {
		return MinerType.SOURCE;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public boolean is (Settings settings) {
		assert (settings !=null);

		if (settings.srcLocalPath == null) {
			return false;
		}

		File dir = new File (settings.srcLocalPath);
		if (!dir.exists() || !dir.isDirectory()) {
			return false;
		}

		dir = dir.getAbsoluteFile ();

		do {
			File girDir = new File (dir, ".git");
			if (girDir.exists() && girDir.isDirectory()) {
				return true;
			}
			
			dir = dir.getParentFile ();
		} while (dir != null);

		return false;
	}

	@Override
	public Miner create (Settings settings, Project project, Model model) {
		return new GitMiner (settings, project, model);
	}

}
