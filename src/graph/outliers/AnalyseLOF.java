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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;

import util.CsvManipulation;
import util.IncrementalMean;
import util.Util;

/**
 * Works with files containing LOF values for different feature pairs with different parameters.
 * E.g. find the param for LOF that gives the highest average percentile for fraud vs normal users for a given feature pair. 
 * @author S
 *
 */
public class AnalyseLOF {
	public static void main(String[] args) {
		run();
//		copyWantedColumnPairs();
	}
	
	/**
	 * Runs {@link AnalyseLOF#findMaxPercentileColumn} over multiple files.
	 */
	static void run() {
		String directory = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/hybridNormalE_multi/";
		File[] files = new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("_bidderGraphFeatures.csv")
//						&& name.contains("2,3")
						;
			}
		});
		
		for (File file : files) {
			findMaxPercentileColumn(file.getAbsolutePath());
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
		StringBuffer headingRow1 = new StringBuffer();
		headingRow1.append("id");
//		StringBuffer headingRow2 = new StringBuffer();
		List<Map<Integer, String>> allLofValues = new ArrayList<>();
		for (File file : fileColumnMap.keySet()) {
			headingRow1.append("," + file.getName().replaceAll(",", "-"));
//			headingRow2.append(b)
			
			Map<Integer, String> lofValues = new HashMap<>();
			List<String[]> rows = CsvManipulation.readWholeFile(file.toPath(), true);
			
			for (String[] row : rows) {
				lofValues.put(Integer.parseInt(row[0]), row[fileColumnMap.get(file)]);
			}
			allLofValues.add(lofValues);
		}
		
		HashSet<Integer> allIds = new HashSet<>();
		for (Map<Integer, String> lofValues : allLofValues) {
			allIds.addAll(lofValues.keySet());
		}
		
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			writer.append(headingRow1.toString());
			writer.newLine();
			
			for (Integer id : allIds) {
				writer.append(id + "");
				for (Map<Integer, String> lofValues : allLofValues) {
					if (lofValues.containsKey(id))
						writer.append("," + lofValues.get(id));
					else 
						writer.append(",");
				}
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Find the column in the given file where the fraud agents (puppets) have the
	 * greatest average percentile.
	 * @param filePath
	 */
	static void findMaxPercentileColumn(String filePath) {
//		readIntoTable("F:/workstuff2011/AuctionSimulation/lof_features_fixed/multi/lof_2,3_syn_repFraud_20k_0_bidderGraphFeatures_few.csv");
//		AnalyseLOF lof = new AnalyseLOF();
		ReadLofFile lof = new ReadLofFile(filePath);
		double maxMean = -1;
		int maxMeanColumn = -1;
		for (int i = 2; i < lof.columnCount; i++) {
			double meanRank = calculateMeanRank(i, lof);
			if (maxMean < meanRank) {
				maxMean = meanRank;
				maxMeanColumn = i;
			}
			//TODO: for convenience can remove
//			System.out.println("column: " + i + ", " + meanRank);
		}
		System.out.println(filePath + ", " + maxMean + ", " + maxMeanColumn);
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
	
	public static List<Double> calculateFraudPercentiles(int columnIndex, ReadLofFile lofFile) {
		ArrayListMultimap<String, Double> valuesForUserType = ArrayListMultimap.create();
		
		for (Integer id : lofFile.userTypeMap.keySet()) {
			String userType = lofFile.userTypeMap.get(id);
			Double value = lofFile.allColumnValues[columnIndex].get(id);
			valuesForUserType.put(userType, value);
		}
		
		List<Double> normalLOF = new ArrayList<>();
		List<Double> fraudLOF = new ArrayList<>();
		for (String userType : valuesForUserType.keySet()) {
			for (double value : valuesForUserType.get(userType)) {
				if (userType.startsWith("Cluster") || userType.equals("TMSeller")) {
					normalLOF.add(value);
				} else {
					fraudLOF.add(value);
				}
			}
		}
		
		List<Double> percentiles = Util.percentiles(normalLOF, fraudLOF);
		return percentiles;
	}
	
	/**
	 * Calculate the average rank of users if they're fraud or not. 
	 * @param columnIndex
	 * @return
	 */
	public static double calculateMeanRank(int columnIndex, ReadLofFile lofFile) {
		List<Double> percentiles = calculateFraudPercentiles(columnIndex, lofFile);;
		IncrementalMean mean = new IncrementalMean();
		mean.add(percentiles);
		return mean.average();
	}

}
