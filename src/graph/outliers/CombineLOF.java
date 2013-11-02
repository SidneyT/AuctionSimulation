package graph.outliers;

import graph.UseMRF;
import graph.outliers.evaluation.LofSwap;
import graph.outliers.evaluation.LofSwap.WriteFraudPercentiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;

import util.CsvManipulation;
import util.SumStat;

/**
 * Combine user LOF scores from multiple feature pairs. 
 */
public class CombineLOF {
	public static void main(String[] args) {
//		repFraud();
//		hybridNormal();
		hybridNormalE();
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

	static void run(String directory, List<LofColumn> columns, String outputFile) {

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
		
		writeLOF(columns, outputFile, finalScores, userTypes);
		
		// print out the mean rank of the scores for fraud users compared to normal users
//		double meanRank = AnalyseLOF.calculateMeanRank(2, new ReadLofFile(outputFile));
//		System.out.println("meanRank: " + meanRank);
		
	}
	
	// write out the score combined from multiple feature pairs
	public static void writeLOF(List<LofColumn> columns, String outputFile, HashMap<Integer, Double> finalScores, HashMap<Integer, String> userTypes) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
			// write headings
			writer.append("id,userType,score,");
			for (LofColumn lofColumn : columns) {
				writer.append(lofColumn.toString() + "|");
			}
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
		useMRF.run();
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
			if (filename.contains(filenameSubstring) && filename.contains(f1 + "," + f2)) {
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
