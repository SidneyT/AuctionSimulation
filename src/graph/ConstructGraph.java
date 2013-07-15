package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.Pair;
import org.jfree.data.xy.XYSeries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

import simulator.database.DBConnection;
import temporary.Chart;
import util.SumStat;
import util.Util;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimDBAuctionIterator;

public class ConstructGraph {
	public static void main(String[] args) {
		run();
	}
	
	private static void run() {
		TMAuctionIterator tmIt = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation_delayedStart0"), true);
		
//		synUniqueVsAllSellers();
//		synUniqueVsAllBidders();
//		synEdgeVsWeightCount(simIt);
//		Chart.XYLINE = false;
//		jaccardChart();
//		tmEdgeVsWeightCount("bidIn, edgesVsWeight", tmIt, EdgeType.reverse(EdgeType.PARTICIPATE));
//		tmEdgeVsWeightCount("bidIn, edgesVsWeight", tmIt, EdgeType.PARTICIPATE);
//		tmEdgeVsWeightCount("bidIn, edgesVsWeight", tmIt, EdgeType.undirected(EdgeType.PARTICIPATE));
//		tmChart("T- bidIn undirected, neighbourCountVsWeight", tmIt, EdgeType.undirected(EdgeType.PARTICIPATE), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart("T- bidIn, neighbourCountVsWeight", tmIt, EdgeType.PARTICIPATE, NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart("T- bidIn reversed, neighbourCountVsWeight", tmIt, EdgeType.reverse(EdgeType.PARTICIPATE), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart("T- WIN undirected, neighbourCountVsWeight", tmIt, EdgeType.undirected(EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart("T- bidIn, neighbourCountVsWeight", tmIt, (EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart("T- bidIn reversed, neighbourCountVsWeight", tmIt, EdgeType.reverse(EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
		synChart("S- bidIn reversed, neighbourCountVsWeight", simIt, EdgeType.reverse(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart("S- bidIn, neighbourCountVsWeight", simIt, EdgeType.PARTICIPATE, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart("S- bidIn, undirected, neighbourCountVsEgonetWeight", simIt, EdgeType.undirected(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart("S- win, neighbourCountVsEgonetWeight", simIt, EdgeType.WIN, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart("S- win, egonetEdgeCountVsEgonetWeight", simIt, EdgeType.WIN, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
//		synChart("S- loss, neighbourCountVsEgonetWeight", simIt, EdgeType.LOSS, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart("S- loss, egonetEdgeCountVsEgonetWeight", simIt, EdgeType.LOSS, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
		synChart("S- inSameAuction, undirected, egonetWeightVsJaccardMean", simIt, EdgeType.undirected(EdgeType.IN_SAME_AUCTION), "Puppet", NodeFeature.EgonetWeight, NodeFeature.jaccard(SumStat.Mean));
	}
	
	private static void tmChart(String chartTitle, TMAuctionIterator tmIt, EdgeTypeI edgeType, NodeFeatureI xAxisFeature, NodeFeatureI yAxisFeature) {
		HashMap<Integer, HashMultiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(tmIt.getIterator(), edgeType);
		HashMap<Integer, Double> xFeature = NodeFeature.values(graph, xAxisFeature);
		HashMap<Integer, Double> yFeature = NodeFeature.values(graph, yAxisFeature);
		
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		Chart chart = new Chart(chartTitle);
		chartAddSeries(chart, logBins.emptyCopy(), xVsY(xFeature, yFeature), "");
		chart.build();
	}
	
	private static void tmEdgeVsWeightCount(String chartTitle, TMAuctionIterator tmIt, EdgeTypeI edgeType) {
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(tmIt.getIterator(), edgeType);
		HashMap<Integer, Double> edgeCounts = NodeFeature.values(allSellers, NodeFeature.EgonetEdgeCount);
		HashMap<Integer, Double> weights = NodeFeature.values(allSellers, NodeFeature.EgonetWeight);
		
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		Chart chart = new Chart();
		chartAddSeries(chart, logBins.emptyCopy(), xVsY(edgeCounts, weights), "");
		chart.build();
	}

	private static void synChart(String chartTitle, SimDBAuctionIterator simIt, EdgeTypeI edgeType, String fraudType, NodeFeatureI xAxisFeature, NodeFeatureI yAxisFeature) {
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.reverse(edgeType));
		HashMap<Integer, Double> edgeCounts = NodeFeature.values(allSellers, xAxisFeature);
		HashMap<Integer, Double> weights = NodeFeature.values(allSellers, yAxisFeature);
		synFraudNormalChart(simIt, chartTitle, edgeCounts, weights, fraudType);
	}

	private static void synEdgeVsWeightCount(SimDBAuctionIterator simIt) {
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.reverse(EdgeType.PARTICIPATE));
		HashMap<Integer, Double> edgeCounts = NodeFeature.values(allSellers, NodeFeature.EgonetEdgeCount);
		HashMap<Integer, Double> weights = NodeFeature.values(allSellers, NodeFeature.EgonetWeight);
		synFraudNormalChart(simIt, "edgeVsWeightCount", edgeCounts, weights, "PuppetSeller");
	}

	/**
	 * Compares the number of unique versus total number of bidders in all auctions submitted by each user.
	 */
	private static void synUniqueVsAllBidders() {
		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation_simple0"), true);
		HashMap<Integer, Set<Integer>> uniqueSellers = GraphOperations.adjacencySet(simIt.getIterator(), EdgeType.reverse(EdgeType.PARTICIPATE));
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.reverse(EdgeType.PARTICIPATE));
		ConstructGraph.synFraudNormalChartHelper(simIt, "unique VS all sellers", uniqueSellers, allSellers, "PuppetSeller");
	}
	
	/**
	 * Compares the number of unique versus total number of sellers whose auctions each bidder made a bid in.
	 */
	private static void synUniqueVsAllSellers() {
		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation_simple0"), true);
		HashMap<Integer, Set<Integer>> uniqueSellers = GraphOperations.adjacencySet(simIt.getIterator(), EdgeType.PARTICIPATE);
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.PARTICIPATE);
		ConstructGraph.synFraudNormalChartHelper(simIt, "unique VS all sellers", uniqueSellers, allSellers, "PuppetBidder");
	}
	
	/**
	 * Plots the average Jaccard index of a node and each of its neighbours. 
	 * Two series are plotted: one for normal users, and one for fraudulent.  
	 */
	private static void jaccardChart() {
		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation_simple0"), true);
		HashMap<Integer, HashMultiset<Integer>> edges1 = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.IN_SAME_AUCTION);
		HashMap<Integer, Double> jaccardIndices = jaccardIndex(edges1);
		System.out.println(jaccardIndices);
		Set<Integer> fraudIds = groupByUserType(simIt).get("PuppetBidder");
		
		HashMap<Integer, Double> jNormal = new HashMap<>(jaccardIndices);
		itRemoveOnly(jNormal.keySet(), fraudIds);
		System.out.println(jNormal);
		HashMap<Integer, Double> jFraud = new HashMap<>(jaccardIndices);
		itRetainOnly(jFraud.keySet(), fraudIds);
		System.out.println(jFraud);
		
		Chart chart = new Chart("Avg jaccard");
		chart.addSeries2(jNormal.values(), "normal");
		chart.addSeries2(jFraud.values(), "fraud");
		Chart.LOGAXIS = false;
		chart.build("", "");
	}
	
	private static <S extends Collection<Integer>, T  extends Collection<Integer>> void synFraudNormalChartHelper(SimDBAuctionIterator simIt, String chartTitle, HashMap<Integer, S> edges1x, HashMap<Integer, T> edges2x, String userType) {
		HashMap<Integer, Double> edges1 = new HashMap<>();
		for (Integer key : edges1x.keySet()) {
			edges1.put(key, Double.valueOf(edges1x.get(key).size()));
		}
		HashMap<Integer, Double> edges2 = new HashMap<>();
		for (Integer key : edges2x.keySet()) {
			edges2.put(key, Double.valueOf(edges2x.get(key).size()));
		}
		
		synFraudNormalChart(simIt, chartTitle, edges1, edges2, userType);
	}

	private static void synFraudNormalChart(SimDBAuctionIterator simIt, String chartTitle, HashMap<Integer, Double> feature1, HashMap<Integer, Double> feature2, String wantedUserType) {
		HashMultimap<String, Integer> userTypeGroups = groupByUserType(simIt);
		
		Set<Integer> fraudIds = new HashSet<>();
		for (String userType : userTypeGroups.keySet()) {
			if (userType.toLowerCase().contains(wantedUserType.toLowerCase())) {
				fraudIds.addAll(userTypeGroups.get(userType));
			}
		}
		
		HashMap<Integer, Double> edges1Normal = new HashMap<>(feature1);
		itRemoveOnly(edges1Normal.keySet(), fraudIds);
		HashMap<Integer, Double> edges1Fraud = new HashMap<>(feature1);
		itRetainOnly(edges1Fraud.keySet(), fraudIds);
		
		HashMap<Integer, Double> edges2Normal = new HashMap<>(feature2);
		itRemoveOnly(edges2Normal.keySet(), fraudIds);
		HashMap<Integer, Double> edges2Fraud = new HashMap<>(feature2);
		itRetainOnly(edges2Fraud.keySet(), fraudIds);
		
		List<double[]> xVsYNormal = xVsY(edges1Normal, edges2Normal);
		List<double[]> xVsYFraud = xVsY(edges1Fraud, edges2Fraud);
		
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		Chart chart = new Chart(chartTitle);
		chartAddSeries(chart, logBins.emptyCopy(), xVsYNormal, "normal");
		chartAddSeries(chart, logBins.emptyCopy(), xVsYFraud, "fraud");
		chart.build();
	}
	
	static HashMap<Pair<Integer, Integer>, Double> jaccardValues = new HashMap<>();
	
	/**
	 * Given the neighbours for every node in edges (<node, neighbours>). 
	 * Calculates the jaccard index for a user and all it's neighbours and calculates the mean value.
	 * @param edges
	 * @return
	 */
	private static HashMap<Integer, Double> jaccardIndex(HashMap<Integer, HashMultiset<Integer>> edges) {
		// jaccard = (A and B) / (A or B)
		
		HashMap<Integer, Double> meanJaccardMap = new HashMap<>();
		for (Integer bidder : edges.keySet()) {
			Set<Integer> bidderNeighbours = edges.get(bidder).elementSet(); // set of bidder's neighbours
			
			double[] neighbourJaccard = new double[bidderNeighbours.size()]; 
			int i = 0; // index
			
			for (Integer neighbour : bidderNeighbours) {
				Pair<Integer, Integer> pair;
				if (bidder < neighbour) // order the pair to always put the user with the smaller id first
					pair = new Pair<>(bidder, neighbour);
				else
					pair = new Pair<>(neighbour, bidder);

				Set<Integer> neighbourNeighbours;
				if (edges.containsKey(neighbour))
					neighbourNeighbours = edges.get(neighbour).elementSet(); // set of neighbour's neighbours
				else 
					neighbourNeighbours = Collections.emptySet();
					
				double jaccard;
				// calculate jaccard index
				if (jaccardValues.containsKey(pair)) // no need to re-calculate
					jaccard = jaccardValues.get(pair);
				else {
					int aAndB = Sets.intersection(bidderNeighbours, neighbourNeighbours).size();
					int aOrB = Sets.union(bidderNeighbours, neighbourNeighbours).size();
					jaccard = (double) aAndB / aOrB;
					jaccardValues.put(pair, jaccard);
				}
				neighbourJaccard[i++] = jaccard;
			}
			
			double meanJaccard = SumStat.Max.summaryValue(Doubles.asList(neighbourJaccard));
			meanJaccardMap.put(bidder, meanJaccard);
		}
		
		return meanJaccardMap;
	}
	
	
	
	
	public static <T> void itRetainOnly(Collection<T> col, Set<T> retain) {
		Iterator<T>  it = col.iterator();
		while (it.hasNext()) {
			T t = it.next();
			if (!retain.contains(t)) {
				it.remove();
			}
		}
	}
	public static <T> void itRemoveOnly(Collection<T> col, Set<T> remove) {
		Iterator<T>  it = col.iterator();
		while (it.hasNext()) {
			T t = it.next();
			if (remove.contains(t)) {
				it.remove();
			}
		}
	}
	
	private static HashMultimap<String, Integer> groupByUserType(SimDBAuctionIterator simIt) {
		HashMultimap<String, Integer> userTypeMultimap = HashMultimap.create();
		Map<Integer, UserObject> users = simIt.users();
		for (Entry<Integer, UserObject> pair : users.entrySet()) {
			userTypeMultimap.put(pair.getValue().userType, pair.getKey());
		}
		return userTypeMultimap;
	}
	
	/**
	 * Plots the points in xVsY using log-log axis, and also the mean and median using the bins in emptyLogBins.
	 * @param chart
	 * @param emptyLogBins
	 * @param xVsY
	 * @param label
	 */
	private static void chartAddSeries(Chart chart, LogarithmicBinning emptyLogBins, List<double[]> xVsY, String label) {
		emptyLogBins.addValues(xVsY);
		ArrayList<Double> binStarts = emptyLogBins.getBins();
		
		Random random = new Random();
		
		// plot all the points
		XYSeries series = new XYSeries(label);
		for (int i = 0; i < xVsY.size(); i++) {
//			series.add(xVsY.get(i)[0], xVsY.get(i)[1]);
			double jitterX = (random.nextDouble() - 0.5) / 5;
			double jitterY = (random.nextDouble() - 0.5) / 5;
			series.add(xVsY.get(i)[0] + jitterX, xVsY.get(i)[1] + jitterY);
		}
		chart.addSeries(series);

		
		// make the series for medians and means
		ArrayList<Double> medians = emptyLogBins.binMedians();
		ArrayList<Double> means = emptyLogBins.binMeans();
		XYSeries medianSeries = new XYSeries(label + "_median");
		XYSeries meanSeries = new XYSeries(label + "_mean");
		for (int i = 0; i < medians.size(); i++) {
			double median = medians.get(i);
			if (!Double.isNaN(median)) {
				medianSeries.add((double) binStarts.get(i), (double) medians.get(i));
				meanSeries.add((double) binStarts.get(i), (double) means.get(i));
			}
		}
		// add the mean/median series to the chart
		chart.addSeries(medianSeries);
		chart.addSeries(meanSeries);
	}
	
//	/**
//	 * Given the maps <id, value_1> and <id, value_2>, match the ids of the maps and returns List<int[]> with {value_1.size(), value_2.size()}.
//	 * @param adjacencyList1
//	 * @param adjacencyList2
//	 * @return
//	 */
//	private static <S extends Collection<Integer>, T extends Collection<Integer>> List<int[]> xVsYSize(HashMap<Integer, S> adjacencyList1, HashMap<Integer, T> adjacencyList2) {
//		List<int[]> xyPairs = new ArrayList<>();
//		
//		Set<Integer> allIds = new HashSet<Integer>(adjacencyList1.keySet());
//		allIds.addAll(adjacencyList2.keySet());
//		
//		for (Integer id : allIds) {
//			int x = 0;
//			if (adjacencyList1.containsKey(id)) {
//				x = adjacencyList1.get(id).size();
//			}
//			
//			int y = 0;
//			if (adjacencyList2.containsKey(id)) {
//				y = adjacencyList2.get(id).size();
//			}
//			
////			if (x == 1 && y > 40)
////				System.out.println("pause");
//			
//			if (x > 0 && y > 0)
//				xyPairs.add(new int[]{x, y});
//			
////			xyPairs.add(new int[]{x + 1, y + 1});
//			
//		}
//		
//		return xyPairs;
//	}
	
	private static List<double[]> xVsY(HashMap<Integer, Double> adjacencyList1, HashMap<Integer, Double> adjacencyList2) {
		List<double[]> xyPairs = new ArrayList<>();
		
		Set<Integer> allIds = new HashSet<Integer>(adjacencyList1.keySet());
		allIds.addAll(adjacencyList2.keySet());
		
		for (Integer id : allIds) {
			Double x = null;
			if (adjacencyList1.containsKey(id)) {
				x = adjacencyList1.get(id);
			}
			
			Double y = null;
			if (adjacencyList2.containsKey(id)) {
				y = adjacencyList2.get(id);
			}
			
			if (x != null && y != null) {
				xyPairs.add(new double[]{x, y});
//				
//				if (x < y)
//					System.out.println("wierd: " + id);
			}
			
			
		}
		
		return xyPairs;
	}
	
	private static void charting(HashMap<Integer, HashMultiset<Integer>> adjacencyList) {
		Frequency outgoingEdgesFreq = new Frequency();
		for (int bidderId : adjacencyList.keySet()) {
			int outEdges = adjacencyList.get(bidderId).size();
			outgoingEdgesFreq.addValue(outEdges);
		}
		
		Chart chart = new Chart();
		
		List<Integer> series = new ArrayList<Integer>();
		for (int i = 1; i < 100; i++) {
			int freq = (int) outgoingEdgesFreq.getCount(i);
			if (freq > 0)
			series.add(freq);
		}
			
		chart.addSeries(series, "blabhalhb");
		chart.build();
	}
}
