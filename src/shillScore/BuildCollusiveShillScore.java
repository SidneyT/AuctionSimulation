package shillScore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import shillScore.BuildShillScore.ShillScoreInfo;
import shillScore.CollusiveShillScore.ScoreType;
import shillScore.evaluation.ShillVsNormalSS;
import shillScore.evaluation.ShillVsNormalSS.TpFpPair;
import simulator.database.DatabaseConnection;
import util.CombinationGenerator;
import util.Util;


/**
 * Based on <i>Detecting Collusive Shill Bidding</i> by <i>Trevathan et al.</i> .
 * Calculates the collusive shill score for the alternating bid, alternating auction,
 * and hybrid strategy.
 */
public class BuildCollusiveShillScore {

	public static void main(String[] args) {
		System.out.println("Start.");
//		for (int i = 0; i < 20; i++) {
//			System.out.println("run " + i);

			ShillScoreInfo ssi = BuildShillScore.build();
			Map<Integer, CollusiveShillScore> collusionScores = new BuildCollusiveShillScore().build(ssi);
			WriteScores.writeCollusiveShillScore(ssi.shillScores, collusionScores, "suffix");

//			Main.run(AlternatingAuction.getAgentAdder(5));
//			run(Type.AltAuc, "");
//			new Main().run(AlternatingBid.getAgentAdder(5));
//			run(Type.AltBid, "");
//			new Main().run(Hybrid.getAgentAdder(5));
			List<Double> scores = getPercentiles(ScoreType.Hybrid, ssi, collusionScores, "");
			
//		}
		System.out.println("Finished.");
	}
	
	public static List<Double> getPercentiles(ScoreType type, ShillScoreInfo ssi, Map<Integer, CollusiveShillScore> collusionScores, String suffix) {
		List<Double> normalCollusionScores = new ArrayList<>();
		List<Double> shillCollusionScores = new ArrayList<>();
		
		for (int id : collusionScores.keySet()) {
			ShillScore ss = ssi.shillScores.get(id);
			CollusiveShillScore css = collusionScores.get(id);
			
			if (ss.userType.toLowerCase().contains("puppet")) {
				shillCollusionScores.add(css.getScore(type, ss));
			} else {
				normalCollusionScores.add(css.getScore(type, ss));
			}
		}

		List<Double> percentiles = ShillVsNormalSS.percentiles(normalCollusionScores, shillCollusionScores);
//		CompareShillScores.writePercentiles(Paths.get("shillingResults", "comparisons", "Percentiles" + type.name + suffix + ".csv"), percentiles, "");
		
//		List<TpFpPair> tpFps = CompareShillScores.generateTpFp(percentiles);
//		CompareShillScores.writeTpFps(Paths.get("shillingResults", "comparisons", "TpFps" + type.name + suffix + ".csv"), tpFps);
		
		return percentiles;
		
	}
	
	public static Map<Integer, CollusiveShillScore> build(ShillScoreInfo ssi) {
		Map<Integer, CollusiveShillScore> collusionScores = new HashMap<>();
		
		// fill up the collusionScores map. 1 collusionShillScore object for every shillScore object
		for (int bidderId : ssi.shillScores.keySet()) {
			collusionScores.put(bidderId, new CollusiveShillScore(bidderId));
		}

		
		Mapping mapping = new Mapping(); // stores the 1 to 1 relationships between index and userId
		int[][] collusionGraph = buildCollusionGraph(mapping);
		double[] etaRatings = normalisedEdgeSums(collusionGraph);
		
		// store the eta ratings in collusiveShillScore objects
		for (int i = 0; i < etaRatings.length; i++) { 
			collusionScores.get(mapping.toId(i)).setEta(etaRatings[i]);
		}
		
		int[][] dualGraph = makeDualGraph(collusionGraph);
		double[] thetaRatings = normalisedEdgeSums(dualGraph);
		
		for (int i = 0; i < thetaRatings.length; i++) { 
			collusionScores.get(mapping.toId(i)).setTheta(thetaRatings[i]);
		}
		
		List<List<Integer>> groupings = groupEta(etaRatings);
		calculateBindingFactors(collusionScores, groupings, ssi.shillScores, ssi.auctionCounts, mapping);
		
//		System.out.println(collusionScores);
		return collusionScores;
	}
	
