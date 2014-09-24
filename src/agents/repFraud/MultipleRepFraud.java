package agents.repFraud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.util.Pair;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Item;
import simulator.records.UserRecord;
import util.Sample;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.Controller;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetI;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Multimap;


/**
 * Fraud type where a collection of Puppets both submit auctions and bid on them.
 * Auctions submitted by a puppet will be bid on by other puppets.
 * Puppets will also bid in normal auctions that have a low price (<= 150)
 */
public class MultipleRepFraud extends EventListener implements Controller {

	final protected BufferHolder bh;
	final protected PaymentSender ps;
	final protected ItemSender is;
	final protected AuctionHouse ah;
	private final List<ItemType> types;
	private final int repTarget;
	
	private final ImmutableBiMap<PuppetI, Integer> puppets; // Map<Puppet, Index>
	private final Map<PuppetI, Long> nextBidTimes; // Map<Puppet, Time>
	private final Map<PuppetI, Long> nextAuctionTimes; // Map<Puppet, Time>
	
	// for generating the times for submitting bids and auctions 
	private final ExponentialDistribution expBid;
	private final ExponentialDistribution expAuc;
	
	private final boolean[][] interactionMatrix;
	
	private final Random r = new Random();
	
	public MultipleRepFraud(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes, int numberOfFraudUsers, int repTarget) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = itemTypes;
		
		this.repTarget = repTarget;

		HashBiMap<PuppetI, Integer> tempPuppets = HashBiMap.create(numberOfFraudUsers);
		for (int i = 0; i < numberOfFraudUsers; i++) {
			PuppetI puppet = new Puppet(bh, ps, is, ah, this, itemTypes);
			ur.addUser(puppet);
			ah.registerForSniping(puppet); // to get notifications when the auctions are about to end, for bidding on low priced items
			tempPuppets.put(puppet, tempPuppets.size());
		}
		puppets = ImmutableBiMap.copyOf(tempPuppets);
		
		// on average, try to get 1 rep every 3 days.
		expBid = new ExponentialDistribution(AuctionHouse.ONE_DAY * 3);
		nextBidTimes = new HashMap<>();
		for (PuppetI puppet : puppets.keySet()) {
			nextBidTimes.put(puppet, Math.round(expBid.sample()));
		}

		// submit auctions at a rate proportion to the number of collaborating fraudsters
		if (numberOfFraudUsers > 1) {
			expAuc = new ExponentialDistribution((double) 80 * AuctionHouse.ONE_DAY / (numberOfFraudUsers - 1));
		} else {
			expAuc = new ExponentialDistribution(Double.MAX_VALUE);
		}
		nextAuctionTimes = new HashMap<>(); 
		for (PuppetI puppet : puppets.keySet()) {
			nextAuctionTimes.put(puppet, Math.round(expAuc.sample()));
		}
		
