package at.ac.tuwien.inso.subcat.utility.phonetic;

import java.util.HashMap;
import java.util.Set;


public abstract class HashFunc {
	private static HashMap<String, HashFunc> registeredHashFuncs;

	private static void initHashFuncs () {
		if (registeredHashFuncs != null) {
			return ;
		}

		registeredHashFuncs = new HashMap<String, HashFunc> ();
		registeredHashFuncs.put ("direct", new DirectHashFunc ());
		registeredHashFuncs.put ("double-metaphone", new DoubleMetaphoneHashFunc ());
		registeredHashFuncs.put ("metaphone", new MetaphoneHashFunc ());
		registeredHashFuncs.put ("caverphone", new CaverphoneHashFunc ());
		registeredHashFuncs.put ("soundex", new SoundexHashFunc ());
		registeredHashFuncs.put ("refined-soundex", new RefinedSoundexHashFunc ());
	}

	public static HashFunc getHashFunc (String name) {
		initHashFuncs ();
		return registeredHashFuncs.get (name);
	}
	
	public static Set<String> getHashFuncNames () {
		initHashFuncs ();
		return registeredHashFuncs.keySet ();
	}
	
	public abstract String[] hash (String str);
}
