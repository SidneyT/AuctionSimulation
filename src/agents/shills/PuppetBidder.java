package agents.shills;

import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.objects.Auction;
import simulator.objects.Bid;
import agents.SimpleUser;

/**
 * Does nothing; does not react to any events. Actions are made by a controller through this class.
 * Makes bids when told to do so.
 */
public class PuppetBidder extends SimpleUser {

	private static final Logger logger = Logger.getLogger(PuppetBidder.class); 
	
	private final Controller controller;
	
	public PuppetBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller) {
		super(bh, ps, is, ah);
		this.controller = controller;
	}

	/**
	 * Makes a bid in the given auction for the given amount.
	 * @param auction
	 * @param bidPrice
	 */
	public void makeBid(Auction auction, int bidPrice) {
		ah.registerForAuction(this, auction);
		Bid bid = new Bid(this, bidPrice);
//		System.out.println(this + " made bid " + bid);
		this.bh.getBidMessageToAh().put(auction, bid);
	}
	
	/**
	 * Tell the controller when this agent wins an auction.
	 */
	@Override
	protected void winAction(Auction auction, long time) {
//		super.winAction(auction, time);
		controller.winAction(this, auction);
	}

	/**
	 * Tell the controller when this agent loses an auction.
	 */
	@Override
	protected void lossAction(Auction auction, long time) {
//		super.lossAction(auction, time);
		controller.lossAction(this, auction);
	}
	
}
