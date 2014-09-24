package graph;

import graph.MRFv2.Node;
import graph.MRFv2.State;
import graph.outliers.AnalyseLOF;
import graph.outliers.CombineLOF.ReadLofFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import simulator.database.DBConnection;
import util.CsvManipulation;
import util.IncrementalMean;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import createUserFeatures.SimDBAuctionIterator;

public class UseMRF {
	public static void main(String[] args) throws IOException {
//		String directory = "graphFeatures_processed/old/";
//		UseMRF useMRF = new UseMRF("syn_repfraud_20k_0", 
//				directory + "lof_19,2_99_syn_repFraud_20k_0_bidderGraphFeatures.csv", 
//				directory + "lof_17,22_80_syn_repfraud_20k_0_sellerGraphFeatures.csv");
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";
//		String directory = "graphFeatures_processed/no_jitter/multi/";
//		String directory = "graphFeatures_processed/no_jitter/multi/propMatrixTuning";
		propMatrixValueTest3(directory);

//		normalRun();
//		allFeaturesRun();
//		paramTuning();
//		edgeRemoveRun();
//		swappedScoresRun();
	}

	public static void propMatrixValueTest3(String directory) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("PropMatrix_3.csv"), Charset.defaultCharset());
		
		MRFv2.paramO = 0.2;
		MRFv2.paramP = 0.4;

		for (int ei = 1; ei < 15; ei++) 
		{
			for (int e2i = 1; e2i < 15; e2i++) 
			{
//				int i = 8;
//				int j = 4;
				double e = ei * 0.04;
				double e2 = e2i * 0.04;
				
				MRFv2.e = e;
				MRFv2.e2 = e2;

				bw.write(e + "," + e2);
				
				for (String name : new String[] {"repFraud", "hybridBothVGS", "hybridNormalEE"}) {
					System.out.println("doing: " + name + "|" + e + ", " + e2);
					String label = name;
					
					UseMRF useMRF = new UseMRF("syn_" + name + "_20k_2", 
							directory + "combinedLof/combinedLof_" + name + "_bidder_2.csv", 
							directory + "combinedLof/combinedLof_" + name + "_seller_2.csv");
					
//					useMRF.graph = GraphOperations.fudgeGraph(useMRF.it, useMRF.graph, 0.5);
					
					useMRF.run(label);

					String outputFilename = directory + "propMatrix_3/finalScores_" + name + "_" + e + "-" + e2 + "_5.csv";
					useMRF.writeScores(outputFilename);

					ReadLofFile lofFile = new ReadLofFile(outputFilename);
					ArrayList<Double>[] c2 = AnalyseLOF.groupLofs(2, lofFile);
					ArrayList<Double>[] c3 = AnalyseLOF.groupLofs(3, lofFile);
					
					double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
					double meanDistBeforeProp = AnalyseLOF.meanDistance(c2[0], c2[1]);
					double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
					double meanDistAfterProp = AnalyseLOF.meanDistance(c3[0], c3[1]);
					
					System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);

					bw.write("," + name + "," + rankBeforeProp + "," + rankAfterProp + "," + meanDistBeforeProp + "," + meanDistAfterProp);
				}
				bw.newLine();
				bw.flush();
			}
		}
		bw.close();
	}

	private static void paramTuning() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";

		for (int j = 1; j < 20; j++) {
			TreeMap<String, IncrementalMean> avgDiff = new TreeMap<>();
//			double paramO = j * 0.05;
//			MRFv2.paramO = paramO;
			MRFv2.paramO = 0.2;
			double paramP = j * 0.05 + 0.15;
			MRFv2.paramP = paramP;
//			MRFv2.paramP = 0.4;

			System.out.print(paramP);
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
					
					String outputFilename = directory + "finalScores/finalScores_" + name + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
	
					ReadLofFile lofFile = new ReadLofFile(outputFilename);
					double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
					double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
					
					if (!avgDiff.containsKey(name))
						avgDiff.put(name, new IncrementalMean());
					avgDiff.get(name).add(rankAfterProp - rankBeforeProp);
					
//					System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);
					System.out.print("," + (rankAfterProp));
	
				}
			}
			System.out.println();
			
			System.out.println(paramP + "," + avgDiff);
		}
	}
	/**
	 * Run where LOF values are from all features at once
	 */
	private static void allFeaturesRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		TreeMap<String, IncrementalMean> avgDiff = new TreeMap<>();

		MRFv2.paramO = 0.2;
