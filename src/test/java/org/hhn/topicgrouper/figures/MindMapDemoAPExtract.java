package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemoAPExtract extends OptimizedTGTester {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private Writer file;

	public MindMapDemoAPExtract(Writer file) throws IOException {
		super(null);
		this.file = file;
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(10, 0, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new APParser(true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.02, 200));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				System.out, 100, true));
		return multiplexer;
	}

	@Override
	protected void done() {
		try {
			FreeMindXMLTopicHierarchyWriter<String> writer = new FreeMindXMLTopicHierarchyWriter<String>(
					true);
			writer.writeToFile(file, mindMapSolutionReporter.getCurrentNodes()
					.values());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		File file = new File("./target/MindMapDemoAPExtract.mm");
		FileWriter writer = new FileWriter(file);
		new MindMapDemoAPExtract(writer).run();
		writer.close();
	}
}