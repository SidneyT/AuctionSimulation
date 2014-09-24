package graph.tmEval;

import graph.EdgeType;
import graph.GraphOperations;
import graph.MRFv2;
import graph.MRFv2.Node;
import graph.MRFv2.State;
import graph.tmEval.visualise.Visualise;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import simulator.database.DBConnection;
import util.CsvManipulation;
import util.CsvManipulation.CsvThing;
import util.IncrementalMean;
import util.IncrementalSD;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildTMFeatures;

/**
 * Analog of UseMRF, but for TradeMe data 
 */
public class UseMRF_TM {
	
	public static void main(String[] args) {
//		for (int i = 0; i < 30; i++)
//			tmRun();
		
//		for (int i = 0; i < 11; i++)
//			compareSuspiciousGroups((double) i / 10);
		compareSuspiciousGroups();
		
//		changeInMembership();
	}
	
	/**
	 * Counts the number of users that are outliers that are classified as fraudulent, and vice versa.
	 */
	public static void changeInMembership() {
		String scoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_trademe_0.12,0.32.csv";
		Scores scores = readScores(scoreFilepath);
		HashMap<Integer, Double> outlierScores = scores.outlierScores;
		HashMap<Integer, Double> fraudScores = scores.fraudScores;

		int outlierThenFraud = 0;
		int outlierThenNotFraud = 0;
		int notOutlierThenFraud = 0;
		int notOutlierThenNotFraud = 0;
		
		for (int key : outlierScores.keySet()) {
			Double outlierScore = outlierScores.get(key);
			Double fraudScore = fraudScores.get(key);
			
			boolean isOutlier = outlierScore > 0.5;
			boolean isFraud = fraudScore > 0.5;
			
			if (isOutlier && isFraud)
				outlierThenFraud++;
			else if (isOutlier && !isFraud)
				outlierThenNotFraud++;
			else if (!isOutlier && isFraud)
				notOutlierThenFraud++;
			else if (!isOutlier && !isFraud)
				notOutlierThenNotFraud++;
		}
		
		System.out.println(outlierThenFraud + "," + outlierThenNotFraud + "," + notOutlierThenFraud + "," + notOutlierThenNotFraud);
		
	}
	
