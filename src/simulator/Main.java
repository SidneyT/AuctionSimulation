package simulator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import createUserFeatures.ClusterAnalysis;




import simulator.buffers.BidsToAh;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.MessagesToAh;
import simulator.buffers.MessagesToUsers;
import simulator.buffers.PaymentSender;
import simulator.buffers.TimeMessage;
import simulator.categories.CategoryRecord;
import simulator.categories.ItemType;
import simulator.categories.MockCategories;
import simulator.database.SaveObjects;
import simulator.database.SimulationDbConn;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.records.UserRecord;
import util.CrashOnAssertionErrorRunnable;
import agent.EventListener;
import agent.SimpleUser;
import agent.bidders.ClusterBidder;
import agent.bidders.ClusterEarly;
import agent.bidders.ClusterSnipe;
import agent.sellers.TimedSeller;

public class Main {

	private final static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start.");
		
		Main.run();
		ClusterAnalysis.clusterSimData("");
		System.out.println("Finished.");
	}
	
	/**
	 * Runs the simulator
	 */
	public static void run(AgentAdder... userAdder) {
		emptyTables();
		try {
			go(userAdder);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		CalculateStats.calculateStats();
	}

	public static void emptyTables() {
		Connection conn = SimulationDbConn.getConnection();
		try {
			Statement stmt = conn.createStatement();
			stmt.execute("TRUNCATE feedback;");
			stmt.execute("TRUNCATE bids;");
			stmt.execute("TRUNCATE auctions;");
			stmt.execute("TRUNCATE users;");
			stmt.execute("TRUNCATE itemtypes;");
			stmt.execute("TRUNCATE categories;");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void go(AgentAdder... agentAdder) throws InterruptedException {
		final UserRecord userRecord = new UserRecord();

		// create buffers
		TimeMessage timeMessage = new TimeMessage();
		MessagesToUsers messagesToUsers = new MessagesToUsers();
		MessagesToAh<Auction> auctionsToAh = new MessagesToAh<Auction>();
		MessagesToAh<Feedback> feedbackToAh = new MessagesToAh<Feedback>();
		BidsToAh bidsToAh = new BidsToAh();
		BufferHolder bh = new BufferHolder(timeMessage, messagesToUsers, auctionsToAh, feedbackToAh, bidsToAh);

		// create item and payment senders
		PaymentSender ps = new PaymentSender();
		ItemSender is = new ItemSender();

		// create item types
		CategoryRecord cr = MockCategories.createCategories();
		SaveObjects.saveCategory(cr.getCategories());
		ArrayList<ItemType> types = ItemType.createItems(20, cr.getCategories());
		SaveObjects.saveItemTypes(types);

		AuctionHouse ah = new AuctionHouse(userRecord, bh);

		// parameters of simulator
		int numClusterBidder = 4000;
		double numberOfDays = 100;
//		double auctionsPerBidder = 1.04; // value from TM data
		double auctionsPerBidder = 1.2; // assuming 35.9% of auctions have no bids, 1.04 becomes 1.62
		double auctionsPerDay = auctionsPerBidder * numClusterBidder / numberOfDays;

//		double probIsSniper = 0.33; // from tradeMeData
		double probIsSniper = 0.5;
		ClusterBidder.setNumUsers(numClusterBidder);
		for (int i = 0; i < (long) ((1 - probIsSniper) * numClusterBidder + 0.5); i++) {
			userRecord.addUser(new ClusterEarly(bh, ps, is, ah, userRecord.nextId()));
		}
		for (int i = 0; i < (long) (probIsSniper * numClusterBidder + 0.5); i++) {
			userRecord.addUser(new ClusterSnipe(bh, ps, is, ah, userRecord.nextId()));
		}
//		System.out.println("average: " + ClusterBidder.debugAverage);

		int numSellers = TimedSeller.targetAuctionsPerDay(auctionsPerDay);
		// creating normal sellers
		TimedSeller.setNumUsers(numSellers);
		for (int i = 0; i < numSellers; i++) {
			userRecord.addUser(new TimedSeller(bh, ps, is, ah, userRecord.nextId(), types));
		}
		
		// TODO: Add fraud agents here
		// initialise shillers; shillers are different from normal agents - their constructors add additional
		// agents to the UserRecord
		for (int i = 0; i < agentAdder.length; i++)
			agentAdder[i].add(bh, ps, is, ah, userRecord, types);
		
//		List<ShillController> scs = new ArrayList<>();
//		int numberOfShillers = 10;
//		for (int i = 0; i < numberOfShillers; i++) {
//			ShillController sc = new ShillController(bh, ps, is, ah, ur, types); // modifies UserRecord
////			TrevathanSimpleShill sc = new TrevathanSimpleShill(bh, ps, is, ah, this.ur, types);
////			scs.add(sc);
////			ah.addEventListener(sc);
//			ah.addEventListener(sc);
//		}
//		SingleShillController sc = new SingleShillController(bh, ps, is, ah, this.ur, types);
//		TrevathanSimpleShill sc = new TrevathanSimpleShill(bh, ps, is, ah, this.ur, types);
//		ah.addEventListener(sc);

		// print out the list of users
		logger.debug(userRecord);

		ExecutorService es = Executors.newFixedThreadPool(4);

		// creating the callables
		Callable<Object> ahCallable = Executors.callable(new CrashOnAssertionErrorRunnable(ah));
		List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
		callables.add(Executors.callable(new CrashOnAssertionErrorRunnable(ps)));
		callables.add(Executors.callable(new CrashOnAssertionErrorRunnable(is)));
		List<Callable<Object>> userCallables = new ArrayList<Callable<Object>>();
		for (SimpleUser user : userRecord.getUsers()) {
			userCallables.add(Executors.callable(new CrashOnAssertionErrorRunnable(user)));
		}
		for (EventListener listeners : ah.getEventListeners()) {
			userCallables.add(Executors.callable(new CrashOnAssertionErrorRunnable(listeners)));
		}
		
		// starting the loops - each loop is 1 time unit
		// 24 * 60 / 5 == 1 day
		long timeUnits = (long) (numberOfDays * 24 * 60 / 5 + 0.5);
//		long tenPercent = (long) (timeUnits/10);
		for (int i = 0; i < timeUnits; i++) {
//			if ((i + 1) % tenPercent == 0) {
//				logger.warn("another 10% done");
//			}
			
			try {
				es.submit(ahCallable).get();
				bh.startUserTurn(); // end AH turn after AH thread has joined
				es.invokeAll(callables);
				if (userCallables == null)
					System.out.println("pause");
				es.invokeAll(userCallables);
				bh.startAhTurn(); // start AH turn after all user threads are joined
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		// clean up
		ah.saveUsers();
		es.shutdown();
		SaveObjects.flush();

//		System.out.println("debugAuctionCounter: " + ah.debugAuctionCounter);
		
		// print out the reputation for all users
		// for (UserInterface user : this.ur.getMap().values()) {
		// System.out.print(user + "," + user.getReputationRecord() + "|");
		// }
		// System.out.println();
		
		logger.info("Simulation done.");
	}

}