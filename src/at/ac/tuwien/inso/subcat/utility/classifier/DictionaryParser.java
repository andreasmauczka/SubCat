package at.ac.tuwien.inso.subcat.utility.classifier;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import at.ac.tuwien.inso.subcat.utility.XmlReader;
import at.ac.tuwien.inso.subcat.utility.XmlReaderException;



/**
 * <dictionary>
 * <class name="" weight="">
 * 	<word name="" weight="" />
 * </class>
 * </dictionary>
 */
public class DictionaryParser {
	private XmlReader reader;

	public Dictionary parseFile (String path) throws XmlReaderException, FileNotFoundException {
		reader = new XmlReader (path);
		Dictionary dict = parse ();
		reader = null;
		return dict;
	}

	public Dictionary parseStream (InputStream input) throws XmlReaderException {
		reader = new XmlReader (input);
		Dictionary dict = parse ();
		reader = null;
		return dict;
	}
	
	private Dictionary parse () throws XmlReaderException {
		String title = reader.getAttribute ("title");
		reader.expectStart ("dictionary", true);

		LinkedList<Class> absoluteClasses = new LinkedList<Class> ();
		LinkedList<Class> relativeClasses = new LinkedList<Class> ();
		while (reader.isStart ("class", true)) {
			parseClass (absoluteClasses, relativeClasses);
		}

		Dictionary dict = new Dictionary (title, absoluteClasses, relativeClasses);

		reader.expectEnd ("dictionary", true);
		return dict;
	}

	private void parseClass (LinkedList<Class> absoluteClasses, LinkedList<Class> relativeClasses) throws XmlReaderException {
		boolean absolute = toBool (reader.getAttribute ("absolute", "false"));
		int weight = toInt (reader.getAttribute ("weight"));
		String name = reader.getAttribute ("name");
		reader.next ();


		HashMap<String, Integer> words = new HashMap<String, Integer> ();
		while (reader.isStart ("word", true)) {
			parseWord (words);
		}

		if (absolute) {
			absoluteClasses.add (new Class (name, words, weight, true));
		} else {
			relativeClasses.add (new Class (name, words, weight, false));			
		}
		
		reader.expectEnd ("class", true);
	}

	private void parseWord (HashMap<String, Integer> words) throws XmlReaderException {
		int weight = toInt (reader.getAttribute ("weight"));
		String name = reader.getAttribute ("name");
		reader.next ();

		words.put (name, weight);

		reader.expectEnd ("word", true);
	}

	private int toInt (String val) throws XmlReaderException {
		try {
			return Integer.valueOf (val);
		} catch(NumberFormatException e) {
			throw new XmlReaderException (e);
		}
	}

	private boolean toBool (String attribute) throws XmlReaderException {
		if (attribute.equals ("true")) {
			return true;
		} else if (attribute.equals ("false")) {
			return false;
		}

		throw new XmlReaderException ("Invalid value, expected 'true' or 'false'");
	}
}
