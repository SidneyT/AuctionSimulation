package graph.outliers.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import util.CsvManipulation;
import util.Sample;

import com.google.common.collect.ArrayListMultimap;

import graph.outliers.AnalyseLOF;
import graph.outliers.CombineLOF;
import graph.outliers.CombineLOF.ReadLofFile;

public class LofSwap {
	public static void main(String[] args) {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";
		
//		WriteFraudPercentiles originalScoresWriter = new WriteFraudPercentiles(directory + "originalPercentiles.csv");
//		WriteFraudPercentiles propagatedScoresWriter = new WriteFraudPercentiles(directory + "propagatedPercentiles_half.csv");
		
		for (String name : new String[]{
				"repFraud", 
				"hybridBothVGS", 
				"hybridNormalEE",
				}) {
			for (int j = 19; j < 20; j++) {
				String bidderScores = directory + "combinedLof/combinedLof_" + name + "_bidder_" + j + ".csv";
				String sellerScores = directory + "combinedLof/combinedLof_" + name + "_seller_" + j + ".csv";

				for (int i = 0; i < 51; i++) {
					double proportionSwapped = i * .02;
	
//					String bidderScoresSwappedOutput = directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionSwapped +"_bidders_" + j + ".csv";
//					String sellerFileSwappedOutput = directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionSwapped +"_sellers_" + j + ".csv";
					String bidderScoresSwappedOutput = directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionSwapped +"_bidders_" + j + ".csv";
					String sellerFileSwappedOutput = directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionSwapped +"_sellers_" + j + ".csv";
					
					swapScores(bidderScores, bidderScoresSwappedOutput, proportionSwapped);
					swapScores(sellerScores, sellerFileSwappedOutput, proportionSwapped);
					
	//				String propagatedSwapped = directory + "propagatedScores_" + proportionSwapped + ".csv";
	//				CombineLOF.runLof("syn_repFraud_20k_0", bidderScoresSwapped, sellerFileSwapped, propagatedSwapped);
	//				List<Double> ranksBeforeProp = AnalyseLOF.calculateFraudPercentiles(2, new ReadLofFile(propagatedSwapped));
	//				List<Double> ranksAfterProp = AnalyseLOF.calculateFraudPercentiles(3, new ReadLofFile(propagatedSwapped));
	//				
	//				Collections.sort(ranksBeforeProp);
	//				Collections.sort(ranksAfterProp);
	//				
	//				if (i == 0) {
	//					originalScoresWriter.firstWrite(ranksBeforeProp, "s" + proportionSwapped);
	//					propagatedScoresWriter.firstWrite(ranksAfterProp, "s" + proportionSwapped);
	//				} else {
	//					originalScoresWriter.subsequentWrites(ranksBeforeProp, "s" + proportionSwapped);
	//					propagatedScoresWriter.subsequentWrites(ranksAfterProp, "s" + proportionSwapped);
	//				}
				}
			}
		}
	}
	
