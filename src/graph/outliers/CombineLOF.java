package graph.outliers;

import graph.UseMRF;
import graph.outliers.evaluation.LofSwap;
import graph.outliers.evaluation.LofSwap.WriteFraudPercentiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;

import util.CsvManipulation;
import util.IncrementalSD;
import util.SumStat;

/**
 * Combine user LOF scores from multiple feature pairs. 
 */
public class CombineLOF {
	public static void main(String[] args) {
//		repFraud();
//		hybridNormal();
//		hybridNormalE();
//		allFraudsBidder();
//		allFraudsSeller();
//		syntheticData();
		tmData();
	}
	
	public static void tmData() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		List<String> inputFiles = Arrays.asList(
				"chosenLof_trademe_jit_bidder.csv", 
				"chosenLof_trademe_jit_seller.csv");
		List<String> outputFiles = Arrays.asList(
				"combinedLof_trademe_jit_bidder.csv", 
				"combinedLof_trademe_jit_seller.csv");
		
		for (int i = 0; i < inputFiles.size(); i++) {
			combineScores(directory + "chosenLof/" + inputFiles.get(i), directory + "combinedLof/" + outputFiles.get(i));
		}
	}
	
	/**
	 * Combine the lof of feature pairs into 1, rank them, then calculate stats about them.
	 */
	public static void syntheticData() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		
		for (String name : new String[]{
				"repFraud",
				"hybridNormalEE", 
				"hybridBothVGS"
				}) {
			for (int i = 0; i < 20; i++) {
				String inputFile = directory + "chosenLof/chosenLof_" + name + "_bidder_" + i + ".csv";
				String outputFile = directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv";
				combineScores(inputFile, outputFile);

				// just to see what the lof values are like between groups
				ReadLofFile lof = new ReadLofFile(outputFile);
				ArrayList<Double>[] groupLofs = AnalyseLOF.groupLofs(2, lof);
				double meanRank = AnalyseLOF.meanRank(groupLofs[0], groupLofs[1]);
				System.out.println(outputFile + " meanRank:" + meanRank);
			}
		}
		
		for (String name : new String[]{"repFraud"
				, "hybridNormalEE", "hybridBothVGS"
				}) {
			for (int i = 0; i < 20; i++) {
				String inputFile = directory + "chosenLof/chosenLof_" + name + "_seller_" + i + ".csv";
				String outputFile = directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv";
				combineScores(inputFile, outputFile);

				// just to see what the lof values are like between groups
				ReadLofFile lof = new ReadLofFile(outputFile);
				ArrayList<Double>[] groupLofs = AnalyseLOF.groupLofs(2, lof);
				double meanRank = AnalyseLOF.meanRank(groupLofs[0], groupLofs[1]);
				System.out.println(outputFile + " meanRank:" + meanRank);
			}
		}
	}
	/**
	 * Copies the LOF values from different columns from different files, combines them into 1, then writes them to a file.
	 */
	public static void allFraudsBidder() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/";
		
		File[] wantedDirectories = new File(directory).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && (pathname.getName().equals("hybridBothVGS") || pathname.getName().equals("hybridNormalEE") || pathname.getName().equals("repFraud"));
			}
		});
		
		for (File wantedDirectory : wantedDirectories) {
			LofColumn b1 = new LofColumn(6, 26, "bidder", 8);
			LofColumn b2 = new LofColumn(3, 24, "bidder", 8);
			LofColumn b3 = new LofColumn(11, 24, "bidder", 8);
			LofColumn b4 = new LofColumn(11, 29, "bidder", 8);
			
			final List<LofColumn> bidderColumns = Arrays.asList(new LofColumn[]{b1
					, b2, b3, b4
					});
			
			String outputFilename = directory + "combinedScores_" + wantedDirectory.getName() + "_bidders.csv";
			run(wantedDirectory.getPath(), bidderColumns, outputFilename);
			
			ReadLofFile lof = new ReadLofFile(outputFilename);
			ArrayList<Double>[] groupLofs = AnalyseLOF.groupLofs(2, lof);
			
			System.out.println("directory: " + wantedDirectory);
			
			double meanRank = AnalyseLOF.meanRank(groupLofs[0], groupLofs[1]);
			System.out.println("meanRank:" + meanRank);

			double meanDiff = AnalyseLOF.meanDistance(groupLofs[0], groupLofs[1]);
			System.out.println("meanDiff:" + meanDiff);
		}
	}
	
	/**
	 * Copies the LOF values from different columns from different files, combines them into 1, then writes them to a file.
	 */
	public static void allFraudsSeller() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/";
		
		File[] wantedDirectories = new File(directory).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && (pathname.getName().equals("hybridBothVGS") || pathname.getName().equals("hybridNormalEE") || pathname.getName().equals("repFraud"));
			}
		});
		
		for (File wantedDirectory : wantedDirectories) {
			LofColumn b1 = new LofColumn(3, 28, "seller", 8);
			LofColumn b2 = new LofColumn(2, 29, "seller", 8);
			LofColumn b3 = new LofColumn(14, 15, "seller", 4);
			LofColumn b4 = new LofColumn(20, 29, "seller", 4);
			
			final List<LofColumn> bidderColumns = Arrays.asList(new LofColumn[]{b1
					, b2, b3, b4
					});
			
			String outputFilename = directory + "combinedScores_" + wantedDirectory.getName() + "_sellers.csv";
			run(wantedDirectory.getPath(), bidderColumns, outputFilename);
			double buyerRanks = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(outputFilename));

			System.out.println("directory: " + wantedDirectory);

			System.out.println("sellerRank:" + buyerRanks);
		}
	}
	
	/**
	 * Combines LOF scores from all the columns in the same file.
	 */
	public static void combineScores(String inputFile, String outputFile) {
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(inputFile), true);
		
		HashMap<Integer, Double> finalValues = new HashMap<>();
		HashMap<Integer, String> userTypes = new HashMap<>();
		for (String[] row : rows) {
			int id = Integer.parseInt(row[0]);
			userTypes.put(id, row[1]);
			List<Double> valueList = new ArrayList<>();
			for (int i = 2; i < row.length; i++) {
				double value = Double.valueOf(row[i]);
				valueList.add(value);
			}
			double combinedValue = SumStat.Max.summaryValue(valueList);
			finalValues.put(id, combinedValue);
		}
		
		String headingRow = "id,userType,outlierScore";
		
		writeLOF(headingRow, outputFile, finalValues, userTypes);
	}
	
	public static void repFraud() {
		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/repFraud_multi/backto500/";
		
//		LofColumn b1 = new LofColumn(3, 17, "bidder", 17);
//		LofColumn b2 = new LofColumn(4, 12, "bidder", 17);
//		LofColumn b3 = new LofColumn(5, 7, "bidder", 17);
//		LofColumn b4 = new LofColumn(7, 12, "bidder", 17);
//		final List<LofColumn> bidderColumns = Arrays.asList(new LofColumn[]{b1, b2, b3, b4});
//		run(directory, bidderColumns, directory + "combinedScores_bidders.csv");
//		double buyerRanks = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(directory + "combinedScores_bidders.csv"));
//		System.out.println("buyerRank:" + buyerRanks);
//		
//		LofColumn s1 = new LofColumn(17, 22, "seller", 17);
//		LofColumn s2 = new LofColumn(17, 18, "seller", 17);
//		LofColumn s3 = new LofColumn(2, 7, "seller", 17);
//		final List<LofColumn> sellerColumns = Arrays.asList(new LofColumn[]{s1, s2, s3});
//		run(directory, sellerColumns, directory + "combinedScores_sellers.csv");
//		double sellerRanks = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(directory + "combinedScores_sellers.csv"));
//		System.out.println("sellerRank:" + sellerRanks);
		
		runLof("syn_repFraud_20k_0", 
				directory + "combinedScores_bidders.csv", 
				directory + "combinedScores_sellers.csv",
				directory + "propagatedScores.csv"
				);
//	
		double meanRankBeforeProp = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(directory + "propagatedScores.csv"));
		double meanRankAfterProp = AnalyseLOF.calculateMeanRank(3, new ReadLofFile(directory + "propagatedScores.csv"));
		System.out.println("meanRank: " + meanRankBeforeProp + ", " + meanRankAfterProp);		

		// write the fraud ranks to file for constructing ROCs
		List<Double> rankBeforeProp = AnalyseLOF.calculateFraudPercentiles(2, new ReadLofFile(directory + "propagatedScores.csv"));
		List<Double> rankAfterProp = AnalyseLOF.calculateFraudPercentiles(3, new ReadLofFile(directory + "propagatedScores.csv"));

		WriteFraudPercentiles writeFruadPercentiles = new WriteFraudPercentiles(directory + "rocs.csv");
		writeFruadPercentiles.firstWrite(rankBeforeProp, "beforeProp");
		writeFruadPercentiles.subsequentWrites(rankAfterProp, "afterProp");
	}

	public static void hybridNormalE() {
		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/hybridNormalE_multi/";
		
//		LofColumn b1 = new LofColumn(4, 17, "bidder", 9);
//		LofColumn b2 = new LofColumn(4, 11, "bidder", 7);
//		final List<LofColumn> bidderColumns = Arrays.asList(new LofColumn[]{b1, b2});
//		run(directory, bidderColumns, directory + "combinedScores_bidders.csv");
//		
//		LofColumn s1 = new LofColumn(2, 3, "seller", 2);
//		LofColumn s2 = new LofColumn(6, 9, "seller", 4);
//		LofColumn s3 = new LofColumn(3, 8, "seller", 6);
//		final List<LofColumn> sellerColumns = Arrays.asList(new LofColumn[]{s1, s2, s3});
//		run(directory, sellerColumns, directory + "combinedScores_sellers.csv");
		
		runLof("syn_hybridNormalE_20k_0", 
//				directory + "combinedScores_bidders.csv", 
//				directory + "combinedScores_sellers.csv",
//				directory + "combinedScores_bidders - Copy.csv", 
//				directory + "combinedScores_sellers - Copy.csv",
//				directory + "combinedScores_bidders_fraudmodified.csv", 
//				directory + "combinedScores_sellers_fraudmodified.csv",
				directory + "combinedScores_bidders_allmodified.csv", 
				directory + "combinedScores_sellers_allmodified.csv",
				directory + "propagatedScores.csv"
				);

		
		double meanRankBeforeProp = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(directory + "propagatedScores.csv"));
		double meanRankAfterProp = AnalyseLOF.calculateMeanRank(3, new ReadLofFile(directory + "propagatedScores.csv"));
		System.out.println("meanRank: " + meanRankBeforeProp + ", " + meanRankAfterProp);		

		// write the fraud ranks to file for constructing ROCs
		List<Double> rankBeforeProp = AnalyseLOF.calculateFraudPercentiles(2, new ReadLofFile(directory + "propagatedScores.csv"));
		List<Double> rankAfterProp = AnalyseLOF.calculateFraudPercentiles(3, new ReadLofFile(directory + "propagatedScores.csv"));
		
		WriteFraudPercentiles writeFruadPercentiles = new WriteFraudPercentiles(directory + "rocs.csv");
		writeFruadPercentiles.firstWrite(rankBeforeProp, "beforeProp");
		writeFruadPercentiles.subsequentWrites(rankAfterProp, "afterProp");
		
	}

	
	public static void hybridNormal() {
		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/hybridNormal_multi/";
		
//		LofColumn b1 = new LofColumn(4, 18, "bidder", 17);
//		LofColumn b2 = new LofColumn(2, 13, "bidder", 17);
//		LofColumn b3 = new LofColumn(2, 3, "bidder", 17);
//		final List<LofColumn> bidderColumns = Arrays.asList(new LofColumn[]{b1, b2, b3});
//		run(directory, bidderColumns, directory + "combinedScores_bidders.csv");
//		
//		LofColumn s1 = new LofColumn(2, 3, "seller", 17);
//		final List<LofColumn> sellerColumns = Arrays.asList(new LofColumn[]{s1});
//		run(directory, sellerColumns, directory + "combinedScores_sellers.csv");
		
		runLof("syn_hybridNormal_20k_0", 
				directory + "combinedScores_bidders.csv", 
				directory + "combinedScores_sellers.csv",
				directory + "propagatedScores.csv"
				);
	
		double meanRankBeforeProp = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(directory + "propagatedScores.csv"));
		double meanRankAfterProp = AnalyseLOF.calculateMeanRank(3, new ReadLofFile(directory + "propagatedScores.csv"));
		System.out.println("meanRank: " + meanRankBeforeProp + ", " + meanRankAfterProp);		
	}

	private static void run(String directory, List<LofColumn> columns, String outputFile) {

		File[] files = new File(directory).listFiles();
		
		// collect the scores from the different files
		ArrayListMultimap<Integer, Double> combinedScoreColumns = ArrayListMultimap.create();
		HashMap<Integer, String> userTypes = new HashMap<>();
		for (File file : files) {
			for (LofColumn lofColumn : columns) {
				if (lofColumn.matches(file.getName())) {
					System.out.println("Files chosen: " + file.getName());
					ReadLofFile lofFile = new ReadLofFile(file.getAbsolutePath());
					
					userTypes.putAll(lofFile.userTypeMap);
					
					Map<Integer, Double> lofValues = lofFile.allColumnValues[lofColumn.column];
//					lofValues = normalise(lofValues); // TODO: control whether values are normalised according to variance
					for (Integer id : lofValues.keySet()) {
//						if (id == 256178)
//							System.out.println(lofValues.get(id) + "");
						
						Double lofValue = lofValues.get(id);
						combinedScoreColumns.put(id, lofValue);
					}
				}
			}
		}
		
		// combine the different scores into one
		HashMap<Integer, Double> finalScores = new HashMap<>();
		for (Integer id : combinedScoreColumns.keySet()) {
			List<Double> scores = combinedScoreColumns.get(id);
			double finalScore = SumStat.Max.summaryValue(scores);
//			double finalScore = SumStat.Mean.summaryValue(scores);
			finalScores.put(id, finalScore);
		}

		StringBuffer sb = new StringBuffer();
		
		// write headings
		sb.append("id,userType,score,");
		for (LofColumn lofColumn : columns) {
			sb.append(lofColumn.toString() + "|");
		}
		
		writeLOF(sb.toString(), outputFile, finalScores, userTypes);
		
		// print out the mean rank of the scores for fraud users compared to normal users
//		double meanRank = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(outputFile));
//		System.out.println("meanRank: " + meanRank);
	}
	
	/**
	 * Normalise different sets of scores... according to variance?
	 * @return 
	 */
	private static HashMap<Integer, Double> normalise(Map<Integer, Double> lofValues) {
		IncrementalSD sd = new IncrementalSD();
		
		for (Integer id : lofValues.keySet()) {
			sd.add(lofValues.get(id));
		}
		
		double stdDev = sd.getSD();
		HashMap<Integer, Double> normalisedValues = new HashMap<>();
		for (Integer id : lofValues.keySet()) {
			double nValue = lofValues.get(id) / stdDev;
			normalisedValues.put(id, nValue);
		}
		
//		System.out.println("standardDeviation: " + sd.getSD());
		return normalisedValues;
	}
	
	// write out the user type and combined score for each user
	public static void writeLOF(String headingRow, String outputFile, HashMap<Integer, Double> finalScores, HashMap<Integer, String> userTypes) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
			
			writer.append(headingRow);
			writer.newLine();
			
			for (Integer id : finalScores.keySet()) {
				writer.append(id + "");
				writer.append("," + userTypes.get(id));
				writer.append("," + finalScores.get(id));
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void runLof(String dbName, String bidderFile, String sellerFile, String outputFile) {
		UseMRF useMRF = new UseMRF(dbName, bidderFile, sellerFile);
		useMRF.run(dbName);
		useMRF.writeScores(outputFile);
	}
	
	public static class LofColumn {
		final int f1;
		final int f2;
		final String filenameSubstring;
		final int column;
		LofColumn(int f1, int f2, String filenameSubstring, int column) {
			this.f1 = f1;
			this.f2 = f2;
			this.filenameSubstring = filenameSubstring;
			this.column = column;
		}
		boolean matches(String filename) {
			if (filename.contains(filenameSubstring) && filename.contains("_" + f1 + "," + f2 + "_")) {
				return true;
			}
			return false;
		}
		@Override
		public String toString() {
			return f1 + "-" + f2 + "-" + 
//					filenameSubstring + "-" + 
					column;
		}
	}
	
	public static class ReadLofFile {
		public final List<String> headings;
		public final int columnCount;
		/** Map(id, user type) */
		public final HashMap<Integer, String> userTypeMap;
		/** Map(id, LOF value)[column Index]*/
		public final Map<Integer, Double>[] allColumnValues;
		
		@SuppressWarnings("unchecked")
		public ReadLofFile(String filePath) {
			List<String[]> lines = CsvManipulation.readWholeFile(Paths.get(filePath), false);
			headings = Arrays.asList(lines.get(0)); // first row of headings
			
			this.columnCount = lines.get(lines.size() - 1).length;
			
			userTypeMap = new HashMap<>();
			allColumnValues = new HashMap[columnCount];
			for (int r = 1; r < lines.size(); r++) {
				Integer id = Integer.parseInt(lines.get(r)[0]);
				String userType = lines.get(r)[1];
				userTypeMap.put(id, userType);
				
				for (int c = 2; c < columnCount; c++) {
					Map<Integer, Double> columnValues;
					if (allColumnValues[c] == null) {
						columnValues = new HashMap<>(); // Map<User Id, LOF value>
						allColumnValues[c] = columnValues;
					} else {
						columnValues = allColumnValues[c]; 
					}
					double value = Double.parseDouble(lines.get(r)[c]);
					columnValues.put(id, value);
				}
			}
		}
	}

	private static void test() {
		File file = new File("F:/workstuff2011/AuctionSimulation/lof_features_fixed/hybridNormal_multi/lof_2,10_syn_hybridNormal_20k_0_bidderGraphFeatures.csv");
		ReadLofFile lofFile = new ReadLofFile(file.getAbsolutePath());
		System.out.println(file.getName());
		System.out.println(lofFile.headings.get(7));
		System.out.println(lofFile.userTypeMap.get(244871));
		System.out.println(lofFile.allColumnValues[7].get(244871));
	}
	
}
