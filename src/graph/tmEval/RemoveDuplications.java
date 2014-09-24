package graph.tmEval;

import graph.outliers.OutlierDetection;
import graph.outliers.OutlierDetection.Triplet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import util.CsvManipulation;

import com.google.common.collect.ArrayListMultimap;

public class RemoveDuplications {

	public static void main(String[] args) throws IOException {
		modifyTM();
	}
	
	public static void modifyTM() throws IOException {
		String path = "F:/workstuff2011/AuctionSimulation/lof_features_fixed2/";
		removeDuplicates(path + "trademe_bidderGraphFeatures.csv", OutlierDetection.tmBidderFeatureTriplets);
		removeDuplicates(path + "trademe_sellerGraphFeatures.csv", OutlierDetection.tmSellerFeatureTriplets);
	}

	public static void removeDuplicates(String filePath, List<Triplet> featureTriplets) throws IOException {
		System.out.println(filePath);
		List<String[]> lines = CsvManipulation.readWholeFile(Paths.get(filePath), false);
		
		String[] columnHeadings = lines.get(0);
		
		for (Triplet triplet : featureTriplets) {
			int c1 = triplet.columns[0];
			int c2 = triplet.columns[1];
			String outputFilepath = filePath.replace(".csv", "") + "_" + c1 + "," + c2 + "_dedupe.csv";

			// group the users by their value pairs
			ArrayListMultimap<Pair<String, String>, Integer> map = ArrayListMultimap.create();
			for (int i = 1; i < lines.size(); i++) {
				String[] line = lines.get(i);
				Pair<String, String> featureValuePair = new Pair<>(line[c1], line[c2]);
				int id = Integer.parseInt(line[0]);
				map.put(featureValuePair, id);
			}
			
			// see how many have the same value.
			for (Pair<String, String> key : map.keySet()) {
				List<Integer> ids = map.get(key);
				if (ids.size() > 400) {
					System.out.print(triplet + ",<" + key.getKey() + "," + key.getValue() + ">,");
					System.out.print(ids.size());
					System.out.println();
				}
			}
			
			// write out the first 400 of ids with the same value pairs
			final int limit = 400;
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilepath), Charset.defaultCharset());
			writer.append("id," + columnHeadings[c1] + "," + columnHeadings[c2]);
			writer.newLine();
			
			HashSet<Integer> duplicatesRemoved = new HashSet<>();
			HashSet<Integer> manyDuplicates = new HashSet<>();
			
			for (Pair<String, String> key : map.keySet()) {
				String featureValue1 = key.getKey();
				String featureValue2 = key.getValue();
				List<Integer> ids = map.get(key);
				
				int limitCounter = 0;
				for (int id : ids) {
					if (++limitCounter <= limit) {
						writer.append(id + "," + featureValue1 + "," + featureValue2);
						writer.newLine();
					} else {
						duplicatesRemoved.add(id);
					}
					manyDuplicates.add(id);
				}
				if (manyDuplicates.size() <= 400) {
					manyDuplicates.clear();
				}
				
			}
			writer.flush();
			
			BufferedWriter writer2 = Files.newBufferedWriter(Paths.get(filePath.replace(".csv", "") + "_" + c1 + "," + c2 + "_removedIds.csv"), Charset.defaultCharset());
			writer2.append("removedId");
			writer2.newLine();
			for (Integer removedId : duplicatesRemoved) {
				writer2.append(removedId + "");
				writer2.newLine();
			}
			writer2.close();

			BufferedWriter writer3 = Files.newBufferedWriter(Paths.get(filePath.replace(".csv", "") + "_" + c1 + "," + c2 + "_manyDuplicates.csv"), Charset.defaultCharset());
			writer3.append("removedId");
			writer3.newLine();
			manyDuplicates.removeAll(duplicatesRemoved);
			for (Integer removedId : manyDuplicates) {
				writer3.append(removedId + "");
				writer3.newLine();
			}
			writer3.close();

			System.out.println("removed: " + duplicatesRemoved.size() + ", " + (lines.size() - duplicatesRemoved.size()) + " remaining, of these " + manyDuplicates.size() + " have many duplicates.");
		
		}
	}

}
