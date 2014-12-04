/* SentimentAggregate.java
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


public abstract class SentimentAggregate<T> {
	private T data;
	
	protected int[] classes;
	
	protected double positiveMean;
	protected double somewhatPositiveMean;
	protected double neutralMean;
	protected double somewhatNegativeMean;
	protected double negativeMean;

	protected double positiveWMean;
	protected double somewhatPositiveWMean;
	protected double neutralWMean;
	protected double somewhatNegativeWMean;
	protected double negativeWMean;

	protected int sentences;
	protected int words;

	
	public SentimentAggregate () {
	}

	public SentimentAggregate (
			int[] classes, double positiveMean,
			double somewhatPositiveMean, double neutralMean,
			double somewhatNegativeMean, double negativeMean,
			double positiveWMean, double somewhatPositiveWMean,
			double neutralWMean, double somewhatNegativeWMean,
			double negativeWMean, int words, int sentences) {

		assert (classes != null);
		assert (classes.length == 5);

		assert (positiveMean >= 0 && positiveMean <= 1);
		assert (somewhatPositiveMean >= 0 && somewhatPositiveMean <= 1);
		assert (neutralMean >= 0 && neutralMean <= 1);
		assert (somewhatNegativeMean >= 0 && somewhatNegativeMean <= 1);
		assert (negativeMean >= 0 && negativeMean <= 1);

		assert (positiveWMean >= 0 && positiveWMean <= 1);
		assert (somewhatPositiveWMean >= 0 && somewhatPositiveWMean <= 1);
		assert (neutralWMean >= 0 && neutralWMean <= 1);
		assert (somewhatNegativeWMean >= 0 && somewhatNegativeWMean <= 1);
		assert (negativeWMean >= 0 && negativeWMean <= 1);

		assert (sentences >= 0);
		assert (words >= 0);

		this.classes = classes;
		this.positiveMean = positiveMean;
		this.somewhatPositiveMean = somewhatPositiveMean;
		this.neutralMean = neutralMean;
		this.somewhatNegativeMean = somewhatNegativeMean;
		this.negativeMean = negativeMean;
		this.positiveWMean = positiveWMean;
		this.somewhatPositiveWMean = somewhatPositiveWMean;
		this.neutralWMean = neutralWMean;
		this.somewhatNegativeWMean = somewhatNegativeWMean;
		this.negativeWMean = negativeWMean;
		this.sentences = sentences;
		this.words = words;
	}

	
	public double getPositiveMean () {
		return positiveMean;
	}

	public double getSomewhatPositiveMean () {
		return somewhatPositiveMean;
	}


	public double getNeutralMean () {
		return neutralMean;
	}

	public double getSomewhatNegativeMean () {
		return somewhatNegativeMean;
	}

	public double getNegativeMean () {
		return negativeMean;
	}

	public double getPositiveWMean () {
		return positiveWMean;
	}

	public double getSomewhatPositiveWMean () {
		return somewhatPositiveWMean;
	}

	public double getNeutralWMean () {
		return neutralWMean;
	}

	public double getSomewhatNegativeWMean () {
		return somewhatNegativeWMean;
	}

	public double getNegativeWMean () {
		return negativeWMean;
	}

	public int getWordCount () {
		return words;
	}

	public int getSentenceCount () {
		return sentences;
	}

	public int getPositiveCount () {
		return classes[4];
	}

	public int getSomewhatPositiveCount () {
		return classes[3];
	}

	public int getNeutralCount () {
		return classes[2];
	}

	public int getSomewhatNegativeCount () {
		return classes[1];
	}

	public int getNegativeCount () {
		return classes[0];
	}

	public T getData () {
		return data;
	}

	public void setData (T data) {
		this.data = data;
	}
}
