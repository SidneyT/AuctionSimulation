package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.jfree.data.xy.XYSeries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;

import simulator.database.DBConnection;
import temporary.Chart;
import temporary.Chart.ChartOpt;
import util.Sample;
import util.SumStat;
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
//		Chart.XYLINE = false;
//		tmChart(new Chart("T- bidIn, undirected, neighbourCountVsWeight"), tmIt, EdgeType.undirected(EdgeType.PARTICIPATE), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		tmChart(new Chart("T- bidIn, neighbourCountVsWeight"), tmIt, EdgeType.PARTICIPATE, NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		tmChart(new Chart("T- bidIn reversed, neighbourCountVsWeight"), tmIt, EdgeType.reverse(EdgeType.PARTICIPATE), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		tmChart(new Chart("T- WIN undirected, neighbourCountVsWeight"), tmIt, EdgeType.undirected(EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		tmChart(new Chart("T- bidIn, neighbourCountVsWeight"), tmIt, (EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		tmChart(new Chart("T- bidIn reversed, neighbourCountVsWeight"), tmIt, EdgeType.reverse(EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		synChart(new Chart("S- bidIn reversed, neighbourCountVsWeight"), simIt, EdgeType.reverse(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- bidIn, neighbourCountVsWeight"), simIt, EdgeType.PARTICIPATE, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- bidIn, undirected, neighbourCountVsEgonetWeight"), simIt, EdgeType.undirected(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- win, neighbourCountVsEgonetWeight"), simIt, EdgeType.WIN, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- win, egonetEdgeCountVsEgonetWeight"), simIt, EdgeType.WIN, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- loss, neighbourCountVsEgonetWeight"), simIt, EdgeType.LOSS, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- loss, egonetEdgeCountVsEgonetWeight"), simIt, EdgeType.LOSS, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight).build();
//		synChart(new Chart("S- inSameAuction, undirected, egonetWeightVsJaccardMean", ChartOpt.NOLOGAXIS, ChartOpt.NOXYLINE), 
//				simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetWeight, NodeFeature.jaccard(SumStat.Mean)).build();
		
		Chart chart = new Chart("T,S- loss, undirected, neighbourCountVsEgonetWeight");
		synChart(chart, simIt, EdgeType.LOSS, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
		tmChart(chart, tmIt, EdgeType.LOSS, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
//		Chart chart = new Chart("T,S- inSameAuction, undirected, egonetWeightVsJaccardMean", ChartOpt.NOLOGAXIS, ChartOpt.NOXYLINE);
//		synChart(chart, simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetWeight, NodeFeature.jaccard(SumStat.Mean));
//		tmChart(chart, tmIt, EdgeType.IN_SAME_AUCTION, NodeFeature.EgonetWeight, NodeFeature.jaccard(SumStat.Mean));
//		Chart chart = new Chart("T,S- inSameAuction, undirected, neighbourCountVsEgonetWeight");
//		synChart(chart, simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
//		tmChart(chart, tmIt, EdgeType.IN_SAME_AUCTION, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeight);
//		Chart chart = new Chart("T,S- bidIn reversed, neighbourCountVsWeight");
//		synChart(chart, simIt, EdgeType.reverse(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart(chart, tmIt, EdgeType.reverse(EdgeType.PARTICIPATE), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		Chart chart = new Chart("T,S- bidIn, neighbourCountVsWeight");
//		synChart(chart, simIt, EdgeType.PARTICIPATE, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart(chart, tmIt, EdgeType.PARTICIPATE, NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		Chart chart = new Chart("T,S- win, neighbourCountVsEgonetWeight");
//		synChart(chart, simIt, EdgeType.WIN, "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart(chart, tmIt, EdgeType.WIN, NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		Chart chart = new Chart("T,S- win, undirected, neighbourCountVsEgonetWeight");
//		synChart(chart, simIt, EdgeType.undirected(EdgeType.WIN), "Puppet", NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);
//		tmChart(chart, tmIt, EdgeType.undirected(EdgeType.WIN), NodeFeature.NodeNeighbourCount, NodeFeature.EgonetWeight);

		chart.build();
		
	}
	
	/**
	 * Plot 2 features against each other for TM data.
	 * @param chartTitle
	 * @param tmIt
	 * @param edgeType
	 * @param xAxisFeature
	 * @param yAxisFeature
	 */
	private static Chart tmChart(Chart chart, TMAuctionIterator tmIt, EdgeTypeI edgeType, NodeFeatureI xAxisFeature, NodeFeatureI yAxisFeature) {
		HashMap<Integer, HashMultiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(tmIt.getIterator(), edgeType);
		HashMap<Integer, Double> xFeature = NodeFeature.values(graph, xAxisFeature);
		HashMap<Integer, Double> yFeature = NodeFeature.values(graph, yAxisFeature);
		
		List<double[]> xVsY = xVsY(xFeature, yFeature);
		xVsY = Sample.randomSample(xVsY, 2000, new Random());
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		chartAddSeries(chart, logBins.emptyCopy(), xVsY, "tm");
		return chart;
	}
	
	/**
	 * Plot 2 features against each other for Syn data.
	 * @param chartTitle
	 * @param simIt
	 * @param edgeType
	 * @param fraudType
	 * @param xAxisFeature
	 * @param yAxisFeature
	 */
	private static Chart synChart(Chart chart, SimDBAuctionIterator simIt, EdgeTypeI edgeType, String fraudType, NodeFeatureI xAxisFeature, NodeFeatureI yAxisFeature) {
		HashMap<Integer, HashMultiset<Integer>> allSellers = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.reverse(edgeType));
		HashMap<Integer, Double> xValues = NodeFeature.values(allSellers, xAxisFeature);
		HashMap<Integer, Double> yValues = NodeFeature.values(allSellers, yAxisFeature);
		return synFraudNormalSeries(chart, simIt, xValues, yValues, fraudType);
	}

	private static Chart synFraudNormalSeries(Chart chart, SimDBAuctionIterator simIt, HashMap<Integer, Double> feature1, HashMap<Integer, Double> feature2, String wantedUserType) {
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
		chartAddSeries(chart, logBins.emptyCopy(), xVsYNormal, "n");
		chartAddSeries(chart, logBins.emptyCopy(), xVsYFraud, "f");
		return chart;
	}
	
	/**
	 * Removes elements from the given collection which are not in the set <code>retain</code>.
	 * @param col
	 * @param retain
	 */
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
}
