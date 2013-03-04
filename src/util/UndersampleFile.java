package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import util.Util.Sampler;

/**
 * For a given file, return a random set of rows, up to the number specified.
 * Returned rows are sorted in lexographical order. 
 */
public class UndersampleFile {
	
	public static void main(String[] args) throws IOException {
//		Path inputFile = Paths.get("F:/workstuff2011/#written_papers/paper_k/results/ratings_trevathan_multipleReweighted_percentiles.csv");
		Path inputFile = Paths.get("F:/workstuff2011/#written_papers/paper_k/results/ratings_waitStart_multipleReweighted_percentiles.csv");
//		Path inputFile = Paths.get("F:/workstuff2011/#written_papers/paper_k/results/NeuralNetwork_ROC_delayedStart.csv");
		
		BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset());

		if (reader.ready())
			reader.readLine(); // skip heading row

//		if (reader.ready())
//			System.out.println(reader.readLine()); // print first line

		
		Sampler<String> sampler = new Sampler<>(50);
		String line = null;
		while(reader.ready()) {
			line = reader.readLine();
			sampler.addItem(line);
		}
		
		List<String> rows = sampler.sample();
		Collections.sort(rows);
		
		for (int i = 0; i < rows.size(); i++) {
			System.out.println(rows.get(i));
		}
//		System.out.println(line);
	}
}
