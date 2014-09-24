package extraExperiments;

import graph.EdgeType;
import graph.GraphOperations;
import graph.MRFv2;
import graph.NodeFeature;
import graph.NodeFeatureI;
import graph.UseMRF;
import graph.outliers.AnalyseLOF;
import graph.outliers.CombineLOF.ReadLofFile;
import graph.outliers.evaluation.ROC_builder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.SimDBAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;
import simulator.database.DBConnection;
import util.CsvManipulation;
import util.CsvManipulation.CsvThing;
import util.IncrementalMean;
import util.IncrementalSD;
import util.Util;

public class MLExtra {

	public static void main(String[] args) {
//		runEval();
//		selectedTableValues();
//		writeEdges();
//		writeTMEdges();
		readTMCores();
//		readCores("F:/workstuff2011/AuctionSimulation/mlExtra", "syn_repfraud_20k_");
//		readCores("F:/workstuff2011/AuctionSimulation/mlExtra", "syn_hybridNormalEE_20k_");
//		readCores("F:/workstuff2011/AuctionSimulation/mlExtra", "syn_hybridBothVGS_20k_");
//		cliqueTest();
//		cliques("syn_repfraud_20k_");
//		cliques("syn_hybridBothVGS_20k_");
//		cliques("syn_hybridNormalEE_20k_");
	}
	
	public static void writeEdges() {
		writeEdges("syn_repfraud_20k_");
		writeEdges("syn_hybridBothVGS_20k_");
		writeEdges("syn_hybridNormalEE_20k_");
	}
	
	public static void cliqueTest() {
		UndirectedGraph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class);
		for (int i = 0; i < 5; i++) {
			graph.addVertex(i);
		}
		
		for (int i = 0; i < 3; i++) {
			for (int j = i + 1; j < 3; j++) {
				graph.addEdge(i, j);
			}
		}
		graph.addEdge(3, 4);
		BronKerboschCliqueFinder<Integer, DefaultEdge> cliqueFinder = new BronKerboschCliqueFinder<>(graph);
		Collection<Set<Integer>> cliques = cliqueFinder.getAllMaximalCliques();
		for (Set<Integer> clique : cliques) {
			System.out.println(clique);
		}
	}
	
	public static void cliques(String dbName) {
		HashMap<Integer, IncrementalSD> nSds = new HashMap<>();
		HashMap<Integer, IncrementalSD> fSds = new HashMap<>();

		for (int i = 0; i < 20; i++) {
			String name = dbName + i;
			Connection conn = DBConnection.getConnection(name);
			SimDBAuctionIterator it = new SimDBAuctionIterator(conn, true);

			EdgeType edgeType = EdgeType.PARTICIPATE;
			System.out.println("starting edge type: " + edgeType);
			Map<Integer, Multiset<Integer>> adjacencyList = GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType);

			Map<Integer, UserObject> users = it.users();
			
			Set<Integer> vertices = new HashSet<>();
			for (Entry<Integer, Multiset<Integer>> entry : adjacencyList.entrySet()) {
				Integer from = entry.getKey();
				Multiset<Integer> tos = entry.getValue();
				
				vertices.add(from);
				for (Integer to : tos) {
					vertices.add(to);
//					writer.append(from + "," + to);
//					writer.newLine();
				}
			}
			
			UndirectedGraph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class);
			for (Integer vertex : vertices) {
				graph.addVertex(vertex);
			}
			
			for (Entry<Integer, Multiset<Integer>> entry : adjacencyList.entrySet()) {
				Integer from = entry.getKey();
				Multiset<Integer> tos = entry.getValue();
				
				for (Integer to : tos) {
					graph.addEdge(from, to);
				}
			}
			
			BronKerboschCliqueFinder<Integer, DefaultEdge> cliqueFinder = new BronKerboschCliqueFinder<>(graph);
			Collection<Set<Integer>> cliques = cliqueFinder.getAllMaximalCliques();
			Multiset<Integer> cliqueSizes = HashMultiset.create();
			
			TreeMap<Integer, Integer> normalCounts = new TreeMap<>();
			TreeMap<Integer, Integer> fraudCounts = new TreeMap<>();
			
			HashMap<Integer, Integer> highestClique = new HashMap<>(); // id, biggest clique
			for (Set<Integer> clique : cliques) {
				int size = clique.size();
				cliqueSizes.add(size);
				
				for (Integer member : clique) {
					
					Integer current = highestClique.get(member);
					if (current == null || current < size) {
						highestClique.put(member, size);
					}
				}
			}
			
			ArrayListMultimap<Integer, Integer> idBySize = ArrayListMultimap.create(); 
			for (Integer id : highestClique.keySet()) {
				idBySize.put(highestClique.get(id), id);
			}
			
			for (Integer size : idBySize.keySet()) {
				List<Integer> ids = idBySize.get(size);
				
				int normal = 0, fraud = 0;
				for (Integer id : ids) {
					String type = users.get(id).userType;
						if (type.equals("ClusterEarly") || type.equals("ClusterSnipe") || type.equals("TMSeller"))
							normal++;
						else
							fraud++;
				}
				
				System.out.println(i + "," + size + "," + normal + "," + fraud);
				
			}
