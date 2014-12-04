package at.ac.tuwien.inso.subcat.utility.phonetic;


public class DirectHashFunc extends HashFunc {
	
	@Override
	public String[] hash (String str) {
		return new String[] {str};
	}
}
