package graph.tmEval;

import graph.EdgeType;
import graph.GraphOperations;
import graph.MRF_NetProbe;
import graph.MRFv2;
import graph.MRF_NetProbe.Node;
import graph.MRF_NetProbe.State;
import graph.tmEval.UseMRF_TM.Change;
import graph.tmEval.UseMRF_TM.OfInterestStats;
import graph.tmEval.UseMRF_TM.Scores;
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

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.SimDBAuctionIterator;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;

public class UseMRF_TM_NetProbe {
	public static void main(String[] args) throws IOException {
//		tmRun();
		compareSuspiciousGroups();
		
//		changeInMembership();
	}
	
	/**
	 * Counts the number of users that are outliers that are classified as fraudulent, and vice versa.
	 */
	public static void changeInMembership() {
		String scoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_NetProbe_trademe_0.2,0.05.csv";
		Scores scores = readScores(scoreFilepath);
		HashMap<Integer, Double> outlierScores = scores.outlierScores;
		HashMap<Integer, double[]> fraudScores = scores.fraudScores;

		int outlierThenFraud = 0;
		int outlierThenNotFraud = 0;
		int notOutlierThenFraud = 0;
		int notOutlierThenNotFraud = 0;
		
		for (int key : outlierScores.keySet()) {
			Double outlierScore = outlierScores.get(key);
			double[] fraudScore = fraudScores.get(key);
			
			boolean isOutlier = outlierScore > 0.5;
			boolean isFraud = fraudScore[2] < 0.5;
			
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
	
	private static void tmRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		MRF_NetProbe.eo = 0.20;
		MRF_NetProbe.ep = 0.05;
		
		UseMRF_TM_NetProbe useMRF = new UseMRF_TM_NetProbe(
				directory + "combinedLof/combinedLof_trademe_jit_bidder.csv", 
				directory + "combinedLof/combinedLof_trademe_jit_seller.csv");
		HashMap<Integer, Node> fraudBeliefs = useMRF.run("trademe_np");
		String outputFilename = directory + "finalScores/finalScores_NetProbe_trademe_" + MRF_NetProbe.eo + "," + MRF_NetProbe.ep + ".csv";
		useMRF.writeScores(outputFilename);
	}
	
	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;
	TMAuctionIterator it;
	
	Connection conn = DBConnection.getConnection("trademe");
	public UseMRF_TM_NetProbe(String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
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
				scores.put(id, score);
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
	MRF_NetProbe mrf;
	public HashMap<Integer, Node> run(String label) {
		mrf = new MRF_NetProbe(graph, outlierScores);
		HashMap<Integer, Node> nodeBeliefs = mrf.run(label);
		
		this.nodeBeliefs = nodeBeliefs;
		
		return nodeBeliefs;
	}
	
	public void writeScores(String outputPath) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			
			writer.append("id,userType,outlierScore,fraudBelief,accompliceBelief,normalBelief,fraudScore");
			writer.newLine();
			
			Map<Integer, Double> outlierScores = mrf.outlierScores();
			
			// write id, userType, fraudScore
			for (Integer user : nodeBeliefs.keySet()) {
				Node node = nodeBeliefs.get(user);
				
				writer.append(user.toString()).append(",");
				writer.append(userTypes.get(user)).append(",");
				writer.append(outlierScores.get(user) + ",");
				writer.append(node.beliefs[0] + ",");
				writer.append(node.beliefs[1] + ",");
				writer.append(node.beliefs[2] + ",");
				writer.append(node.state.toString());
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		return ofInterestStats;
	}
	
	public static HashMap<Integer, String> classToColor(HashMap<Integer, String> classLabels) {
		HashMap<Integer, String> vertexStyles = new HashMap<>();
		for (Integer id : classLabels.keySet()) {
			String vertexStyle;
			String classLabel = classLabels.get(id);
			if (classLabel.equals("NORMAL")) {
				vertexStyle = "fillColor=#8CCFFF;";
			} else if (classLabel.equals("FRAUD")) {
				vertexStyle = "fillColor=#F78B97;";
			} else if (classLabel.equals("ACCOMPLICE")) {
				vertexStyle = "fillColor=#CCE334;";
			} else {
				assert false;
				vertexStyle = "fillColor=#FFFFFF;";
			}
			vertexStyles.put(id, vertexStyle);
		}
		return vertexStyles;
	}
	
	public static HashMap<Integer, OfInterestStats> compareSuspiciousGroups() {
		return compareSuspiciousGroups(0.5);
	}
	public static HashMap<Integer, OfInterestStats> compareSuspiciousGroups(double threshold) {
//		String scoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_NetProbe_trademe_0.2,0.05.csv";
		String scoreFilepath = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/finalScores/finalScores_NetProbe_trademe_0.04,0.12.csv";
		Scores scores = readScores(scoreFilepath);
		HashMap<Integer, Double> outlierScores = scores.outlierScores;
		HashMap<Integer, double[]> fraudScores = scores.fraudScores;
		HashMap<Integer, String> classLabels = scores.classLabels;
		
		HashMap<Integer, String> vertexStyles = classToColor(classLabels);
		
		HashSet<Integer> frauds = new HashSet<>();
		for (Integer id : fraudScores.keySet()) {
			if (fraudScores.get(id)[2] < threshold) // if normal belief is lower than the threshold
//			if (fraudScores.get(id)[2] < fraudScores.get(id)[1] || fraudScores.get(id)[2] < fraudScores.get(id)[0])
				frauds.add(id);
		}
		
		final HashMap<Integer, OfInterestStats> ofInterestStats = stats(threshold, frauds, frauds);
		
//		Visualise.bigGraph(graph, fraudScores, winGraph);
		

		String picDirectory = "F:/workstuff2011/AuctionSimulation/tmFraudGraphs_np/";
		// write the egonet diagrams to file
//		for (int testId : sortedOfInterest.keySet()) {
		for (int testId : ofInterestStats.keySet()) {
			try {
//				if (testId==2213358)
//					Visualise.makeGraph(testId, graph, vertexStyles, winGraph, picDirectory);
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
		CsvThing userCsvThing = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/TradeMeUserFeatures_11Features.csv");
		Map<String, Double> fraudUserAverages = CompareFeatureValues.groupAverage(userCsvThing, Iterables.filter(userCsvThing.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalUserAverages = CompareFeatureValues.groupAverage(userCsvThing, Iterables.filter(userCsvThing.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(UseMRF_TM.printMaps(fraudUserAverages, normalUserAverages));
		
		CsvThing graphCsvThingBidders = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_bidderGraphFeatures_all.csv");
		Map<String, Double> fraudGraphAveragesBidders = CompareFeatureValues.groupAverage(graphCsvThingBidders, Iterables.filter(graphCsvThingBidders.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalGraphAveragesBidders = CompareFeatureValues.groupAverage(graphCsvThingBidders, Iterables.filter(graphCsvThingBidders.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(UseMRF_TM.printMaps(fraudGraphAveragesBidders, normalGraphAveragesBidders));
		
		CsvThing graphCsvThingSellers = CsvManipulation.readCsvFile("F:/workstuff2011/AuctionSimulation/graphFeatures/trademe_sellerGraphFeatures_all.csv");
		Map<String, Double> fraudGraphAveragesSellers = CompareFeatureValues.groupAverage(graphCsvThingSellers, Iterables.filter(graphCsvThingSellers.featureValues.keySet(), isFraudPredicate), true);
		Map<String, Double> normalGraphAveragesSellers = CompareFeatureValues.groupAverage(graphCsvThingSellers, Iterables.filter(graphCsvThingSellers.featureValues.keySet(), isNormalPredicate), true);
		System.out.println(UseMRF_TM.printMaps(fraudGraphAveragesSellers, normalGraphAveragesSellers));
		
		System.out.println("number of interesting frauds " + ofInterestStats.size());

		// print out values for each egonet
		for (Integer key : ofInterestStats.keySet()) {
			System.out.println(key + "," + ofInterestStats.get(key));
		}
		return ofInterestStats;
	}

	public static Scores readScores(String scoreFilepath) {
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(scoreFilepath), true);
		
		HashMap<Integer, Double> outlierScores = new HashMap<>();
		HashMap<Integer, double[]> fraudScores = new HashMap<>();
		HashMap<Integer, String> classLabels = new HashMap<>();
		
		for (String[] row : rows) {
			int id = Integer.parseInt(row[0]);
			double outlierScore = MRFv2.normaliseOutlierScore(Double.parseDouble(row[2]));
			double[] fraudScore = new double[]{Double.parseDouble(row[3]), Double.parseDouble(row[4]), Double.parseDouble(row[5])};
			String classLabel = row[6];
			
			outlierScores.put(id, outlierScore);
			fraudScores.put(id, fraudScore);
			classLabels.put(id, classLabel);
		}
		
		// count the number of users with their scores increase/decreased after propagation
//		EnumMultiset<Change> changeCounts = EnumMultiset.create(Change.class);
//		for (Integer id : outlierScores.keySet()) {
//			changeCounts.add(UseMRF_TM.change(outlierScores.get(id), fraudScores.get(id)));
//		}
//		System.out.println(changeCounts);
		
		
		return new Scores(outlierScores, fraudScores, classLabels);
	}
	
	public static class Scores {
		final HashMap<Integer, Double> outlierScores;
		final HashMap<Integer, double[]> fraudScores; // Map<id, double[fraud, accomplice, normal belief]>
		final HashMap<Integer, String> classLabels;
		Scores (HashMap<Integer, Double> outlierScores, HashMap<Integer, double[]> fraudScores, HashMap<Integer, String> classLabels) {
			this.outlierScores = outlierScores;
			this.fraudScores = fraudScores;
			this.classLabels = classLabels;
		}
	}
	
}
