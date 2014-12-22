/* Classifier.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.utility.classifier;

import java.util.Iterator;
import java.util.List;


public class Classifier {
	private Dictionary dict;

	public Classifier (Dictionary dict) {
		assert (dict != null);

		this.dict = dict;
	}

	public Class classify (List<String> document) {
		return classify (document, null);
	}

	public Class classify (List<String> document, List<ClassScore> scores) {
		assert (document != null);
		
		if (document.size () == 0) {
			return null;
		}

		// Absolute categories:
		Class abs = null;
		for (Class cl : dict.getAbsoluteClasses ()) {
			if (classifyAbsolute (cl, document)) {
				if (abs == null || cl.getWeight () > abs.getWeight ()) {
					abs = cl;
				}
			}
		}

		if (abs != null) {
			return abs;
		}


		// Relative categories:
		ClassScore tmp = null;

		for (Class cl : dict.getRelativeClasses ()) {
			int score = classifyRelative (cl, document);
			
			if (scores != null && score > 0) {
				scores.add (new ClassScore (cl, score));
			}

			if (tmp == null) {
				if (score > 0) {
					tmp = new ClassScore (cl, score);
				}
			} else if (score > tmp.score || (score == tmp.score && cl.getWeight () >= tmp.cl.getWeight ())) {
				tmp = new ClassScore (cl, score);
			}
		}
		
		return (tmp == null)? null : tmp.cl;
	}

	private int classifyRelative (Class cl, List<String> document) {
		assert (cl != null);
		assert (!cl.isAbsolute ());
		assert (document != null);

		int score = 0;

		Iterator<String> iter = document.iterator ();
		while (iter.hasNext ()) {
			String word = iter.next ();

			Integer weight = cl.getWordWeight (word);
			if (weight != null) {
				score += weight;
			}
		}

		return score;
	}

	private boolean classifyAbsolute (Class cl, List<String> document) {
		assert (cl != null);
		assert (cl.isAbsolute ());
		assert (document != null);

		for (String word : document) {
			if (cl.contains (word)) {
				return true;
			}
		}

		return false;
	}

	/** /
	public static void main (String[] args) {
		List<Class> absoluteClasses = new ArrayList<Class> ();
		List<Class> relativeClasses = new ArrayList<Class> ();

		// Class "First":
		HashMap<String, Integer> words = new HashMap<String, Integer> ();
		relativeClasses.add (new Class ("First", words, 1, false));
		words.put ("FIRST_SECOND1_1", 1);
		words.put ("FIRST_SECOND2_1", 1);
		words.put ("FIRST3_1", 1);

		// Class "Second":
		words = new HashMap<String, Integer> ();
		relativeClasses.add (new Class ("Second", words, 2, false));
		words.put ("FIRST_SECOND1_1", 1);
		words.put ("FIRST_SECOND2_1", 1);
		

		// Class "FirstAbs": (absolute)
		words = new HashMap<String, Integer> ();
		absoluteClasses.add (new Class ("FirstAbs", words, 2, true));
		words.put ("FIX_FIRST_SEC_1", 1);
		words.put ("FIX_FST_1", 1);


		// Class "SecAbs": (absolute)
		words = new HashMap<String, Integer> ();
		absoluteClasses.add (new Class ("SecAbs", words, 3, true));
		words.put ("FIX_FIRST_SEC_1", 1);


		
		Dictionary dict = new Dictionary ("Name", absoluteClasses, relativeClasses);
		Classifier classifier = new Classifier (dict);


		// Expected Result:
		//   Second! Second.weight > First.weight, Second.score = 2, First.score = 2
		List<String> document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");

		test (1, classifier, document, "Second");


		// Expected Result:
		//   First! First.weight > Second.weight, Second.score = 2, First.score = 3
		document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");
		document.add ("FIRST3_1");

		test (2, classifier, document, "First");


		// Expected Result:
		//   SecAbs!
		document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");
		document.add ("FIX_SEC_1");

		test (3, classifier, document, "SecAbs");


		// Expected Result:
		//   SecAbs!
		document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");
		document.add ("FIX_SEC_1");
		document.add ("FIX_FIRST_SEC_1");

		test (4, classifier, document, "SecAbs");


		// Expected Result:
		//   SecAbs!
		document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");
		document.add ("FIX_FST_1");
		document.add ("FIX_FIRST_SEC_1");

		test (5, classifier, document, "SecAbs");


		// Expected Result:
		//   FirstAbs!
		document = new ArrayList<String> ();
		document.add ("filler1");
		document.add ("filler2");
		document.add ("filler3");
		document.add ("FIRST_SECOND1_1");
		document.add ("FIRST_SECOND2_1");
		document.add ("FIX_FST_1");

		test (6, classifier, document, "FirstAbs");


		// Expected Result:
		//   FirstAbs!
		document = new ArrayList<String> ();
		document.add ("NA");
		document.add ("BA");

		test (7, classifier, document, "(null)");
	}

	private static void test (int id, Classifier classifier, List<String> document, String expected) {
		System.out.println ("------------------------");
		System.out.println ("Experiment " + id);
		
		List<ClassScore> full = new ArrayList<ClassScore> ();
		Class cl = classifier.classify (document, full);

		for (ClassScore score : full) {
			System.out.println (" * " + ((score.cl == null)? "(null)" : score.cl.getName ()) + "\t" + score.score);
		}

		System.out.println ("Got:      = " + ((cl == null)? "(null)" : cl.getName ()));
		System.out.println ("Expected: = " + expected);
	}
	/ **/
}
