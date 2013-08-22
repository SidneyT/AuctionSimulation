package graph;

import graph.EdgeType.SellerEdges;
import graph.NodeFeature.FirstEigenvalue;

import java.io.BufferedReader;
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

import org.apache.commons.math3.util.Pair;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jfree.data.xy.XYSeries;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

import simulator.database.DBConnection;
import temporary.Chart;
import temporary.Chart.ChartOpt;
import util.SumStat;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimDBAuctionIterator;

public class ConstructGraph {
	public static void main(String[] args) {
//		makeCharts();
		run2();
//		eigenValueTest();
//		sellerEdges();
//		try {
//			repairCSVFile();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private static void repairCSVFile() throws IOException {
		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
//		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation"), true);
		
		List<EdgeTypeI> edgeTypes = Arrays.<EdgeTypeI>asList(
				EdgeType.WIN, EdgeType.LOSS, EdgeType.PARTICIPATE, EdgeType.IN_SAME_AUCTION 
				);

		ArrayList<ImmutableMap<Integer, Multiset<Integer>>> graphs = new ArrayList<>();
		
		for (EdgeTypeI edgeType : edgeTypes) {
			graphs.add(GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType));
			
		}
		
		List<NodeFeatureI> features = Arrays.asList(NodeFeature.NodeEdgeCount, NodeFeature.NodeWeightCount, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount,
				NodeFeature.jaccard(SumStat.Max));

		BufferedReader reader = Files.newBufferedReader(Paths.get("allBidderValues.csv"), Charset.defaultCharset());
		
		String heading = null;
		ArrayList<String[]> lines = new ArrayList<>();
		if (reader.ready())
			heading = reader.readLine();
		while (reader.ready()) {
			lines.add(reader.readLine().split(","));
		}
	
		BufferedWriter writer = Files.newBufferedWriter(Paths.get("allBidderValues_fixed.csv"), Charset.defaultCharset());
		writer.append(heading);
		writer.newLine();
		for (String[] line : lines) {
			int id = Integer.parseInt(line[0]);
			int skipped = 0;
			
			if (id == 22) {
				System.out.println("");
			}
			
			writer.append(id + ",");
			
			if (!graphs.get(0).containsKey(id)) {
				writer.append(",,,,,");
				skipped += 1;
			}
			
			for (int i = 1; i < 6; i++) {
				writer.append(line[i] + ",");
			}
			
			if (!graphs.get(1).containsKey(id)) {
				writer.append(",,,,,");
				skipped += 1;
			}
			
			if (line.length > 10)
				for (int i = 1 + 5; i < 6 + 5; i++) {
					writer.append(line[i] + ",");
			}
			
			if (!graphs.get(2).containsKey(id))
				writer.append(",,,,,");
			
			if (line.length > 15)
			for (int i = 1 + 10; i < 6 + 10; i++) {
				writer.append(line[i] + ",");
			}
			if (line.length > 15 && skipped == 1)
				writer.append(line[16]);
			
			if (!graphs.get(3).containsKey(id))
				writer.append(",,,,,");
			
			if (line.length > 20)
			for (int i = 1 + 15; i < 7 + 15; i++) {
				writer.append(line[i] + ",");
			}

			writer.newLine();
		}
		
