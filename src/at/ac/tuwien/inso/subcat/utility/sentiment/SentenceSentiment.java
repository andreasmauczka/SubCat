/* SentenceSentiment.java
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


package at.ac.tuwien.inso.subcat.utility.sentiment;


public class SentenceSentiment {
	private final double negative;
	private final double somewhatNegative;
	private final double neutral;
	private final double somewhatPositive;
	private final double positive;
	private final int wordCount;
	private final int sentiment;


	public SentenceSentiment (double negative, double somewhatNegative, double neutral,
			double somewhatPositive, double positive, int sentiment, int wordCount) {

		assert (negative >= 0 && negative <= 1);
		assert (somewhatNegative >= 0 && somewhatNegative <= 1);
		assert (neutral >= 0 && neutral <= 1);
		assert (somewhatPositive >= 0 && somewhatPositive <= 1);
		assert (positive >= 0 && positive <= 1);
		assert (sentiment >= 0 && sentiment <= 4);
		assert (wordCount > 0);
		
		this.negative = negative;
		this.somewhatNegative = somewhatNegative;
		this.neutral = neutral;
		this.somewhatPositive = somewhatPositive;
		this.positive = positive;
		this.sentiment = sentiment;
		this.wordCount = wordCount;
	}

	public double getNegative () {
		return negative;
	}

	public double getSomewhatNegative () {
		return somewhatNegative;
	}

	public double getNeutral () {
		return neutral;
	}

	public double getSomewhatPositive () {
		return somewhatPositive;
	}

	public double getPositive () {
		return positive;
	}

	public int getWordCount () {
		return wordCount;
	}

	public int getSentimentClass () {
		return sentiment;
	}
}
