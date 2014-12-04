package at.ac.tuwien.inso.subcat.utility.phonetic;

import org.apache.commons.codec.language.Soundex;


public class SoundexHashFunc extends HashFunc {
	private Soundex calc = new Soundex ();

	@Override
	public String[] hash (String str) {
		return new String[] { calc.soundex (str) };
	}
}