		writer.flush();
		
	}
	
	private static void allCombos() {
//		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation"), true);
		generateBidderValues(it);
	}
	
	public static <T extends AuctionObject> ArrayListMultimap<Integer, Double> generateBidderValues(Iterable<Pair<T, List<BidObject>>> auctionIterable) {
		// instantiate different graphs
		List<EdgeTypeI> edgeTypes = Arrays.<EdgeTypeI>asList(
				EdgeType.WIN, EdgeType.LOSS, EdgeType.PARTICIPATE, EdgeType.IN_SAME_AUCTION 
				);
		
		List<NodeFeatureI> features = Arrays.asList(NodeFeature.NodeEdgeCount, NodeFeature.NodeWeightCount, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount,
				NodeFeature.jaccard(SumStat.Max));
		
		ArrayListMultimap<Integer, Double> allFeaturesValues = ArrayListMultimap.create();
		StringBuffer headingBuffer = new StringBuffer();
		headingBuffer.append("id,");
		
		for (EdgeTypeI edgeType : edgeTypes) {
			System.out.println("starting edge type: " + edgeType);
			Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(auctionIterable.iterator(), edgeType);
			System.out.println("size: " + graph.size());
			for (NodeFeatureI feature : features) {
				headingBuffer.append(edgeType + "_" + feature + ',');
				System.out.println("starting feature: " + feature);
				HashMap<Integer, Double> featureValues = NodeFeature.values(graph, feature);
				for (Integer user : allFeaturesValues.keySet()) {
					if (!featureValues.containsKey(user)) { // this user has no value for this feature... so just put a NaN there to signify no value.
						allFeaturesValues.put(user, Double.NaN);
					} else {
						allFeaturesValues.put(user, featureValues.get(user));
					}
				}
//				break;
			}
//			break;
		}
		
		{
			EdgeType edgeType = EdgeType.IN_SAME_AUCTION;
			System.out.println("starting edge type: " + edgeType);
			Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(auctionIterable.iterator(), edgeType);
			
			FirstEigenvalue feature = NodeFeature.firstEigenvalue_sym(graph);
			HashMap<Integer, Double> featureValues = NodeFeature.values(graph, feature);
			headingBuffer.append(edgeType + "_" + feature);
			for (Integer user : featureValues.keySet()) {
				if (!featureValues.containsKey(user)) { // this user has no value for this feature... so just put a NaN there to signify no value.
					allFeaturesValues.put(user, Double.NaN);
				} else {
					allFeaturesValues.put(user, featureValues.get(user));
				}
			}
		}
		
		writeAllValues("allBidderValues.csv", headingBuffer, allFeaturesValues);
		
		return allFeaturesValues;
	}
	public static <T extends AuctionObject> ArrayListMultimap<Integer, Double> generateSellerValues(Iterable<Pair<T, List<BidObject>>> auctionIterable) {
		// instantiate different graphs
		List<EdgeTypeI> edgeTypes_asym = Arrays.<EdgeTypeI>asList(
				EdgeType.reverse(EdgeType.WIN), EdgeType.reverse(EdgeType.LOSS), EdgeType.reverse(EdgeType.PARTICIPATE)
				);
		
		List<NodeFeatureI> features_asym = Arrays.asList(NodeFeature.NodeEdgeCount, NodeFeature.NodeWeightCount, NodeFeature.EgonetEdgeCount, NodeFeature.EgonetWeightCount,
				NodeFeature.jaccard(SumStat.Max));
		
		ArrayListMultimap<Integer, Double> allFeaturesValues = ArrayListMultimap.create();
		StringBuffer headingBuffer = new StringBuffer();
		headingBuffer.append("id,");
		
		for (EdgeTypeI edgeType : edgeTypes_asym) {
			System.out.println("starting edge type: " + edgeType);
			Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(auctionIterable.iterator(), edgeType);
			System.out.println("size: " + graph.size());
			for (NodeFeatureI feature : features_asym) {
				headingBuffer.append(edgeType + "_" + feature + ',');
				System.out.println("starting feature: " + feature);
				HashMap<Integer, Double> featureValues = NodeFeature.values(graph, feature);
				for (Integer user : featureValues.keySet()) {
					if (!featureValues.containsKey(user)) { // this user has no value for this feature... so just put a NaN there to signify no value.
						allFeaturesValues.put(user, Double.NaN);
					} else {
						allFeaturesValues.put(user, featureValues.get(user));
					}
				}
//				break;
			}
//			break;
		}
		
		{
			SellerEdges edgeType = EdgeType.sellerEdges();
			System.out.println("starting edge type: " + edgeType);
			Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(auctionIterable.iterator(), edgeType);

			for (NodeFeatureI feature : features_asym) {
				headingBuffer.append(edgeType + "_" + feature + ',');
				System.out.println("starting feature: " + feature);
				HashMap<Integer, Double> featureValues = NodeFeature.values(graph, feature);
				for (Integer user : featureValues.keySet()) {
					allFeaturesValues.put(user, featureValues.get(user));
				}
			}
	
			FirstEigenvalue feature = NodeFeature.firstEigenvalue_sym(graph);
			HashMap<Integer, Double> featureValues = NodeFeature.values(graph, feature);
			headingBuffer.append(edgeType + "_" + feature);
			for (Integer user : featureValues.keySet()) {
				if (!featureValues.containsKey(user)) { // this user has no value for this feature... so just put a NaN there to signify no value.
					allFeaturesValues.put(user, Double.NaN);
				} else {
					allFeaturesValues.put(user, featureValues.get(user));
				}
			}
		}
		
		writeAllValues("allSellerValues.csv", headingBuffer, allFeaturesValues);
		
		return allFeaturesValues;
	}
	
	public static void writeAllValues(String filename, StringBuffer headings, ArrayListMultimap<Integer, Double> allFeaturesValues) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename), Charset.defaultCharset());
			
			bw.append(headings.toString());
			bw.newLine();
			
			StringBuffer sb = new StringBuffer();
			for (Integer user : allFeaturesValues.keySet()) {
				sb.append(user + ",");

				List<Double> values = allFeaturesValues.get(user);
				for (Double value : values) {
					sb.append(value).append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append("\n");
				
			}
			bw.append(sb.toString());
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void eigenValueTest() {
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_10k_test8"), true);
		EdgeTypeI edgeType = EdgeType.undirected(EdgeType.PARTICIPATE);
		Map<Integer, Multiset<Integer>> participateList = GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType);
		
		HashMap<Integer, Double> firstEigenvalues = new HashMap<>();

		long t1 = System.nanoTime();

//		int count = 0;
		for (int userId : participateList.keySet()) {
//			if (++count % 500 == 0) {
//				System.out.println("done " + count);
//				break;
//			}
			Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(participateList, userId);
			// convert egonet adjacency list to a matrix
			double[][] matrix = new double[egonet.size()][egonet.size()]; 
			List<Integer> ids = new ArrayList<>(egonet.keySet());
			for (int neighbour : egonet.keySet()) {
				int index = ids.indexOf(neighbour);
				for (int nneighbour : egonet.get(neighbour)) {
					matrix[index][ids.indexOf(nneighbour)]++;
				}
			}
			
			DoubleMatrix eigenvalues = Eigen.symmetricEigenvalues(new DoubleMatrix(matrix));
			firstEigenvalues.put(userId, eigenvalues.max());
//			System.out.println(eigenvalues.max());
		}
		
		long t2 = System.nanoTime() - t1;
		System.out.println(t2 / 1000000);
	}
	
	private static void sellerEdges() {
//		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_100k_test5"), true);
		HashMap<Integer, Multiset<Integer>> linkedSellers = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.sellerEdges());
		
		HashMap<Integer, Double> totalSharedBidderCount = NodeFeature.values(linkedSellers, NodeFeature.NodeWeightCount);
		HashMap<Integer, Double> uniqueSharedBidderCount = NodeFeature.values(linkedSellers, NodeFeature.NodeEdgeCount);

		ValueFrequencies valueFrequenciesA = new ValueFrequencies();
		for (Double value : totalSharedBidderCount.values()) {
			valueFrequenciesA.addValue(value);
		}
