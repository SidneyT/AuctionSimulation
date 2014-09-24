package simulator;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import agents.EventListener;
import agents.SimpleUserI;
import agents.bidders.ClusterEarly;
import agents.bidders.ClusterSnipe;
import agents.sellers.TMSeller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import simulator.buffers.BidsToAh;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.MessagesToAh;
import simulator.buffers.MessagesToUsers;
import simulator.buffers.PaymentSender;
import simulator.buffers.TimeMessage;
import simulator.categories.CategoryRecord;
import simulator.categories.CreateCategories;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.database.SaveObjects;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.records.UserRecord;
import util.CrashOnAssertionErrorRunnable;

/**
 * Contains the methods for running the simulation.
 */
public class Simulation {

	final static Logger logger = Logger.getLogger(Simulation.class);

	static final int NUMBER_OF_THREADS = 7;
	
	/**
	 * Runs the simulator
	 */
	public static void run(SaveObjects saveObjects, AgentAdder... agentAdders) {
		Simulation.run(saveObjects, 20000, agentAdders);
//		CalculateStats.calculateStats();
	}

	public static void run(SaveObjects saveObjects, int numberOfBidders, AgentAdder... agentAdders) {
		try {
			Simulation.go(saveObjects, numberOfBidders, agentAdders);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	static void go(SaveObjects saveObjects, int numberOfBidders, AgentAdder... agentAdders) throws InterruptedException {
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
			final CategoryRecord cr = CreateCategories.TMCategories();
			ArrayList<ItemType> itemTypes = CreateItemTypes.TMItems(cr.getRoot().getChildren());
	
			saveObjects.saveCategories(cr.getCategories());
			saveObjects.saveItemTypes(itemTypes);
	
			final UserRecord userRecord = new UserRecord();
			final AuctionHouse ah = new AuctionHouse(userRecord, bh, saveObjects);
	
			// parameters of simulator
			final double numberOfDays = 100;
			final double auctionsPerBidder = 1.4;
			final double auctionsPerDay = auctionsPerBidder * numberOfBidders / numberOfDays;
	
	//		double probIsSniper = 0.33; // from tradeMeData
			final double probIsSniper = 0.5;
			int numberOfSnipers = (int) (probIsSniper * numberOfBidders + 0.5);
			for (int i = 0; i < numberOfSnipers; i++) {
				userRecord.addUser(new ClusterSnipe(bh, ps, is, ah, itemTypes));
			}
			for (int i = 0; i < numberOfBidders - numberOfSnipers; i++) {
				userRecord.addUser(new ClusterEarly(bh, ps, is, ah, itemTypes));
			}
	//		System.out.println("average: " + ClusterBidder.debugAverage);
	
			int numSellers = TMSeller.sellersRequired(auctionsPerDay);
			// creating normal sellers
			for (int i = 0; i < numSellers; i++) {
				userRecord.addUser(new TMSeller(bh, ps, is, ah, itemTypes));
			}
			
			// Add fraud agents here
			for (int i = 0; i < agentAdders.length; i++) {
				agentAdders[i].add(bh, ps, is, ah, userRecord, itemTypes);
			}
			
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
			for (SimpleUserI user : userRecord.getUsers()) {
				userCallablesBuilder.add(Executors.callable(new CrashOnAssertionErrorRunnable(user)));
	//			userCallablesBuilder.add(Executors.callable(user));
			}
			for (EventListener listener : ah.getEventListeners()) {
				userCallablesBuilder.add(Executors.callable(new CrashOnAssertionErrorRunnable(listener)));
	//			userCallablesBuilder.add(Executors.callable(listeners));
			}
			for (Runnable runnable : ah.getRunnables()) {
				userCallablesBuilder.add(Executors.callable(new CrashOnAssertionErrorRunnable(runnable)));
	//			userCallablesBuilder.add(Executors.callable(listeners));
			}
			
			ImmutableSet<Callable<Object>> userCallables = userCallablesBuilder.build();
			
			int simulationDurationInUnits = AuctionHouse.ONE_DAY * 100;
			for (int i = 0; i < simulationDurationInUnits; i++) {
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
	
			logger.debug("Simulation done.");
		}

}