//		MRFv2.paramP = 0.4;
		MRFv2.paramP = .40;
		
		for (String name : new String[]{
				"repFraud",
				"hybridBothVGS",
				"hybridNormalEE",
				}) {
			
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				UseMRF useMRF = new UseMRF("syn_" + name + "_20k_" + i, 
						directory + "allLof/allLof_" + name + "_bidder_" + i + ".csv", 
						directory + "allLof/allLof_" + name + "_seller_" + i + ".csv");
				useMRF.run(label);
				
				String outputFilename = directory + "finalScores_allFeatures/finalScores_" + name + "_" + i + ".csv";
				useMRF.writeScores(outputFilename);

				ReadLofFile lofFile = new ReadLofFile(outputFilename);
				double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
				double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
				
				if (!avgDiff.containsKey(name))
					avgDiff.put(name, new IncrementalMean());
				avgDiff.get(name).add(rankAfterProp - rankBeforeProp);
				
				System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);

			}
		}
		System.out.println(avgDiff);
	}
	
	private static void normalRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		TreeMap<String, IncrementalMean> avgDiff = new TreeMap<>();

		MRFv2.paramO = 0.2;
//		MRFv2.paramP = 0.4;
		MRFv2.paramP = .45;
		
		for (String name : new String[]{
//				"repFraud",
//				"hybridBothVGS"
				"hybridNormalEE",
				}) {
			
			for (int i = 0; i < 1; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				UseMRF useMRF = new UseMRF("syn_" + name + "_20k_" + i, 
						directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv", 
						directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv");
//				useMRF.graph = GraphOperations.fudgeGraph(useMRF.it, useMRF.graph, 0.02 * 0);
				useMRF.run(label);
				
				String outputFilename = directory + "finalScores/finalScores_" + name + "_" + i + ".csv";
				useMRF.writeScores(outputFilename);

				ReadLofFile lofFile = new ReadLofFile(outputFilename);
				double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
				double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
				
				if (!avgDiff.containsKey(name))
					avgDiff.put(name, new IncrementalMean());
				avgDiff.get(name).add(rankAfterProp - rankBeforeProp);
				
				System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);

			}
		}
		System.out.println(avgDiff);
	}
	
	private static void edgeRemoveRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		MRFv2.paramO = 0.2;
