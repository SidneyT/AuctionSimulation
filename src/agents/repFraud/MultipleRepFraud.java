package agents.repFraud;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import agents.EventListener;
import agents.puppets.Puppet;
import agents.puppets.PuppetMaster;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;

public class MultipleRepFraud extends EventListener {

	final protected BufferHolder bh;
	final protected PaymentSender ps;
	final protected ItemSender is;
	final protected AuctionHouse ah;
	private final List<ItemType> types;
	
	private final List<Puppet> puppets;
	private final ExponentialDistribution exp;
	private Map<Puppet, Long> nextBidTimes;
	
	private final PuppetMaster master;
	
	private final boolean[][] interactionMatrix;
	
	public MultipleRepFraud(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, int numberOfFraudUsers) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		master = createMaster();
		
		puppets = new ArrayList<>(numberOfFraudUsers);
		for (int i = 0; i < numberOfFraudUsers; i++) {
			Puppet puppet = new Puppet(bh, ps, is, ah, master);
			ur.addUser(puppet);
			ah.registerForSniping(puppet); // to get notifications when the auctions are about to end, for bidding on low priced items
			ah.addEventListener(puppet);
			puppets.add(puppet);
		}
		
		int numberOfAuctions = 20;
		exp = new ExponentialDistribution((double) (100 * 24 * 60 / 5) / numberOfAuctions);
		
		nextBidTimes = new HashMap<>();
		for (int i = 0; i < numberOfFraudUsers; i++) {
			nextBidTimes.put(puppets.get(i), Math.round(exp.sample()));
		}
		
		interactionMatrix = new boolean[numberOfFraudUsers][numberOfFraudUsers];
	}
	
	private PuppetMaster createMaster() {
		return new PuppetMasterAdapter() {
			@Override
			public void puppetWinAction(Puppet puppet, Auction auction) {
				ps.send(2, auction, auction.getCurrentPrice(), puppet, auction.getSeller());
				awaitingItem.add(auction);
			}

			@Override
			public void puppetItemReceivedAction(Puppet puppet, Set<ItemSold> items) {
				for (ItemSold item : items) {
					boolean awaiting = awaitingItem.remove(item.getAuction());
					assert awaiting;
					
					bh.getFeedbackToAh().put(new Feedback(Val.POS, puppet, item.getAuction()));
				}
			}

			@Override
			public void puppetEndSoonAction(Puppet puppet, Auction auction) {
				if (auction.minimumBid() <= 150) {
					if (nextBidTimes.get(puppet) < bh.getTime()) {
//						System.out.println(puppet + " endSoon for " + auction + " at price " + auction.minimumBid() + " at time " + bh.getTime());
						bh.getBidMessageToAh().put(auction, new Bid(puppet, auction.minimumBid()));
						
						nextBidTimes.put(puppet, bh.getTime() + Math.round(exp.sample()));
					}
				}
			}
			
		};
	}
	
	public static AgentAdder getAgentAdder(final int numberOfGroups, final int numberOfUsersPerGroup) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					MultipleRepFraud rf = new MultipleRepFraud(bh, ps, is, ah, ur, types, numberOfUsersPerGroup);
					ah.addEventListener(rf);
				}
			}
			
			@Override
			public String toString() {
				return "MultiRepFraud." + numberOfGroups;
			}
		};
	}

	

}
