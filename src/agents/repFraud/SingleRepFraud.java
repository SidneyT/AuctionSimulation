package agents.repFraud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.distribution.ExponentialDistribution;

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
import simulator.objects.Feedback;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import agents.EventListener;
import agents.puppets.Puppet;
import agents.puppets.PuppetMaster;

public class SingleRepFraud extends EventListener {

	final protected BufferHolder bh;
	final protected PaymentSender ps;
	final protected ItemSender is;
	final protected AuctionHouse ah;
	private final List<ItemType> types;
	private final int repTarget;
	
	private final Puppet puppet;
	private final ExponentialDistribution exp;
	private long nextBidTime;
	
	private final PuppetMaster master;
	
	public SingleRepFraud(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, int repTarget) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		this.repTarget = repTarget;
		
		master = createMaster();
		
		puppet = new Puppet(bh, ps, is, ah, master);
		ur.addUser(puppet);
		ah.registerForSniping(puppet); // to get notifications when the auctions are about to end, for bidding on low priced items
		
		// on average, try to get 1 rep every 3 days.
		exp = new ExponentialDistribution(3 * 24 * 60 / 5);
		nextBidTime = Math.round(exp.sample());
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
					
					Feedback feedback = new Feedback(Val.POS, puppet, item.getAuction());
					bh.getFeedbackToAh().put(feedback);
				}
			}

			@Override
			public void puppetEndSoonAction(Puppet puppet, Auction auction) {
				if (auction.minimumBid() <= 150) {
					testAndBid(auction);
				}
			}
			
		};
	}
	
	/**
	 * Tests whether bidding criteria are met. If yes, bid. If not, do nothing.
	 * The criteria are: if target netRep has not been met, if nextBidTime has passed. 
	 * @param auction
	 */
	private void testAndBid(Auction auction) {
		if (puppet.getReputationRecord().getNetRep() < repTarget && nextBidTime < bh.getTime()) {
			if (auction.getWinner() == puppet)
				return;
			bh.getBidMessageToAh().put(auction, new Bid(puppet, auction.minimumBid()));

			nextBidTime = bh.getTime() + Math.round(exp.sample());
		}
	}

	public static AgentAdder getAgentAdder(final int numberOfGroups, final int repTarget) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					SingleRepFraud rf = new SingleRepFraud(bh, ps, is, ah, ur, types, repTarget);
					ah.addEventListener(rf);
				}
			}
			
			@Override
			public String toString() {
				return "SingleRepFraud." + numberOfGroups;
			}
		};
	}

}