	public static HashMap<Integer, OfInterestStats> stats(double threshold, Set<Integer> frauds, Set<Integer> wantedIds) {
		Connection conn = DBConnection.getConnection("trademe");
		TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		ImmutableMap<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.PARTICIPATE));
		ImmutableMap<Integer, Multiset<Integer>> winGraph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.WIN));

		final HashMap<Integer, Integer> ofInterest = new HashMap<>();
		HashMap<Integer, OfInterestStats> ofInterestStats = new HashMap<>();

		for (Integer wanted : wantedIds) {
			if (!graph.containsKey(wanted))
				continue;
			Multiset<Integer> wantedNeighbours = graph.get(wanted);
			
			int totalWeight = wantedNeighbours.size();
			int fraudWeight = 0;
			int edgeCount = wantedNeighbours.elementSet().size();
			int fraudEdgeCount = 0;
			int weightBetweenNeighbours = 0;
			int winCount = 0;
			for (Integer neighbour : wantedNeighbours.elementSet()) {
				int weight = wantedNeighbours.count(neighbour);
				
				if (frauds.contains(neighbour)) {
					fraudWeight += weight;
					fraudEdgeCount++;
				}
				
				if (graph.containsKey(neighbour)) {
					for (Integer neighbour2 : graph.get(neighbour)) {
						if (wantedNeighbours.contains(neighbour2))
							weightBetweenNeighbours++;
					}
				}

				if (winGraph.containsKey(wanted)) {
					winCount += winGraph.get(wanted).count(neighbour);
				}
			}
			
//			if (edgeCount >= 2) {
				ofInterest.put(wanted, fraudWeight);
				ofInterestStats.put(wanted, new OfInterestStats(totalWeight, fraudWeight, edgeCount, fraudEdgeCount, winCount, weightBetweenNeighbours));
//			}
		}
		//		Visualise.bigGraph(graph, fraudScores, winGraph);
		return ofInterestStats;
	}
	
	
	public static HashMap<Integer, OfInterestStats> compareSuspiciousGroups() {
		return compareSuspiciousGroups(0.5);
	}
	public static HashMap<Integer, OfInterestStats> compareSuspiciousGroups(double threshold) {
		String scoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_trademe_0.2,0.45,0.12,0.32.csv";
		Scores scores = readScores(scoreFilepath);
		HashMap<Integer, Double> fraudScores = scores.fraudScores;
		HashMap<Integer, Double> outlierScores = scores.outlierScores;

		HashSet<Integer> frauds = new HashSet<>();
		for (Integer id : fraudScores.keySet()) {
//			if (fraudScores.get(id) >= threshold)
			if (fraudScores.get(id) >= 0.5)
//			if (outlierScores.get(id) >= 0.5)
				frauds.add(id);
		}
		
		HashMap<Integer, String> vertexStyles = new HashMap<>();
		for (Integer id : fraudScores.keySet()) {
			String vertexStyle;
			if (fraudScores.get(id) < 0.5) {
				vertexStyle = "fillColor=#8CCFFF;";
			} else {
				vertexStyle = "fillColor=#F78B97;";
			}
			vertexStyles.put(id, vertexStyle);
		}
		
		final HashMap<Integer, OfInterestStats> ofInterestStats = stats(threshold, frauds, frauds);
		
		String picDirectory = "F:/workstuff2011/AuctionSimulation/tmFraudGraphs/";
		// write the egonet diagrams to file
		for (int testId : ofInterestStats.keySet()) {
			try {
//				Visualise.makeGraph(testId, graph, vertexStyles, winGraph, picDirectory);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Predicate<Integer> isFraudPredicate = new Predicate<Integer>() {
			public boolean apply(Integer number) {
				return ofInterestStats.keySet().contains(number);
			}
		};
		Predicate<Integer> isNormalPredicate = Predicates.not(isFraudPredicate);
		
		// compare the statistics of fraud and non fraud groups
		CsvManipulation.CsvThing userCsvThing = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/TradeMeUserFeatures_11Features.csv");
		Map<String, Double> fraudUserAverages = CompareFeatureValues.groupAverage(userCsvThing, Iterables.filter(userCsvThing.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalUserAverages = CompareFeatureValues.groupAverage(userCsvThing, Iterables.filter(userCsvThing.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(printMaps(fraudUserAverages, normalUserAverages));
		
		CsvManipulation.CsvThing graphCsvThingBidders = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_bidderGraphFeatures_all.csv");
		Map<String, Double> fraudGraphAveragesBidders = CompareFeatureValues.groupAverage(graphCsvThingBidders, Iterables.filter(graphCsvThingBidders.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalGraphAveragesBidders = CompareFeatureValues.groupAverage(graphCsvThingBidders, Iterables.filter(graphCsvThingBidders.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(printMaps(fraudGraphAveragesBidders, normalGraphAveragesBidders));
		
		CsvManipulation.CsvThing graphCsvThingSellers = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_sellerGraphFeatures_all.csv");
		Map<String, Double> fraudGraphAveragesSellers = CompareFeatureValues.groupAverage(graphCsvThingSellers, Iterables.filter(graphCsvThingSellers.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalGraphAveragesSellers = CompareFeatureValues.groupAverage(graphCsvThingSellers, Iterables.filter(graphCsvThingSellers.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(printMaps(fraudGraphAveragesSellers, normalGraphAveragesSellers));
		
		System.out.println("number of interesting frauds " + ofInterestStats.size());

		// print out values for each egonet
		for (Integer key : ofInterestStats.keySet()) {
//			System.out.println(key + "," + ofInterestStats.get(key));
		}
		
		return ofInterestStats;
	}
	
//	static String printMaps(Map<String, ?> map1, Map<String, ?> map2) {
//		StringBuffer sb = new StringBuffer();
//		for (String key : Sets.union(map1.keySet(), map2.keySet())) {
//			sb.append(key + ",");
//			if (map1.containsKey(key))
//				sb.append(map1.get(key));
//			sb.append(",");
//			if (map2.containsKey(key))
//				sb.append(map2.get(key));
//			sb.append("\r\n");
//		}
//		return sb.toString();
//	}
	
	@SafeVarargs
	static String printMaps(Map<String, ?>... maps) {
		StringBuffer sb = new StringBuffer();
		
		HashSet<String> allIds = new HashSet<>();
		for (Map<String, ?> map : maps) {
			allIds.addAll(map.keySet());
		}
		
		for (String key : allIds) {
			sb.append(key + "");
			for (Map<String, ?> map : maps) {
				if (map.containsKey(key)) {
					sb.append(",");
					sb.append(map.get(key));
				}
			}
			sb.append("\r\n");
		}
		return sb.toString();
	}
	
	public static class OfInterestStats {
		final int totalWeight;
		final int fraudWeight;
		final int edgeCount;
		final int weightBetweenNeighbours;
		final int fraudEdgeCount;
		final int winCount;
		
		OfInterestStats(int totalWeight, int fraudWeight, int edgeCount, int fraudEdgeCount, int winCount, int weightBetweenNeighbours) {
			this.totalWeight = totalWeight;
			this.fraudWeight = fraudWeight;
			this.edgeCount = edgeCount;
			this.fraudEdgeCount = fraudEdgeCount;
			this.weightBetweenNeighbours = weightBetweenNeighbours;
			this.winCount = winCount;
		}
		
		public static String headings() {
			return "totalWeight,fraudWeight,edgeCount,fraudEdgeCount,winCount,weightBetweenNeighbours";
		}
		@Override
		public String toString() {
			return totalWeight + "," + fraudWeight + "," + edgeCount + "," + fraudEdgeCount + "," + winCount + "," + weightBetweenNeighbours;
		}
	}
	
	public static Scores readScores(String scoreFilepath) {
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(scoreFilepath), true);
		
		HashMap<Integer, Double> outlierScores = new HashMap<>();
		HashMap<Integer, Double> fraudScores = new HashMap<>();
		
		for (String[] row : rows) {
			int id = Integer.parseInt(row[0]);
			double outlierScore = MRFv2.normaliseOutlierScore(Double.parseDouble(row[2]));
			double fraudScore = Double.parseDouble(row[3]);
			
			outlierScores.put(id, outlierScore);
			fraudScores.put(id, fraudScore);
			
		}
		
		// count the number of users with their scores increase/decreased after propagation
		EnumMultiset<Change> changeCounts = EnumMultiset.create(Change.class);
		for (Integer id : outlierScores.keySet()) {
			changeCounts.add(change(outlierScores.get(id), fraudScores.get(id)));
		}
		System.out.println(changeCounts);
		
		
		return new Scores(outlierScores, fraudScores);
	}
	
	public static class Scores {
		final HashMap<Integer, Double> outlierScores;
		final HashMap<Integer, Double> fraudScores;
		Scores (HashMap<Integer, Double> outlierScores, HashMap<Integer, Double> fraudScores) {
			this.outlierScores = outlierScores;
			this.fraudScores = fraudScores;
		}
	}
	
	public static Change change(double before, double after) {
		if (before > after)
			return Change.DOWN;
		else if (after > before)
			return Change.UP;
		else
			return Change.SAME;
	}
	
	public enum Change {
		UP, DOWN, SAME;
	}
	
	
	/**
	 * Run propagation over the TradeMe data.
	 */
	private static void tmRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		TreeMap<String, IncrementalMean> avgDiff = new TreeMap<>();

		MRFv2.paramO = 0.2;
		MRFv2.paramP = 0.4;
//		MRFv2.paramP = .45;
		
		MRFv2.e = 0.12;
		MRFv2.e2 = 0.32;
		
		for (int i = 0; i < 1; i++) {
			UseMRF_TM useMRF = new UseMRF_TM( 
					directory + "combinedLof/combinedLof_trademe_jit_bidder.csv", 
					directory + "combinedLof/combinedLof_trademe_jit_seller.csv");
			HashMap<Integer, Node> fraudBeliefs = useMRF.run("trademe");
			
			String outputFilename = directory + "finalScores/finalScores_trademe_" + MRFv2.paramO + "," + MRFv2.paramP + "," + MRFv2.e + "," + MRFv2.e2 + ".csv";
			useMRF.writeScores(outputFilename);
		}
		System.out.println(avgDiff);
	}

	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;
	TMAuctionIterator it;

	Connection conn = DBConnection.getConnection("trademe");
	public UseMRF_TM(String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
		this.it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		
		this.graph = new HashMap<>(GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.PARTICIPATE)));
		
		readOutlierScores(bidderOutlierScoresFile, sellerOutlierScoresFile);
	}
		
	private void readOutlierScores(String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
		HashMap<Integer, Double> scores = new HashMap<>();
		HashMap<Integer, String> userTypes = new HashMap<>();
		
		for (String file : new String[]{bidderOutlierScoresFile, sellerOutlierScoresFile}){
			List<String[]> lines = CsvManipulation.readWholeFile(Paths.get(file), true);
			for (String[] line : lines) {
				int id = Integer.parseInt(line[0]);
				String userType = line[1];
				userTypes.put(id, userType);
	
				double score = Double.parseDouble(line[2]);
				if (scores.containsKey(id)) {
					if (scores.get(id) < score) {
						scores.put(id, score);
					}
				} else {
					scores.put(id, score);
				}
			}
		}
		
		this.outlierScores = scores;
		this.userTypes = userTypes;

		// remove from the graph nodes which don't have a user profile (i.e. not completely crawled) 
		// remove nodes
		Iterator<Integer> nodeIt = graph.keySet().iterator();
		while (nodeIt.hasNext()) {
			Integer id = nodeIt.next();
			if (!outlierScores.containsKey(id)) {
				nodeIt.remove();
//				System.out.println("removing: " + id);
			}
		}
		// remove links to removed nodes
		for (Integer id : graph.keySet()) {
			Iterator<Integer> edgesIt = graph.get(id).iterator();
			HashMultiset<Integer> edgesToKeep = HashMultiset.create();
			while (edgesIt.hasNext()) {
				Integer edgeTo = edgesIt.next();
				if (outlierScores.containsKey(edgeTo)) {
					edgesToKeep.add(edgeTo);
				}
			}
			this.graph.put(id, edgesToKeep);
		}
		
		Iterator<Integer> outlierScoreIt = outlierScores.keySet().iterator();
		while(outlierScoreIt.hasNext()) {
			Integer id = outlierScoreIt.next();
			if (!graph.keySet().contains(id))
				outlierScoreIt.remove();
		}
		
	}
	
	Map<Integer, Node> nodeBeliefs;
	MRFv2 mrf;
	public HashMap<Integer, Node> run(String label) {
		mrf = new MRFv2(graph, outlierScores);
		HashMap<Integer, Node> fraudBeliefs = mrf.run(label);
		
		this.nodeBeliefs = fraudBeliefs;
		
		return fraudBeliefs;
	}
	
	public void writeScores(String outputPath) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			
			writer.append("id,userType,outlierScore,fraudScore");
			writer.newLine();
			
			Map<Integer, Double> outlierScores = mrf.origOutlierScores;
			
			// write id, userType, fraudScore
			for (Integer user : nodeBeliefs.keySet()) {
				writer.append(user.toString()).append(",");
				writer.append(userTypes.get(user)).append(",");
				writer.append(outlierScores.get(user).toString());
				Node node = nodeBeliefs.get(user);
				writer.append("," + node.beliefs[0]);
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String resultsLine() {
		TreeMap<String, Multiset<State>> stateCounts = new TreeMap<>();
		for (Integer user : nodeBeliefs.keySet()) {
			String userType = userTypes.get(user);
			if (!stateCounts.containsKey(userType)) {
				stateCounts.put(userType, HashMultiset.<State>create());
			}
			stateCounts.get(userType).add(nodeBeliefs.get(user).state);
		}
		
		StringBuffer sb = new StringBuffer();
		for (String userType : stateCounts.keySet()) {
			sb.append(userType);
			for (State state : State.values()) {
				sb.append("," + stateCounts.get(userType).count(state));
			}
			sb.append(",");
		}
		return sb.deleteCharAt(sb.length() - 1).toString();
	}
	

}
