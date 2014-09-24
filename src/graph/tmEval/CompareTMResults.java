package graph.tmEval;

import graph.EdgeType;
import graph.GraphOperations;
import graph.tmEval.UseMRF_TM.OfInterestStats;
import graph.tmEval.visualise.Visualise;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import simulator.database.DBConnection;
import util.CsvManipulation;
import util.CsvManipulation.CsvThing;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;

/**
 * Compare the users found as fraudulent by SPAN and NetProbe.
 */
public class CompareTMResults {

	public static void main(String[] args) {
//		splitIntoGroups();
		
		
//		compareAll();
		divideGraphByGroups();
		
//		makeDeepGraphs();
	}
	
	public static void compareAll() {
		Groups groups = splitIntoGroups(); 
		List<String> groupNames = Arrays.asList("inBoth", "inSpanOnly", "inNpOnly", "inNone");
		
		System.out.println(groups.inBoth.size() + "," + groups.inSpanOnly.size() + "," + groups.inNpOnly.size() + "," + groups.inNone.size());
		
		List<String> fileNames = Arrays.asList("userFeatures", "bidderGraphFeatures", "sellerGraphFeatures");
		int csvCount = 0;
		for (CsvThing csvThing : new CsvThing[]{
				CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/TradeMeUserFeatures_11Features.csv"),
				CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_bidderGraphFeatures_all.csv"),
				CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_sellerGraphFeatures_all.csv"),}) {
//			try {
//				BufferedWriter bw = Files.newBufferedWriter(Paths.get("F:/workstuff2011/AuctionSimulation/graphFeatures/" + fileNames.get(csvCount) + ".csv"), Charset.defaultCharset());
//				bw.write(Joiner.on(",").join(csvThing.headingRow) + ",group");
//				
//				int groupCount = 0;
//				for (Set<Integer> group : new Set[] { groups.inBoth, groups.inSpanOnly, groups.inNpOnly, groups.inNone }) {
//					
//					System.out.println("Group " + csvCount + " of size " + group.size());
//					ArrayListMultimap<Integer, Double> featureValues = csvThing.featureValues;
//					bw.newLine();
//					for (Integer idInGroup : group) {
//						if (csvThing.featureValues.containsKey(idInGroup)) {
////							System.out.println(idInGroup + "," + csvThing.featureValues.get(idInGroup));
//							bw.write(idInGroup + "," + Joiner.on(",").useForNull("").join(csvThing.featureValues.get(idInGroup)) + "," + groupNames.get(groupCount));
//							bw.newLine();
//						}
//					}
//					bw.flush();
//					groupCount++;
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			csvCount++;
			
			Map<String, Double> bothAvg = CompareFeatureValues.groupAverage(csvThing, groups.inBoth, true);
			Map<String, Double> spanAvg = CompareFeatureValues.groupAverage(csvThing, groups.inSpanOnly, true);
			Map<String, Double> npAvg = CompareFeatureValues.groupAverage(csvThing, groups.inNpOnly, true);
			Map<String, Double> noneAvg = CompareFeatureValues.groupAverage(csvThing, groups.inNone, true);
			System.out.println(UseMRF_TM.printMaps(bothAvg, spanAvg, npAvg, noneAvg));
		}

//		writeGroupsToFile(groups, groupNames);
	}
	
	private static void divideGraphByGroups() {
		Groups groups = splitIntoGroups(); 
		List<String> groupNames = Arrays.asList("inBoth", "inSpanOnly", "inNpOnly", "inNone");
		
		String spanScoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_trademe_0.2,0.45,0.12,0.32.csv";
		HashMap<Integer, Double> spanScores = UseMRF_TM.readScores(spanScoreFilepath).fraudScores;

		String npScoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_NetProbe_trademe_0.04,0.12.csv";
		HashMap<Integer, String> npScores = UseMRF_TM_NetProbe.readScores(npScoreFilepath).classLabels;
		
		Connection conn = DBConnection.getConnection("trademe");
		TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		ImmutableMap<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.PARTICIPATE));
		ImmutableMap<Integer, Multiset<Integer>> winGraph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.WIN));
		
		int groupCount = 0;
		for (Set<Integer> group : new Set[] { groups.inBoth, groups.inSpanOnly, groups.inNpOnly, groups.inNone }) {
			HashMap<Integer, String> vertexStyles = new HashMap<>();
			HashMap<Integer, Double> fakeFraudScores = new HashMap<>();
			for (Integer id : group) {
				boolean isSpanFraud = spanScores.get(id) > 0.5;
				String npType = npScores.get(id);
				vertexStyles.put(id, colourString(isSpanFraud, npType));
				
//				if (isSpanFraud || !npType.equals("NORMAL")) {
					fakeFraudScores.put(id, 1d);
//				} else {
//					fakeFraudScores.put(id, 0d);
//				}
			}
			
			for (Integer id : group) {
				if (id != 2115794 && id != 1264609 && id != 41007)
					continue;
				try {
//					Visualise.makeGraph(id, graph, vertexStyles, winGraph, "F:/workstuff2011/AuctionSimulation/tmFraudGraphs_Picked/");
					Visualise.makeGraph(id, graph, vertexStyles, winGraph, "F:/workstuff2011/AuctionSimulation/tmFraudGraphs_" + groupNames.get(groupCount) + "/");
				} catch (Exception e) {
					e.printStackTrace();
				}
//				if (id.equals(133005)) {
//					boolean isSpanFraud2 = spanScores.get(id) > 0.5;
//					String npType2 = npScores.get(id);
//					System.out.println(colourString(isSpanFraud2, npType2));
//				}
			}
			
			groupCount++;
		}
	}
	
	private static void makeDeepGraphs() {
		String spanScoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_trademe_0.2,0.45,0.12,0.32.csv";
		HashMap<Integer, Double> spanScores = UseMRF_TM.readScores(spanScoreFilepath).fraudScores;

		String npScoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_NetProbe_trademe_0.04,0.12.csv";
		HashMap<Integer, String> npScores = UseMRF_TM_NetProbe.readScores(npScoreFilepath).classLabels;
		
		Connection conn = DBConnection.getConnection("trademe");
		TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		ImmutableMap<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.PARTICIPATE));
		ImmutableMap<Integer, Multiset<Integer>> winGraph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.WIN));
		
		HashMap<Integer, String> vertexStyles = new HashMap<>();
		HashMap<Integer, Double> fakeFraudScores = new HashMap<>();
		for (Integer id : Sets.union(spanScores.keySet(), npScores.keySet())) {
			boolean isSpanFraud = spanScores.get(id) > 0.5;
			String npType = npScores.get(id);
			vertexStyles.put(id, colourString(isSpanFraud, npType));
			
			if (isSpanFraud || !npType.equals("NORMAL")) {
				fakeFraudScores.put(id, 1d);
			} else {
				fakeFraudScores.put(id, 0d);
			}
		}
		
		Visualise.boundGraph(graph, fakeFraudScores, winGraph, vertexStyles, "F:/workstuff2011/AuctionSimulation/tmFraudGraphsDepth2/", 2);

	}
	
	private static String colourString(boolean isSpanFraud, String npType) {
		String vertexStyle;
		if (isSpanFraud && npType.equals("NORMAL")) { // span only
			vertexStyle = "fillColor=#7777FF;"; // blue
		} else if (isSpanFraud && npType.equals("ACCOMPLICE")) { // span and np
			vertexStyle = "fillColor=#FFAAAA;"; // light red
		} else if (isSpanFraud && npType.equals("FRAUD")) { // span and np
			vertexStyle = "fillColor=#FF4444;"; // dark red
		} else if (!isSpanFraud && npType.equals("ACCOMPLICE")) { // np only
			vertexStyle = "fillColor=#22DD22;"; // light green
		} else if (!isSpanFraud && npType.equals("FRAUD")) { // np only
			vertexStyle = "fillColor=#00AA00;"; // dark green
		} else if (!isSpanFraud && npType.equals("NORMAL")) { // none
			vertexStyle = "fillColor=#FFFFFF;"; // white
		} else {
			vertexStyle = "";
			assert false;
		}
		return vertexStyle;
	}
	
	private static void writeGroupsToFile(Groups groups, List<String> groupNames) {
		HashSet<Integer> all = new HashSet<>();
		all.addAll(groups.inBoth); 
		all.addAll(groups.inSpanOnly);
		all.addAll(groups.inNpOnly);
		all.addAll(groups.inNone);
		
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures/";
		HashMap<Integer, OfInterestStats> spanStats = UseMRF_TM.stats(0.5, Sets.union(groups.inSpanOnly, groups.inBoth), all);
		writeGroupsToFileHelper(groups, groupNames, spanStats, directory + "spanStats_grouped.csv");
		
		HashMap<Integer, OfInterestStats> npStats = UseMRF_TM_NetProbe.stats(0.5, Sets.union(groups.inNpOnly, groups.inBoth), all);
		writeGroupsToFileHelper(groups, groupNames, npStats, directory + "npStats_grouped.csv");
	}
	
	private static void writeGroupsToFileHelper(Groups groups, List<String> groupNames, HashMap<Integer, OfInterestStats> stats, String filepath) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(filepath), Charset.defaultCharset());
			
			bw.write("id," + OfInterestStats.headings() + ",group");
			bw.newLine();
			
			int groupCount = 0;
			for (Set<Integer> group : new Set[] { groups.inBoth, groups.inSpanOnly, groups.inNpOnly, groups.inNone,}) {
				for (Integer id : group) {
					if (stats.containsKey(id)) {
						bw.write(id + "," + stats.get(id).toString() + "," + groupNames.get(groupCount));
						bw.newLine();
					} else {
						System.out.println(groupNames.get(groupCount) + " missing " + id);
					}
				}
				groupCount++;
			}			
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean npIsFraud(double[] fraudScores) {
		return fraudScores[2] < 0.5;
	}
	
	private static boolean spanIsFraud(double fraudScore) {
		return fraudScore > 0.5;
	}
	
	public static class Groups {
		final Set<Integer> inBoth;
		final Set<Integer> inSpanOnly;
		final Set<Integer> inNpOnly;
		final Set<Integer> inNone;
		
		Groups(Set<Integer> inBoth, Set<Integer> inSpanOnly, Set<Integer> inNpOnly, Set<Integer> inNone) {
			this.inBoth = inBoth;
			this.inSpanOnly = inSpanOnly;
			this.inNpOnly = inNpOnly;
			this.inNone = inNone;
		}
	}
	
	public static Groups splitIntoGroups() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/";
		
		String npScoreFilepath = directory + "finalScores_NetProbe_trademe_0.04,0.12.csv";
		UseMRF_TM_NetProbe.Scores npScores = UseMRF_TM_NetProbe.readScores(npScoreFilepath);
		HashMap<Integer, double[]> npScoreMap = npScores.fraudScores;
		
		String spanScoreFilepath = directory + "finalScores_trademe_0.2,0.45,0.12,0.32.csv";
		UseMRF_TM.Scores spanScores = UseMRF_TM.readScores(spanScoreFilepath);
		HashMap<Integer, Double> spanScoreMap = spanScores.fraudScores;
		
		HashSet<Integer> allIds = new HashSet<>();
		
		allIds.addAll(npScoreMap.keySet());
		allIds.addAll(spanScoreMap.keySet());
		
//		for (Integer id : allIds) {
//			if (!npScoreMap.containsKey(id))
//				System.out.println("np," + id);
//			if (!spanScoreMap.containsKey(id))
//				System.out.println("span" + id);
//		}
		
		Set<Integer> inBoth = new HashSet<>();
		Set<Integer> inSpanOnly = new HashSet<>();
		Set<Integer> inNpOnly = new HashSet<>();
		Set<Integer> inNone = new HashSet<>();
		
		for (Integer id : allIds) {
//			Double asdf = spanScoreMap.get(id);
//			double[] qwer = npScoreMap.get(id);
			boolean isSpanFraud = spanIsFraud(spanScoreMap.get(id)); 
			boolean isNpFraud = npIsFraud(npScoreMap.get(id)); 
			if (isSpanFraud && isNpFraud) {
				inBoth.add(id);
			} else if (isSpanFraud && !isNpFraud) {
				inSpanOnly.add(id);
			} else if (!isSpanFraud && isNpFraud) {
				inNpOnly.add(id);
			} else {
				inNone.add(id);
			}
		}
		
		return new Groups(inBoth, inSpanOnly, inNpOnly, inNone);
	}
	
}
