package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;

public class TGPerplexityCalculator<T> {
	private final boolean bowFactor;

	public TGPerplexityCalculator() {
		this(true);
	}

	public TGPerplexityCalculator(boolean bowFactor) {
		this.bowFactor = bowFactor;
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider, TGSolution<T> s) {
		double sumA = 0;
		long sumB = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			for (int i = 0; i < getInDocSplits(doc); i++) {
				Document<T> rd = createReferenceDoc(i, doc);
				Document<T> d = createTestDoc(i, doc, rd);
				sumA += computeLogProbability(rd, d, s);
				sumB += d.getSize();
			}
		}
		return Math.exp(sumA / sumB);
	}
	
	protected int getInDocSplits(Document<T> d) {
		return 1;
	}

	protected Document<T> createReferenceDoc(int i, Document<T> d) {
		return d;
	}

	protected Document<T> createTestDoc(int i, Document<T> d, Document<T> rd) {
		return d;
	}

	public double computeLogProbability(Document<T> refD, Document<T> d,
			TGSolution<T> s) {
		double res = bowFactor ? logFacN(d.getSize()) : 0;

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int sIndex = s.getIndex(word);
			if (sIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0 /* && words != null */) {
					if (bowFactor) {
						res -= logFacN(wordFr);
					}
					int topicIndex = s.getTopicForWord(sIndex);
					TIntCollection words = s.getTopics()[topicIndex];
					res += wordFr
							* computeWordLogProbability(sIndex, refD, s, words,
									topicIndex);
				}
			}
		}
		return res;
	}

	protected double computeWordLogProbability(int sIndex, Document<T> d,
			TGSolution<T> s, TIntCollection words, int topicIndex) {
		int topicFrInDoc = 0;
		TIntIterator it = words.iterator();
		while (it.hasNext()) {
			int swIndex = it.next();
			int dIndex = d.getProvider().getIndex(s.getWord(swIndex));
			if (dIndex >= 0) {
				topicFrInDoc += d.getWordFrequency(dIndex);
			}
		}
		return // Estimate log p(t|d):
		Math.log(smoothedPtd(topicFrInDoc, d.getSize(), sIndex, topicIndex, s))
				+
				// Estimate log p(w|t):
				Math.log(((double) s.getGlobalWordFrequency(sIndex))
						/ s.getTopicFrequency(topicIndex));
	}

	protected double smoothedPtd(int topicFrInDoc, int docSize, int wordIndex,
			int topicIndex, TGSolution<T> s) {
		double lambda = getSmoothingLambda();
		return ((1 - lambda) * topicFrInDoc / docSize)
				// Smoothing via global frequency of topic;
				+ (lambda * s.getTopicFrequency(topicIndex))
				/ s.getSize();
	}
	
	protected double getSmoothingLambda() {
		return 0;
	}

	public static double logFacN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}
}
