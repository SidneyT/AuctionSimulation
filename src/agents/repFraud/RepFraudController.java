package agents.repFraud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction.AuctionLength;
import simulator.records.UserRecord;
import util.Sample;
import agents.shills.puppets.TMSellerPuppet;

/**
 * This class coordinates two groups of reputation fraud agents:
 * sellers who want their reputation raised, and puppets who bid and win in fraudulent auctions to give positive feedback to sellers.
 * The time at which reputation inflation begins for each seller uniformly distributed in the first 80 days of the simulation.
 * The target reputation inflation is between 50 - 250, in increments of 50. 
 */
public class RepFraudController implements Runnable {

	private final HiredRepInflaters repInflator;
	private final HashMap<Integer, TMSellerPuppet> repFraudStartTimes = new HashMap<>(); // Map<time to start inflating rep, seller>
	private final Random r = new Random();
	
	public RepFraudController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes) {
		this(bh, ps, is, ah, ur, itemTypes, 40, 256);
	}
	
	private final BufferHolder bh;
	
	public RepFraudController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes, int sellerCount, int puppetCount) {
		this.bh = bh;
		
		// controls the puppets making bids and inflating rep
		repInflator = new HiredRepInflaters(bh, ps, is, ah, ur, itemTypes, 500);
		
		// sellers getting rep from the repInflator
		List<Integer> randomStartTimes = Sample.randomSample(AuctionLength.ONE_DAY.length() * 80, sellerCount, r);
		for (Integer startTime : randomStartTimes) {
			
			// randomly allocate start times, when the repInflators will begin submitting auctions and inflate rep.
			int repInflationTarget = (r.nextInt(8)) * 10 + 30;
//			int repInflationTarget = 100;
			TMSellerPuppet seller = new TMSellerPuppet(bh, ps, is, ah, itemTypes, repInflationTarget);
			repFraudStartTimes.put(startTime, seller);
			ur.addUser(seller);
		}
	}

	@Override
	public void run() {
		// register the sellers with the repInflator at the right times
		int currentTime = bh.getTime();
		if (repFraudStartTimes.containsKey(currentTime)) {
			TMSellerPuppet seller = repFraudStartTimes.get(currentTime);
			repInflator.repFraudSeller(seller, seller.repInflationTarget());
		}
	}

	public static AgentAdder getAgentAdder(final int numberOfGroups, final int sellerCount, final int puppetCount) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> itemTypes) {
				for (int i = 0; i < numberOfGroups; i++) {
					RepFraudController repFraudController = new RepFraudController(bh, ps, is, ah, ur, itemTypes, sellerCount, puppetCount);
					ah.addRunnable(repFraudController);
				}
			}
			
			@Override
			public String toString() {
				return "RepFraudController." + numberOfGroups;
			}
		};
	}
	
}
