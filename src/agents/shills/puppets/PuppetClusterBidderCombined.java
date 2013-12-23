package agents.shills.puppets;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.records.ReputationRecord;
import agents.bidders.ClusterBidder;
import agents.bidders.ClusterEarly;
import agents.bidders.ClusterSnipe;
import agents.shills.Controller;

/**
 * Does nothing; does not react to any events. Actions are made by a controller through this class.
 * Makes bids when told to do so.
 */
public class PuppetClusterBidderCombined implements PuppetI {

	private static final Logger logger = Logger.getLogger(PuppetClusterBidderCombined.class); 
	
	private final ClusterBidder normal;
	private final Puppet puppetV;

	private final Controller controller;
	private final BufferHolder bh;
	
	
	public PuppetClusterBidderCombined(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes, final Controller controller) {
		this.controller = controller;
		this.bh = bh;
		
		if (Math.random() < 0.5) {
			this.normal = new ClusterEarly(bh, ps, is, ah, itemTypes) {
				protected int numberOfAuctionsPer100Days(double random) { // modify behaviour to participate make it participate in more auctions...
					return Math.min(4, super.numberOfAuctionsPer100Days(r.nextDouble()));
				}
			};
		} else {
			this.normal = new ClusterSnipe(bh, ps, is, ah, itemTypes) {
				protected int numberOfAuctionsPer100Days(double random) { // modify behaviour to participate make it participate in more auctions...
					return Math.min(4, super.numberOfAuctionsPer100Days(r.nextDouble()));
				}
			};
		}
//		System.out.println(normal.getId() + ", " + normal.interestTimes);
		
		// reuse the id, so that actions by both "normal" and "puppet" are viewed as by the same agent in the simulation.
		int normalId = this.normal.getId();
		this.puppetV = new PuppetV(bh, ps, is, ah, itemTypes, controller, normalId);
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void run() {
		normal.run();
		puppetV.run();
	}
	
	@Override
	public void winAction(Auction auction, int time) {
		normal.winAction(auction, time);
		puppetV.winAction(auction, time);
	}

	@Override
	public void lossAction(Auction auction, int time) {
//		normal.lossAction(auction, time);
		puppetV.lossAction(auction, time);
	}

	@Override
	public void newAction(Auction auction, int time) {
		if (!controller.isFraud(auction))
			normal.newAction(auction, time);
	}

	/**
	 * For sellers.  Will record that this user is waiting for 
	 * @param auction
	 */
	@Override
	public void soldAction(Auction auction, int time) {
		normal.soldAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in SimpleUser
		puppetV.soldAction(auction, time);
	}
	
	/**
	 * For sellers.  Will receive payment, then post a positive feedback.
	 * @param auction
	 */
	@Override
	public void gotPaidAction(Collection<Payment> paymentSet) {
		normal.gotPaidAction(paymentSet); // only need to call with 1 field. Both normal & puppet calls the same method in SimpleUser
		puppetV.gotPaidAction(paymentSet);
	}

	/**
	 * For bidders.  Will receive an item, then post a positive feedback.
	 * @param itemSet synchronised set
	 */
	@Override
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		normal.itemReceivedAction(itemSet); // only need to call with 1 field. Both normal & puppet calls the same method in SimpleUser
		puppetV.itemReceivedAction(itemSet);
	}

	@Override
	public void priceChangeAction(Auction auction, int time) {
		normal.priceChangeAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in EventListener
	}

	@Override
	public void expiredAction(Auction auction, int time) {
		normal.expiredAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in EventListener
	}

	@Override
	public void endSoonAction(Auction auction, int time) {
		normal.endSoonAction(auction, time);
	}

	@Override
	public ReputationRecord getReputationRecord() {
		return normal.getReputationRecord();
	}

	@Override
	public void addFeedback(Feedback feedback) {
		this.getReputationRecord().addFeedback(this.getId(), feedback);
	}

	@Override
	public int getId() {
		return normal.getId();
	}

	@Override
	public void makeBid(Auction auction, int bidPrice) {
		puppetV.makeBid(auction, bidPrice);
	}

	@Override
	public void submitAuction(Auction auction) {
		bh.getAuctionMessagesToAh().put(auction);
	}

	@Override
	public Auction submitAuction() {
		throw new UnsupportedOperationException();
	}

	public static PuppetFactoryI getFactory() {
		return new PuppetFactoryI() {
			@Override
			public PuppetI instance(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
				return new PuppetClusterBidderCombined(bh, ps, is, ah, itemTypes, controller);
			}
		};
	}

}
