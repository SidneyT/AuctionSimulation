package shillScore.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Reads a file that contains the 6 shill score ratings, and calculates a weighted average of those
 * scores using sets of weights given.
 * Scores of fraud and normal users are written in different files.
 */
public class ReweightShillScores {
	public static void main(String[] args) throws IOException {
//		trevathan();
//		waitStart();
		
		multipleReweights();
		
		System.out.println("Finished.");
	}
	
	private static void multipleReweights() {
		
		try {
			String weightFilesDirectory = "F:/workstuff2011/AuctionSimulation/";
//			CSVReader csvReader = new CSVReader(new FileReader(weightFilesDirectory + "NeuralNetworkWeights_trevathan_top20_noBias.txt"));
			CSVReader csvReader = new CSVReader(new FileReader(weightFilesDirectory + "NeuralNetworkWeights_waitStart_top20_noBias.txt"));
			
			Builder<List<Double>> weightListsBuilder = ImmutableList.<List<Double>>builder();
			weightListsBuilder.add(ImmutableList.of(9.0,2.0,5.0,2.0,2.0,2.0));
			
			String[] weightLine;
			while ((weightLine = csvReader.readNext()) != null) { // iterate over rows of weights
				Builder<Double> weightList = ImmutableList.builder();
				// convert strings into a list of doubles
				for (String weight : weightLine) {
					weightList.add(Double.parseDouble(weight));
				}
				weightListsBuilder.add(weightList.build());
			}
			csvReader.close();
			
			List<Integer> wantedColumns = ImmutableList.of(0,1,2,3,4,5);
			List<List<Double>> weightLists = weightListsBuilder.build();

			String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/waitStart/combined";

			// recalculate scores for negative (normal) examples
			Path normalInputFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly.csv");
			Path normalOutputFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
			recalculate(wantedColumns, weightLists, normalInputFile, normalOutputFile);

			// recalculate scores for positive (fraud) examples
			Path fraudInputFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly.csv");
			Path fraudOutputFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly_multipleReweighted_noBias.csv");
			recalculate(wantedColumns, weightLists, fraudInputFile, fraudOutputFile);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private static void trevathan() {
		List<Integer> wantedColumns = ImmutableList.of(0,1,2,3,4,5);
		Collection<List<Double>> weightSets = ImmutableList.<List<Double>>of(
				ImmutableList.of(9.0,2.0,5.0,2.0,2.0,2.0), // default from SS
				ImmutableList.of(1.0,1.0,1.0,1.0,1.0,1.0), // equivalent weights
				ImmutableList.of(0.161529576,0.00139649,-0.104264416,0.523692377,0.045103799,0.372542174), // best weights for trevathan from NN, fraud 2%, misclassification costs FN=FP*20
				ImmutableList.of(0.129963381,0.003533005,-0.099330495,0.41730838,0.042112939,0.506412788)// second best weights for waitStart from NN
				);

		String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/trevathan/combined";
		Path inputFile = Paths.get(directory, "ratings_trevathan_normal_forNNweightsEval_ratingsOnly.csv");
		Path outputFile = Paths.get(directory, "ratings_trevathan_normal_forNNweightsEval_ratingsOnly_reweighted.csv");
//		Path inputFile = Paths.get(directory, "ratings_trevathan_fraud_forNNweightsEval_ratingsOnly.csv");
//		Path outputFile = Paths.get(directory, "ratings_trevathan_fraud_forNNweightsEval_ratingsOnly_reweighted.csv");
		recalculate(wantedColumns, weightSets, inputFile, outputFile);
	}
	private static void waitStart() {
		List<Integer> wantedColumns = ImmutableList.of(0,1,2,3,4,5);
		Collection<List<Double>> weightSets = ImmutableList.<List<Double>>of(
				ImmutableList.of(9.0,2.0,5.0,2.0,2.0,2.0), // default from SS
				ImmutableList.of(1.0,1.0,1.0,1.0,1.0,1.0), // equivalent weights
				ImmutableList.of(0.659097753,0.06996329,0.107652634,0.051495608,-0.221279431,0.333070146), // best weights for waitStart from NN, fraud 2%, misclassification costs FN=FP*20
				ImmutableList.of(0.74074493,0.017283856,0.218627834,-0.028302271,-0.29263766,0.344283311)// second best weights for waitStart from NN
				);

		String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/waitStart/combined";
//		Path inputFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly.csv");
//		Path outputFile = Paths.get(directory, "ratings_waitStart_normal_forNNweightsEval_ratingsOnly_reweighted.csv");
		Path inputFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly.csv");
		Path outputFile = Paths.get(directory, "ratings_waitStart_fraud_forNNweightsEval_ratingsOnly_reweighted.csv");
		recalculate(wantedColumns, weightSets, inputFile, outputFile);
	}

	public static void recalculate(List<Integer> ratingColumns, Collection<List<Double>> weights, Path inputFile, Path outputFile) {
		
		try {
			BufferedWriter outputWriter = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
			BufferedReader br = Files.newBufferedReader(inputFile, Charset.defaultCharset());
	
			if (br.ready()) { // skip the first line with the column headings
				outputWriter.append(br.readLine()).append(",");
				outputWriter.append(heading(weights));
				outputWriter.newLine();
			}
	
			while(br.ready()) {
				String line = br.readLine();
				String[] parts = line.split(",");
				
				outputWriter.append(line).append(","); // copy the original line
				outputWriter.append(line(weights, ratingColumns, parts)); // add the recalculated scores
				outputWriter.newLine();
			}
			
			outputWriter.flush();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String heading(Collection<List<Double>> weightSets) {
		StringBuilder sb = new StringBuilder();
		for (List<Double> weightSet : weightSets) {
			sb.append(weightSet.toString().replace(", ", "|"));
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	private static String line(Collection<List<Double>> weightSets, List<Integer> ratingColumns, String[] parts) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (List<Double> weightSet : weightSets) {
			double newScore = 0;
			for (int i = 0; i < ratingColumns.size(); i++) {
				Double rating = Double.parseDouble(parts[ratingColumns.get(i)]);
				newScore += weightSet.get(i) * rating;
			}
//			newScore += -221.793742153912;
			sb.append(newScore + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
