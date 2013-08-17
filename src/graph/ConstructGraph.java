package graph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.util.Pair;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jfree.data.xy.XYSeries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Ints;

import simulator.database.DBConnection;
import temporary.Chart;
import temporary.Chart.ChartOpt;
import util.Sample;
import util.SumStat;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimDBAuctionIterator;

public class ConstructGraph {
	public static void main(String[] args) {
//		makeCharts();
		run2();
//		eigenValueTest();
	}
	
	private static void eigenValueTest() {
		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
//		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_100k_test5"), true);
		EdgeTypeI edgeType = EdgeType.undirected(EdgeType.PARTICIPATE);
		HashMap<Integer, HashMultiset<Integer>> participateList = GraphOperations.duplicateAdjacencyList(it.getIterator(), edgeType);
		HashMap<Integer, HashMap<Integer, Multiset<Integer>>> egonets = NodeFeature.findAllEgonets(edgeType, participateList);
		
		HashMap<Integer, double[][]> matricies = new HashMap<>();
		for (int userId : egonets.keySet()) {
			HashMap<Integer, Multiset<Integer>> egonet = egonets.get(userId);
			// convert to matrix
			double[][] matrix = new double[egonet.size()][egonet.size()]; 
			List<Integer> ids = new ArrayList<>(egonet.keySet());
			for (int neighbour : egonet.keySet()) {
				int index = ids.indexOf(neighbour);
				for (int nneighbour : egonet.get(neighbour)) {
					matrix[index][ids.indexOf(nneighbour)]++;
				}
			}
			matricies.put(userId, matrix);
//			for (int i = 0; i < matrix.length; i++) {
//				System.out.println(Arrays.toString(matrix[i]));
//			}
//			System.out.println();
		}
		
		long t1 = System.nanoTime();
		
//		int breakI = 0;
		HashMap<Integer, Double> firstEigenvalues = new HashMap<>();
		for (int userId : matricies.keySet()) {
			
			double[][] matrix = matricies.get(userId);
			DoubleMatrix eigenValues = Eigen.symmetricEigenvalues(new DoubleMatrix(matrix));
			firstEigenvalues.put(userId, eigenValues.max());
			
//			System.out.println(userId + ", " + eigenValues.max());
//			
//			if (breakI++ > 100)
//				break;
			
		}
		
		long t2 = System.nanoTime() - t1;
		System.out.println(t2 / 1000000);
//		System.out.println(firstEigenvalues);
		
	}
	
