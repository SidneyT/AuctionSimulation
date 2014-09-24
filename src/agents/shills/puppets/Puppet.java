package agents.shills.puppets;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Item;
import agents.SimpleUser;
import agents.shills.Controller;

/**
 * Actions are made by a controller through this class.
 * Makes bids when told to do so.
 * 
 * WILL send and process payment/items, and give feedback like a normal user by default.
 */
public class Puppet extends SimpleUser implements PuppetI {

	private static final Logger logger = Logger.getLogger(Puppet.class); 
	
	protected final Controller controller;
	private final List<ItemType> itemTypes;
	private String name;
	
	final Random r = new Random();
	
	public Puppet(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
		super(bh, ps, is, ah);
		this.controller = controller;
		this.itemTypes = itemTypes;
	}
	
	public Puppet(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes, int id) {
		super(bh, ps, is, ah, id);
		this.controller = controller;
		this.itemTypes = itemTypes;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
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
	public void winAction(Auction auction, int time) {
		super.winAction(auction, time);
		controller.winAction(this, auction);
	}

	/**
	 * Tell the controller when this agent loses an auction.
	 */
	@Override
	public void lossAction(Auction auction, int time) {
//		super.lossAction(auction, time);
		controller.lossAction(this, auction);
	}
	
	@Override
	public void soldAction(Auction auction, int time) {
		super.soldAction(auction, time);
		controller.soldAction(this, auction);
	}

	@Override
	public void expiredAction(Auction auction, int time) {
		super.expiredAction(auction, time);
		controller.expiredAction(this, auction);
	}
	
	@Override
	public void submitAuction(Auction auction) {
		this.bh.getAuctionMessagesToAh().put(auction);
	}
	
	@Override
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		super.itemReceivedAction(itemSet);
		controller.itemReceivedAction(this, itemSet);
	}
	
	// submit a default auction
	public Auction submitAuction() {
		Item item = new Item(CreateItemTypes.pickType(itemTypes, r.nextDouble()), "item" + r.nextInt());
		Auction auction = new Auction(this, item, AuctionHouse.SEVEN_DAYS, getPrice(), 0, 1);
		this.bh.getAuctionMessagesToAh().put(auction);
		
		logger.debug(this + " submitting shill auction " + auction + " at " + bh.getTime());

		return auction;
	}
	
	private int getPrice() { return 100; }
	
	@Override
	public String getName() {
		if (name != null) {
			return name;
		} else {
			return this.getClass().getSimpleName();
		}
	}
	
	public static PuppetFactoryI getFactory() {
		return new PuppetFactoryI() {
			@Override
			public PuppetI instance(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
				return new Puppet(bh, ps, is, ah, controller, itemTypes);
			}
		};
	}
	
}
