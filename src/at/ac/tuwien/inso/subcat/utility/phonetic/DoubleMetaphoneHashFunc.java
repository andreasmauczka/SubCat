package at.ac.tuwien.inso.subcat.utility.phonetic;

import org.apache.commons.codec.language.DoubleMetaphone;


public class DoubleMetaphoneHashFunc extends HashFunc {
	private DoubleMetaphone calc = new DoubleMetaphone ();

	@Override
	public String[] hash (String str) {
		return new String[] { calc.doubleMetaphone (str) };
	}
}
