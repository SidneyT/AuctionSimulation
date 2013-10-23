package graph;

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

import com.google.common.collect.Multiset;

import createUserFeatures.SimDBAuctionIterator;

public class UseMRF {
	public static void main(String[] args) {
		String directory = "lof_features_fixed/";
		UseMRF useMRF = new UseMRF("syn_repfraud_20k_0", 
				directory + "lof_19,2_99_syn_repFraud_20k_0_bidderGraphFeatures.csv", 
				directory + "lof_17,22_80_syn_repfraud_20k_0_sellerGraphFeatures.csv");
		useMRF.run();
		useMRF.writeScores(directory + "finalScores_syn_repfraud_20k_0.csv");
	}

	Map<Integer, Multiset<Integer>> graph; 
	Map<Integer, Double> outlierScores;
	Map<Integer, String> userTypes;

	public UseMRF(String dbName, String bidderOutlierScoresFile, String sellerOutlierScoresFile) {
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
	
	Map<Integer, Double> fraudScores;
	MRF mrf;
	public Map<Integer, Double> run() {
		mrf = new MRF(graph, outlierScores);
		HashMap<Integer, Double> fraudBeliefs = mrf.run();
		
		this.fraudScores = fraudBeliefs;
		
		return fraudBeliefs;
	}
	
	public void writeScores(String outputPath) {
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			
			writer.append("id,userType,outlierScore,fraudScore");
			writer.newLine();
			
			Map<Integer, Double> outlierScores = mrf.normalisedOutlierScores();
			
			// write id, userType, fraudScore
			for (Integer user : fraudScores.keySet()) {
				writer.append(user.toString()).append(",");
				writer.append(userTypes.get(user)).append(",");
				writer.append(outlierScores.get(user) + "").append(",");
				writer.append(fraudScores.get(user).toString());
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
