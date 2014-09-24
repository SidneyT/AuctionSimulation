package extraExperiments;

import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import agents.shills.LowBidShillPair;
import agents.shills.SimpleShillPair;
import agents.shills.strategies.LateStartTrevathanStrategy;
import agents.shills.strategies.LowPriceStrategy;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;
import agents.shills.strategies.WaitStartStrategy;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimDBAuctionIterator;
import simulator.AgentAdder;
import simulator.Simulation;
import simulator.categories.ItemType;
import simulator.database.DBConnection;
import simulator.database.SaveToDatabase;
import simulator.database.SimulationCreateTableStmts;
import util.IncrementalSD;

public class DMSExtra {
	public static void main(String[] args) {
//		run("gen_simple_");
//		run("gen_latestart_");
//		run("gen_lowbid_");
//		paramGenerationTest();
		agentPerformanceEval();
		
//		setGeneration2();
//		setGeneration();
//		setLPGeneration();
	}
	
	public static void run(String dbName) {
		IncrementalSD sd = new IncrementalSD();
		for (int i = 0; i < 30; i++) {
			Connection conn = DBConnection.getConnection(dbName + i);
			SimDBAuctionIterator it = new SimDBAuctionIterator(conn, true);
			
			Map<Integer, UserObject> users = it.users();
			Set<Integer> fraudBidders = new HashSet<>();
			Set<Integer> fraudSellers = new HashSet<>();
			for (UserObject uo : users.values()) {
				if (uo.userType.equalsIgnoreCase("sb") || uo.userType.equalsIgnoreCase("puppet")) {
					fraudBidders.add(uo.userId);
				}
				if (uo.userType.equalsIgnoreCase("ss") || uo.userType.equalsIgnoreCase("puppet")) {
					fraudSellers.add(uo.userId);
				}
			}
			
			HashSet<Integer> fraudAuctionIds = new HashSet<>();
			int fraudBid = 0, totalBid = 0;
			for (Pair<SimAuction, List<BidObject>> a : it) {
//				System.out.println(a.getKey() + "," + a.getValue().size());
				List<BidObject> bids = a.getValue();
				
				SimAuction auction = a.getKey();
				if (fraudSellers.contains(auction.sellerId))
					for (BidObject b : bids) {
						if (fraudBidders.contains(b.bidderId)) {
							fraudBid++;
							fraudAuctionIds.add(a.getKey().listingId);
						}
						totalBid++;
					}
			}
//			System.out.println(fraudBid + "," + fraudAuctionIds.size());
			sd.add((double) fraudBid/fraudAuctionIds.size());
		}
		System.out.println(sd.average() + "," + sd.getSD());
	}
	
	private static void paramGenerationTest() {
		double theta = 0.95;
		double alpha = 0.85;
		double mu = 0.85;
		for (int runNum = 10; runNum < 20; runNum++) {
			for (int i = 0; i < 20; i++) {
				paramGenerationTestInner(0.99 - 0.01 * i, alpha, mu, runNum);
				paramGenerationTestInner(theta, 0.99 - 0.01 * i, mu, runNum);
				paramGenerationTestInner(theta, alpha, 0.99 - 0.01 * i, runNum);
			}
		}
	}
	
