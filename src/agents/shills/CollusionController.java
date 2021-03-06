package agents.shills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import util.Sample;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.puppets.PuppetI;
import agents.shills.strategies.Strategy;

/**
 *	Group of users working together, acting as both bidders and sellers. 
 */
public class CollusionController extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(CollusionController.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	protected final List<PuppetI> cbs;
	
	protected final Set<Auction> currentShillAuctions; // auctions ready to be bid on by collaborating agents
	protected final Set<Auction> allShillAuctions; // all shill auctions
	protected List<ItemType> itemTypes;
	
	protected final int numberOfAuctions; // number of auctions submitted by the shill seller
	
	protected final Strategy strategy;

	private int shillsPerAuction;

	public CollusionController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes, 
			Strategy strategy, PuppetFactoryI factory, int groupSize, int numberOfAuctions, int shillsPerAuction) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.itemTypes = itemTypes;
		this.strategy = strategy;
		
		this.shillsPerAuction = shillsPerAuction;
		
		cbs = new ArrayList<>(groupSize);
		for (int i = 0; i < groupSize; i++) {
//			PuppetClusterBidderCombined cb = new PuppetClusterBidderCombined(bh, ps, is, ah, this, types);
			PuppetI cb = factory.instance(bh, ps, is, ah, this, itemTypes);
			ur.addUser(cb);
			this.cbs.add(cb);
		}
	
		currentShillAuctions = new HashSet<>();
		allShillAuctions = new HashSet<>();
		
		this.numberOfAuctions = numberOfAuctions;
		setNumberOfAuctions(numberOfAuctions);
	}
	
	private final Set<Auction> waiting = new HashSet<>();
	private final ArrayListMultimap<Long, Auction> futureBid = ArrayListMultimap.create();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTime();
		
		// TODO: can make another set "shillAuctionsToCheck" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : currentShillAuctions) {
			// check if a shill is already winning in the auction
			if (cbs.contains(shillAuction.getWinner()))
				continue;
			
			// skip auction if it's already scheduled to make a bid
			if (waiting.contains(shillAuction))
				continue;
			
			if (this.strategy.shouldBid(shillAuction, currentTime)) {
				long wait = strategy.wait(shillAuction); // the wait is expected to possibly give a value of 0. 
//				if (wait > 0) {
//					// record when to bid in the future
					waiting.add(shillAuction);
					futureBid.put(currentTime + wait, shillAuction);
//					System.out.println(currentTime + ": delay by " + wait + " to " + (currentTime + wait) + " at " + shillAuction + " for " + shillAuction.getId());
//				} else {
//					PuppetBidder chosen = pickBidder(shillAuction);
////					System.out.println("making now at " + currentTime + " by " + chosen + " for " + shillAuction.getId());
//					chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
//				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.removeAll(currentTime);
		if (finishedWaiting != null) {
			Set<Auction> finishedWaitingSet = new HashSet<>(finishedWaiting);
			if (finishedWaitingSet.size() != finishedWaiting.size()) {
				System.out.println("the sizes should be the same...");
			}
			for (Auction shillAuction : finishedWaiting) {
				PuppetI chosen = pickBidder(shillAuction);
//				System.out.println(currentTime + ": making bid at time " + currentTime + " by " + chosen + " for " + shillAuction.getId());
				chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
			}
			this.waiting.removeAll(finishedWaiting);
		}
	
		
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) { // decide whether to submit a new auction
			// pick a seller and submit an auction
			Auction submitted = pickSeller().submitAuction();
			allShillAuctions.add(submitted);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	protected PuppetI pickSeller() {
		return cbs.get(r.nextInt(cbs.size())); // pick the next seller randomly
	}

	protected Map<Auction, List<PuppetI>> shillsAssigned = new HashMap<>(); // Map<Auction, Shills assigned to that auction>
	protected PuppetI pickBidder(Auction auction) {
		List<PuppetI> selected = Collections.emptyList();
		if (!shillsAssigned.containsKey(auction)) {
		// pick the set of users to use for this auction

			while (selected.contains(auction.getSeller()) || selected.isEmpty()) {
				selected = selectSet();
			}
			shillsAssigned.put(auction, selected);
//			System.out.println("choosing from: " + selected + " for " + auction);
		} else {
			selected = shillsAssigned.get(auction);
		}
		
		// pick the bidder to bid for this auction
		return simplePickBidder(auction, selected);
	}
	
	/**
	 * Select the set of shills to use in the shill auction
	 */
	protected List<PuppetI> selectSet() {
//		List<PuppetBidder> selected = new ArrayList<>(shillsPerAuction);
//		// goes through each combination of users deterministically.
//		int[] combination = cb.getNext();
//		for (int i = 0; i < combination.length; i++) {
//			selected.add(cbs.get(combination[i]));
//		}
		
		List<PuppetI> selected = Sample.randomSample(cbs, shillsPerAuction, r);
		Collections.shuffle(selected);
		return selected;
	}
	
	/*
	 * Copied from Alternating Bid class
	 */
	Map<Auction, Integer> alternatingBidderAssigned = new HashMap<>(); // Map<auction, index of next bidder who should bid in that auction>
	protected PuppetI simplePickBidder(Auction auction, List<PuppetI> bidders) {
		if (!alternatingBidderAssigned.containsKey(auction)) {
			PuppetI chosen = bidders.get(0);
			alternatingBidderAssigned.put(auction, 1 % bidders.size());
//			System.out.println("chosen " + chosen);
			return chosen;
		} else {
			int index = alternatingBidderAssigned.put(auction, (alternatingBidderAssigned.get(auction) + 1) % bidders.size());
			return bidders.get(index);
		}
	}

	
	
	private List<Integer> auctionTimes;
	public final Random r = new Random();
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		int latest = (AuctionHouse.HUNDRED_DAYS - AuctionHouse.SEVEN_DAYS) / AuctionHouse.UNIT_LENGTH;
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(r.nextInt(latest));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	@Override
	public void newAction(Auction auction, int time) {
		super.newAction(auction, time);
		if (allShillAuctions.contains(auction)) {
			ah.registerForAuction(this, auction);
			currentShillAuctions.add(auction);
		}
	}

	@Override
	public void priceChangeAction(Auction auction, int time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	public void lossAction(Auction auction, int time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		assert currentShillAuctions.contains(auction);
		currentShillAuctions.remove(auction);
	}

	@Override
	public void winAction(Auction auction, int time) {
		assert false : "This method should never be called, since this class can neither bid nor win.";
	}

	@Override
	public void expiredAction(Auction auction, int time) {
		super.expiredAction(auction, time);
	}

	@Override
	public void soldAction(Auction auction, int time) {
		assert false : "This method should never be called, since this class can not submit auctions.";
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + strategy.toString(); 
	}
	
	@Override
	public void winAction(SimpleUserI agent, Auction auction) {}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {}
	
	
	@Override
	public boolean isFraud(Auction auction) {
		return allShillAuctions.contains(auction);
	}
	
	@Override
	public void soldAction(SimpleUserI agent, Auction auction) {}

	@Override
	public void expiredAction(SimpleUserI agent, Auction auction) {}

	@Override
	public void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet) {}
	
	@Override
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet) {}

	@Override
	public void endSoonAction(PuppetI agent, Auction auction) {}

	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final PuppetFactoryI factory) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					CollusionController sc = new CollusionController(bh, ps, is, ah, ur, types, strategy, Puppet.getFactory(), 13, 13 * 6, 3);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "Hybrid." + numberOfAgents + "." + strategy;
			}
		};
	}
	/**
	 * Varies the group size in CollusionController
	 */
	public static AgentAdder getAgentAdderVaryGroupSize(final int numberOfAgents, final Strategy strategy, final PuppetFactoryI factory) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				Random r = new Random();
				for (int i = 0; i < numberOfAgents; i++) {
					int groupSize = r.nextInt(16) + 5;
					CollusionController sc = new CollusionController(bh, ps, is, ah, ur, types, strategy, Puppet.getFactory(), groupSize, groupSize * 6, 3);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "Hybrid." + numberOfAgents + "." + strategy;
			}
		};
	}
}
