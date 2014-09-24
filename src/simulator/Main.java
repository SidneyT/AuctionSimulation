package simulator;


import java.util.Arrays;

import simulator.database.DBConnection;
import simulator.database.KeepObjectsInMemory;
import simulator.database.SaveToDatabase;
import simulator.database.SimulationCreateTableStmts;
import agents.repFraud.RepFraudController;
import agents.shills.*;
import agents.shills.puppets.*;
import agents.shills.strategies.*;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start.");
		long t1 = System.nanoTime();

//		for (int i = 0; i < 20; i++) {
//			for (int j = 10; j > 0; j--) {
////				System.out.println(j);
//				AuctionHouse.changeUnitLength(j);
//				normalsOnly();
//				break;
//			}
//		}
//		
//		withFraudsIntoDiffDatabases();
//		whileAway1();
//		whileAway2();
		
		long delta = System.nanoTime() - t1;
		System.out.println(delta / 1000000000 + "s");
		System.out.println("Finished.");
	}

//	private static void whileAway2() {
//		ConstructGraph.allCombosFraudOnlyMultiDB();
//		ConstructGraph.allCombosMultiDB();
//	}
	// has loops for running big batches of simulations
	private static void whileAway1() {
		Strategy trevathan = new TrevathanStrategy(0.95, 0.85, 0.85);
		Strategy waitStart = new WaitStartStrategy(0.95, 0.85, 0.85);
		Strategy lowPrice = new LowPriceStrategy();

//		for (int i : Arrays.asList(0)) {
//			String databaseName = "syn_normal_20k_" + i;
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 20000);
//			System.out.println("finished run " + i);
//		}

//		AgentAdder simpleWS = SimpleShillPair.getAgentAdder(200, waitStart); // can use 20, since each submits 10 auctions.
//		for (int i : Arrays.asList(1,2,3,4)) {
//			String databaseName = "syn_simpleWS_20k_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 20000, simpleWS);
//			System.out.println("finished run " + i);
//		}

////		AgentAdder hybridBoth = CollusionController.getAgentAdder(10 * 5, waitStart, PuppetClusterBidderCombined.getFactory());
//		AgentAdder hybridBoth = CollusionController.getAgentAdder(10 * 3, waitStart, PuppetClusterBidderSellerCombined.getFactory());
////		Main.run(SaveToDatabase.instance(), hybrid);
//		for (int i : Arrays.asList(0)) {
//			String databaseName = "syn_hybridBothBS_20k_" + i;
//			// construct database and tables to store simulation data
////			DBConnection.createDatabase(databaseName);
////			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 20000, hybridBoth);
//			System.out.println("finished run " + i);
//		}

		AgentAdder hybridBothVGS = CollusionController.getAgentAdderVaryGroupSize(10 * 3, waitStart, PuppetClusterBidderSellerCombined.getFactory());
//		Main.run(SaveToDatabase.instance(), hybrid);
		for (int i : Arrays.asList(5,6,7,8,9,10,11,12,13,14,15,16,17,18,19)) {
			String databaseName = "syn_hybridBothVGS_20k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 20000, hybridBothVGS);
			System.out.println("finished run " + i);
		}

		AgentAdder hybrid = HybridT.getAgentAdder(10 * 5, waitStart, PuppetClusterBidderCombined.getFactory());
//		Main.run(SaveToDatabase.instance(), hybrid);
		for (int i : Arrays.asList(20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39)) {
			String databaseName = "syn_hybridNormalEE_20k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), 20000, hybrid);
			System.out.println("finished run " + i);
		}

//		AgentAdder repFraud = RepFraudController.getAgentAdder(1, 40, 500);
//		for (int i : Arrays.asList(3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19)) {
//			String databaseName = "syn_repFraud_20k_small_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 20000, repFraud);
//			
//			System.out.println("finished run " + i);
//		}
		
//		AgentAdder repFraud_3 = RepFraudController.getAgentAdder(3, 40, 800);
//		for (int i : Arrays.asList(10,11,12,13,14,15,16,17,18,19)) {
//			String databaseName = "syn_repFraud_20k_3_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 20000, repFraud_3);
//			
//			System.out.println("finished run " + i);
//		}
		
	}

	private static void withFraudsIntoDiffDatabases() {
		Strategy travethan = new TrevathanStrategy(0.95, 0.85, 0.85);
		Strategy waitStart = new WaitStartStrategy(0.95, 0.85, 0.85);
		Strategy lowPrice = new LowPriceStrategy();

//		String databaseName = "syn_normal_100k_2";
//		DBConnection.createDatabase(databaseName);
//		SimulationCreateTableStmts.createSimulationTables(databaseName);
//		// run the simulation and store everything into the database
//		Simulation.run(SaveToDatabase.instance(databaseName), 100000);

//		AgentAdder trevathan = SimpleShillPair.getAgentAdder(30, travethan); // can use 20, since each submits 10 auctions.
//		for (int i = 0; i < 30; i++) {
//			String databaseName = "auction_simulation_simple" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Main.run(SaveToDatabase.instance(databaseName), trevathan);
//		}
//
//		AgentAdder delayedStart = SimpleShillPair.getAgentAdder(30, waitStart);
//		for (int i = 0; i < 30; i++) {
//			String databaseName = "auction_simulation_delayedStart" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Main.run(SaveToDatabase.instance(databaseName), delayedStart);
//		}
//		AgentAdder hybrid = HybridLowPrice.getAgentAdder(20, waitStart, lowPrice); // can use 20, since each submits 10 auctions.
//		for (int i = 0; i < 30; i++) {
//			String databaseName = "syn_hybridLP_" + i;
//			// construct database and tables to store simulation data
////			DBConnection.createDatabase(databaseName);
////			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Main.run(SaveToDatabase.instance(databaseName), hybrid);
//		}
//		AgentAdder hybrid = HybridTVaryCollusion.getAgentAdder(10 * 25, waitStart); // can use 20, since each submits 10 auctions.
//		for (int i = 0; i < 3; i++) {
//			String databaseName = "syn_hybridTVC_10k_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 100000, hybrid);
//		}
//		AgentAdder hybrid = HybridT.getAgentAdder(10 * 25, waitStart, PuppetClusterBidderCombined.getFactory());
////		Main.run(SaveToDatabase.instance(), hybrid);
//		for (int i = 2; i < 4; i++) {
//			String databaseName = "syn_hybridNormal_100k_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 100000, hybrid);
//			System.out.println("finished run " + i);
//		}
//		AgentAdder repFraud = RepFraudController.getAgentAdder(3, 40, 800);
//		for (int i = 2; i < 4; i++) {
//			String databaseName = "syn_repFraud_100k_3_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Simulation.run(SaveToDatabase.instance(databaseName), 100000, repFraud);
//			
//			System.out.println("finished run " + i);
//		}
}
	
	/**
	 * Generates synthetic data with only normal users.
	 */
	private static void normalsOnly() {
		long start = System.nanoTime();
		Simulation.run(KeepObjectsInMemory.instance());
//		Simulation.run(SaveToDatabase.instance());
		long end = System.nanoTime();
		System.out.println((end - start) / 1000000);
//		ClusterAnalysis.clusterSimData("");
	}

}