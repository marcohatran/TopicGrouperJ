package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.FrequencyProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.util.MathExt;

public class TWCPerplexityErrorRateVaryAlpha extends
		AbstractPerplexityErrorRateExperiment<String> {
	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;
	private DocumentProvider<String>[] documentProviders;
	

	public TWCPerplexityErrorRateVaryAlpha(Random random, int gibbsIterations) {
		super(random, gibbsIterations);
		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
	}
	
	protected double alphaFromStep(int step) {
		return (step + 1) * 0.5;
	}

	protected HoldOutSplitter<String> createHoldoutSplitter(DocumentProvider<String> documentProvider, int step, int repeat) {
		// Get the same kind of split across steps (but different for each repeat)
		return new HoldOutSplitter<String>(new Random(repeat), documentProvider, 0.5, 1);
	}
	
	@Override
	protected DocumentProvider<String> createDocumentProvider(int step, int repeat) {
		return documentProviders[repeat];
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return new LDAGibbsSampler<String>(documentProvider, new double[] {
				alphaFromStep(step), 0.5, 0.5, 0.5 }, 0.5, random);
	}
	
	@Override
	public void run(int steps, int avgC)
			throws IOException {
		// Get the same documents across steps (but different for each repeat)
		documentProviders = new DocumentProvider[avgC];
		for (int i = 0; i < avgC; i++) {
			documentProviders[i] = new TWCLDAPaperDocumentGenerator(random, new double[] { 5, 0.5,
					0.5, 0.5 }, 12000, 100, 100, 30, 30, 0, null, 0.5, 0.8);
		}
		
		super.run(steps, avgC);
	}

	@Override
	protected void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations,
			final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] perplexity3, double[] acc) {
		final LDAGibbsSampler<String> gibbsSampler = createGibbsSampler(step,
				documentProvider);

		gibbsSampler.solve(gibbsIterations / 4, gibbsIterations, new BasicLDAResultReporter<String>(
				System.out, 10));

		perplexity1[repeat] = calc1.computePerplexity(testDocumentProvider,
				gibbsSampler);

		perplexity2[repeat] = calc2.computePerplexity(testDocumentProvider,
				gibbsSampler);

		perplexity3[repeat] = calc3.computePerplexity(testDocumentProvider,
				gibbsSampler);
		
		FrequencyProvider ldaFrequencyProvider = new FrequencyProvider() {
			@Override
			public int getFrequency(int topic, int wordIndex) {
				return gibbsSampler.getTopicWordAssignmentCount(topic,
						wordIndex);
			}

			@Override
			public boolean isCorrectTopic(int topic, int index) {
				Integer w = Integer.valueOf(documentProvider.getWord(index));
				return topic == w / 100;
			}
		};

		acc[repeat] = accuracyCalculator.computeAccuracy(documentProvider,
				gibbsSampler.getNTopics(), ldaFrequencyProvider);
	}
	
	@Override
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/" + createLDACSVBaseFileName() + ".csv")));
		printLDACSVFileHeader(pw);
		return pw;
	}
	
	protected void printLDACSVFileHeader(PrintStream pw) {
		pw.print("alpha1;");
		pw.print("perplexityAvg;");
		pw.print("perplexityAvg_stddev;");
		pw.print("perplexityLR;");
		pw.print("perplexityLR_stddev;");
		pw.print("perplexityETheta;");
		pw.print("perplexityETheta_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
	}
	
	protected String createLDACSVBaseFileName() {
		return "TWCPerplexityErrorRateVaryAlphaLDA";
	}
	
	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/" + createTGCSVBaseFileName() + ".csv")));
		printTGCSVFileHeader(pw);
		return pw;
	}
	
	protected void printTGCSVFileHeader(PrintStream pw) {
		pw.print("x;");
		pw.print("perplexity;");
		pw.print("perplexity_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
	}
	
	protected String createTGCSVBaseFileName() {
		return "TWCPerplexityErrorRateVaryAlphaTG";
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCLikelihoodTG.csv")));
		pw.print("ntopics;");
		pw.print("improvement;");
		pw.println("improvementratio;");
		return pw;
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == 0) {
			final double[] lastImprovement = new double[1];
			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
					1, documentProvider, 1);
			topicGrouper.solve(new TGSolutionListener<String>() {
				@Override
				public void updatedSolution(int newTopicIndex,
						int oldTopicIndex, double improvement, int t1Size,
						int t2Size, final TGSolution<String> solution) {
					if (repeat == 0) {
						if (solution.getNumberOfTopics() < 20) {
							pw3.print(solution.getNumberOfTopics());
							pw3.print(";");
							pw3.print(improvement);
							pw3.print(";");
							if (lastImprovement[0] != 0) {
								pw3.print(improvement / lastImprovement[0]);
								pw3.print(";");
							}
							lastImprovement[0] = improvement;
							pw3.println();
						}
					}
					if (solution.getNumberOfTopics() == 4) {
						tgAcc[repeat] = computeTGAccuracy(solution,
								documentProvider);

						tgPerplexity[repeat] = computeTGPerplexity(solution,
								testDocumentProvider);
					}
				}

				@Override
				public void initialized(TGSolution<String> initialSolution) {
				}

				@Override
				public void initalizing(double percentage) {
				}

				@Override
				public void done() {
				}

				@Override
				public void beforeInitialization(int maxTopics, int documents) {
				}
			});
		}
	}

	protected double computeTGAccuracy(final TGSolution<String> solution,
			final DocumentProvider<String> documentProvider) {
		final int[] topicIds = solution.getTopicIds();

		return accuracyCalculator.computeAccuracy(documentProvider, 4,
				new FrequencyProvider() {
					@Override
					public int getFrequency(int topic, int wordIndex) {
						return solution.getTopicForWord(wordIndex) == topicIds[topic] ? solution
								.getGlobalWordFrequency(wordIndex) : 0;
					}

					@Override
					public boolean isCorrectTopic(int topic, int index) {
						Integer w = Integer.valueOf(documentProvider
								.getWord(index));
						return topic == w / 100;
					}
				});
	}

	protected double computeTGPerplexity(TGSolution<String> solution,
			DocumentProvider<String> testDocumentProvider) {
		return perplexityCalculator.computePerplexity(testDocumentProvider,
				solution);
	}

	@Override
	protected void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] perplexity3, double[] acc) {
		pw.print(alphaFromStep(step));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity1));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity1));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity2));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity2));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity3));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity3));
		pw.print("; ");
		pw.print(1 - MathExt.avg(acc));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(acc));
		pw.println("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		if (step == 0) {
			double tgPerplexityAvg = MathExt.avg(tgPerplexity);
			double tgAccAvg = MathExt.avg(tgAcc);

			for (int h = 0; h < 2; h++) {
				pw.print(0.25 + (4.75 * h));
				pw.print("; ");
				pw.print(tgPerplexityAvg);
				pw.print("; ");
				pw.print(MathExt.sampleStdDev(tgPerplexity));
				pw.print("; ");
				pw.print(1.0 - tgAccAvg);
				pw.print("; ");
				pw.print(MathExt.sampleStdDev(tgAcc));
				pw.println("; ");
			}
		}
	}

	public static void main(String[] args) throws IOException {
		new TWCPerplexityErrorRateVaryAlpha(new Random(42), 1000).run(20, 3);
	}
}
