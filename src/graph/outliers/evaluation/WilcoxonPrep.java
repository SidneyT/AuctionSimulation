package graph.outliers.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import util.CsvManipulation;

/**
 * Class for reformatting final scores into 1 or 0 depending on whether they were classified correctly.
 * Orders scores in descending order and uses a threshold to do label everything above threshold as fraudulent.
 *
 */
public class WilcoxonPrep {
	public static void main(String[] args) {
		run();
	}

	private static void run() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\finalScores\\";
		for (String name : new String[]{"repFraud","hybridBothVGS","hybridNormalEE",}) {
			for (int i = 0; i < 20; i++) {
				String spanFilename = directory + "finalScores_" + name + "_" + i + ".csv";
				String lfsFilename = directory + "finalScores_" + name + "_NetProbe_" + i + ".csv";
				String outputFilename = directory + "wilcox_" + name + "_" + i + ".csv";
				writeNew(spanFilename, lfsFilename, outputFilename);
			}
		}
	}
	
	private static void writeNew(String spanFile, String lfsFile, String outputFile) {
		List<String[]> spanRows = CsvManipulation.readWholeFile(Paths.get(spanFile), true);
		
		HashMap<Integer, Boolean> isFraudMap = new HashMap<>();
		for (String[] row : spanRows) {
			int id = Integer.parseInt(row[0]);
			boolean isFraud = ROC_builder.isFraud(row[1]);
			isFraudMap.put(id, isFraud);
		}

		ArrayList<Pair<Double, Integer>> spanFraudScores = getOrderedPairs(spanFile, 3);
		ArrayList<Pair<Double, Integer>> lfsFraudScores = getOrderedPairs2(lfsFile, 5);
		
		assert spanFraudScores.size() == lfsFraudScores.size();
		
		HashMap<Integer, Boolean> spanCorrectMap = getCorrectMap(spanFraudScores, isFraudMap, 0.01);
		HashMap<Integer, Boolean> lfsCorrectMap = getCorrectMap(lfsFraudScores, isFraudMap, 0.01);
		
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
			bw.append("id,span,lfs");
			bw.newLine();
			for (Integer id : spanCorrectMap.keySet()) {
				int spanCorrect = spanCorrectMap.get(id) ? 1 : 0;
				int lfsCorrect = lfsCorrectMap.get(id) ? 1 : 0;
				bw.append(id + ",").append(spanCorrect+",").append(lfsCorrect+ "");
				bw.newLine();
			}
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static ArrayList<Pair<Double, Integer>> getOrderedPairs(String filePath, int column) {
		ArrayList<Pair<Double, Integer>> pairs = new ArrayList<>();
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(filePath), true);
		for (String[] row : rows) {
			int id = Integer.parseInt(row[0]);
			double fraudBelief = Double.parseDouble(row[column]);
			
			Pair<Double, Integer> givenActualPairs = new Pair<>(fraudBelief, id);
			pairs.add(givenActualPairs);
		}
		
		Collections.shuffle(pairs);
		
		Collections.sort(pairs, new Comparator<Pair<Double, Integer>>() {
			@Override
			public int compare(Pair<Double, Integer> pair1, Pair<Double, Integer> pair2) {
				int compare1 = Double.compare(pair2.getKey(), pair1.getKey()); // descending order
				return compare1;
			}
		});
		
		return pairs;
	}
	private static ArrayList<Pair<Double, Integer>> getOrderedPairs2(String filePath, int column) {
		ArrayList<Pair<Double, Integer>> pairs = new ArrayList<>();
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(filePath), true);
		for (String[] row : rows) {
			int id = Integer.parseInt(row[0]);
			double fraudBelief = Double.parseDouble(row[column]);
			
			Pair<Double, Integer> givenActualPairs = new Pair<>(1d - fraudBelief, id);
			pairs.add(givenActualPairs);
		}
		
		Collections.shuffle(pairs);
		
		Collections.sort(pairs, new Comparator<Pair<Double, Integer>>() {
			@Override
			public int compare(Pair<Double, Integer> pair1, Pair<Double, Integer> pair2) {
				int compare1 = Double.compare(pair2.getKey(), pair1.getKey()); // descending order
				return compare1;
			}
		});
		
		return pairs;
	}
	
	private static HashMap<Integer, Boolean> getCorrectMap(ArrayList<Pair<Double, Integer>> sortedFraudScores, HashMap<Integer, Boolean> isFraudMap, double FPR) {
		HashMap<Integer, Boolean> correctMap = new HashMap<>();
		
		int threshold = (int) (FPR * sortedFraudScores.size());
		
		int wrong = 0;
		for (Pair<Double, Integer> score : sortedFraudScores) {
			Integer id = score.getValue();
			Boolean isFraud = isFraudMap.get(id);
			
			if (!isFraud)
				wrong++;
			
			if (wrong >= threshold) { // passed threshold. everything should be normal
				correctMap.put(id, isFraud == false);
			} else { // before threshold, everything should be fraudulent
				correctMap.put(id, isFraud == true);
			}
		}
		
		return correctMap;
	}
	
}