//				int normal = 0, fraud = 0;
//				for (Integer member : clique) {
//					String type = users.get(id).userType;
//					if (type.equals("ClusterEarly") || type.equals("ClusterSnipe") || type.equals("TMSeller"))
//						normal++;
//					else
//						fraud++;
//				}
//				
//				Integer nCount = normalCounts.get(size);
//				if (nCount == null) {
//					nCount = 0;
//				}
//				Integer fCount = fraudCounts.get(size);
//				if (fCount == null) {
//					fCount = 0;
//				}
//				normalCounts.put(size, nCount + normal);
//				fraudCounts.put(size, fCount + fraud);

//			for (Integer size : normalCounts.keySet()) {
//				System.out.println(i + "," + size + "," + normalCounts.get(size) + "," + fraudCounts.get(size));
//			}
			
//			for (Integer size : normalCounts.keySet()) {
//				nSds.(normalCounts.get(size));
//			}
			
			
			System.out.println(cliqueSizes);
		}
		
//		for (Integer size : nSds.keySet()) {
//			System.out.println(size + "," + nSds.get(size) + "," + fSds.get(size));
//		}
	}
	
	public static void writeEdges(String dbName) {
		for (int i = 0; i < 20; i++) {
			String name = dbName + i;
			Connection conn = DBConnection.getConnection(name);
			SimDBAuctionIterator it = new SimDBAuctionIterator(conn, true);

			try {
				BufferedWriter writer = Files.newBufferedWriter(Paths.get("F:/workstuff2011/AuctionSimulation/mlExtra", name + ".csv"));
				EdgeType edgeType = EdgeType.PARTICIPATE;
				Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType);

				for (Entry<Integer, Multiset<Integer>> entry : graph.entrySet()) {
					Integer from = entry.getKey();
					Multiset<Integer> tos = entry.getValue();
					
					for (Integer to : tos) {
						writer.append(from + "," + to);
						writer.newLine();
					}
				}
				
				writer.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void writeTMEdges() {
		for (int i = 0; i < 20; i++) {
			String name = "trademe";
			Connection conn = DBConnection.getConnection(name);
			TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);

			try {
				BufferedWriter writer = Files.newBufferedWriter(Paths.get("F:/workstuff2011/AuctionSimulation/mlExtra", name + ".csv"));
				EdgeType edgeType = EdgeType.PARTICIPATE;
				Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType);

				for (Entry<Integer, Multiset<Integer>> entry : graph.entrySet()) {
					Integer from = entry.getKey();
					Multiset<Integer> tos = entry.getValue();
					
					for (Integer to : tos) {
						writer.append(from + "," + to);
						writer.newLine();
					}
				}
				
				writer.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void readCores(String dir, String prefix) {
		TreeMap<Integer, IncrementalSD> normalSds = new TreeMap<>(); // <k, sd>
		TreeMap<Integer, IncrementalSD> fraudSds = new TreeMap<>(); // <k, sd>
		IncrementalSD aucSd = new IncrementalSD(); // <k, sd>
		IncrementalSD val005 = new IncrementalSD();
		IncrementalSD val01 = new IncrementalSD();
		IncrementalSD val05 = new IncrementalSD();
		
		System.out.println(prefix);
		for (int i = 0; i < 20; i++) {
			String corename = prefix + i + "_kcore.csv";
			ArrayListMultimap<Integer, Integer> maxK = readCores(Paths.get(dir, corename));
			
			String dbName = prefix + i;
			SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection(dbName), true);
			
			Map<Integer, UserObject> users = it.users();

			int totalN = 0, totalF = 0;
			for (Integer id : users.keySet()) {
				String type = users.get(id).userType;
				if (type.equals("ClusterEarly") || type.equals("ClusterSnipe") || type.equals("TMSeller"))
					totalN++;
				else
					totalF++;
			}
			
			ArrayList<Pair<Double, Double>> FPR_TPR = new ArrayList<>();
			for (Integer k : maxK.keySet()) {
				IncrementalSD nsd = normalSds.get(k);
				IncrementalSD fsd = fraudSds.get(k);
				if (nsd == null) {
					nsd = new IncrementalSD();
					normalSds.put(k, nsd);
				}
				if (fsd == null) {
					fsd = new IncrementalSD();
					fraudSds.put(k, fsd);
				}

				int fraud = 0;
				int normal = 0;
				for (Integer id : maxK.get(k)) {
					String type = users.get(id).userType;
					if (type.equals("ClusterEarly") || type.equals("ClusterSnipe") || type.equals("TMSeller"))
						normal++;
					else
						fraud++;
//					types.add(type);
				}
//				System.out.println(k + "," + normal + "," + fraud);
				nsd.add((double) normal / totalN);
				fsd.add((double) fraud / totalF);
				FPR_TPR.add(new Pair<>((double) normal / totalN, (double) fraud / totalF));
			}
//			System.out.println(types);
//			break;
			FPR_TPR.add(0, new Pair<>(0d,0d));
			FPR_TPR.add(new Pair<>(1d,1d));
			
			FPR_TPR.sort((p1, p2) -> {int c1 = p1.getKey().compareTo(p2.getKey()); return c1 == 0 ? p1.getValue().compareTo(p2.getValue()) : c1;});
			double auc  = ROC_builder.AUC(FPR_TPR);
			val005.add(interpolatePairsPoint(0.005, FPR_TPR));
			val01.add(interpolatePairsPoint(0.01, FPR_TPR));
			val05.add(interpolatePairsPoint(0.05, FPR_TPR));
			aucSd.add(auc);
		}
		for (Integer k : normalSds.keySet()) {
			System.out.println(k + "," + normalSds.get(k).average() + "," + fraudSds.get(k).average());
		}
		System.out.println(aucSd.average() + "," + aucSd.getSD() + "," + val005.average() + "," + val005.getSD() + "," + val01.average() + "," + val01.getSD() + "," + val05.average() + "," + val05.getSD());
	}
	
	private static double interpolatePairsPoint(double val, ArrayList<Pair<Double, Double>> FPR_TPR) {
		Pair<Double, Double> pp = null;
		for (Pair<Double, Double> p : FPR_TPR) {
			if (p.getKey() >= val) {
				double result = (val - pp.getKey())/(p.getKey() - pp.getKey()) * (p.getValue() - pp.getValue()) + pp.getValue();
				return result;
			}
			pp = p;
		}
		assert false;
		return -1;
	}
	
	public static void readTMCores() {
		Path path = Paths.get("F:/workstuff2011/AuctionSimulation/mlExtra/trademe_kcore.csv");
		ArrayListMultimap<Integer, Integer> maxK = ArrayListMultimap.create(); // <k, id>
		List<String[]> rows = CsvManipulation.readWholeFile(path, false);
		for (String[] row : rows) {
			int k = Integer.parseInt(row[0]);
			for (int i = 1; i < row.length; i++) {
				int id = Integer.parseInt(row[i]);
				maxK.put(k, id);
			}
		}
		for (Integer k : maxK.keySet()) {
			System.out.println(k + "," + maxK.get(k).size());
		}
	}
	
	public static ArrayListMultimap<Integer, Integer> readCores(Path path) {
		ArrayListMultimap<Integer, Integer> maxK = ArrayListMultimap.create(); // <k, id>
		List<String[]> rows = CsvManipulation.readWholeFile(path, false);
		for (String[] row : rows) {
			int k = Integer.parseInt(row[0]);
			for (int i = 1; i < row.length; i++) {
				int id = Integer.parseInt(row[i]);
				maxK.put(k, id);
			}
		}
		return maxK;
	}
	
	private static void runEval() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";

//		for (int j = 1; j < 20; j++) {
		{
			TreeMap<String, IncrementalMean> avgDiff = new TreeMap<>();
			MRFv2.paramO = 0.2;
//			MRFv2.paramP = 0.4;
			MRFv2.paramP = 0.45;

			for (String name : new String[]{
//					"repFraud",
//					"hybridBothVGS",
					"hybridNormalEE",
					}) {
				
				for (int i = 0; i < 20; i++) {
					String label = name + "_" + i;
//					System.out.println("doing: " + label);
					
					UseMRF useMRF = new UseMRF("syn_" + name + "_20k_" + i, 
							directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv", 
							directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv");
					useMRF.run(label);
					
					String outputFilename = directory + "mlExtraEval/finalScoresW_" + name + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
	
					ReadLofFile lofFile = new ReadLofFile(outputFilename);
					double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
					double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
					
					if (!avgDiff.containsKey(name))
						avgDiff.put(name, new IncrementalMean());
					avgDiff.get(name).add(rankAfterProp - rankBeforeProp);
					
//					System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);
					System.out.println(i + "," + rankBeforeProp + "," + (rankAfterProp));
	
				}
			}
			System.out.println();
			
			System.out.println(avgDiff);
		}
	}
	
	private static void selectedTableValues() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";
		for (String name : new String[]{
				"repFraud", 
				"hybridBothVGS", 
				"hybridNormalEE",
				}) {
			
			IncrementalSD sd005 = new IncrementalSD();
			IncrementalSD sd01 = new IncrementalSD();
			IncrementalSD sd05 = new IncrementalSD();
			IncrementalSD auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				if (name.equals("hybridNormalEE") && Arrays.asList(new Integer[]{8,11,13,15,16,17}).contains(i))
				continue;
				
				ArrayList<Pair<Double, Double>> fpr_tpr = ROC_builder.run(directory + "mlExtraEval/finalScoresW_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROCW_" + name + "_" + i + ".csv", 3);
//				System.out.println(ROC_builder.TPR(fpr_tpr, 0.005));
				sd005.add(ROC_builder.TPR(fpr_tpr, 0.005));
				sd01.add(ROC_builder.TPR(fpr_tpr, 0.01));
				sd05.add(ROC_builder.TPR(fpr_tpr, 0.05));
				auc.add(ROC_builder.AUC(fpr_tpr));
			}
			System.out.println(name + "," + sd005.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());

			sd005 = new IncrementalSD();
			sd01 = new IncrementalSD();
			sd05 = new IncrementalSD();
			auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				if (name.equals("hybridNormalEE") && !Arrays.asList(new Integer[]{1,2,3,4,5,6}).contains(i))
					continue;
				ArrayList<Pair<Double, Double>> fpr_tpr = ROC_builder.run(directory + "mlExtraEval/finalScores_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROC_" + name + "_" + i + ".csv", 3);
				sd005.add(ROC_builder.TPR(fpr_tpr, 0.005));
				sd01.add(ROC_builder.TPR(fpr_tpr, 0.01));
				sd05.add(ROC_builder.TPR(fpr_tpr, 0.05));
				auc.add(ROC_builder.AUC(fpr_tpr));
			}
			System.out.println(name + "_NW," + sd005.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());

			sd005 = new IncrementalSD();
			sd01 = new IncrementalSD();
			sd05 = new IncrementalSD();
			auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				ArrayList<Pair<Double, Double>> fpr_tpr = ROC_builder.run(directory + "mlExtraEval/finalScoresW_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROCW_" + name + "_UP_" + i + ".csv", 2);
				sd005.add(ROC_builder.TPR(fpr_tpr, 0.005));
				sd01.add(ROC_builder.TPR(fpr_tpr, 0.01));
				sd05.add(ROC_builder.TPR(fpr_tpr, 0.05));
				auc.add(ROC_builder.AUC(fpr_tpr));
			}
			System.out.println(name + "_UP," + sd005.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());
			
		}
	}

}
