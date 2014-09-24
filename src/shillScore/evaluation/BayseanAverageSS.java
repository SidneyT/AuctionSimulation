package shillScore.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;

import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimAuctionIterator;
import createUserFeatures.SimDBAuctionIterator;


import agents.shills.SimpleShillPair;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;
import shillScore.BuildShillScore;
import shillScore.ShillScore;
import shillScore.BuildShillScore.ShillScoreInfo;
import simulator.AgentAdder;
import simulator.database.DBConnection;
import util.IncrementalMean;
import util.Util;


public class BayseanAverageSS {

	public static void main(String[] args) {
		
		for (int i = 0; i < 20; i++) {
			String dbName = "syn_simplet_10k_" + i;
			
			System.out.println("run " + i);
			
			Connection conn = DBConnection.getConnection(dbName);
			SimAuctionIterator simIt = new SimDBAuctionIterator(conn, true);
			
			// run simulation
//			Main.run(simplePairAdderA);
			
			//build shill scores and baysean SS
			ShillScoreInfo ssi = BuildShillScore.build(simIt);

//			System.out.println(Joiner.on("\r\n").join(ssi.shillScores.values()));
			
			BayseanSS bayseanSS = new BayseanSS(ssi.shillScores.values(), ssi.auctionCounts);

			// write percentile comparisons between SS and BSS
			writeSSandBSSPercentiles(ssi, bayseanSS, dbName);
			
			// record rank information
			Path rankFile = Paths.get("shillingResults", "comparisons", "rank.csv");
			Map<Integer, UserObject> users = simIt.users();
			ShillVsNormalSS.writeRanks(ssi.shillScores, bayseanSS, ssi.auctionBidders, ssi.auctionCounts, users, rankFile, dbName + ".BSS");
			ShillVsNormalSS.ssRankForShills(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, users, rankFile, dbName);
		}
		
	}
	
//	public static Map<Integer, Double> calculateSSBayseanAverages(ShillScoreInfo ssi) {
//		
//		Map<Integer, ShillScore> shillScores = ssi.shillScores;
//		IncrementalAverage avgNumLoss = new IncrementalAverage(); 
//		IncrementalAverage avgShillScore = new IncrementalAverage();
//		for (ShillScore ss : shillScores.values()) {
//			if (ss.lossCount == 0)
//				continue;
//			avgNumLoss.incrementalAvg(ss.lossCount);
//			avgShillScore.incrementalAvg(ss.getShillScore(ssi.auctionCounts));
//		}
//		
////		System.out.println("avgNumLoss:" + avgNumLoss);
////		System.out.println("avgShillScore:" + avgShillScore);
//		
//		Map<Integer, Double> bayseanSS = new HashMap<>();
//		for (Entry<Integer, ShillScore> ssEntry : shillScores.entrySet()) {
//			ShillScore ss = ssEntry.getValue();
//			double bss = ss.bayseanSS(avgNumLoss.getAverage(), avgShillScore.getAverage(), ssi.auctionCounts);
////			double bss = ss.bayseanSS(1, avgShillScore.getAverage(), ssi.auctionCounts);
//			bayseanSS.put(ssEntry.getKey(), bss);
////			bayseanSS.put(ssEntry.getKey(), ssEntry.getValue().getShillScore(ssi.auctionCounts)); // remove 
//		}
//		
////		writeSSandBSS(ssi, bayseanSS);
//		return bayseanSS;
//	}
	
	public static class BayseanSS {
		private final IncrementalMean avgNumLoss = new IncrementalMean(); 
		private final IncrementalMean avgShillScore = new IncrementalMean();
		
		private final Multiset<Integer> auctionCounts; 
		public BayseanSS(Collection<ShillScore> shillScores, Multiset<Integer> auctionCounts) {
			for (ShillScore ss : shillScores) {
//				System.out.println(ss);
				if (ss.getLossCount() == 0)
					continue;
				avgNumLoss.add(ss.getLossCount());
				avgShillScore.add(ss.getShillScore(auctionCounts));
			}
			this.auctionCounts = auctionCounts;
		}
		
		public double bss(ShillScore ss, int sellerId) {
//			return ss.getShillScore(auctionCounts, sellerId);
			return ss.bayseanSS(avgNumLoss.average(), avgShillScore.average(), auctionCounts, sellerId);
		}
		
		public double bss(ShillScore ss) {
			return ss.bayseanSS(avgNumLoss.average(), avgShillScore.average(), auctionCounts);
		}
	}
	
	public static void writeSSandBSSPercentiles(ShillScoreInfo ssi, BayseanSS bayseanSS, String runLabel) {
		
		List<Double> shillSS = new ArrayList<>();
		List<Double> normalSS = new ArrayList<>();
		List<Double> shillBSS = new ArrayList<>();
		List<Double> normalBSS = new ArrayList<>();
		List<Double> shillESS = new ArrayList<>();
		List<Double> normalESS = new ArrayList<>();

		for (int id : ssi.shillScores.keySet()) {
			ShillScore ss = ssi.shillScores.get(id);
			String userType = ss.userType.toLowerCase();
			
			double score = ss.getShillScore(ssi.auctionCounts);
			if (Double.isNaN(score))
				continue;
			
			double bscore = bayseanSS.bss(ss);
			
//			System.out.println("||" + id + "," + userType);
			if (userType.contains("puppet") || userType.contains("sb")) {
				shillSS.add(score);
				shillBSS.add(bscore);
				shillESS.add(ss.getShillScore(ssi.auctionCounts, ShillScore.EQUAL_WEIGHTS));
			} else {
				normalSS.add(score);
				normalBSS.add(bscore);
				normalESS.add(ss.getShillScore(ssi.auctionCounts, ShillScore.EQUAL_WEIGHTS));
			}
			
//			System.out.println(id + "," + userType + "," + Joiner.on(",").join(Doubles.asList(ss.getRawScores(ssi.auctionCounts))) + "," + score + "," + bscore);
		}
		
		List<Double> ssPercentiles = Util.percentiles(normalSS, shillSS);
		List<Double> bssPercentiles = Util.percentiles(normalBSS, shillBSS);
		List<Double> essPercentiles = Util.percentiles(normalESS, shillESS);
//		System.out.println(normalSS);
//		System.out.println(shillSS);
		ShillVsNormalSS.writePercentiles(Paths.get("shillingResults", "comparisons", "SSvsBSS.csv"), runLabel, Arrays.asList(ssPercentiles, bssPercentiles, essPercentiles));
	}
	
	public static void writeSSandBSS(ShillScoreInfo ssi, Map<Integer, Double> bayseanSS) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "newMeasures", "baysean.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (int id : ssi.shillScores.keySet()) {
				if (ssi.shillScores.get(id).getLossCount() == 0)
					continue;
				bw.append(id + ",");
				bw.append(ssi.shillScores.get(id).getShillScore(ssi.auctionCounts) + ",");
				bw.append(bayseanSS.get(id) + "");
				bw.newLine();
			}
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
