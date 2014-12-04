/* SentimentAnalyser.java
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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.ejml.simple.SimpleMatrix;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;


public class SentimentAnalyser<T> {
	private StanfordCoreNLP pipeline = null;

	public SentimentAnalyser () {
	}

	public SentimentBlock<T> get (String str) {
		return get (str, null);
	}

	public SentimentBlock<T> get (String str, T data) {
		if (pipeline == null) {
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
			pipeline = new StanfordCoreNLP(props);
		}

		LinkedList<SentenceSentiment> sentiments = new LinkedList<SentenceSentiment> ();
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
		
		Annotation annotation = pipeline.process (str);
		List<CoreMap> sentences = annotation.get (CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			Tree tree = sentence.get (SentimentCoreAnnotations.AnnotatedTree.class);
			// TODO: calculate it instead
			int predictedClass = RNNCoreAnnotations.getPredictedClass (tree);
			SimpleMatrix matrix = RNNCoreAnnotations.getPredictions (tree);

			classes[predictedClass]++;
			
			int sentenceWordCount = tree.getLeaves ().size ();
			
			SentenceSentiment sentiment = new SentenceSentiment (
				matrix.get (0, 0),
				matrix.get (1, 0),
				matrix.get (2, 0),
				matrix.get (3, 0),
				matrix.get (4, 0),
				predictedClass,
				sentenceWordCount);

			positiveSum += sentiment.getPositive ();
			somewhatPositiveSum += sentiment.getSomewhatPositive ();
			neutralSum += sentiment.getNeutral ();
			somewhatNegativeSum += sentiment.getSomewhatNegative ();
			negativeSum += sentiment.getNegative ();

			positiveWSum += sentiment.getPositive ()*sentenceWordCount;
			somewhatPositiveWSum += sentiment.getSomewhatPositive ()*sentenceWordCount;
			neutralWSum += sentiment.getNeutral ()*sentenceWordCount;
			somewhatNegativeWSum += sentiment.getSomewhatNegative ()*sentenceWordCount;
			negativeWSum += sentiment.getNegative ()*sentenceWordCount;
			
			words += sentenceWordCount;

			sentiments.add (sentiment);
		}

		double positiveMean = positiveSum / sentiments.size ();
		double somewhatPositiveMean = somewhatPositiveSum / sentiments.size ();
		double neutralMean = neutralSum / sentiments.size ();
		double somewhatNegativeMean = somewhatNegativeSum / sentiments.size ();
		double negativeMean = negativeSum / sentiments.size ();

		double positiveWMean = positiveWSum / words;
		double somewhatPositiveWMean = somewhatPositiveWSum / words;
		double neutralWMean = neutralWSum / words;
		double somewhatNegativeWMean = somewhatNegativeWSum / words;
		double negativeWMean = negativeWSum / words;

		//System.out.println ("n:" + positiveMean  + "," +  somewhatPositiveMean  + "," + neutralMean + "," + somewhatNegativeMean + "," + negativeMean);
		//System.out.println ("w:" + positiveWMean  + "," +  somewhatPositiveWMean  + "," + neutralWMean + "," + somewhatNegativeWMean + "," + negativeWMean);

		SentimentBlock<T> block = new SentimentBlock<T> (
			sentiments,
			classes,
			positiveMean,
			somewhatPositiveMean,
			neutralMean,
			somewhatNegativeMean,
			negativeMean,
			positiveWMean,
			somewhatPositiveWMean,
			neutralWMean,
			somewhatNegativeWMean,
			negativeWMean,
			words);

		block.setData (data);
		return block;
	}
}
