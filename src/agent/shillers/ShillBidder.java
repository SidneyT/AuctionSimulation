package agent.shillers;

import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.objects.Auction;
import simulator.objects.Bid;
import util.Util;
import agent.SimpleUser;

public class ShillBidder extends SimpleUser {

	private static final Logger logger = Logger.getLogger(ShillBidder.class); 
	
	private final ShillController controller;
	
	public ShillBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId, ShillController controller) {
		super(bh, ps, is, ah, uniqueId);
		this.controller = controller;
	}

	public void makeBid(Auction auction, long bidPrice) {
		bh.getBidMessageToAh().put(auction, new Bid(this, bidPrice));
	}
	public void makeBid(Auction auction) {
		makeBid(auction, auction.getCurrentPrice() + Util.minIncrement(auction.getCurrentPrice()));
	}
	
	public void registerForAuction(Auction auction) {
		logger.debug(this + " registered for auction " + auction);
		this.ah.registerForAuction(this, auction);
	}
	
//	@Override
//	protected void newAction(Auction auction) {
//		this.ah.registerForAuction(this, auction);
//	}

}