//		try {
//			BufferedWriter bw = Files.newBufferedWriter(Paths.get("output.csv"), Charset.defaultCharset(),
//					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//
//			for (Integer uid : linkedSellers.keySet()) {
//				bw.append(uid + "," + totalSharedBidderCount.get(uid) + "," + uniqueSharedBidderCount.get(uid));
//				// bw.append(uid + "," + sellerAuctionCount.get(uid) + "," + sellerAuctionUniqueCount.get(uid));
//				bw.newLine();
//			}
//
//			// bw.write(valueFrequenciesA.toString());
//			bw.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	private static void run2() {
//		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
//		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_10k_test7"), true);
//		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_100k_test0"), true);
//		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_repFraud_100k_0"), true);
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation"), true);
		
//		Iterator<Pair<TMAuction, List<BidObject>>> thing = it.iterator();
////		Iterator<Pair<SimAuction, List<BidObject>>> thing = it.iterator();
//		int totalBids = 0;
//		int totalAuctions = 0;
//		while (thing.hasNext()) {
//			int bidCount = thing.next().getValue().size();
//			if (bidCount > 20)
//				System.out.println("warning; bid count > 20");
//			totalBids += bidCount;
//			totalAuctions++;
//		}
//		System.out.println(totalBids + "," + totalAuctions);
		
