package agents.shills;

import java.util.ArrayList;
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
import simulator.records.ReputationRecord;
import agents.SimpleUserI;
import agents.bidders.ClusterBidder;
import agents.bidders.ClusterEarly;
import agents.shills.puppets.PuppetFactoryI;

/**
 * Does nothing; does not react to any events. Actions are made by a controller through this class.
 * Makes bids when told to do so.
 */
public class PuppetClusterBidderCombined implements PuppetI {

	private static final Logger logger = Logger.getLogger(PuppetClusterBidderCombined.class); 
	
	private final ClusterEarly normal;
	private final PuppetBidder puppet;

//	private final Controller controller;
	
	public PuppetClusterBidderCombined(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
//		this.controller = controller;

		this.normal = new ClusterEarly(bh, ps, is, ah, itemTypes);
		System.out.println(normal.getId() + ", " + normal.interestTimes);
		
		// reuse the id, so that actions by both "normal" and "puppet" are viewed as by the same agent in the simulation.
		int normalId = this.normal.getId();
		this.puppet = new PuppetBidder(bh, ps, is, ah, controller, normalId);
	}

	@Override
	public void run() {
		normal.run();
		puppet.run();
	}
	
	@Override
	public void winAction(Auction auction, long time) {
		normal.winAction(auction, time);
		puppet.winAction(auction, time);
	}

	@Override
	public void lossAction(Auction auction, long time) {
		normal.lossAction(auction, time);
		puppet.lossAction(auction, time);
	}

	@Override
	public void newAction(Auction auction, long time) {
		normal.newAction(auction, time);
	}

	/**
	 * For sellers.  Will record that this user is waiting for 
	 * @param auction
	 */
	@Override
	public void soldAction(Auction auction, long time) {
		normal.soldAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in SimpleUser 
	}
	
	/**
	 * For sellers.  Will receive payment, then post a positive feedback.
	 * @param auction
	 */
	@Override
	public void gotPaidAction(Collection<Payment> paymentSet) {
		normal.gotPaidAction(paymentSet); // only need to call with 1 field. Both normal & puppet calls the same method in SimpleUser
	}

	/**
	 * For bidders.  Will receive an item, then post a positive feedback.
	 * @param itemSet synchronised set
	 */
	@Override
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		normal.itemReceivedAction(itemSet);
	}

	@Override
	public void priceChangeAction(Auction auction, long time) {
		normal.priceChangeAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in EventListener
	}

	@Override
	public void expiredAction(Auction auction, long time) {
		normal.expiredAction(auction, time); // only need to call with 1 field. Both normal & puppet calls the same method in EventListener
	}

	@Override
	public void endSoonAction(Auction auction, long time) {
		normal.endSoonAction(auction, time);
	}

	@Override
	public ReputationRecord getReputationRecord() {
		return normal.getReputationRecord();
	}

	@Override
	public int getId() {
		return normal.getId();
	}

	@Override
	public void makeBid(Auction auction, int bidPrice) {
		puppet.makeBid(auction, bidPrice);
	}

	public static PuppetFactoryI getFactory() {
		return new PuppetFactoryI() {
			@Override
			public PuppetI instance(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
				return new PuppetClusterBidderCombined(bh, ps, is, ah, controller, itemTypes);
			}
		};
	}

}