	private static void paramGenerationTestInner(double theta, double alpha, double mu, int runNum) {
		Strategy trevathan = new TrevathanStrategy(theta, alpha, mu);
		Strategy lateStart = new LateStartTrevathanStrategy(0.95, 0.85, 0.85);
		Strategy lowPrice = new LowPriceStrategy();

//		AgentAdder simpleWS = SimpleShillPair.getAgentAdder(200, waitStart); // can use 20, since each submits 10 auctions.
		AgentAdder simpleWS = SimpleShillPair.getAgentAdder(100, trevathan); // can use 20, since each submits 10 auctions.
		
		try {
			String databaseName = "syn_simpleT_10k_t" + Math.round(theta * 100) + "a" + Math.round(alpha * 100) + "m" + Math.round(mu * 100) + "_" + runNum;
			System.out.println("running " + databaseName);
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 10000, simpleWS);
//			System.out.println("finished run " + i);
		} catch (Exception e) {
			System.out.println("skipping");
		}
		
	}

	private static void setGeneration() {
		Strategy lateStart = new LateStartTrevathanStrategy(0.95, 0.85, 0.85);

		AgentAdder simpleLS = SimpleShillPair.getAgentAdder(100, lateStart); // can use 20, since each submits 10 auctions.
		for (int i = 10; i < 20; i++) {
			String databaseName = "syn_simpleLS2_10k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 10000, simpleLS);
			System.out.println("finished run " + i);
		}
		
	}
	
	private static void setGeneration2() {
		Strategy trevathan = new TrevathanStrategy(0.95, 0.85, 0.85);
		
		AgentAdder simpleTreva = SimpleShillPair.getAgentAdder(100, trevathan); // can use 20, since each submits 10 auctions.
		for (int i = 12; i < 20; i++) {
			String databaseName = "syn_simpleT_10k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 10000, simpleTreva);
			System.out.println("finished run " + i);
		}
		
	}

	private static void setLPGeneration() {
		Strategy trevathan = new TrevathanStrategy(0.95, 0.85, 0.85);
		Strategy lowPrice = new LowPriceStrategy();

		AgentAdder simpleLP = LowBidShillPair.getAgentAdder(100, trevathan, lowPrice); // can use 20, since each submits 10 auctions.
		for (int i = 0; i < 20; i++) {
			String databaseName = "syn_simpleLP_10k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 10000, simpleLP);
			System.out.println("finished run " + i);
		}
		
	}

	static void agentPerformanceEval() {
		for (int j = 0; j < 20; j++) {
//			for (int i : Arrays.asList(6,7,8,9)) {
			for (int i = 0; i < 10; i++) {

				double theta = 0.95;
				double alpha = 0.85;
				double mu = 0.99 - 0.01 * j;
//
//				String num = Math.round(theta) + "";
				String databaseName = "syn_simplet_10k_t" + Math.round(theta * 100) + "a" + Math.round(alpha * 100) + "m" + Math.round(mu * 100) + "_" + i;
//				String databaseName = "syn_simpleLS2_10k_" + i;
//				String databaseName = "syn_simplews_20k_t95a85m84_" + i;
//				String databaseName = "syn_simplels2_10k_" + i;
				agentPerformanceEvalInner(databaseName);
			}
		}
//		"syn_simplews_20k_t96a85m85_1"
	}

	static void agentPerformanceEvalInner(String dbName) {
		Connection conn = DBConnection.getConnection(dbName);
		SimDBAuctionIterator it = new SimDBAuctionIterator(conn, true);
		
		Map<Integer, UserObject> users = it.users();
		Set<Integer> fraudBidders = new HashSet<>();
		Set<Integer> fraudSellers = new HashSet<>();
		for (UserObject uo : users.values()) {
			if (uo.userType.equalsIgnoreCase("sb") || uo.userType.equalsIgnoreCase("puppet")) {
				fraudBidders.add(uo.userId);
			}
			if (uo.userType.equalsIgnoreCase("ss") || uo.userType.equalsIgnoreCase("puppet")) {
				fraudSellers.add(uo.userId);
			}
		}
		
		Map<Integer, ItemType> itemTypes = it.itemTypes();
		
		HashSet<Integer> fraudAuctionIds = new HashSet<>();
		int fraudBid = 0, totalBid = 0;
		IncrementalSD fraudPrice = new IncrementalSD();
		IncrementalSD normalPrice = new IncrementalSD();
		int countLoss = 0;
		int countWin = 0;
		for (Pair<SimAuction, List<BidObject>> a : it) {
//			System.out.println(a.getKey() + "," + a.getValue().size());
			List<BidObject> bids = a.getValue();
			
			SimAuction auction = a.getKey();

			double trueValuation = itemTypes.get(auction.itemTypeId).getTrueValuation();
			BidObject finalBid = bids.get(bids.size() - 1);
			double proportion = (double) finalBid.amount / trueValuation;
			
			
			if (fraudSellers.contains(auction.sellerId)) {
				// count the number of auctions won or lost by shills
				if (bids.size() > 1) {
					if (fraudBidders.contains(finalBid.bidderId)) {
						countWin++;
					} else {
						countLoss++;
						fraudPrice.add(proportion);
					}
				}
				
				for (BidObject b : bids) {
					if (fraudBidders.contains(b.bidderId)) {
						fraudBid++;
						fraudAuctionIds.add(a.getKey().listingId);
					}
					totalBid++;
				}
			} else {
				normalPrice.add(proportion);
			}
		}
		System.out.println(dbName + "," + countWin + "," + countLoss + "," + fraudPrice.average() + "," + fraudPrice.getSD() + "," +normalPrice.average() + "," + normalPrice.getSD());
	}
	
}
