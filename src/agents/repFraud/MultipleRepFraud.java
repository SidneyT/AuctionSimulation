package agents.repFraud;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

import agents.EventListener;
import agents.SimpleUser;
import agents.puppets.Puppet;
import agents.puppets.PuppetMaster;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import simulator.objects.Item;
import simulator.objects.ItemCondition;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import util.Sample;

public class MultipleRepFraud extends EventListener {

	final protected BufferHolder bh;
	final protected PaymentSender ps;
	final protected ItemSender is;
	final protected AuctionHouse ah;
	private final List<ItemType> types;
	private final int repTarget;
	
	private final HashBiMap<Puppet, Integer> puppets; // Map<Puppet, Index>
	private final Map<Puppet, Long> nextBidTimes; // Map<Puppet, Time>
	private final Map<Puppet, Long> nextAuctionTimes; // Map<Puppet, Time>
	
	// for generating the times for submitting bids and auctions 
	private final ExponentialDistribution expBid;
	private final ExponentialDistribution expAuc;
	
	private final PuppetMaster master;
	
	private final boolean[][] interactionMatrix;
	
	private final Random r = new Random();
	
	public MultipleRepFraud(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, int numberOfFraudUsers, int repTarget) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		this.repTarget = repTarget;

		master = createMaster();
		
		puppets = HashBiMap.create(numberOfFraudUsers);
		for (int i = 0; i < numberOfFraudUsers; i++) {
			Puppet puppet = new Puppet(bh, ps, is, ah, master);
			ur.addUser(puppet);
			ah.registerForSniping(puppet); // to get notifications when the auctions are about to end, for bidding on low priced items
			puppets.put(puppet, puppets.size());
		}
		
		// on average, try to get 1 rep every 3 days.
		expBid = new ExponentialDistribution(3 * 24 * 60 / 5);
		nextBidTimes = new HashMap<>();
		for (Puppet puppet : puppets.keySet()) {
			nextBidTimes.put(puppet, Math.round(expBid.sample()));
		}

		// submit auctions at a rate proportion to the number of collaborating fraudsters
		if (numberOfFraudUsers > 1) {
			expAuc = new ExponentialDistribution((double) 80 * 24 * 60 / 5 / (numberOfFraudUsers - 1));
		} else {
			expAuc = new ExponentialDistribution(Double.MAX_VALUE);
		}
		nextAuctionTimes = new HashMap<>(); 
		for (Puppet puppet : puppets.keySet()) {
			nextAuctionTimes.put(puppet, Math.round(expAuc.sample()));
		}
		
