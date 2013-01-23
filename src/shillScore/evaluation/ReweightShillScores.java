package shillScore.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Reads a file that contains the 6 shill score ratings, and calculates a weighted average of those
 * scores using sets of weights given.
 * Scores of fraud and normal users are written in different files.
 */
public class ReweightShillScores {
	public static void main(String[] args) throws IOException {
		List<Integer> wantedColumns = ImmutableList.of(0,1,2,3,4,5);
		Collection<List<Double>> weightSets = ImmutableList.<List<Double>>of(
//				ImmutableList.of(9.0,2.0,5.0,2.0,2.0,2.0), // default from SS
				ImmutableList.of(1.0,1.0,1.0,1.0,1.0,1.0), // equivalent weights
				ImmutableList.of(0.0820,0.0049,-0.0319,0.5041,0.2407,0.2003), // given by NN
				ImmutableList.of(0.000252653,0.10053923,0.005029133,-0.034119916,0.453745575,0.246415436,0.228137889) // average from NN
//				ImmutableList.of(0.039578424,18.68319145,1.117054671,-7.275003797,114.901611,54.85097605,45.64194598)
				);

		String directory = "F:/workstuff2011/AuctionSimulation/shillingResults/waitStart/combined";
		Path inputFile = Paths.get(directory, "ratings_waitStart_fraud_ratingsOnly.csv");
		Path outputFile = Paths.get(directory, "ratings_waitStart_fraud_ratingsOnly_reweighted.csv");
		recalculate(wantedColumns, weightSets, inputFile, outputFile);
	}

	public static void recalculate(List<Integer> ratingColumns, Collection<List<Double>> weights, Path inputFile, Path outputFile) throws IOException {
		
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
		System.out.println("Done.");
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
		double newScore = 0;
		StringBuilder sb = new StringBuilder();
		for (List<Double> weightSet : weightSets) {
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
