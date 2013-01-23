package simulator;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import createUserFeatures.ClusterAnalysis;

import simulator.buffers.BidsToAh;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.MessagesToAh;
import simulator.buffers.MessagesToUsers;
import simulator.buffers.PaymentSender;
import simulator.buffers.TimeMessage;
import simulator.categories.CategoryRecord;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.categories.CreateCategories;
import simulator.database.SaveToDatabase;
import simulator.database.SaveObjects;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.records.UserRecord;
import util.CrashOnAssertionErrorRunnable;
import agents.EventListener;
import agents.SimpleUser;
import agents.bidders.ClusterBidder;
import agents.bidders.ClusterEarly;
import agents.bidders.ClusterSnipe;
import agents.sellers.TMSeller;
import agents.sellers.TimedSeller;

public class Main {

	private static final int NUMBER_OF_THREADS = 3;
	private final static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start.");
		
		Main.run(SaveToDatabase.instance());
//		ClusterAnalysis.clusterSimData("");
		System.out.println("Finished.");
	}
	
	/**
	 * Runs the simulator
	 */
	public static void run(SaveObjects saveObjects, AgentAdder... userAdder) {
		try {
			go(saveObjects, userAdder);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		CalculateStats.calculateStats();
	}

	private static void go(SaveObjects saveObjects, AgentAdder... agentAdder) throws InterruptedException {
		// create buffers
		final TimeMessage timeMessage = new TimeMessage();
		final MessagesToUsers messagesToUsers = new MessagesToUsers();
		final MessagesToAh<Auction> auctionsToAh = new MessagesToAh<Auction>();
		final MessagesToAh<Feedback> feedbackToAh = new MessagesToAh<Feedback>();
		final BidsToAh bidsToAh = new BidsToAh();
		final BufferHolder bh = new BufferHolder(timeMessage, messagesToUsers, auctionsToAh, feedbackToAh, bidsToAh);

		// create item and payment senders
		final PaymentSender ps = new PaymentSender();
		final ItemSender is = new ItemSender();

		// create item types
//		final CategoryRecord cr = CreateCategories.mockCategories();
//		ArrayList<ItemType> types = CreateItemTypes.mockItems(20, cr.getCategories());
		final CategoryRecord cr = CreateCategories.TMCategories();
		ArrayList<ItemType> itemTypes = CreateItemTypes.TMItems(cr.getRoot().getChildren());

		saveObjects.saveCategories(cr.getCategories());
		saveObjects.saveItemTypes(itemTypes);

		final UserRecord userRecord = new UserRecord();
		final AuctionHouse ah = new AuctionHouse(userRecord, bh, saveObjects);

		// parameters of simulator
		final int numClusterBidder = 4000;
		final double numberOfDays = 100;
//		final double auctionsPerBidder = 1.04; // value from TM data
//		final double auctionsPerBidder = 1.25; // assuming 35.9% of auctions have no bids, 1.04 becomes 1.62
		final double auctionsPerBidder = 1.4;
		final double auctionsPerDay = auctionsPerBidder * numClusterBidder / numberOfDays;

//		double probIsSniper = 0.33; // from tradeMeData
		final double probIsSniper = 0.5;
		ClusterBidder.setNumUsers(numClusterBidder);
		for (int i = 0; i < (long) ((1 - probIsSniper) * numClusterBidder + 0.5); i++) {
			userRecord.addUser(new ClusterEarly(bh, ps, is, ah, itemTypes));
		}
		for (int i = 0; i < (long) (probIsSniper * numClusterBidder + 0.5); i++) {
			userRecord.addUser(new ClusterSnipe(bh, ps, is, ah, itemTypes));
		}
//		System.out.println("average: " + ClusterBidder.debugAverage);

		// add sellers
//		int numSellers = TimedSeller.sellersRequired(auctionsPerDay);
//		// creating normal sellers
//		for (int i = 0; i < numSellers; i++) {
//			userRecord.addUser(new TimedSeller(bh, ps, is, ah, types));
//		}
		int numSellers = TMSeller.sellersRequired(auctionsPerDay);
		// creating normal sellers
		for (int i = 0; i < numSellers; i++) {
			userRecord.addUser(new TMSeller(bh, ps, is, ah, itemTypes));
		}
		
		// TODO: Add fraud agents here
		// initialise shillers; shillers are different from normal agents - their constructors add additional
		// agents to the UserRecord
		for (int i = 0; i < agentAdder.length; i++)
			agentAdder[i].add(bh, ps, is, ah, userRecord, itemTypes);
		
		// print out the list of users
		logger.debug(userRecord);

		ExecutorService es = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

		// creating the callables
//		final Callable<Object> ahCallable = Executors.callable(new CrashOnAssertionErrorRunnable(ah));
		final ImmutableList<Callable<Object>> callables = ImmutableList.of(
				Executors.callable(ps),
				Executors.callable(is)
				);
		final ImmutableSet.Builder<Callable<Object>> userCallablesBuilder = ImmutableSet.builder();
		for (SimpleUser user : userRecord.getUsers()) {
			userCallablesBuilder.add(Executors.callable(new CrashOnAssertionErrorRunnable(user)));
//			userCallablesBuilder.add(Executors.callable(user));
		}
		for (EventListener listeners : ah.getEventListeners()) {
			userCallablesBuilder.add(Executors.callable(new CrashOnAssertionErrorRunnable(listeners)));
//			userCallablesBuilder.add(Executors.callable(listeners));
		}
		ImmutableSet<Callable<Object>> userCallables = userCallablesBuilder.build();
		
		// starting the loops - each loop is 1 time unit
		// 24 * 60 / 5 == 1 day
		long timeUnits = (long) (numberOfDays * 24 * 60 / 5 + 0.5);
//		long tenPercent = (long) (timeUnits/10);
		for (int i = 0; i < timeUnits; i++) {
//			if ((i + 1) % tenPercent == 0) {
//				logger.warn("another 10% done");
//			}
//			System.out.println("time unit: " + i);
			try {
				ah.run();
				bh.startUserTurn();
				
				// invokeAll() blocks until all tasks are complete
				es.invokeAll(callables); 
				es.invokeAll(userCallables);
				
				bh.startAhTurn();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// clean up
		ah.saveUsers();
		es.shutdown();
		saveObjects.cleanup();

//		System.out.println("debugAuctionCounter: " + ah.debugAuctionCounter);
		
		// print out the reputation for all users
		// for (UserInterface user : this.ur.getMap().values()) {
		// System.out.print(user + "," + user.getReputationRecord() + "|");
		// }
		// System.out.println();
		
		logger.info("Simulation done.");
	}

}