//		// gives the number of auctions sold by the seller
//		Map<Integer, Multiset<Integer>> winList = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.reverse(EdgeType.WIN));
//		HashMap<Integer, Double> sellerAuctionCount = NodeFeature.values(winList, NodeFeature.NodeWeightCount);
//		HashMap<Integer, Double> sellerAuctionUniqueCount = NodeFeature.values(winList, NodeFeature.NodeEdgeCount);
////		TreeMultiset<Integer> frequencies = TreeMultiset.create();
////		for (Integer key : winList.keySet()) {
////			frequencies.add(winList.get(key).size());
////		}
////		for (int frequency : frequencies.elementSet()) {
////			System.out.println(frequency + ", " + frequencies.count(frequency));
////		}

		// gives the number of participations (unique and total) by bidders to a particular seller's auctions
		Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), (EdgeType.PARTICIPATE));
		HashMap<Integer, Double> nodeEdgeCount = NodeFeature.values(graph, NodeFeature.NodeEdgeCount);
		HashMap<Integer, Double> nodeWeightCount = NodeFeature.values(graph, NodeFeature.NodeWeightCount);
//		HashMap<Integer, Double> eigenValue = NodeFeature.values(graph, NodeFeature.firstEigenvalue_asym(graph));

		System.out.println("start");
//		for (int uid : new TreeSet<>(participants.keySet())) {
//			System.out.println(uid + "," + winList.get(uid).size() + "," + uniqueParticipantsCount.get(uid) + "," + totalParticipantsCount.get(uid));
//		}
		
//		Multiset<Integer> userw = graph.get(2816252);
//		System.out.println(userw);
//		Multiset<Integer> userp = graph.get(2816252);
//		System.out.println(userp);
//		
//		System.out.println(nodeEdgeCount.get(2816252));
//		System.out.println(nodeWeightCount.get(2816252));
		
//		ValueFrequencies valueFrequenciesA = new ValueFrequencies();
//		for (Double value : nodeEdgeCount.values()) {
//			valueFrequenciesA.addValue(value);
//		}
//		System.out.println(valueFrequenciesA.toString());
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get("output.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			for (Integer uid : graph.keySet()) {
				bw.append(uid + "," + nodeWeightCount.get(uid) + "," + nodeEdgeCount.get(uid));
//				bw.append(uid + "," + sellerAuctionCount.get(uid) + "," + sellerAuctionUniqueCount.get(uid));
				bw.newLine();
			}
			
//			bw.write(valueFrequenciesA.toString());
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class ValueFrequencies {
		public Multiset<Double> frequencies = HashMultiset.create();
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
		Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(tmIt.iterator(), edgeType);
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
		Map<Integer, Multiset<Integer>> adjacencyList = GraphOperations.duplicateAdjacencyList(simIt.iterator(), EdgeType.reverse(edgeType));
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