		interactionMatrix = new boolean[numberOfFraudUsers][numberOfFraudUsers];
//		Arrays.fill(interactionMatrix, false); // unnecessary, default value is false.
	}
	
	
	
	Multimap<Integer, Pair<Auction, PuppetI>> bidInFutureMap = ArrayListMultimap.create();
	private void bidInFuture(int timeToBid, Auction auction, PuppetI puppet) {
		bidInFutureMap.put(timeToBid, new Pair<Auction, PuppetI>(auction, puppet));
	}
	
	@Override
	public void newAction(Auction auction, int time) {
		super.newAction(auction, time);
		
		// Check if the auction is by known repFraud. 
		// If yes, assign one of the others to bid in it. Else do nothing.
		if (puppets.containsKey(auction.getSeller())) {
			PuppetI picked = pickUninteractedPuppet(auction.getSeller());
			bidInFuture(auction.getEndTime() - 3, auction, picked);
			addInteraction((PuppetI) auction.getSeller(), picked);
		}
		
	}
	
	@Override
	public void run() {
		super.run();
		
		// check if any puppets should submit an auction
		testAndSubmit();
		
		// check if any bids should be made now from bidInFutureMap
		if (bidInFutureMap.containsKey(bh.getTime())) {
			for (Pair<Auction, PuppetI> pair : bidInFutureMap.get(bh.getTime())) {
				Auction auction = pair.getKey();
				if (!auction.hasNoBids())
					throw new RuntimeException("Error. RepFraud Auction should not have any bids yet.");
				
				// Fee is 7.9% of sale price with a minimum of 50 cent, so max price while still paying 50 cents is 632.
				int amount = r.nextInt(6) * 50 + 400; // gives random amount between 400 - 600 with 50 increments 
				Bid bid = new Bid(pair.getValue(), amount);
				bh.getBidMessageToAh().put(pair.getKey(), bid);
			}
		}
	}
	
	private void testAndSubmit() {
		for (Entry<PuppetI, Long> puppetEntry : nextAuctionTimes.entrySet()) {
			PuppetI puppet = puppetEntry.getKey();
			// first test if there anyone you haven't interacted with in the group
			if (!hasInteractionsLeft(puppet))
				continue; // if already interacted with everyone, continue;

			// next test if the time has arrived
			if (nextAuctionTimes.get(puppet) > bh.getTime()) {
				continue;
			}
			
			// give the puppet a new nextAuctionTime
			nextAuctionTimes.put(puppet, bh.getTime() + Math.round(expAuc.sample()));
			
			submitAuction(puppet);
		}
	}
	
	private void submitAuction(PuppetI puppet) {
		Item newItem = new Item(types.get(r.nextInt(types.size())), "item" + r.nextInt(100000));
		Auction auction = new Auction(puppet, newItem, 2016, 100, 0, 0.0); // auction has 0 popularity so no one will bid on it.
		bh.getAuctionMessagesToAh().put(auction);
		
	}

	private boolean hasInteractionsLeft(PuppetI puppet) {
		int puppetIndex = puppets.get(puppet);
		boolean[] row = interactionMatrix[puppets.get(puppet)];
		for (int i = 0; i < interactionMatrix.length; i++) 
			if (!row[i] && i != puppetIndex) 
				return true;
		return false;
	}
	
	private PuppetI pickUninteractedPuppet(SimpleUserI puppet) {
		Sample<Integer> sampler = new Sample<>(1);
		
		int puppetIndex = puppets.get(puppet);
		for (int i = 0; i < interactionMatrix.length; i++) { 
			if (!interactionMatrix[puppetIndex][i] && i != puppetIndex)
				sampler.add(i);
		}
		
		PuppetI chosenPuppet;
		if (sampler.getSample().isEmpty()) {
			chosenPuppet = null;
			throw new RuntimeException("Execution should never reach here.");
		} else {
			chosenPuppet = puppets.inverse().get(sampler.getSample().get(0));
		}
		
		return chosenPuppet;
	}
	
	private void addInteraction(PuppetI p1, PuppetI p2) {
		int p1Index = puppets.get(p1);
		int p2Index = puppets.get(p2);
		interactionMatrix[p1Index][p2Index] = true;
		interactionMatrix[p2Index][p1Index] = true;
	}
	
	/**
	 * Tests whether bidding criteria are met. If yes, bid. If not, do nothing.
	 * The criteria are: if target netRep has not been met, if nextBidTime has passed. 
	 * @param auction
	 */
	private void testAndBid(PuppetI puppet, Auction auction) {
		if (puppet.getReputationRecord().getNetRep() < repTarget && nextBidTimes.get(puppet) < bh.getTime()) {
			bh.getBidMessageToAh().put(auction, new Bid(puppet, auction.minimumBid()));

			nextBidTimes.put(puppet, bh.getTime() + Math.round(expBid.sample()));
		}
	}
	
	public static AgentAdder getAgentAdder(final int numberOfGroups, final int numberOfUsersPerGroup, final int repTarget) {
		return new AgentAdder() {
			MultipleRepFraud rf2;
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					MultipleRepFraud rf = new MultipleRepFraud(bh, ps, is, ah, ur, types, numberOfUsersPerGroup, repTarget);
					ah.addEventListener(rf);
					
					rf2 = rf;
				}
			}
			
			@Override
			public String toString() {
				for (int i = 0; i < rf2.interactionMatrix.length; i++) {
					System.out.println(Arrays.toString(rf2.interactionMatrix[i]));
				}
				return "MultiRepFraud." + numberOfGroups;
			}
		};
	}

	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public boolean isFraud(Auction auction) {
		return false;
	}

	@Override
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet) {
	}

	@Override
	public void soldAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void expiredAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet) {
	}

	@Override
	public void endSoonAction(PuppetI agent, Auction auction) {
		if (puppets.containsKey(auction.getSeller()) && auction.minimumBid() <= 150) {
			testAndBid(agent, auction);
		}
	}

}
