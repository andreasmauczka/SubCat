package at.ac.tuwien.inso.subcat.utility.phonetic;

import org.apache.commons.codec.language.RefinedSoundex;


public class RefinedSoundexHashFunc extends HashFunc {
	private RefinedSoundex calc = new RefinedSoundex ();

	@Override
	public String[] hash (String str) {
		return new String[] { calc.soundex (str) };
	}
}