		interactionMatrix = new boolean[numberOfFraudUsers][numberOfFraudUsers];
//		Arrays.fill(interactionMatrix, false); // unnecessary, default value is false.
	}
	
	
	
	Multimap<Long, Pair<Auction, Puppet>> bidInFutureMap = ArrayListMultimap.create();
	private void bidInFuture(long timeToBid, Auction auction, Puppet puppet) {
		bidInFutureMap.put(timeToBid, new Pair<Auction, Puppet>(auction, puppet));
	}
	
	@Override
	protected void newAction(Auction auction, long time) {
		super.newAction(auction, time);
		
		// Check if the auction is by known repFraud. 
		// If yes, assign one of the others to bid in it. Else do nothing.
		if (puppets.containsKey(auction.getSeller())) {
			Puppet picked = pickUninteractedPuppet(auction.getSeller());
			bidInFuture(auction.getEndTime() - 3, auction, picked);
			addInteraction((Puppet) auction.getSeller(), picked);
		}
		
	}
	
	@Override
	public void run() {
		super.run();
		
		// check if any puppets should submit an auction
		testAndSubmit();
		
		// check if any bids should be made now from bidInFutureMap
		if (bidInFutureMap.containsKey(bh.getTime())) {
			for (Pair<Auction, Puppet> pair : bidInFutureMap.get(bh.getTime())) {
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
		for (Entry<Puppet, Long> puppetEntry : nextAuctionTimes.entrySet()) {
			Puppet puppet = puppetEntry.getKey();
			// first test if there anyone you haven't interacted with in the group
			if (!interactionsLeft(puppet))
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
	
	private void submitAuction(Puppet puppet) {
		Item newItem = new Item(types.get(r.nextInt(types.size())), "item" + r.nextInt(100000));
		Auction auction = new Auction(puppet, newItem, 2016, 100, 0, 0.0); // auction has 0 popularity so no one will bid on it.
		bh.getAuctionMessagesToAh().put(auction);
		
	}

	private boolean interactionsLeft(Puppet puppet) {
		int puppetIndex = puppets.get(puppet);
		boolean[] row = interactionMatrix[puppets.get(puppet)];
		for (int i = 0; i < interactionMatrix.length; i++) 
			if (!row[i] && i != puppetIndex) 
				return true;
		return false;
	}
	
	private Puppet pickUninteractedPuppet(SimpleUser puppet) {
		Sample<Integer> sampler = new Sample<>(1);
		
		int puppetIndex = puppets.get(puppet);
		for (int i = 0; i < interactionMatrix.length; i++) { 
			if (!interactionMatrix[puppetIndex][i] && i != puppetIndex)
				sampler.add(i);
		}
		
		Puppet chosenPuppet;
		if (sampler.getSample().isEmpty()) {
			chosenPuppet = null;
			throw new RuntimeException("Execution should never reach here.");
		} else {
			chosenPuppet = puppets.inverse().get(sampler.getSample().get(0));
		}
		
		return chosenPuppet;
	}
	
	private void addInteraction(Puppet p1, Puppet p2) {
		int p1Index = puppets.get(p1);
		int p2Index = puppets.get(p2);
		interactionMatrix[p1Index][p2Index] = true;
		interactionMatrix[p2Index][p1Index] = true;
	}
	
	private PuppetMaster createMaster() {
		return new PuppetMasterAdapter() {
			// acting as buyer
			@Override
			public void puppetWinAction(Puppet puppet, Auction auction) {
				ps.send(2, auction, auction.getCurrentPrice(), puppet, auction.getSeller());
				awaitingItem.add(auction);
			}

			// acting as buyer
			@Override
			public void puppetItemReceivedAction(Puppet puppet, Set<ItemSold> items) {
				for (ItemSold item : items) {
					boolean awaiting = awaitingItem.remove(item.getAuction());
					assert awaiting;
					
					Feedback feedback = new Feedback(Val.POS, puppet, item.getAuction());
					System.out.println(puppet + " submitting received feedback: " + feedback);
					bh.getFeedbackToAh().put(feedback);
				}
			}

			@Override
			public void puppetEndSoonAction(Puppet puppet, Auction auction) {
				if (auction.minimumBid() <= 150) {
					testAndBid(puppet, auction);
				}
			}

			// acting as seller 
			@Override
			public void puppetGotPaidAction(Puppet puppet, Collection<Payment> paymentSet) {
				for (Payment payment : paymentSet) {
					boolean exists = awaitingPayment.remove(payment.getAuction());
					assert(exists); 
					
					is.send(2, payment.getAuction(), payment.getAuction().getItem(), ItemCondition.GOOD, puppet, payment.getSender());
					
					Feedback feedback = new Feedback(Val.POS, puppet, payment.getAuction());
					System.out.println(puppet + " submitting paid feedback: " + feedback);
					bh.getFeedbackToAh().put(feedback);
				}
			}

		};
	}
	
	/**
	 * Tests whether bidding criteria are met. If yes, bid. If not, do nothing.
	 * The criteria are: if target netRep has not been met, if nextBidTime has passed. 
	 * @param auction
	 */
	private void testAndBid(Puppet puppet, Auction auction) {
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

	

}
