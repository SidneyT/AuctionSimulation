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
import simulator.records.UserRecord;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.Controller;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetI;

public class SingleRepFraud extends EventListener implements Controller {

	final private BufferHolder bh;
//	final private PaymentSender ps;
//	final private ItemSender is;
//	final private AuctionHouse ah;
//	private final List<ItemType> types;
	private final int repTarget;
	
	private final PuppetI puppet; // trying to inflate the reputation of the puppet by bidding in auctions
	private final ExponentialDistribution exp;
	private long nextBidTime;
	
	public SingleRepFraud(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, int repTarget) {
		super(bh);
		this.bh = bh;
//		this.ps = ps;
//		this.is = is;
//		this.ah = ah;
//		this.types = types;
		
		this.repTarget = repTarget;
		
		puppet = new Puppet(bh, ps, is, ah, this, types);
		ur.addUser(puppet);
		ah.registerForSniping(this); // to get notifications when the auctions are about to end, for bidding on low priced items
		
		// on average, try to get 1 rep every 3 days.
		exp = new ExponentialDistribution(AuctionHouse.ONE_DAY * 3);
		nextBidTime = Math.round(exp.sample());
	}

	@Override
	public void run() {
		super.run();
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
			puppet.makeBid(auction, auction.minimumBid());

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

	@Override
	public void endSoonAction(Auction auction, int time) {
		if (auction.minimumBid() <= 150) {
			testAndBid(auction);
		}

	}
	
	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public boolean isFraud(Auction auction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void soldAction(SimpleUserI agent, Auction auction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void expiredAction(SimpleUserI agent, Auction auction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet) {
	}

	@Override
	public void endSoonAction(PuppetI agent, Auction auction) {
	}
}
