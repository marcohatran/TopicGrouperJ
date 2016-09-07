package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDAPerplexityCalculatorInDoc<T> extends
		AbstractLDAPerplexityCalculator<T> {
	public LDAPerplexityCalculatorInDoc(boolean bowFactor) {
		super(bowFactor);
	}

	@Override
	protected void updatePtd(LDAGibbsSampler<T> sampler, Document<T> d, int dSize, int dIndex) {
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = (((double) sampler.getDocumentTopicAssignmentCount(
					dIndex, i) / sampler.getDocumentSize(dIndex)));
		}
	}
}