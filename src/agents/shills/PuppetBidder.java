package agents.shills;

import java.util.Set;

import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.objects.Auction;
import simulator.objects.Bid;
import agents.SimpleUser;

/**
 * Does nothing; does not react to any events.
 * Makes bids when told to do so.
 */
public class PuppetBidder extends SimpleUser {

	private static final Logger logger = Logger.getLogger(PuppetBidder.class); 
	
	private final Controller controller;
	
	public PuppetBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId, Controller controller) {
		super(bh, ps, is, ah, uniqueId);
		this.controller = controller;
	}

	/**
	 * Makes a bid in the given auction for the given amount.
	 * @param auction
	 * @param bidPrice
	 */
	public void makeBid(Auction auction, long bidPrice) {
		ah.registerForAuction(this, auction);
		this.bh.getBidMessageToAh().put(auction, new Bid(this, bidPrice));
	}
	
	/**
	 * Makes the lowest valid bid for this auction.
	 * @param auction
	 */
	public void makeBid(Auction auction) {
		this.makeBid(auction, auction.minimumBid());
	}

	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		super.itemReceivedAction(itemSet);
	}
	
	@Override
	protected void winAction(Auction auction, long time) {
		super.winAction(auction, time);
		controller.winAction(this, auction);
	}

//	Set<Auction> lostAuctions = new HashSet<>();
	
	@Override
	protected void loseAction(Auction auction, long time) {
		super.loseAction(auction, time);
		controller.lossAction(this, auction);
		
//		logger.info(this + " got LOSS message for " + auction);
//		if (!lostAuctions.add(auction))
//			System.out.println("repeated message");
		
	}
	
}