	public static class WriteFraudPercentiles {
		final String outputFilename;
		private final BufferedWriter writer;
		public WriteFraudPercentiles(String outputFilename) {
			this.outputFilename = outputFilename;
			
			try {
				writer = Files.newBufferedWriter(Paths.get(outputFilename), Charset.defaultCharset());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public void firstWrite(List<Double> scores, String headingRow) {
			try {
				writer.write("TPR," + headingRow);
				writer.newLine();
				
				double i = 0;
				
				for (Double percentile : scores) {
					writer.append(i++ / scores.size() + ",");
					writer.append(percentile + "");
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
		
		public void subsequentWrites(List<Double> scores, String columnName) {
			CsvManipulation.addColumn(Paths.get(outputFilename), Paths.get(outputFilename), columnName, scores);
		}
		
	}
	
	public static void swapScores(String inputFile, String outputFile, double propSwap) {
		LofSwap swap1 = new LofSwap(inputFile, outputFile);
		final Picker normal = new Picker() {
			@Override
			public boolean test(String userType) {
				return userType.startsWith("Cluster") || userType.equals("TMSeller");
			}};
		Picker fraud = new Picker() {
			@Override
			public boolean test(String userType) {
				return !normal.test(userType);
			}
		};
			
		swap1.swapProp(normal, fraud, propSwap); //TODO: change fraud scores using scores from normal users  
//		swap1.swapProp(fraud, normal, propSwap); //TODO: change normal scores using scores from fraud users
		swap1.done();
	}
	
	private final String scoreFilePath;
	private final String outputPath;
	private final ReadLofFile reader;
	private final ArrayListMultimap<String, Integer> idByType; // Map<User Type, Id>
	
	private final Map<Integer, Double> originalScores;
	private final HashMap<Integer, Double> swappedScores;
	
	/**
	 * 
	 * @param scoreFilePath Input file path. <br> First column is <code>id</code>, second is <code>userType</code>, third is LOF <code>score</code>
	 * @param outputPath output file with scores swapped between userTypes
	 */
	public LofSwap(String scoreFilePath, String outputPath) {
		this.scoreFilePath = scoreFilePath;
		this.outputPath = outputPath;
		this.reader = new ReadLofFile(scoreFilePath);
		
		idByType = ArrayListMultimap.create();
		HashMap<Integer, String> userTypes = reader.userTypeMap;
		Map<Integer, Double> scores = reader.allColumnValues[2];
		for (Integer id : scores.keySet()) {
			String userType = userTypes.get(id);
			idByType.put(userType, id);
		}
		
		originalScores = scores;
		swappedScores = new HashMap<>(scores);
	}
	
	public void swap(Picker valuesFrom, Picker valuesTo, int count) {
		Set<String> userTypes = idByType.keySet();
		
		ArrayList<Integer> idFrom  = new ArrayList<>();
		ArrayList<Integer> idReplace = new ArrayList<>();

		// split the ids up into two groups, users whose lof value will be replaced,
		// and user's who's lof value will be copied from
		for (String userType : userTypes) {
			if(valuesFrom.test(userType)) {
				idFrom.addAll(idByType.get(userType));
			}
			
			if(valuesTo.test(userType)) {
				idReplace.addAll(idByType.get(userType));
			}
		}
		
		swap2(idFrom, idReplace, count);
	}
	
	public void swapProp(Picker valuesFrom, Picker valuesTo, double proportion) {
		Set<String> userTypes = idByType.keySet();
		
		ArrayList<Integer> idFrom  = new ArrayList<>();
		ArrayList<Integer> idReplace = new ArrayList<>();

		// split the ids up into two groups, users whose lof value will be replaced,
		// and user's who's lof value will be copied from
		for (String userType : userTypes) {
			if(valuesFrom.test(userType)) {
				idFrom.addAll(idByType.get(userType));
			}
			
			if(valuesTo.test(userType)) {
				idReplace.addAll(idByType.get(userType));
			}
		}
		
		int numberToSwap = (int) Math.round(idReplace.size() * proportion);
//		System.out.println("swapping: " + numberToSwap);
		swap2(idFrom, idReplace, numberToSwap);
	}
	
	private void swap2(ArrayList<Integer> idFrom, ArrayList<Integer> idReplace, int count) {
		Random r = new Random(13579);
		// sample the ids to randomly select the right number of lof values to swap
		
		// use a loop in case count is greater than the list size
		ArrayList<Integer> idFromSelected = new ArrayList<>();
		int countDup1 = count;
		while (idFrom.size() < countDup1) {
			idFromSelected.addAll(idFrom);
			countDup1 -= idFrom.size();
		}
		idFromSelected.addAll(Sample.randomSample(idFrom, countDup1, r));
		
		ArrayList<Integer> idReplaceSelected = new ArrayList<>();
		int countDup2 = count;
		while (idReplace.size() < countDup2) {
			idReplaceSelected.addAll(idReplace);
			countDup2 -= idReplace.size();
		}
		idReplaceSelected.addAll(Sample.randomSample(idReplace, countDup2, r));

		Collections.shuffle(idFromSelected);
		Collections.shuffle(idReplaceSelected);
		
		for (int i = 0; i < idFromSelected.size(); i++) {
			int idSel = idFromSelected.get(i);
			int idRep = idReplaceSelected.get(i);
			Double value = originalScores.get(idSel);
//			System.out.println("From, To: " + idSel + ", " + idRep);
			swappedScores.put(idRep, value);
		}
	}
	
	public void done() {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			
			writer.append("id,userType" +
//					",OriginalScore" +
					",SwappedScore");
			writer.newLine();
			
			for (int id : swappedScores.keySet()) {
				writer.append(id + ",");
				writer.append(reader.userTypeMap.get(id) + ",");
//				writer.append(originalScores.get(id) + ",");
				writer.append(swappedScores.get(id) + "");
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Chooses the userType.
	 */
	public interface Picker {
		boolean test(String userType);
	};
}
