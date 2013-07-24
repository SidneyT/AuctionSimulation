package agents.shills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;


import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Util;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.strategies.Strategy;

public abstract class CollusiveShillController extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(CollusiveShillController.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	protected final List<PuppetSeller> css;
	protected final List<PuppetI> cbs;
	// Map<Auction, Registered>. Registered == true if the auction is ready to be bid on by controlled shills, false otherwise.
	protected final Set<Auction> shillAuctions;
	protected final Set<Auction> expiredShillAuctions;
	protected List<ItemType> types;
	
	protected final int numberOfAuctions; // number of auctions submitted by the shill seller
	
	protected final Strategy strategy;
	public CollusiveShillController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> itemTypes, Strategy strategy, PuppetFactoryI factory, int numSeller, int biddersPerSeller, int numberOfAuctions) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = itemTypes;
		this.strategy = strategy;
		
		// set up the shill seller, only need 1.
		css = new ArrayList<>(numSeller);
		for (int i = 0; i < numSeller; i++) {
			PuppetSeller ss = new PuppetSeller(bh, ps, is, ah, this, itemTypes);
			ur.addUser(ss);
			css.add(ss);
		}
		
		cbs = new ArrayList<>(biddersPerSeller);
		for (int i = 0; i < biddersPerSeller; i++) {
//			PuppetBidder cb = new PuppetBidder(bh, ps, is, ah, this);
			PuppetI cb = factory.instance(bh, ps, is, ah, this, itemTypes);
			ur.addUser(cb);
			this.cbs.add(cb);
		}
	
		shillAuctions = new HashSet<>();
		expiredShillAuctions = new HashSet<>();
		
		this.numberOfAuctions = numberOfAuctions;
		setNumberOfAuctions(numberOfAuctions);
	}
	
	private final Set<Auction> waiting = new HashSet<>();
	private final ArrayListMultimap<Long, Auction> futureBid = ArrayListMultimap.create();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTimeMessage().getTime();
		
		// TODO: can make another set "shillAuctionsToCheck" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions) {
			// check if a shill is already winning in the auction
			if (cbs.contains(shillAuction.getWinner()))
				continue;
			
			// skip auction if it's already scheduled to make a bid
			if (waiting.contains(shillAuction))
				continue;
			
			if (this.strategy.shouldBid(shillAuction, currentTime)) {
				long wait = strategy.wait(shillAuction); // the wait is expected to possibly give a value of 0. 
//				if (wait > 0) {
//					// record when to bid in the future
					waiting.add(shillAuction);
					futureBid.put(currentTime + wait, shillAuction);
//					System.out.println(currentTime + ": delay by " + wait + " to " + (currentTime + wait) + " at " + shillAuction + " for " + shillAuction.getId());
//				} else {
//					PuppetBidder chosen = pickBidder(shillAuction);
////					System.out.println("making now at " + currentTime + " by " + chosen + " for " + shillAuction.getId());
//					chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
//				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.removeAll(currentTime);
		if (finishedWaiting != null) {
			Set<Auction> finishedWaitingSet = new HashSet<>(finishedWaiting);
			if (finishedWaitingSet.size() != finishedWaiting.size()) {
				System.out.println("the sizes should be the same...");
			}
			for (Auction shillAuction : finishedWaiting) {
				PuppetI chosen = pickBidder(shillAuction);
//				System.out.println(currentTime + ": making bid at time " + currentTime + " by " + chosen + " for " + shillAuction.getId());
				chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
			}
			this.waiting.removeAll(finishedWaiting);
		}
	
		
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) { // decide whether to submit a new auction
			// pick a seller and submit an auction
			pickSeller().submitAuction();
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	private List<Integer> auctionTimes;
	public final Random r = new Random();
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		int latest = 100 * 24 * 60 / 5;
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(r.nextInt(latest));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	@Override
	public void newAction(Auction auction, long time) {
		super.newAction(auction, time);
		if (auction.getSeller().equals(this)) {
			ah.registerForAuction(this, auction);
			shillAuctions.add(auction);
		}
	}

	@Override
	public void priceChangeAction(Auction auction, long time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	public void lossAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		assert shillAuctions.contains(auction);
		assert !expiredShillAuctions.contains(auction);
		shillAuctions.remove(auction);
		expiredShillAuctions.add(auction);
	}

	@Override
	public void winAction(Auction auction, long time) {
		assert false : "This method should never be called, since this class can neither bid nor win.";
	}

	@Override
	public void expiredAction(Auction auction, long time) {
		super.expiredAction(auction, time);
	}

	@Override
	public void soldAction(Auction auction, long time) {
		super.soldAction(auction, time);
		this.awaitingPayment.add(auction);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + strategy.toString(); 
	}
	
	protected abstract PuppetI pickBidder(Auction auction);
	protected abstract PuppetSeller pickSeller();
	
	
	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
	}
	
	@Override
	public boolean isFraud(Auction auction) {
		throw new NotImplementedException();
	}
}
