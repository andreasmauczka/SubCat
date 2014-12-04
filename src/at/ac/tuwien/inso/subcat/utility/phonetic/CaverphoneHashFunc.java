package at.ac.tuwien.inso.subcat.utility.phonetic;

import org.apache.commons.codec.language.Caverphone;


public class CaverphoneHashFunc extends HashFunc {
	private Caverphone calc = new Caverphone ();

	@Override
	public String[] hash (String str) {
		return new String[] { calc.caverphone (str) };
	}
}
