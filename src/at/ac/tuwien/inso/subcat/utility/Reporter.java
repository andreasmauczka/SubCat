/* Reporter.java
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

package at.ac.tuwien.inso.subcat.utility;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;



public class Reporter {
	private int errors = 0;
	private int warnings = 0;
	private long startTime = -1;

	
	public void error (String context, String msg) {
		System.out.println ("error: " + context + ": " + msg);
		errors++;
	}
	
	public void warning (String context, String msg) {
		System.out.println ("warning: " + context + ": " + msg);
		warnings++;
	}

	public void note(String context, String msg) {
		System.out.println ("note: " + context + ": " + msg);
	}
	
	public int getErrors () {
		return errors;
	}
	
	public int getWarnings () {
		return warnings;
	}

	public void printSummary () {
		if (errors > 0) {
			System.out.println ("Mining failed - " + warnings + " warning(s), " + errors + " error(s).");
		} else {
			System.out.println ("Mining succeeded - " + warnings + " warning(s)");			
		}
	}

	public void printSummary (boolean showTime) {
		printSummary ();

		if (showTime && startTime >= 0) {
			long diff = System.currentTimeMillis () - startTime;

			double seconds = (diff /= 1000) % 60;
			double minutes = (diff /= 60) % 60;
			double hours = (diff /= 60) % 24;
			double days = diff /= 24;

			StringBuilder builder = new StringBuilder ("  Total time: ");

			NumberFormat format = DecimalFormat.getInstance ();
			format.setRoundingMode (RoundingMode.DOWN);
	        format.setMinimumFractionDigits (0);
	        format.setMaximumFractionDigits (0);

	        if (days > 0) {
				builder.append (format.format (days) + " day(s), ");
			}
			if (hours > 0) {
				builder.append (format.format (hours) + " hour(s), ");
			}
			if (minutes > 0) {
				builder.append (format.format (minutes) + " minute(s), ");
			}
			builder.append (format.format (seconds) + " second(s)");

			System.out.println (builder);
		}
	}

	public void startTimer() {
		startTime = System.currentTimeMillis ();
	}
}
