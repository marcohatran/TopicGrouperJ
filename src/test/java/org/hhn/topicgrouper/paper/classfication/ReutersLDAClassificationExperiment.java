package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.lda.LDANBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;

public class ReutersLDAClassificationExperiment {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;
	protected final PrintStream printStream;
	protected final OutputStreamMultiplexer os;

	public ReutersLDAClassificationExperiment() throws IOException {
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		createTrainingAndTestProvider(res);
		testProvider = res[0];
		trainingProvider = res[1];

		os = new OutputStreamMultiplexer();
		os.addOutputStream(System.out);
		os.addOutputStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + ".csv")));
		printStream = new PrintStream(os);
		printStream.println("topics; microAvg; macroAvg");
	}

	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		// Use ModApte split:
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);
	}

	public void run(boolean optimizeAlphaBeta) {
		for (int topics = 1; topics <= 9; topics++) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		for (int topics = 10; topics <= 99; topics += 10) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		for (int topics = 100; topics <= 999; topics += 100) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		for (int topics = 1000; topics <= 5000; topics += 1000) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		printStream.close();
	}

	protected void runExperiment(final int topics, boolean optimizeAlphaBeta) {
		final LDAGibbsSampler<String> ldaGibbsSampler = createGibbsSampler(
				topics, trainingProvider, optimizeAlphaBeta);
		ldaGibbsSampler.solve(1000, 1000, new BasicLDAResultReporter<String>(
				System.out, 10) {
			@Override
			public void done(LDAGibbsSampler<String> sampler) {
				super.done(sampler);
				SupervisedDocumentClassifier<String, String> classifier = createClassifier(ldaGibbsSampler);
				classifier.train(trainingProvider);
				double[] res = classifier.test(testProvider, null);
				printStream.println(topics + "; " + res[0] + "; " + res[1]);
			}

			@Override
			public void updatedSolution(LDAGibbsSampler<String> sampler,
					int iteration) {
			}
		});
	}

	protected LDAGibbsSampler<String> createGibbsSampler(int topics,
			DocumentProvider<String> documentProvider, boolean optimizeAlphaBeta) {
		// Initial alpha and beta like in:
		// http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
		// and
		// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
		LDAGibbsSampler<String> ldaGibbsSampler = new LDAGibbsSampler<String>(
				new Random(42), documentProvider, createAlpha(topics), 0.1);
		ldaGibbsSampler.setUpdateAlphaBeta(optimizeAlphaBeta);
		return ldaGibbsSampler;
	}

	protected double[] createAlpha(int topics) {
		return LDAGibbsSampler.symmetricAlpha(50d, topics);
	}

	protected DocumentProvider<String> createDocumentProvider() {
		return trainingProvider;
	}

	protected SupervisedDocumentClassifier<String, String> createClassifier(
			LDAGibbsSampler<String> ldaGibbsSampler) {
		return new LDANBClassifier<String, String>(0, ldaGibbsSampler, 200, 50);
	}

	public static void main(String[] args) throws IOException {
		new ReutersLDAClassificationExperiment().run(false);
	}
}
