package agent.shillers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;



import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.records.UserRecord;
import util.Util;
import agent.EventListener;

public class ShillController extends EventListener {
	
	private static final Logger logger = Logger.getLogger(ShillController.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	ShillSeller ss;
	List<ShillBidder> shillBidders;
	Set<ShillBidder> shillBidderSet;
	Set<Auction> shillAuctions;
	Set<Auction> expiredShillAuctions;
	List<ItemType> types;
	
	private Random r;
	
	public ShillController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types) {
		super(bh, ur.nextId());
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		r = new Random();

		// set up the shill seller
		ShillSeller ss = new ShillSeller(bh, ps, is, ah, ur.nextId(), this, types);
		ur.addUser(ss);
		this.ss = ss;
		
		// set up the shill bidders
		int numberOfShillBidders = 2;
		shillBidders = new ArrayList<>();
		for (int i = 0; i < numberOfShillBidders; i++) {
			ShillBidder sb = new ShillBidder(bh, ps, is, ah, ur.nextId(), this);
			ur.addUser(sb);
			shillBidders.add(sb);
		}
		shillBidderSet = new HashSet<>();
		shillBidderSet.addAll(shillBidders);
	
		shillAuctions = new HashSet<>();
		expiredShillAuctions = new HashSet<>();
		
		setAuctionTimes(10);
	}

	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTimeMessage().getTime();
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions) {
			if (shillAuction.percentageElapsed(currentTime) > 0.6) {
				if (shillAuction.getCurrentPrice() < shillAuction.getStartPrice() * 2) {
					// current price is too low, so make a bid
					ShillBidder sb = randomBidder(shillAuction);
					sb.makeBid(shillAuction, shillAuction.getStartPrice() * 2);
					continue; // continue so there won't be 2 bids by shillers at the same time for the same auction
				}
			}
			// help the bidding war along, if there is one
			long timeLimit = currentTime - 20;
			int numberOfBids = numberOfBids(shillAuction.getBidHistory(), timeLimit);
			if (numberOfBids > 8) {
				if (r.nextDouble() < 0.5) {
					logger.debug("There were " + numberOfBids + " bids in after the time specified for " + shillAuction + ", so going to make a shill bid at " + currentTime + ".");
					randomBidder(shillAuction).makeBid(shillAuction);
				}
			}
		}
		
		// make the bidders follow a normal behaviour pattern?

		
		
		// decide whether to submit a new auction
//		if (currentTime % 20 == 1) {
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) {
			Auction newShillAuction = ss.submitAuction();
			shillAuctions.add(newShillAuction);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}


	private List<Integer> auctionTimes;
	private void setAuctionTimes(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(Util.randomInt(Math.random(), 0, 10080));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}

	
	/**
	 * Counts the number of bids that were made after the timeLimit by bidders
	 * who are not shill bidders from this controller.
	 * @param bidHistory
	 * @param time
	 */
	private int numberOfBids(List<Bid> bidHistory, long timeLimit) {
		int count = 0;
		for (int i = bidHistory.size() - 1; i >= 0 && bidHistory.get(i).getTime() >= timeLimit; i--) {
			Bid bid = bidHistory.get(i);
			if (!shillBidderSet.contains(bid.getBidder()))
				count++;
		}
		return count;
	}
	
	/**
	 * Randomly pick a shill bidder that is not already winning the auction.
	 * @param shillAuction
	 * @return
	 */
	private ShillBidder randomBidder(Auction shillAuction) {
		ShillBidder picked;
		do {
			int bidderIndex = (int) (r.nextDouble() * shillBidders.size());
			picked = shillBidders.get(bidderIndex);
		} while (picked.equals(shillAuction.getWinner()));
		
		return picked;
	}

	public Set<Auction> shillAuctions() {
		return this.shillAuctions;
	}

	@Override
	protected void newAction(Auction auction) {
		if (shillAuctions.contains(auction)) {
			ah.registerForAuction(this, auction);
			for (ShillBidder sb : shillBidders) {
				sb.registerForAuction(auction);
			}
		}
	}

	@Override
	protected void priceChangeAction(Auction auction) {
	}

	@Override
	protected void loseAction(Auction auction) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void winAction(Auction auction) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void expiredAction(Auction auction) {
	}

	@Override
	protected void soldAction(Auction auction) {
	}

	@Override
	protected void gotPaidAction(Set<Payment> paymentSet) {
	}
	
}
