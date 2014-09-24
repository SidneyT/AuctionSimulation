package shillScore.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import util.Util;

import com.google.common.collect.ArrayListMultimap;

public class CompareSSPercentiles {
	public static void main(String[] args) {
		run();
		System.out.println("Finished.");
	}
	
	public static void run() {
//		List<Integer> columnsWanted = Arrays.asList(7,9,10);
		List<Integer> columnsWanted = Arrays.asList(7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27);
//		List<Integer> columnsWanted = Arrays.asList(7,8,9);
//		String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/trevathan/combined";
		String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/waitStart/combined";
//		Path fraudFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly_reweighted.csv");
////		Path fraudFile = Paths.get(directory, "testing.csv");
//		Path normalFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly_reweighted.csv");
//		Path fraudFile = Paths.get(directory, "ratings_trevathan_fraud_forNNweightsEval_ratingsOnly_reweighted.csv");
//		Path normalFile = Paths.get(directory, "ratings_trevathan_normal_forNNweightsEval_ratingsOnly_reweighted.csv");

//		Path fraudInputFile = Paths.get(directory, "ratings_trevathan_fraud_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
//		Path normalInputFile = Paths.get(directory, "ratings_trevathan_normal_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
		Path fraudInputFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
		Path normalInputFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
		
		ArrayListMultimap<Integer, Double> fraudScores = getShillScores(columnsWanted, fraudInputFile);
		ArrayListMultimap<Integer, Double> normalScores = getShillScores(columnsWanted, normalInputFile);
		
		System.out.println("size of fraudScores:" + fraudScores.size());
		System.out.println("size of normalScores:" + normalScores.size());
		
		System.out.println("gathered shill scores");
		
		// calculate percentiles...
		List<List<Double>> percentileLists = new ArrayList<>();
		for (int column : columnsWanted) {
			System.out.println("calculating percentile for column " + column);
			List<Double> percentiles = Util.percentiles(normalScores.get(column), fraudScores.get(column));
			percentileLists.add(percentiles);
		}
		
		System.out.println("calculated percentiles");
		
		StringBuilder headingString = new StringBuilder();
		// build row heading
		headingString.append("fractionComplete,");
		for (int column : columnsWanted) {
			headingString.append("column").append(column + ",");
		}
		headingString.deleteCharAt(headingString.length() - 1);

		// write out lists of percentiles, with each list in 1 column
//		Path outputFile = Paths.get(directory, "ratings_trevathan_multipleReweighted_percentiles_noBias.csv");
		Path outputFile = Paths.get(directory, "ratings_waitStart_reweighted_percentiles.csv");
		try {
			BufferedWriter outputWriter = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
			outputWriter.append(headingString);
			outputWriter.newLine();
			
			int listSize = percentileLists.get(0).size();
			for (int i = 0; i < listSize; i++) {
				outputWriter.append((double) i/listSize + ",");
				for (int j = 0; j < percentileLists.size(); j++) {
					outputWriter.append(percentileLists.get(j).get(i) + "");
					if (j < percentileLists.size() - 1) {
						outputWriter.append(",");
					}
				}
				outputWriter.newLine();
			}
			
			outputWriter.flush();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 
	}
	
	public static List<Double> getShillScores(int columnWanted, Path inputFile) {
		return getShillScores(Collections.singletonList(columnWanted), inputFile).get(columnWanted);
	}
	
	/**
	 * Returns each specified column as a list of doubles from the file given
	 * @param columnsWanted
	 * @param inputFile
	 * @return
	 * @throws IOException 
	 */
	public static ArrayListMultimap<Integer, Double> getShillScores(List<Integer> columnsWanted, Path inputFile) {
		ArrayListMultimap<Integer, Double> scores = ArrayListMultimap.create();
		
		BufferedReader br;
		try {
			br = Files.newBufferedReader(inputFile, Charset.defaultCharset());

			if (br.ready()) {
				br.readLine(); // skip the first line with the headings
			}
			while (br.ready()) {
				String[] parts = br.readLine().split(",");
				for (int column: columnsWanted) {
					double score = Double.parseDouble(parts[column]);
					scores.put(column, score);
				}
			}
			
			return scores;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
