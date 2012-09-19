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
	// Map<Auction, Registered>
	protected final Map<Auction, Boolean> shillAuctions;
	protected final Set<Auction> expiredShillAuctions;
	protected List<ItemType> types;
	
	protected final Strategy strategy;
	
	public CollusiveShillController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int numSeller, int bidderPerAgent) {
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
		
		cbs = new ArrayList<>(bidderPerAgent);
		for (int i = 0; i < bidderPerAgent; i++) {
			PuppetBidder cb = new PuppetBidder(bh, ps, is, ah, this);
			ur.addUser(cb);
			this.cbs.add(cb);
		}
	
		shillAuctions = new HashMap<>();
		expiredShillAuctions = new HashSet<>();
		
		setNumberOfAuctions(40);
		
	}
	
	private final Set<Auction> waiting = new HashSet<>();
	private final Map<Long, List<Auction>> futureBid = new HashMap<>();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTimeMessage().getTime();
		
		// TODO: can make another set "toCheckShillAuctions" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions.keySet()) {
			if (shillAuctions.get(shillAuction) == false || cbs.contains(shillAuction.getWinner()))
				continue;
			
			if (this.strategy.shouldBid(shillAuction, currentTime)) {
				long wait = strategy.wait(shillAuction); 
				if (wait > 0) {
					// record when to bid in the future
					waiting.add(shillAuction);
					Util.mapListAdd(futureBid, currentTime + wait, shillAuction);
				} else {
					PuppetBidder chosen = pickBidder(shillAuction, cbs);
					chosen.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.remove(currentTime);
		if (finishedWaiting != null) {
			for (Auction shillAuction : finishedWaiting) {
				pickBidder(shillAuction, cbs).makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
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
	
	protected PuppetSeller pickSeller() {
		return css.get(0);
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
//			sb.registerForAuction(auction);
			
			shillAuctions.put(auction, true);
		}
	}

	@Override
	protected void priceChangeAction(Auction auction, long time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	protected void loseAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void winAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
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
	
	protected abstract PuppetBidder pickBidder(Auction auction, List<PuppetBidder> bidders);
	
	@Override
	public void winAction(SimpleUser agent, Auction auction) {
	}

	@Override
	public void lossAction(SimpleUser agent, Auction auction) {
	}
	

}
