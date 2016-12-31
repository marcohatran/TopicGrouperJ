package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class TwentyNGVocabDFClassificationExperimentOpt extends TwentyNGVocabDFClassificationExperiment {
	public TwentyNGVocabDFClassificationExperimentOpt() throws IOException {
		super();
	}

	@Override
	protected double initialLambda() {
		return 0.3;
	}
	
	public static void main(String[] args) throws IOException {
		new TwentyNGVocabDFClassificationExperimentOpt().run(true);
	}
}