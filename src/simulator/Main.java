package simulator;

import simulator.database.DBConnection;
import simulator.database.SaveToDatabase;
import simulator.database.SimulationCreateTableStmts;
import agents.repFraud.RepFraudController;

public class Main {

	

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start.");
		long t1 = System.nanoTime();

//		normalsOnly();
		withFraudsIntoDiffDatabases();
		
		long delta = System.nanoTime() - t1;
		System.out.println(delta / 1000000);
		System.out.println("Finished.");
	}


	public static void withFraudsIntoDiffDatabases() {
//		Strategy travethan = new TrevathanStrategy(0.95, 0.85, 0.85);
//		Strategy waitStart = new WaitStartStrategy(0.95, 0.85, 0.85);
//		Strategy lowPrice = new LowPriceStrategy();
//
//		String databaseName = "syn_normal_10k_test8";
////		DBConnection.createDatabase(databaseName);
////		SimulationCreateTableStmts.createSimulationTables(databaseName);
//		// run the simulation and store everything into the database
//		Main.run(SaveToDatabase.instance(databaseName));

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
//		for (int i = 0; i < 1; i++) {
//			String databaseName = "syn_hybridTVC_10k_" + i;
//			// construct database and tables to store simulation data
////			DBConnection.createDatabase(databaseName);
////			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Main.run(SaveToDatabase.instance(databaseName), hybrid);
//		}
//		AgentAdder hybrid = HybridT.getAgentAdder(10 * 25, waitStart, PuppetClusterBidderCombined.getFactory());
////		Main.run(SaveToDatabase.instance(), hybrid);
//		for (int i = 0; i < 5; i++) {
//			String databaseName = "syn_hybridNormal_100k_" + i;
//			// construct database and tables to store simulation data
//			DBConnection.createDatabase(databaseName);
//			SimulationCreateTableStmts.createSimulationTables(databaseName);
//			
//			// run the simulation and store everything into the database
//			Main.run(SaveToDatabase.instance(databaseName), hybrid);
//		}
		AgentAdder repFraud = RepFraudController.getAgentAdder(1, 40, 800);
		for (int i = 0; i < 5; i++) {
			String databaseName = "syn_repFraud_100k_" + i;
			// construct database and tables to store simulation data
			DBConnection.createDatabase(databaseName);
			SimulationCreateTableStmts.createSimulationTables(databaseName);
			
			// run the simulation and store everything into the database
			Simulation.run(SaveToDatabase.instance(databaseName), repFraud);
			System.out.println("finished run " + i);
		}
}
	
	/**
	 * Generates synthetic data with only normal users.
	 */
	public static void normalsOnly() {
		Simulation.run(SaveToDatabase.instance());
//		ClusterAnalysis.clusterSimData("");
	}

}