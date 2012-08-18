package agent.shills;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;


import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Item;
import simulator.objects.Auction.AuctionLength;
import agent.EventListener;
import agent.SimpleUser;

/**
 * Does nothing; does not react to any events.
 * Has 1 method so that it submits and auction when told to do so.
 */
public class PuppetSeller extends SimpleUser {

	private static final Logger logger = Logger.getLogger(PuppetSeller.class); 
	
	private final Controller controller;
	private List<ItemType> items;
	private final Random r;
	
	public PuppetSeller(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId, Controller controller, List<ItemType> types) {
		super(bh, ps, is, ah, uniqueId);
		this.controller = controller;
		this.r = new Random();
		
		this.items = types;
	}

	protected Auction submitAuction() {
		Item item = new Item(ItemType.pickType(items, r.nextDouble()), "item" + (int) (r.nextDouble() * 100000));
		Auction auction = new Auction(this, item, AuctionLength.SEVEN_DAYS.timeUnits(), (long) getPrice(), 0, 1);
		this.bh.getAuctionMessagesToAh().put(auction);
		
		logger.info(this + " submitting shill auction " + auction + " at " + bh.getTimeMessage().getTime());

		return auction;
	}
	
	private double getPrice() {
		return 100;
	}

}
