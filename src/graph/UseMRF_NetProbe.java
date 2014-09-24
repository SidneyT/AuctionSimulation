package graph;

import graph.MRF_NetProbe.Node;
import graph.MRF_NetProbe.State;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import simulator.database.DBConnection;
import util.CsvManipulation;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import createUserFeatures.SimDBAuctionIterator;

public class UseMRF_NetProbe {
	public static void main(String[] args) throws IOException {
		String directory = "graphFeatures_processed/no_jitter/";
//		propMatrixValueTest(directory);
		
//		normalRun(); // run the score propagation
//		edgeRemoveRun();
		swappedScoresRun();
	}
	
	private static void normalRun() {
		String directory = "graphFeatures_processed/no_jitter/";
		for (String name : new String[]{"repFraud", "hybridNormalEE", "hybridBothVGS"}) {
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_" + name + "_20k_" + i, 
						directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv", 
						directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv");
				useMRF.run(label);
				
				String outputFilename = directory + "finalScores/finalScores_" + name + "_NetProbe_" + i + ".csv";
				useMRF.writeScores(outputFilename);
//				useMRF.printCounts();
				System.out.println(useMRF.resultsLine());
			}
		}
	}
	
	/**
	 * Randomly select and remove edges between fraudulent and normal users.
	 */
	private static void edgeRemoveRun() {
		String directory = "graphFeatures_processed/no_jitter/";
		for (String name : new String[]{"repFraud", "hybridNormalEE", "hybridBothVGS"}) {
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				for (int j = 0; j < 51; j++) {
					double proportionOfEdgesRemoved = 0.02 * j;
					UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_" + name + "_20k_" + i, 
							directory + "combinedLof/combinedLof_" + name + "_bidder_" + i + ".csv", 
							directory + "combinedLof/combinedLof_" + name + "_seller_" + i + ".csv");
					// remove edges
					useMRF.graph = GraphOperations.fudgeGraph(useMRF.it, useMRF.graph, proportionOfEdgesRemoved);
//					useMRF.graph = GraphOperations.fudgeGraphFraudsOnly(useMRF.it, useMRF.graph, proportionOfEdgesRemoved);
					useMRF.run(label);
					
					String outputFilename = directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_NetProbe_" + proportionOfEdgesRemoved + "_" + i + ".csv";
//					String outputFilename = directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_NetProbe_" + proportionOfEdgesRemoved + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
//					useMRF.printCounts();
					System.out.println(proportionOfEdgesRemoved + "," + useMRF.resultsLine());
				}
			}
		}
	}

	/**
	 * Run propagation over the files with swapped scores.
	 */
	private static void swappedScoresRun() {
		String directory = "graphFeatures_processed/no_jitter/";
		for (String name : new String[]{
//				"repFraud", 
//				"hybridNormalEE", 
				"hybridBothVGS"
				}) {
			for (int i = 0; i < 20; i++) {
				String label = name + "_" + i;
				System.out.println("doing: " + label);
				
				for (int j = 0; j < 51; j++) {
					double proportionValuesSwapped = 0.02 * j;
					UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_" + name + "_20k_" + i, 
//							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionValuesSwapped + "_bidders_" + i + ".csv", 
//							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_fc_" + proportionValuesSwapped + "_sellers_" + i + ".csv");
							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionValuesSwapped + "_bidders_" + i + ".csv", 
							directory + "combinedLofScoresSwapped/swappedScores_" + name + "_nc_" + proportionValuesSwapped + "_sellers_" + i + ".csv");
					useMRF.run(label);
					
//					String outputFilename = directory + "finalScores_fScoresSwapped/finalScores_" + name + "_NetProbe_" + proportionValuesSwapped + "_" + i + ".csv";
					String outputFilename = directory + "finalScores_nScoresSwapped/finalScores_" + name + "_NetProbe_" + proportionValuesSwapped + "_" + i + ".csv";
					useMRF.writeScores(outputFilename);
//					useMRF.printCounts();
					System.out.println(proportionValuesSwapped + "," + useMRF.resultsLine());
				}
			}
		}
	}

	private static void propMatrixValueTest(String directory) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("PropMatrixTest_NetProbe_0.csv"), Charset.defaultCharset());
		
		for (int i = 1; i < 15; i++) 
		{
//			int i = 1;
//			int j = 3;
			for (int j = 1; j < 15; j++) 
			{
				double eo = i * 0.04;
				double ep = j * 0.04;

				MRF_NetProbe.eo = eo;
				MRF_NetProbe.ep = ep;

				StringBuffer sb = new StringBuffer();
				sb.append(eo + "," + ep);
				for (String name : new String[] { "repFraud", "hybridNormalEE", "hybridBothVGS" }) {

					String label = name + "_0";
					
					System.out.println("doing: " + name + "|" + eo + ", " + ep);

					UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_" + name + "_20k_0", 
							directory + "combinedLof/combinedLof_" + name + "_bidder_0.csv", 
							directory + "combinedLof/combinedLof_" + name + "_seller_0.csv");
//					UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_" + name + "_20k_0", 
//							directory + name + "combinedScores_bidders.csv", 
//							directory + name + "combinedScores_sellers.csv");
					try {
						useMRF.run(label);
					} catch (AssertionError e){
						e.printStackTrace();
						sb = new StringBuffer();
						continue;
					}

					String outputFilename = directory + "/temp4_netprobe/finalScores_" + name + "_" + eo + "-" + ep + "_NetProbe.csv";
					useMRF.writeScores(outputFilename);

					sb.append("," + name + "," + useMRF.resultsLine());
				}
				
				if (sb.length() != 0) {
					bw.write(sb.toString());
					bw.newLine();
				}
			}
			bw.flush();
		}
		bw.close();
	}

	
	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;
	SimDBAuctionIterator it;
	public UseMRF_NetProbe(String dbName, String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
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
				scores.put(id, score);
			}
		}
		
		this.outlierScores = scores;
		this.userTypes = userTypes;
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
	
	private void printCounts() {
		TreeMap<String, Multiset<State>> stateCounts = new TreeMap<>();
		for (Integer user : nodeBeliefs.keySet()) {
			String userType = userTypes.get(user);
			if (!stateCounts.containsKey(userType)) {
				stateCounts.put(userType, HashMultiset.<State>create());
			}
			stateCounts.get(userType).add(nodeBeliefs.get(user).state);
		}
		
		for (String userType : stateCounts.keySet()) {
			System.out.print(userType);
			for (State state : State.values()) {
				System.out.print(", " + state + ": " + stateCounts.get(userType).count(state));
			}
			System.out.println();
		}
		
	}
	/**
	 * Results in 1 line...
	 */
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
