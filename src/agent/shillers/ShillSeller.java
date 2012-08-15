package agent.shillers;

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
import agent.SimpleUser;

public class ShillSeller extends SimpleUser{

	private static final Logger logger = Logger.getLogger(ShillSeller.class); 
	
	private final ShillController controller;
	private List<ItemType> items;
	private final Random r;
	
	public ShillSeller(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId, ShillController controller, List<ItemType> types) {
		super(bh, ps, is, ah, uniqueId);
		this.controller = controller;
		this.r = new Random();
		
		this.items = types;
	}

	protected Auction submitAuction() {
		Item item = new Item(ItemType.pickType(items, r.nextDouble()), "item" + (int) (r.nextDouble() * 100000));
		Auction auction = new Auction(this, item, AuctionLength.SEVEN_DAYS.timeUnits(), (long) getPrice(), 0, 1);
		this.bh.getAuctionMessagesToAh().put(auction);
		
		logger.info(this + " submitting auction " + auction + " at " + bh.getTimeMessage().getTime() + ".");

		return auction;
	}
	
	private double getPrice() {
		// y = 81.952 * e^(4.8083*x)
//		return 81.952 * Math.pow(Math.exp(1), 4.8083 * r.nextDouble());
		return 100;
	}

}