	private static void run2() {
//		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_10k_test7"), true);
		
//		Iterator<Pair<SimAuction, List<BidObject>>> thing = it.getIterator();
//		int total = 0;
//		while (thing.hasNext()) {
//			int bidCount = thing.next().getValue().size();
//			total += bidCount;
//		}
//		System.out.println(total);
		
		// gives the number of auctions sold by the seller
		HashMap<Integer, HashMultiset<Integer>> winList = GraphOperations.duplicateAdjacencyList(it.getIterator(), EdgeType.reverse(EdgeType.WIN));
		HashMap<Integer, Double> sellerAuctionCount = NodeFeature.values(winList, NodeFeature.NodeWeightCount);
		HashMap<Integer, Double> sellerAuctionUniqueCount = NodeFeature.values(winList, NodeFeature.NodeEdgeCount);
//		TreeMultiset<Integer> frequencies = TreeMultiset.create();
//		for (Integer key : winList.keySet()) {
//			frequencies.add(winList.get(key).size());
//		}
//		for (int frequency : frequencies.elementSet()) {
//			System.out.println(frequency + ", " + frequencies.count(frequency));
//		}

		// gives the number of participations (unique and total) by bidders to a particular seller's auctions
		HashMap<Integer, HashMultiset<Integer>> participants = GraphOperations.duplicateAdjacencyList(it.getIterator(), EdgeType.reverse(EdgeType.PARTICIPATE));
		HashMap<Integer, Double> uniqueParticipantsCount = NodeFeature.values(participants, NodeFeature.NodeEdgeCount);
		HashMap<Integer, Double> totalParticipantsCount = NodeFeature.values(participants, NodeFeature.NodeWeightCount);

		System.out.println("start");
//		for (int uid : new TreeSet<>(participants.keySet())) {
//			System.out.println(uid + "," + winList.get(uid).size() + "," + uniqueParticipantsCount.get(uid) + "," + totalParticipantsCount.get(uid));
//		}
		
		ValueFrequencies valueFrequenciesA = new ValueFrequencies();
		for (Double value : sellerAuctionCount.values()) {
			valueFrequenciesA.addValue(value);
		}
		System.out.println(valueFrequenciesA.toString());
//		try {
//			BufferedWriter bw = Files.newBufferedWriter(Paths.get("output.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//			
//			for (Integer uid : participants.keySet()) {
//				bw.append(uid + "," + totalParticipantsCount.get(uid) + "," + uniqueParticipantsCount.get(uid));
////				bw.append(uid + "," + sellerAuctionCount.get(uid) + "," + sellerAuctionUniqueCount.get(uid));
//				bw.newLine();
//			}
//			
////			bw.write(valueFrequenciesA.toString());
//			bw.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public static class ValueFrequencies {
		public HashMultiset<Double> frequencies = HashMultiset.create();
		public void addValue(double value) {
			frequencies.add(value);
		}
		public void addValue(Double value) {
			frequencies.add(value);
		}
		public TreeMap<Double, Integer> get() {
			TreeMap<Double, Integer> valueFrequencies = new TreeMap<>();
			for (Double value : frequencies.elementSet()) {
				valueFrequencies.put(value, valueFrequencies.get(value));
			}
			return valueFrequencies;
		}
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (Double value : frequencies.elementSet()) {
				sb.append(value + "," + frequencies.count(value));
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	
	private static void makeCharts() {
		TMAuctionIterator tmIt = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
//		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation_delayedStart0"), true);
		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_100000"), true);
//		SimDBAuctionIterator simIt = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_4000"), true);
		
//		synUniqueVsAllSellers();
//		synUniqueVsAllBidders();
//		Chart.XYLINE = false;
//		tmChart(new Chart("T- bidIn, undirected, neighbourCountVsWeight"), tmIt, EdgeType.undirected(EdgeType.PARTICIPATE), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		tmChart(new Chart("T- bidIn, neighbourCountVsWeight"), tmIt, EdgeType.PARTICIPATE, NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		tmChart(new Chart("T- bidIn reversed, neighbourCountVsWeight"), tmIt, EdgeType.reverse(EdgeType.PARTICIPATE), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		tmChart(new Chart("T- WIN undirected, neighbourCountVsWeight"), tmIt, EdgeType.undirected(EdgeType.WIN), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		tmChart(new Chart("T- bidIn, neighbourCountVsWeight"), tmIt, (EdgeType.WIN), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		tmChart(new Chart("T- bidIn reversed, neighbourCountVsWeight"), tmIt, EdgeType.reverse(EdgeType.WIN), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
//		synChart(new Chart("S- bidIn reversed, neighbourCountVsWeight"), simIt, EdgeType.reverse(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- bidIn, neighbourCountVsWeight"), simIt, EdgeType.PARTICIPATE, "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- bidIn, undirected, neighbourCountVsEgonetWeight"), simIt, EdgeType.undirected(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- win, neighbourCountVsEgonetWeight"), simIt, EdgeType.WIN, "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- win, egonetEdgeCountVsEgonetWeight"), simIt, EdgeType.WIN, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- loss, neighbourCountVsEgonetWeight"), simIt, EdgeType.LOSS, "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- loss, egonetEdgeCountVsEgonetWeight"), simIt, EdgeType.LOSS, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount).build();
//		synChart(new Chart("S- inSameAuction, undirected, egonetWeightVsJaccardMean", ChartOpt.NOLOGAXIS, ChartOpt.NOXYLINE), 
//				simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetWeightCount, NodeFeature.jaccard(SumStat.Mean)).build();
		
		Chart chartA = new Chart("T,S- loss, undirected, neighbourCountVsEgonetWeight");
		synChart(chartA, simIt, EdgeType.LOSS, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartA, tmIt, EdgeType.LOSS, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount);
		chartA.build();

		Chart chartB = new Chart("T,S- inSameAuction, undirected, egonetWeightVsJaccardMean", ChartOpt.NOLOGAXIS, ChartOpt.NOXYLINE);
		synChart(chartB, simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetWeightCount, NodeFeature.jaccard(SumStat.Mean));
		tmChart(chartB, tmIt, EdgeType.IN_SAME_AUCTION, NodeFeature.EgonetWeightCount, NodeFeature.jaccard(SumStat.Mean));
		chartB.build();

		Chart chartC = new Chart("T,S- inSameAuction, undirected, neighbourCountVsEgonetWeight");
		synChart(chartC, simIt, EdgeType.IN_SAME_AUCTION, "Puppet", NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartC, tmIt, EdgeType.IN_SAME_AUCTION, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount);
		chartC.build();
		
		Chart chartD = new Chart("T,S- PARTICIPATE reversed, neighbourCountVsWeight");
		synChart(chartD, simIt, EdgeType.reverse(EdgeType.PARTICIPATE), "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartD, tmIt, EdgeType.reverse(EdgeType.PARTICIPATE), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		chartD.build();
		
		Chart chartE = new Chart("T,S- PARTICIPATE, neighbourCountVsWeight");
		synChart(chartE, simIt, EdgeType.PARTICIPATE, "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartE, tmIt, EdgeType.PARTICIPATE, NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		chartE.build();
		
		Chart chartF = new Chart("T,S- win, neighbourCountVsEgonetWeight");
		synChart(chartF, simIt, EdgeType.WIN, "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartF, tmIt, EdgeType.WIN, NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		chartF.build();
		
		Chart chartG = new Chart("T,S- win, undirected, neighbourCountVsEgonetWeight");
		synChart(chartG, simIt, EdgeType.undirected(EdgeType.WIN), "Puppet", NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		tmChart(chartG, tmIt, EdgeType.undirected(EdgeType.WIN), NodeFeature.NodeEdgeCount, NodeFeature.EgonetWeightCount);
		chartG.build();
		
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
//		xVsY = Sample.randomSample(xVsY, 2000, new Random());
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
		HashMap<Integer, HashMultiset<Integer>> adjacencyList = GraphOperations.duplicateAdjacencyList(simIt.getIterator(), EdgeType.reverse(edgeType));
		HashMap<Integer, Double> xValues = NodeFeature.values(adjacencyList, xAxisFeature);
		HashMap<Integer, Double> yValues = NodeFeature.values(adjacencyList, yAxisFeature);
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
		chartAddSeries(chart, logBins.emptyCopy(), xVsYFraud, "f");
		chartAddSeries(chart, logBins.emptyCopy(), xVsYNormal, "n");
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

		// plot all the points
		XYSeries series = new XYSeries(label);
		for (int i = 0; i < xVsY.size(); i++) {
//			series.add(xVsY.get(i)[0], xVsY.get(i)[1]);
			double jitterX = (random.nextDouble() - 0.5) / 5;
			double jitterY = (random.nextDouble() - 0.5) / 5;
			series.add(xVsY.get(i)[0] + jitterX, xVsY.get(i)[1] + jitterY);
		}
		chart.addSeries(series);

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
