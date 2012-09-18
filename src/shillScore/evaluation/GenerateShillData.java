package shillScore.evaluation;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import createUserFeatures.BuildSimFeatures;
import createUserFeatures.Feature;
import createUserFeatures.Features;
import createUserFeatures.SimAuctionDBIterator;
import createUserFeatures.SimAuctionIterator;
import createUserFeatures.SimAuctionMemoryIterator;
import createUserFeatures.UserFeatures;

import agents.shills.Hybrid;
import agents.shills.LowBidShillPair;
import agents.shills.ModifiedHybrid;
import agents.shills.MultiSellerHybrid;
import agents.shills.NonAltHybrid;
import agents.shills.RandomHybrid;
import agents.shills.SimpleShillPair;
import agents.shills.strategies.LowPriceStrategy;
import agents.shills.strategies.ModifiedTrevathanStrategy;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;
import shillScore.BuildCollusiveShillScore;
import shillScore.BuildShillScore;
import shillScore.CollusiveShillScore;
import shillScore.ShillScore;
import shillScore.WriteScores;
import shillScore.BuildShillScore.ShillScoreInfo;
import shillScore.CollusiveShillScore.ScoreType;
import simulator.AgentAdder;
import simulator.Main;
import simulator.database.DBConnection;
import simulator.database.KeepObjectsInMemory;

public class GenerateShillData {

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

		int numberOfRuns = 1000;
		
//		writeSSandPercentiles(simplePairAdderA, numberOfRuns, new double[]{1,1,1,1,1,1});
		run(simplePairAdderA, numberOfRuns);
//		run(simplePairAdderA, numberOfRuns, new double[]{0.0820,0.0049,-0.0319,0.5041,0.2407,0.2003});
//		writeSSandPercentiles(simplePairAdderB, numberOfRuns);
//		writeSSandPercentiles(simplePairAdderC, numberOfRuns);
		
//		collusiveShillPairMultipleRuns(hybridAdderB, numberOfRuns);
//		collusiveShillPairMultipleRuns(randomHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(multisellerHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(hybridAdderC, numberOfRuns);
//		collusiveShillPairMultipleRuns(nonAltHybridA, numberOfRuns);
	}
	
	private static void run(AgentAdder adder, int numberOfRuns, double[]... weightSets) {
		for (int runNumber = 772; runNumber < numberOfRuns; runNumber++) {
			System.out.println("starting run " + runNumber);
			
//			List<Feature> featuresSelected = Features.defaultFeatures;
			List<Features> featuresSelected = Arrays.asList(Features.values());

			KeepObjectsInMemory objInMem = new KeepObjectsInMemory();
			Main.run(objInMem, adder); // run simulator
			Map<Integer, UserFeatures> features = new BuildSimFeatures(true).build(new SimAuctionMemoryIterator(objInMem, true)); // build features
			BuildSimFeatures.writeToFile(features.values(), // write features
					featuresSelected, 
					Paths.get("single_feature_shillvsnormal", "synUserFeatures_" + Features.fileLabels(featuresSelected) + "_" + runNumber + ".csv")
					);
			writeSSandPercentiles(new SimAuctionMemoryIterator(objInMem, true), adder, runNumber, weightSets); // build and write shill scores
			
//			BuildSimFeatures buildFeatures = new BuildSimFeatures(true);
//			Main.run(adder);
//			writeSSandPercentiles(new SimAuctionDBIterator(DBConnection.getSimulationConnection(), true), adder, runNumber, weightSets);
//			Map<Integer, UserFeatures> features = buildFeatures.build(new SimAuctionDBIterator(DBConnection.getSimulationConnection(), true));
			
//			return;
		}
	}
	
	/**
	 * Calculate shill scores for synthetic data.
	 * Write those scores out to files, and also the ssPercentiles. 
	 * @param adder
	 * @param runNumber
	 * @param weightSets
	 */
	private static void writeSSandPercentiles(SimAuctionIterator simAuctionIterator, AgentAdder adder, int runNumber, double[]... weightSets) {
		String runLabel = adder.toString() + "." + runNumber;

		// build shillScores
		ShillScoreInfo ssi = BuildShillScore.build(simAuctionIterator);
		
		// write out shill scores
		WriteScores.writeShillScores(ssi.shillScores, ssi.auctionCounts, runLabel, weightSets);
		
		// write out how many wins/losses by shills, and the normalised final price compared to non-shill auctions
		ShillWinLossPrice.writeToFile(runLabel);
		
		List<List<Double>> ssPercentiless = new ArrayList<List<Double>>();
		ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, ShillScore.DEFAULT_WEIGHTS));
		for (double[] weights : weightSets) {// calculate the percentiles for the other weight sets
			ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, weights));
		}
		String ssPercentilesRunLabel = runLabel;
		for (double[] weights : weightSets) {
			ssPercentilesRunLabel += "." + Arrays.toString(weights).replaceAll(", " , "");
		}
		
		// write out percentiles
		ShillVsNormalSS.writePercentiles(Paths.get("shillingResults", "comparisons", "ssPercentiles.csv"), ssPercentilesRunLabel, ssPercentiless);
		
		// write out the number of shill auctions for which the shill had the highest (or not highest) SS
		ShillVsNormalSS.ssRankForShills(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, Paths.get("shillingResults", "comparisons", "rank.csv"), runLabel, weightSets);
		
//		WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
	}

	public static List<Double> splitAndCalculatePercentiles(Collection<ShillScore> sss, Map<Integer, Integer> auctionCounts, double[] weights) {
		List<Double> shillSS = new ArrayList<>();
		List<Double> normalSS = new ArrayList<>();
		
		for (ShillScore ss : sss) { // sort SS for shills and normals into different lists
//			if (ss.userType.toLowerCase().contains("puppet")) { // TODO:
			if (ss.getId() > 5000) { // TODO:
				shillSS.add(ss.getShillScore(auctionCounts, weights));
			} else {
				normalSS.add(ss.getShillScore(auctionCounts, weights));
			}
		}
		return ShillVsNormalSS.percentiles(normalSS, shillSS);
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
			ShillVsNormalSS.writePercentiles(Paths.get("shillingResults", "comparisons", "cssPercentiles.csv"), runLabel, Collections.singletonList(ssPercentiles));
			
//			WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
		}
	}

}
