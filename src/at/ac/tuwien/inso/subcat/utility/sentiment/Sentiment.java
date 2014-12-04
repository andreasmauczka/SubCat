/* Sentiment.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Sentiment<T> extends SentimentAggregate<T> {
	private T data;
	
	public Sentiment (Collection<SentimentBlock<T>> lst) {
		assert (lst != null);
		assert (lst.size () > 0);

		List<SentenceSentiment> sentences = new ArrayList<SentenceSentiment> ();

		int[] classes = new int[5];

		double positiveSum = 0;
		double somewhatPositiveSum = 0;
		double neutralSum = 0;
		double somewhatNegativeSum = 0;
		double negativeSum = 0;

		double positiveWSum = 0;
		double somewhatPositiveWSum = 0;
		double neutralWSum = 0;
		double somewhatNegativeWSum = 0;
		double negativeWSum = 0;

		int words = 0;


		for (SentimentBlock<T> sent : lst) {
			int sentCount = sent.getSentenceCount ();
			int sentWord = sent.words;
			words += sent.words;

			classes[0] += sent.classes[0];
			classes[1] += sent.classes[1];
			classes[2] += sent.classes[2];
			classes[3] += sent.classes[3];
			classes[4] += sent.classes[4];

			positiveSum += sent.positiveMean * sentCount;
			somewhatPositiveSum += sent.somewhatPositiveMean * sentCount;
			neutralSum += sent.neutralMean * sentCount;
			somewhatNegativeSum += sent.somewhatNegativeMean * sentCount;
			negativeSum += sent.negativeMean * sentCount;

			positiveWSum += sent.positiveWMean * sentWord;
			somewhatPositiveWSum += sent.somewhatPositiveWMean * sentWord;
			neutralWSum += sent.neutralWMean * sentWord;
			somewhatNegativeWSum += sent.somewhatNegativeWMean * sentWord;
			negativeWSum += sent.negativeWMean * sentWord;
		}

		this.positiveMean = positiveSum / sentences.size ();
		this.somewhatPositiveMean = somewhatPositiveSum / sentences.size ();
		this.neutralMean = neutralSum / sentences.size ();
		this.somewhatNegativeMean = somewhatNegativeSum / sentences.size ();
		this.negativeMean = negativeSum / sentences.size ();

		this.positiveWMean = positiveWSum / words;
		this.somewhatPositiveWMean = somewhatPositiveWSum / words;
		this.neutralWMean = neutralWSum / words;
		this.somewhatNegativeWMean = somewhatNegativeWSum / words;
		this.negativeWMean = negativeWSum / words;

		this.classes = classes;
		this.words = words;
	}


	public T getData () {
		return data;
	}

	public void setData (T data) {
		this.data = data;
	}
}