	/**
	 * Calculate the binding factor B (for alternating bid) and A (for alternating auction) 
	 * for all users, and store them in the collusionScores map.
	 * @param etaGroups Users grouped by their etaRatings 
	 * @param shillScores 
	 * @param auctionCounts 
	 * @return
	 */
	private static Map<Integer, CollusiveShillScore> calculateBindingFactors(Map<Integer, CollusiveShillScore> collusionScores, 
			List<List<Integer>> etaGroups, Map<Integer, ShillScore> shillScores, Map<Integer, Integer> auctionCounts, Mapping mapping) {
		for (List<Integer> group : etaGroups) {
			for (int i = 0; i < group.size(); i++) {
				int iId = mapping.toId(group.get(i));
				CollusiveShillScore cs = collusionScores.get(iId);
				for (int j = 0; j < group.size(); j++) {
					if (i == j) // or jId == iId
						continue;
					int jId = mapping.toId(group.get(j));
					// calculate binding factor B
					double jBeta = shillScores.get(jId).getBeta();
//					System.out.println("jId: " + shillScores.get(jId));
					double iBeta = shillScores.get(iId).getBeta();
//					System.out.println("iId: " + shillScores.get(iId));
					double bindingFactorB = bindingFactor(iBeta, jBeta);
					cs.bindingFactorB.incrementalAvg(bindingFactorB);
					
					// calculate binding factor A
					double jAlpha = shillScores.get(jId).getAlpha(auctionCounts).maxAlpha;
					double iAlpha = shillScores.get(iId).getAlpha(auctionCounts).maxAlpha;
					double bindingFactorA = bindingFactor(iAlpha, jAlpha);
					cs.bindingFactorA.incrementalAvg(bindingFactorA);
				}
				
			}
		}
		return collusionScores;
	}
	
