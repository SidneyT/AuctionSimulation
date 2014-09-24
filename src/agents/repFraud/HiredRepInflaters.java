package agents.repFraud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ArrayListMultimap;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Item;
import simulator.records.UserRecord;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.Controller;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetI;

public class HiredRepInflaters extends EventListener implements Controller {

	private final UserRecord ur;
	private final List<ItemType> itemTypes;
	
	private final ArrayList<PuppetI> puppets = new ArrayList<>();
	
	protected final Random r = new Random();
	
	private final ConcurrentHashMap<SimpleUserI, Integer> employerSellers = new ConcurrentHashMap<>(); // sellers using the puppets in this class to inflate their reputation; Map<Seller, RepTarget>
	private final ConcurrentHashMap<Auction, PuppetI> fraudAuctions = new ConcurrentHashMap<>();
	private final ArrayListMultimap<Integer, Auction> snipeTimes = ArrayListMultimap.create();
	
	public HiredRepInflaters(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes, int groupSize) {
		super(bh);
		
		this.ur = ur;
		this.itemTypes = itemTypes;
		
		ah.addEventListener(this);
		
		for (int i = 0; i < groupSize; i++) {
			Puppet ss = new Puppet(bh, ps, is, ah, this, itemTypes);
			ur.addUser(ss);
			puppets.add(ss);
		}
		
		
	}

//	@Override
//	public void run() {
//		super.run();
//	}
	
	@Override
	public void run() {
		super.run();
		
		Iterator<SimpleUserI> it = employerSellers.keySet().iterator();
		while(it.hasNext()) {
			// iterate through the employerSellers to see if any needs to have more auctions submitted and bid upon to make the threshold
			SimpleUserI seller = it.next();
			int remaining = employerSellers.get(seller);
			if (remaining <= 0) {
				it.remove();
			} else {
				boolean submitted = tryTosubmitFraudAuctionFor(seller);
				if (submitted) {
					employerSellers.put(seller, remaining - 1);
				}
			}
		}
		
		if (snipeTimes.containsKey(bh.getTime())) {
			List<Auction> toSnipe = snipeTimes.removeAll(bh.getTime());
			for (Auction auction : toSnipe) {
				snipe(auction);
			}
		}
		
	}

	private boolean tryTosubmitFraudAuctionFor(SimpleUserI seller) {
		if (r.nextDouble() < 0.1) {
			Item item = new Item(CreateItemTypes.pickType(itemTypes, r.nextDouble()), "item" + r.nextInt(100000));
			Auction auction = new Auction(seller, item, AuctionHouse.SEVEN_DAYS, 100, 0, 0.1); // auction of low popularity, so few normal users will bid on them
			seller.submitAuction(auction);
			// remember the auction is a fraud one, and assign a puppet to it
			fraudAuctions.put(auction, selectPuppet());
			return true;
		}
		return false;
	}
	
	private void snipe(Auction auction) {
//		if (auction.hasNoBids()) { // test to make sure no one else has bid on it yet
			PuppetI puppet = fraudAuctions.get(auction);
			puppet.makeBid(auction, auction.minimumBid());
//		}
	}
	
	private PuppetI selectPuppet() {
		PuppetI picked = puppets.get(r.nextInt(puppets.size()));
		return picked;
	}
	
	@Override
	public void newAction(Auction auction, int time) {
		if (isFraud(auction)) { // if the auction is a fraud one this instance submitted, make a puppet bid on it when it's close to ending
			snipeTimes.put(auction.getEndTime() - 3, auction);
		}
	}
	
	@Override
	public void endSoonAction(Auction auction, int time) {
		super.endSoonAction(auction, time);
	}
	
	public void repFraudSeller(SimpleUserI seller, int repIncreaseWanted) {
		employerSellers.put(seller, repIncreaseWanted);
	}
	
	@Override
	public boolean isFraud(Auction auction) {
		return fraudAuctions.containsKey(auction);
	}
	
	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
		assert isFraud(auction); // puppets should only bid on fraud auctions
		fraudAuctions.remove(auction);
	}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
		assert isFraud(auction); // puppets should only bid on fraud auctions
		fraudAuctions.remove(auction);

		// since a puppet failed to sucessfully increase rep, increment record
		SimpleUserI seller = auction.getSeller();
		int currentRemaining;
		if (employerSellers.containsKey(seller))
			currentRemaining = employerSellers.get(seller) + 1;
		else
			currentRemaining = 1;
		employerSellers.put(seller, currentRemaining);
	}

	@Override
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet) {}
	
	// these should never be called, since the puppets never act as sellers
	@Override
	public void endSoonAction(PuppetI agent, Auction auction) {assert false;}
	@Override
	public void soldAction(SimpleUserI agent, Auction auction) {assert false;}
	@Override
	public void expiredAction(SimpleUserI agent, Auction auction) {assert false;}
	@Override
	public void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet) {assert false;}

}
