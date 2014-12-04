package at.ac.tuwien.inso.subcat.utility.phonetic;

import org.apache.commons.codec.language.Metaphone;



public class MetaphoneHashFunc extends HashFunc {
	private Metaphone calc = new Metaphone ();

	@Override
	public String[] hash (String str) {
		return new String[] { calc.metaphone (str) };
	}
}
