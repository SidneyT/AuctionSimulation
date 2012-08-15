package createShillScores.evaluation;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import agent.shills.Hybrid;
import agent.shills.LowPriceStrategy;
import agent.shills.ModifiedHybrid;
import agent.shills.LowBidShillPair;
import agent.shills.ModifiedTrevathanStrategy;
import agent.shills.MultiSellerHybrid;
import agent.shills.NonAltHybrid;
import agent.shills.RandomHybrid;
import agent.shills.SimpleShillPair;
import agent.shills.Strategy;
import agent.shills.TrevathanStrategy;
import simulator.AgentAdder;
import simulator.Main;
import createShillScores.BuildCollusiveShillScore;
import createShillScores.BuildShillScore;
import createShillScores.CollusiveShillScore;
import createShillScores.CollusiveShillScore.ScoreType;
import createShillScores.ShillScore;
import createShillScores.WriteScores;
import createShillScores.BuildShillScore.ShillScoreInfo;

public class ShillEvaluation {

	public static void main(String[] args) {
		// strategies
		Strategy travethanStrategy = new TrevathanStrategy(0.95, 0.85, 0.85);
		Strategy modifiedTravethanStrategy = new ModifiedTrevathanStrategy(0.95, 0.85, 0.85);
		Strategy lowPriceStrategy = new LowPriceStrategy();
		
		// adders
		AgentAdder simplePairAdderA = SimpleShillPair.getAgentAdder(20, travethanStrategy); // can use 20, since each submits 10 auctions.
		AgentAdder simplePairAdderB = SimpleShillPair.getAgentAdder(20, modifiedTravethanStrategy);
		AgentAdder simplePairAdderC = LowBidShillPair.getAgentAdder(20, travethanStrategy, lowPriceStrategy);
		AgentAdder hybridAdderA = Hybrid.getAgentAdder(5, travethanStrategy, 4); // use only 5 groups, since each group submits 40 auctions. if too many will affect normal auctions too much. 
		AgentAdder hybridAdderB = Hybrid.getAgentAdder(5, modifiedTravethanStrategy, 4);
		AgentAdder hybridAdderC = ModifiedHybrid.getAgentAdder(5, modifiedTravethanStrategy, lowPriceStrategy, 4);
		AgentAdder randomHybridAdderA = RandomHybrid.getAgentAdder(5, travethanStrategy, 4);
		AgentAdder multisellerHybridAdderA = MultiSellerHybrid.getAgentAdder(5, travethanStrategy, 3, 4);
		
		AgentAdder nonAltHybridA = NonAltHybrid.getAgentAdder(5, travethanStrategy, 4);

		int numberOfRuns = 20;
		
//		singleShillPairMultipleRuns(simplePairAdderA, numberOfRuns, new int[]{1,1,1,1,1,1});
//		singleShillPairMultipleRuns(simplePairAdderB, numberOfRuns);
		singleShillPairMultipleRuns(simplePairAdderC, numberOfRuns);
		
//		collusiveShillPairMultiple  Runs(hybridAdderB, numberOfRuns);
//		collusiveShillPairMultipleRuns(randomHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(multisellerHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(hybridAdderC, numberOfRuns);
//		collusiveShillPairMultipleRuns(nonAltHybridA, numberOfRuns);
	}
	
	private static void singleShillPairMultipleRuns(AgentAdder adder, int numberOfRuns, int[]... weightSets) {
//		for (int i = 999; i < 1000; i++) {
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("starting run " + i);
			
			// run the simulator with the adder
			Main.run(adder);
			
			String runLabel = adder.toString() + "." + i;

			// build shillScores
			ShillScoreInfo ssi = BuildShillScore.build();
			
			// write out shill scores
			WriteScores.writeShillScores(ssi.shillScores, ssi.auctionCounts, runLabel, weightSets);
			
			// write out how many wins/losses by shills, and the normalised final price compared to non-shill auctions
			ShillWinLossPrice.writeToFile(runLabel);
			
			List<List<Double>> ssPercentiless = new ArrayList<List<Double>>();
			
			ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, ShillScore.DEFAULT_WEIGHTS));
			
			// calculate the percentiles for the other ones.
			for (int[] weights : weightSets) {
				ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, weights));
			}
			
			String ssPercentilesRunLabel = runLabel;
			for (int[] weights : weightSets) {
				ssPercentilesRunLabel += "." + Arrays.toString(weights).replaceAll(", " , "");
			}
			
			// write out percentiles
			CompareShillScores.writePercentiles(Paths.get("shillingResults", "comparisons", "ssPercentiles.csv"), ssPercentilesRunLabel, ssPercentiless);
			
			// write out the number of shill auctions for which the shill had the highest (or not highest) SS
			CompareShillScores.ssRankForShills(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, Paths.get("shillingResults", "comparisons", "rank.csv"), runLabel, weightSets);
			
//			WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
		}
	}

	public static List<Double> splitAndCalculatePercentiles(Collection<ShillScore> sss, Map<Integer, Integer> auctionCounts, int[] weights) {
		List<Double> shillSS = new ArrayList<>();
		List<Double> normalSS = new ArrayList<>();
		
		for (ShillScore ss : sss) { // sort SS for shills and normals into different lists
			if (ss.userType.toLowerCase().contains("puppet")) {
				shillSS.add(ss.getShillScore(auctionCounts, weights));
			} else {
				normalSS.add(ss.getShillScore(auctionCounts, weights));
			}
		}
		return CompareShillScores.percentiles(normalSS, shillSS);
	}
	
	private static void collusiveShillPairMultipleRuns(AgentAdder adder, int numberOfRuns) {
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("starting run " + i);
			
			// run the simulator with the adder
			Main.run(adder);
			
			String runLabel = adder.toString() + "." + i;

			// build shillScores
			ShillScoreInfo ssi = BuildShillScore.build();
			Map<Integer, CollusiveShillScore> css = BuildCollusiveShillScore.build(ssi);

			// write out shill scores
			WriteScores.writeShillScores(ssi.shillScores, ssi.auctionCounts, runLabel);
			WriteScores.writeCollusiveShillScore(ssi.shillScores, css, runLabel);
			
			// write out how many wins/losses by shills, and the normalised final price compared to non-shill auctions
			ShillWinLossPrice.writeToFile(runLabel);
			
			
			List<Double> ssPercentiles = BuildCollusiveShillScore.getPercentiles(ScoreType.Hybrid, ssi, css, runLabel);
			// write out percentiles
			CompareShillScores.writePercentiles(Paths.get("shillingResults", "comparisons", "cssPercentiles.csv"), runLabel, Collections.singletonList(ssPercentiles));
			
//			WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
		}
	}

}
