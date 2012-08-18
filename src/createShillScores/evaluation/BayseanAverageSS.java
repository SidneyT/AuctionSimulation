package createShillScores.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import agent.shills.SimpleShillPair;
import agent.shills.Strategy;
import agent.shills.TrevathanStrategy;

import simulator.AgentAdder;
import simulator.Main;
import util.IncrementalAverage;

import createShillScores.BuildShillScore;
import createShillScores.ShillScore;
import createShillScores.BuildShillScore.ShillScoreInfo;

public class BayseanAverageSS {

	public static void main(String[] args) {
		
		for (int i = 0; i < 20; i++) {
			System.out.println("run " + i);
			Strategy travethanStrategy = new TrevathanStrategy(0.95, 0.85, 0.85);
			AgentAdder simplePairAdderA = SimpleShillPair.getAgentAdder(20, travethanStrategy);
			
			String label = simplePairAdderA.toString() + "." + i;
			
			// run simulation
//			Main.run(simplePairAdderA);
			
			//build shill scores and baysean SS
			ShillScoreInfo ssi = BuildShillScore.build();
			
			BayseanSS bayseanSS = new BayseanSS(ssi.shillScores.values(), ssi.auctionCounts);

			// write percentile comparisons between SS and BSS
			writeSSandBSSPercentiles(ssi, bayseanSS, simplePairAdderA.toString());
			
			// record rank information
			Path rankFile = Paths.get("shillingResults", "comparisons", "rank.csv");
			CompareShillScores.writeRanks(ssi.shillScores, bayseanSS, ssi.auctionBidders, ssi.auctionCounts, rankFile, label + ".BSS");
			CompareShillScores.ssRankForShills(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, rankFile, label);
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
		private final IncrementalAverage avgNumLoss = new IncrementalAverage(); 
		private final IncrementalAverage avgShillScore = new IncrementalAverage();
		
		private final Map<Integer, Integer> auctionCounts; 
		public BayseanSS(Collection<ShillScore> shillScores, Map<Integer, Integer> auctionCounts) {
			for (ShillScore ss : shillScores) {
				if (ss.lossCount == 0)
					continue;
				avgNumLoss.incrementalAvg(ss.lossCount);
				avgShillScore.incrementalAvg(ss.getShillScore(auctionCounts));
			}
			this.auctionCounts = auctionCounts;
		}
		
		public double bss(ShillScore ss, int sellerId) {
//			return ss.getShillScore(auctionCounts, sellerId);
			return ss.bayseanSS(avgNumLoss.getAverage(), avgShillScore.getAverage(), auctionCounts, sellerId);
		}
		
		public double bss(ShillScore ss) {
			return ss.bayseanSS(avgNumLoss.getAverage(), avgShillScore.getAverage(), auctionCounts);
		}
	}
	
	public static void writeSSandBSSPercentiles(ShillScoreInfo ssi, BayseanSS bayseanSS, String runLabel) {
		
		List<Double> shillSS = new ArrayList<>();
		List<Double> normalSS = new ArrayList<>();
		List<Double> shillBSS = new ArrayList<>();
		List<Double> normalBSS = new ArrayList<>();

		for (int id : ssi.shillScores.keySet()) {
			ShillScore ss = ssi.shillScores.get(id);
			if (ss.userType.toLowerCase().contains("puppet")) {
				shillSS.add(ss.getShillScore(ssi.auctionCounts));
				shillBSS.add(bayseanSS.bss(ss));
			} else {
				normalSS.add(ss.getShillScore(ssi.auctionCounts));
				normalBSS.add(bayseanSS.bss(ss));
			}
		}
		
		List<Double> ssPercentiles = CompareShillScores.percentiles(normalSS, shillSS);
		List<Double> bssPercentiles = CompareShillScores.percentiles(normalBSS, shillBSS);
		
		CompareShillScores.writePercentiles(Paths.get("shillingResults", "comparisons", "SSvsBSS.csv"), runLabel, Arrays.asList(ssPercentiles, bssPercentiles));
	}
	
	public static void writeSSandBSS(ShillScoreInfo ssi, Map<Integer, Double> bayseanSS) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "newMeasures", "baysean.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (int id : ssi.shillScores.keySet()) {
				if (ssi.shillScores.get(id).lossCount == 0)
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