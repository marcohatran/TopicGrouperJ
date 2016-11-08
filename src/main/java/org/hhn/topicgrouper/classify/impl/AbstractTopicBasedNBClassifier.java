package org.hhn.topicgrouper.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedNBClassifier<T, L> implements SupervisedDocumentClassifier<T, L> {
	private final List<L> labels;
	private final List<TDoubleList> logptc;
	private final TDoubleList logpc;
	
	private final double smoothingLambda;

	public AbstractTopicBasedNBClassifier(double lambda) {
		logptc = new ArrayList<TDoubleList>();
		logpc = new TDoubleArrayList();
		labels = new ArrayList<L>();
		this.smoothingLambda = lambda;
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		logptc.clear();
		logpc.clear();
		labels.clear();

		int ntopics = getNTopics();
		int nDocs = provider.getDocuments().size();
		double lambdaSum = smoothingLambda * ntopics;
		double[] sum = new double[ntopics];
		
		int l = 0;		
		for (L label : provider.getAllLabels()) {
			labels.add(label);
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			TDoubleList pt = logptc.size() <= l ? null : logptc.get(l);
			if (pt == null) {
				pt = new TDoubleArrayList();
				logptc.add(pt);
			}
			else {
				pt.clear();
			}
			logpc.add(Math.log(((double) labeledDocs.size()) / nDocs));
			int total = 0;
			Arrays.fill(sum, 0);
			for (LabeledDocument<T, L> d : labeledDocs) {
				double[] ftd = getFtd(d);
				for (int t = 0; t < ntopics; t++) {
					sum[t] += ftd[t];
				}
				total += d.getSize();
			}

			for (int t = 0; t < ntopics; t++) {
				pt.add(Math.log((sum[t] + smoothingLambda) / (total + lambdaSum)));
			}
			l++;
		}
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = getNTopics();
		double[] ftd = getFtd(d);
		int l = 0;
		for (L label : labels) {
			double sum = logpc.get(l);
			for (int t = 0; t < ntopics; t++) {
				sum += logptc.get(l).get(t) * ftd[t];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
			l++;
		}
		return bestLabel;
	}

//	protected double log(double x) {
//		if (x == 0) {
//			throw new IllegalStateException("log(0) undefined");
//		}
//		return Math.log(x);
//	}

	protected abstract double[] getFtd(Document<T> d);

	protected abstract int getNTopics();
}