	private static int[][] buildCollusionGraph(Mapping mapping){
		try {
			Connection conn = DatabaseConnection.getSimulationConnection();
			
			// make a collusion graph of the right size
			CallableStatement stmt1;
				stmt1 = conn.prepareCall("SELECT COUNT(DISTINCT bidderId) as count FROM bids as b JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL");
			ResultSet countResult = stmt1.executeQuery();
			countResult.next();
			int userCount = countResult.getInt("count");
			int[][] collusionGraph = new int[userCount][userCount];
			
			CallableStatement stmt2 = conn.prepareCall(
					"SELECT a.listingId, b.bidderId FROM auctions as a " +
					"JOIN bids as b ON a.listingId=b.listingId " +
					"WHERE endTime IS NOT NULL ORDER BY a.listingId;");
			ResultSet bidsResult = stmt2.executeQuery();
	
			int listingId = -1;
			Set<Integer> seen = new HashSet<>();
			while (bidsResult.next()) {
				int newListingId = bidsResult.getInt("listingId");
				if (listingId != newListingId) {
					if (!seen.isEmpty()) {
						collusionGraphUpdate(collusionGraph, new ArrayList<>(seen), mapping);
						seen.clear();
					}
					listingId = newListingId;
				}
				seen.add(bidsResult.getInt("bidderId"));
			}
			collusionGraphUpdate(collusionGraph, new ArrayList<>(seen), mapping);
			
			return collusionGraph;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void collusionGraphUpdate(int[][] collusionGraph, List<Integer> bidderIds, Mapping mapping) {
		if (bidderIds.size() <= 1) {
			mapping.toIndex(bidderIds.get(0));
			return;
		}
		
		CombinationGenerator cb = new CombinationGenerator(bidderIds.size(), 2);
		while (cb.hasMore()) {
			int[] pair = cb.getNext();
			int firstId = mapping.toIndex(bidderIds.get(pair[0]));
			int secondId = mapping.toIndex(bidderIds.get(pair[1]));
			collusionGraph[firstId][secondId]++;
			collusionGraph[secondId][firstId]++;
		}
	}
	
	private static int[][] makeDualGraph(int[][] collusionGraph) {
		int[][] dualCollusionGraph = new int[collusionGraph.length][collusionGraph[0].length];
		for (int i = 0; i < collusionGraph.length; i++) {
			for (int j = 0; j < collusionGraph[i].length; j++) {
				if (collusionGraph[i][j] == 0)
				dualCollusionGraph[i][j] = 1;
			}
		}
		
		return dualCollusionGraph;
	}
	
	private static class Mapping {
		private Map<Integer, Integer> mapping = new HashMap<>(); // map (userId, index)
		private List<Integer> reverseMapping = new ArrayList<>(); // index is index, value is userId.
		private int toIndex(int userId) {
			Integer index;
			index = mapping.get(userId);
			if (index != null) {
				return index;
			} else {
				mapping.put(userId, reverseMapping.size());
				reverseMapping.add(userId);
				return reverseMapping.size() - 1;
			}
		}
		private int toId(int index) {
			return reverseMapping.get(index);
		}
	}
	
	private static double bindingFactor(double s1, double s2) {
		if (s1 == s2)
			return 1;
		else if (s1 < s2)
			return s1/s2;
		else // s2 > s1
			return s2/s1;
	}
	
	/**
	 * collusion ratings. Sum of edge weights normalised with max/min from all bidders.
	 * @param collusionGraph
	 * @return
	 */
	private static double[] normalisedEdgeSums(int[][] collusionGraph) {
		// for normalising
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;

		double[] collusionRatings = new double[collusionGraph.length];
		for (int i = 0; i < collusionGraph.length; i++) {
			int rating = 0; // hold the sum of edge values 
			for (int j = 0; j < collusionGraph[i].length; j++) {
				rating += collusionGraph[i][j];
			}
			if (rating > max)
				max = rating;
			if (rating < min)
				min = rating;
			collusionRatings[i] = rating;
		}
		
		// normalise
		for (int i = 0; i < collusionRatings.length; i++) {
			collusionRatings[i] = Util.normalise(collusionRatings[i], min, max);
		}
		
		return collusionRatings;
	}
	
	/**
	 * Group users according to their eta ratings.
	 * @param collusionRatings
	 * @return
	 */
	private static List<List<Integer>> groupEta(double[] collusionRatings) {
		List<List<Integer>> groups = new ArrayList<>();
		List<Double> groupValue = new ArrayList<>();
		double error = 0.02;
		for (int i = 0; i < collusionRatings.length; i++) {
			boolean addedToAGroup = false;
			for (int j = 0; j < groups.size(); j++) {
				double value = collusionRatings[i];
				if (Math.abs(groupValue.get(j) - value) < error) { // belongs in this group
					groups.get(j).add(i);
					addedToAGroup = true;
				}
			}
			if (!addedToAGroup) {
				groups.add(new ArrayList<Integer>());
				groups.get(groups.size() - 1).add(i);
				groupValue.add(collusionRatings[i]);
			}
		}
		return groups;
	}
	
	private void test() {
		Mapping mapping = new Mapping();
		int[][] collusionGraph = new int[4][4];
		collusionGraphUpdate(collusionGraph, Arrays.asList(3,6,9), mapping);
		System.out.println("mapping: " + mapping);
		for (int i = 0; i < collusionGraph.length; i++) {
			System.out.println("cg: " + Arrays.toString(collusionGraph[i]));
		}
		collusionGraphUpdate(collusionGraph, Arrays.asList(3,6,4), mapping);
		System.out.println("mapping: " + mapping);
		for (int i = 0; i < collusionGraph.length; i++) {
			System.out.println("cg: " + Arrays.toString(collusionGraph[i]));
		}
		collusionGraphUpdate(collusionGraph, Arrays.asList(3,6,9), mapping);
		System.out.println("mapping: " + mapping);
		for (int i = 0; i < collusionGraph.length; i++) {
			System.out.println("cg: " + Arrays.toString(collusionGraph[i]));
		}
		System.out.println("reverseMapping: " + mapping.reverseMapping);
		System.out.println("array: " + Arrays.toString(normalisedEdgeSums(collusionGraph)));
	}
	
	private void testGroupEtaRatings() {
		System.out.println(groupEta(new double[]{0.1,0.2,0.12,0.3,0.14,0.3}));
	}

}