//		MRFv2.paramP = 0.40;
		MRFv2.paramP = 0.45;

		for (String name : new String[]{
//				"repFraud", 
//				"hybridBothVGS",
				"hybridNormalEE", 
				}) {
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				for (int j = 0; j < 51; j++) {
					double proportionOfEdgesRemoved = 0.02 * j;
					UseMRF useMRF = new UseMRF("syn_" + name + "_20k_" + i, 
							directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv", 
							directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv");
					// remove edges
					useMRF.graph = GraphOperations.fudgeGraph(useMRF.it, useMRF.graph, proportionOfEdgesRemoved);
//					useMRF.graph = GraphOperations.fudgeGraphFraudsOnly(useMRF.it, useMRF.graph, proportionOfEdgesRemoved);
					useMRF.run(label);
					
					String outputFilename = directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_" + proportionOfEdgesRemoved + "_" + i + ".csv";
//					String outputFilename = directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_" + proportionOfEdgesRemoved + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
					System.out.println(proportionOfEdgesRemoved + "," + useMRF.resultsLine());
				}
			}
		}
	}

	private static void swappedScoresRun() {
		String directory = "graphFeatures_processed/no_jitter/";

		MRFv2.paramO = 0.2;
		MRFv2.paramP = 0.40;
//		MRFv2.paramP = 0.45;

		for (String name : new String[]{
				"repFraud", 
				"hybridBothVGS",
//				"hybridNormalEE",
				}) {
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				for (int j = 0; j < 51; j++) {
					double proportionValuesSwapped = 0.02 * j;
					UseMRF useMRF = new UseMRF("syn_" + name + "_20k_" + i, 
//							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionValuesSwapped + "_bidders_" + i + ".csv", 
//							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionValuesSwapped + "_sellers_" + i + ".csv");
							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionValuesSwapped + "_bidders_" + i + ".csv", 
							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionValuesSwapped + "_sellers_" + i + ".csv");
					useMRF.run(label);
					
//					String outputFilename = directory + "finalScores_fScoresSwapped/finalScores_" + name + "_" + proportionValuesSwapped + "_" + i + ".csv";
					String outputFilename = directory + "finalScores_nScoresSwapped/finalScores_" + name + "_" + proportionValuesSwapped + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
//					useMRF.printCounts();
					System.out.println(proportionValuesSwapped + "," + useMRF.resultsLine());
				}
			}
		}
	}

	public static void propMatrixValueTest(String directory) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("PropMatrixTest_2.4.csv"), Charset.defaultCharset());
		
		for (int i = 1; i < 15; i++) {
			for (int j = 1; j < 15; j++) {
//				int i = 8;
//				int j = 4;
				double e = i * 0.04;
				double e2 = j * 0.04;
				
				MRFv2.e = e;
				MRFv2.e2 = e2;

				for (String name : new String[] { "repFraud", "hybridBothVGS", "hybridNormalEE", }) {

					System.out.println("doing: " + name + "|" + e + ", " + e2);

					String label = name;
					
					UseMRF useMRF = new UseMRF("syn_" + name + "_20k_0", 
							directory + name + "combinedScores_bidders_norm.csv", 
							directory + name + "combinedScores_sellers_norm.csv");
					
					useMRF.run(label);

					String outputFilename = directory + "finalScores_" + name + "_" + e + "-" + e2 + "_2.2.csv";
					useMRF.writeScores(outputFilename);

					ReadLofFile lofFile = new ReadLofFile(outputFilename);
					ArrayList<Double>[] c2 = AnalyseLOF.groupLofs(2, lofFile);
					ArrayList<Double>[] c3 = AnalyseLOF.groupLofs(3, lofFile);
					
					double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
					double meanDistBeforeProp = AnalyseLOF.meanDistance(c2[0], c2[1]);
					double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
					double meanDistAfterProp = AnalyseLOF.meanDistance(c3[0], c3[1]);
					
					System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);

					bw.write(name + "," + e + "," + e2 + "," + rankBeforeProp + "," + rankAfterProp + "," + meanDistBeforeProp + "," + meanDistAfterProp + ",");
				}
				bw.newLine();
			}
			bw.flush();
		}
		bw.close();
	}
	
	public static void propMatrixValueTest2(String directory) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("PropMatrixTest_2.2.csv"), Charset.defaultCharset());
		
		for (int i = 1; i < 15; i++) {
			for (int j = 1; j < 15; j++) {
				for (String name : new String[] {"repFraud", "hybridBothVGS", "hybridNormalEE"}) {
					double e = i * 0.04;
					double e2 = j * 0.04;

					MRFv2.e = e;
					MRFv2.e2 = e2;

					System.out.println("doing: " + name + "|" + e + ", " + e2);

					String outputFilename = directory + "finalScores_" + name + "_" + e + "-" + e2 + "_2.csv";

					ReadLofFile lofFile = new ReadLofFile(outputFilename);
					ArrayList<Double>[] c2 = AnalyseLOF.groupLofs(2, lofFile);
					ArrayList<Double>[] c3 = AnalyseLOF.groupLofs(3, lofFile);
					
					double rankBeforeProp = AnalyseLOF.calculateMeanRank(2, lofFile);
					double meanDistBeforeProp = AnalyseLOF.meanDistance(c2[0], c2[1]);
					double rankAfterProp = AnalyseLOF.calculateMeanRank(3, lofFile);
					double meanDistAfterProp = AnalyseLOF.meanDistance(c3[0], c3[1]);
					
					System.out.println("rank before and after: " + rankBeforeProp + ", " + rankAfterProp);

					bw.write(name + "," + e + "," + e2 + "," + rankBeforeProp + "," + rankAfterProp + "," + meanDistBeforeProp + "," + meanDistAfterProp + ",");
				}
				bw.newLine();
			}
			bw.flush();
		}
		bw.close();
	}

	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;
	SimDBAuctionIterator it;

	public UseMRF(String dbName, String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
		this.it = new SimDBAuctionIterator(DBConnection.getConnection(dbName), true);
		
		this.graph = GraphOperations.duplicateAdjacencyList(it.iterator(), EdgeType.undirected(EdgeType.PARTICIPATE));
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
	}
	
	Map<Integer, Node> nodeBeliefs;
//	MRF mrf;
	MRFv2 mrf;
	public HashMap<Integer, Node> run(String label) {
//		mrf = new MRF(graph, outlierScores);
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
//				writer.append("," + node.state);
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
