package graph.outliers;

import graph.outliers.CombineLOF.ReadLofFile;

import java.io.BufferedWriter;
import java.io.File;
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
import util.IncrementalMean;
import util.IncrementalSD;
import util.Util;

/**
 * Works with files containing LOF values for different feature pairs with different parameters.
 * Reads through files to find the range parameter that gives the best performance for each file (feature pair).
 */
public class AnalyseLOF {
	public static void main(String[] args) {
		run();
//		copyWantedColumnPairs();
		System.out.println("Finished.");
	}
	
	/**
	 * Runs {@link AnalyseLOF#findMaxPercentileColumn} over multiple files.
	 */
	static void run() {
//		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/hybridNormalE_multi/";
//		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/hybridBothVGS/";
//		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/hybridNormalEE/";
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/repFraud/";
//		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/multi/simplews/";
		
		System.out.println("directory: " + directory);
		
		File[] bidderFiles = new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("_bidderGraphFeatures.csv")
//						&& name.contains("2,3")
						;
			}
		});
		
		ArrayList<MaxPercentileObj> bidderMaxPercentileObjs = new ArrayList<>();
		for (File file : bidderFiles) {
			bidderMaxPercentileObjs.addAll(columnStats(file.getPath()));
		}
		writeMaxPercentileColumns(bidderMaxPercentileObjs, directory + "featureMeanRanks_bidder.csv");

				
		File[] sellerFiles = new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("_sellerGraphFeatures.csv")
						;
			}
		});
		
		ArrayList<MaxPercentileObj> sellerMaxPercentileObjs = new ArrayList<>();
		for (File file : sellerFiles) {
			sellerMaxPercentileObjs.addAll(columnStats(file.getPath()));
		}
		
		writeMaxPercentileColumns(sellerMaxPercentileObjs, directory + "featureMeanRanks_seller.csv");
		
	}
	
	public static void writeMaxPercentileColumns(ArrayList<MaxPercentileObj> maxPercentileObjs, String outputFile) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
			
			bw.append(MaxPercentileObj.columnString());
			bw.newLine();
			
			for (MaxPercentileObj obj : maxPercentileObjs) {
				bw.append(obj.toString());
				bw.newLine();
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	static void copyWantedColumnPairs() {
		final List<String> wantedColumnPairs = Arrays.asList(new String[]{"3,17", "5,17", "3,18", "5,18", "9,17", "7,17", "9,18", "7,18", "8,17", "8,18", "10,17", "4,17", "4,18", "10,18", "3,19", "5,19", "3,20", "5,20", "4,19", "8,20", "10,20", "7,20", "9,20", "4,20", "10,19", "8,19", "9,19", "7,19", "4,13", "4,15", "5,15", "2,13", "3,15", "2,15", "5,13", "2,14", "4,12", "2,12", "4,14", "3,13", "5,7", "3,7", "5,9", "3,9", "10,12", "7,13", "8,15", "9,13", "8,14", "9,14", "8,12", "7,15", "10,15", "9,15", "9,12", "10,14", "10,13", "7,12", "7,14", "8,13"});
		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/repFraud_multi/300/";
		File[] files = new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String wanted : wantedColumnPairs) {
					if (name.contains(wanted))
						return name.endsWith("_bidderGraphFeatures_few.csv");
				}
				return false;
			}
		});
		
		HashMap<File, Integer> fileColumnMap = new HashMap<>();
		for (File file : files) {
			fileColumnMap.put(file, 17);
		}
		
		copyColumns(fileColumnMap, directory + "wantedPairs.csv");
	}
	
	/**
	 * Copies a specific column from each file in the map into a new file.
	 * @param fileColumnMap
	 */
	static void copyColumns(Map<File, Integer> fileColumnMap, String outputPath) {
		StringBuffer headingRow = new StringBuffer();
		headingRow.append("id");
//		StringBuffer headingRow2 = new StringBuffer();
		List<Map<Integer, String>> allLofValues = new ArrayList<>();
		for (File file : fileColumnMap.keySet()) {
			headingRow.append("," + file.getName().replaceAll(",", "-"));
//			headingRow2.append(b)
			
			Map<Integer, String> lofValues = new HashMap<>();
			List<String[]> rows = CsvManipulation.readWholeFile(file.toPath(), true);
			
			for (String[] row : rows) {
				lofValues.put(Integer.parseInt(row[0]), row[fileColumnMap.get(file)]);
			}
			allLofValues.add(lofValues);
		}
		
		CsvManipulation.writeMaps(outputPath, allLofValues, headingRow.toString());
	}
	
	/**
	 * Find the column in the given file where the fraud agents (puppets) have the
	 * greatest average percentile.
	 * @param filePath
	 */
	static List<MaxPercentileObj> columnStats(String filePath) {
//		readIntoTable("F:/workstuff2011/AuctionSimulation/lof_features_fixed/multi/lof_2,3_syn_repFraud_20k_0_bidderGraphFeatures_few.csv");
//		AnalyseLOF lof = new AnalyseLOF();
		ReadLofFile lof = new ReadLofFile(filePath);
		String[] pathParts = filePath.split("\\\\");
		
		List<MaxPercentileObj> allObjs = new ArrayList<>();
		for (int i = 2; i < lof.columnCount; i++) {
			ArrayList<Double>[] groupLofs = groupLofs(i, lof);
			double meanRank = meanRank(groupLofs[0], groupLofs[1]);
			double meanDiff = meanDistance(groupLofs[0], groupLofs[1]);
			double variance = variance(groupLofs[0], groupLofs[1]);
			
			MaxPercentileObj maxPercentileObj = new MaxPercentileObj(pathParts[pathParts.length - 1], i, meanRank, meanDiff, variance);
			allObjs.add(maxPercentileObj);
			
			//TODO: for convenience can remove
//			System.out.println("column: " + i + ", " + meanRank);
		}
//		System.out.println(filePath + ", " + maxMean + ", " + maxMeanColumn);
		return allObjs;
	}
	
	private static class MaxPercentileObj {
		public final String filename;
		public final int column;
		public final double meanRank;
		public final double meanDiff;
		public final double sd;
		public MaxPercentileObj(final String filename, final int column, final double meanRank, final double meanDiff, final double sd) {
			this.filename = filename;
			this.column = column;
			this.meanRank = meanRank;
			this.meanDiff = meanDiff;
			this.sd = sd;
		}
		
		public static String columnString() {
			return "filename,c0,c1,meanRank,meanDiff,sd,column";
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(filename.replace(",", "-") + ", ");
			String[] columns = filename.split("_")[1].split(",");
			sb.append(columns[0] + "," + columns[1] + ",");
			sb.append(meanRank + ", ");
			sb.append(meanDiff + ", ");
			sb.append(sd + ", ");
			sb.append(column + "");
			
			return sb.toString();
		}
	}
	
	/**
	 * Calculate the mean LOF value for each user type for the given column.
	 */
	static void calculateMeanValueByGroup(int columnIndex, ReadLofFile lof) {
		ArrayListMultimap<String, Double> valuesForUserType = ArrayListMultimap.create();
		
		Map<Integer, Double> columnValues = lof.allColumnValues[columnIndex];
		for (Integer id : lof.userTypeMap.keySet()) {
			String userType = lof.userTypeMap.get(id);
			Double value = columnValues.get(id);
			valuesForUserType.put(userType, value);
		}
		
		for (String userType : valuesForUserType.keySet()) {
			IncrementalMean mean = new IncrementalMean();
			mean.add(valuesForUserType.get(userType));
			System.out.println(userType + ": " + mean);
		}
	}
	
	/**
	 * Calculate the average rank of users if they're fraud or not. 
	 * @param columnIndex
	 * @return
	 */
	public static double calculateMeanRank(int columnIndex, ReadLofFile lofFile) {
		List<Double> percentiles = calculateFraudPercentiles(columnIndex, lofFile);
		IncrementalMean mean = new IncrementalMean();
		mean.add(percentiles);
		return mean.average();
	}

	public static double meanRank(List<Double> fraudLofs, List<Double> normalLofs) {
		List<Double> percentiles = calculateFraudPercentiles(fraudLofs, normalLofs);
		IncrementalMean mean = new IncrementalMean();
		mean.add(percentiles);
		return mean.average();
	}

	/**
	 * Returns two lists of values in an array. First is the lof values of fraudulent
	 * users, second is the lof values of normal users.
	 * @param columnIndex
	 * @param lofFile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<Double>[] groupLofs(int columnIndex, ReadLofFile lofFile) {
		ArrayListMultimap<String, Double> valuesForUserType = ArrayListMultimap.create();
		
		for (Integer id : lofFile.userTypeMap.keySet()) {
			String userType = lofFile.userTypeMap.get(id);
			Double value = lofFile.allColumnValues[columnIndex].get(id);
			valuesForUserType.put(userType, value);
		}
		
		ArrayList<Double> normalLOF = new ArrayList<>();
		ArrayList<Double> fraudLOF = new ArrayList<>();
		for (String userType : valuesForUserType.keySet()) {
			for (double value : valuesForUserType.get(userType)) {
				if (isFraud(userType)) {
					fraudLOF.add(value);
				} else {
					normalLOF.add(value);
				}
			}
		}
		
		return new ArrayList[]{fraudLOF, normalLOF};
	}
	
	public static boolean isFraud(String userType) {
		return !(userType.startsWith("Cluster") || userType.equals("TMSeller"));
	}
	
	/**
	 * @return Difference between the means of the two lists.
	 */
	public static double meanDistance(List<Double> fraudLofs, List<Double> normalLofs) {
		IncrementalMean meanF = new IncrementalMean();
		meanF.add(fraudLofs);
		IncrementalMean meanN = new IncrementalMean();
		meanN.add(normalLofs);
		
		double meanDist = (meanF.average() - meanN.average()) / meanN.average(); 
		return meanDist;
	}
	/**
	 * @return standard deviation of the two means
	 */
	public static double variance(List<Double> fraudLofs, List<Double> normalLofs) {
		IncrementalSD sd = new IncrementalSD();
		sd.add(fraudLofs);
		sd.add(normalLofs);
		
		return sd.getSD();
	}
	
	public static List<Double> calculateFraudPercentiles(int columnIndex, ReadLofFile lofFile) {
		ArrayList<Double>[] groupLofs = groupLofs(columnIndex, lofFile);
		List<Double> percentiles = Util.percentiles(groupLofs[1], groupLofs[0]);
		return percentiles;
	}
	public static List<Double> calculateFraudPercentiles(List<Double> fraudLofs, List<Double> normalLofs) {
		List<Double> percentiles = Util.percentiles(normalLofs, fraudLofs);
		return percentiles;
	}
	
	
}
