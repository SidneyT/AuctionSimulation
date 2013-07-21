package agents.shills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import util.Util;
import agents.EventListener;
import agents.SimpleUser;
import agents.shills.strategies.Strategy;

public abstract class CollusiveShillController extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(CollusiveShillController.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	protected final List<PuppetSeller> css;
	protected final List<PuppetBidder> cbs;
	// Map<Auction, Registered>. Registered == true if the auction is ready to be bid on by controlled shills, false otherwise.
	protected final Map<Auction, Boolean> shillAuctions;
	protected final Set<Auction> expiredShillAuctions;
	protected List<ItemType> types;
	
	protected final int numberOfAuctions; // number of auctions submitted by the shill seller
	
	protected final Strategy strategy;
	public CollusiveShillController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int numSeller, int biddersPerSeller, int numberOfAuctions) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		this.strategy = strategy;
		
		// set up the shill seller, only need 1.
		css = new ArrayList<>(numSeller);
		for (int i = 0; i < numSeller; i++) {
			PuppetSeller ss = new PuppetSeller(bh, ps, is, ah, this, types);
			ur.addUser(ss);
			css.add(ss);
		}
		
		cbs = new ArrayList<>(biddersPerSeller);
		for (int i = 0; i < biddersPerSeller; i++) {
			PuppetBidder cb = new PuppetBidder(bh, ps, is, ah, this);
			ur.addUser(cb);
			this.cbs.add(cb);
		}
	
		shillAuctions = new HashMap<>();
		expiredShillAuctions = new HashSet<>();
		
		this.numberOfAuctions = numberOfAuctions;
		setNumberOfAuctions(numberOfAuctions);
	}
	
	private final Set<Auction> waiting = new HashSet<>();
	private final Map<Long, List<Auction>> futureBid = new HashMap<>();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTimeMessage().getTime();
		
		// TODO: can make another set "shillAuctionsToCheck" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions.keySet()) {
			// check if auction is ready, and if a shill is already winning in the auction
			if (shillAuctions.get(shillAuction) == false || cbs.contains(shillAuction.getWinner()))
				continue;
			
			// skip auction if it's already scheduled to make a bid
			if (waiting.contains(shillAuction))
				continue;
			
			if (this.strategy.shouldBid(shillAuction, currentTime)) {
				long wait = strategy.wait(shillAuction); // the wait is expected to possibly give a value of 0. 
//				if (wait > 0) {
//					// record when to bid in the future
					waiting.add(shillAuction);
					Util.mapListAdd(futureBid, currentTime + wait, shillAuction);
//					System.out.println(currentTime + ": delay by " + wait + " to " + (currentTime + wait) + " at " + shillAuction + " for " + shillAuction.getId());
//				} else {
//					PuppetBidder chosen = pickBidder(shillAuction);
////					System.out.println("making now at " + currentTime + " by " + chosen + " for " + shillAuction.getId());
//					chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
//				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.remove(currentTime);
		if (finishedWaiting != null) {
			Set<Auction> finishedWaitingSet = new HashSet<>(finishedWaiting);
			if (finishedWaitingSet.size() != finishedWaiting.size()) {
				System.out.println("the sizes should be the same...");
			}
			for (Auction shillAuction : finishedWaiting) {
				PuppetBidder chosen = pickBidder(shillAuction);
//				System.out.println(currentTime + ": making bid at time " + currentTime + " by " + chosen + " for " + shillAuction.getId());
				chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
			}
			this.waiting.removeAll(finishedWaiting);
		}
	
		// decide whether to submit a new auction
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) {
			Auction newShillAuction = pickSeller().submitAuction();
			shillAuctions.put(newShillAuction, false);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	private List<Integer> auctionTimes;
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(Util.randomInt(Math.random(), 0, (int) (100 * 24 * 60 / 5 + 0.5)));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	public Set<Auction> getShillAuctions() {
		return this.shillAuctions.keySet();
	}

	@Override
	protected void newAction(Auction auction, long time) {
		super.newAction(auction, time);
		if (shillAuctions.containsKey(auction)) {
			ah.registerForAuction(this, auction);
			shillAuctions.put(auction, true);
		}
	}

	@Override
	protected void priceChangeAction(Auction auction, long time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	protected void lossAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void winAction(Auction auction, long time) {
		assert(false) : "This method should never be called, since this class can neither bid nor win.";
	}

	@Override
	protected void expiredAction(Auction auction, long time) {
		super.expiredAction(auction, time);
	}

	@Override
	protected void soldAction(Auction auction, long time) {
		super.soldAction(auction, time);
		this.awaitingPayment.add(auction);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + strategy.toString(); 
	}
	
	protected abstract PuppetBidder pickBidder(Auction auction);
	protected abstract PuppetSeller pickSeller();
	
	
	@Override
	public void winAction(SimpleUser agent, Auction auction) {
	}

	@Override
	public void lossAction(SimpleUser agent, Auction auction) {
	}
	

}
