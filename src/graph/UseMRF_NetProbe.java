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

import simulator.database.DBConnection;
import util.CsvManipulation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import createUserFeatures.SimDBAuctionIterator;

public class UseMRF_NetProbe {
	public static void main(String[] args) {
		String directory = "graphFeatures_processed/old/";
		UseMRF_NetProbe useMRF = new UseMRF_NetProbe("syn_repfraud_20k_0", 
				directory + "lof_19,2_99_syn_repFraud_20k_0_bidderGraphFeatures.csv", 
				directory + "lof_17,22_80_syn_repfraud_20k_0_sellerGraphFeatures.csv");
		useMRF.run();
		useMRF.writeScores(directory + "finalScores_NP_syn_repfraud_20k_0.csv");
		useMRF.printCounts();
	}

	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;

	public UseMRF_NetProbe(String dbName, String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection(dbName), true);
		
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
	public HashMap<Integer, Node> run() {
		mrf = new MRF_NetProbe(graph, outlierScores);
		HashMap<Integer, Node> nodeBeliefs = mrf.run();
		
		this.nodeBeliefs = nodeBeliefs;
		
		return nodeBeliefs;
	}
	
	public void writeScores(String outputPath) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			
			writer.append("id,userType,outlierScore,fraudScore");
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
		HashMap<String, Multiset<State>> stateCounts = new HashMap<>();
		for (Integer user : nodeBeliefs.keySet()) {
			String userType = userTypes.get(user);
			if (!stateCounts.containsKey(userType)) {
				stateCounts.put(userType, HashMultiset.<State>create());
			}
			stateCounts.get(userType).add(nodeBeliefs.get(user).state);
		}
		
		for (String userType : stateCounts.keySet()) {
			System.out.println(userType + ": " + stateCounts.get(userType));
		}
		
	}

	
}
