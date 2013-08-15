package simulator.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import util.Util;

import agents.SimpleUser;
import agents.SimpleUserI;

/**
 * Not thread safe. Should be single thread access only.
 */
public class Auction {

	private static final Logger logger = Logger.getLogger(Auction.class);

	private static final AtomicInteger auctionId = new AtomicInteger(); // for allocating unique auction ids

	private final int id;
	private int startTime;
	private final SimpleUserI seller;
	private final int startPrice;
	private final int reservePrice;
	private int currentPrice;
	private final int duration;
	private final Item item;
	private final List<Bid> bidHistory;
	private int bidCount;
//	private Bid firstBid;
//	private Bid lastBid;
	private SimpleUserI winner; // whoever's winning, or won the auction
	private int endTime;
	
	private final double popularity;

//	public Auction(SimpleUser seller, CategoryNode category, Item item, long duration, long startPrice,
//			long reservePrice) {
//		this(seller, category, item, duration, startPrice, reservePrice, 1);
//	}
//
//	public Auction(SimpleUser seller, CategoryNode category, Item item, long duration, long startPrice, long reservePrice) {
//		this(seller, category, item, duration, startPrice, reservePrice, 1);
//	}

	public Auction(SimpleUserI seller, Item item, int duration, int startPrice,
			int reservePrice, double popluarity) {

		if (!argumentsValid(seller, item, duration, startPrice, reservePrice)) {
			throw new IllegalArgumentException();
		}

		this.id = auctionId.getAndIncrement();
		this.startTime = -1;
		this.endTime = -1;
		this.seller = seller;
		this.item = item;
		this.duration = duration;
		this.currentPrice = startPrice;
		this.bidCount = 0;
		this.bidHistory = new ArrayList<>();
//		this.firstBid = null;
//		this.lastBid = null;
		this.startPrice = startPrice;
		this.reservePrice = reservePrice;
		
		this.popularity = popluarity;
	}

	private boolean argumentsValid(SimpleUserI seller, Item item, long duration,
			long startPrice, long reservePrice) {
		return seller != null && duration > 0 && startPrice >= 0 && reservePrice >= 0;
	}

	public int getId() {
		return this.id;
	}

//	public void setId(int id) {
//		if (this.id == -1)
//			this.id = id;
//		else if (this.id != id) {
//			logger.error("Auction id is being set again to the same value of " + id + ".");
//		} else {
//			logger.error("Auction id cannont be changed.");
//			assert false;
//		}
//	}

	public SimpleUserI getSeller() {
		return seller;
	}

	public int getStartPrice() {
		return startPrice;
	}

	public int getReservePrice() {
		return this.reservePrice;
	}

	public boolean reserveMet() {
		return this.currentPrice >= this.reservePrice;
	}

//	public CategoryNode getCategory() {
//		return this.category;
//	}

	public int getCurrentPrice() {
		logger.debug("Reporting current price as: " + this.currentPrice);
		return currentPrice;
	}

	public int getDuration() {
		return duration;
	}

	public int getStartTime() {
		return startTime;
	}

	// public void setEndtime(long endtime) {
	// this.endTime = endtime;
	// }
	public int extendAuction(long time) {
		this.endTime += time;
		return this.endTime;
	}

	public int getEndTime() {
		assert (endTime != -1);
		return endTime;
	}

	public void setStartTime(int startTime) {
		if (this.startTime == -1) {
			this.startTime = startTime;
			this.endTime = startTime + duration;
		} else {
			throw new RuntimeException("Auction start time cannot be changed from " + this.startTime + " to "
					+ startTime + ".");
		}
	}

	public Item getItem() {
		return item;
	}

	public void addBid(Bid bid) {
//		if (this.firstBid == null) {
//			this.firstBid = bid;
//		}
//		lastBid = bid;
		bidCount++;
		bidHistory.add(bid);
		this.currentPrice = bid.getPrice();
		logger.debug("Setting current price as: " + bid.getPrice());
		winner = bid.getBidder();
	}

	/**
	 * Bids must be sorted from lowest to highest, and must be from the same
	 * time period
	 */
	public void addBids(List<Bid> bids) {
		this.bidHistory.addAll(bids);
		bidCount += bids.size();
		bidHistory.addAll(bids);
		Bid winningBid = bids.get(bids.size() - 1);
		this.currentPrice = winningBid.getPrice();
		winner = winningBid.getBidder();
	}

	public List<Bid> getBidHistory() {
		return Collections.unmodifiableList(bidHistory);
	}
	public int getBidCount() {
		return this.bidCount;
	}
	
	public Bid getFirstBid() {
//		return this.firstBid;
		if (bidCount != 0)
			return this.bidHistory.get(0);
		else
			return null;
	}
	
	public Bid getLastBid() {
//		return this.lastBid;
		if (bidCount != 0)
			return this.bidHistory.get(this.bidHistory.size() - 1);
		else
			return null;
	}

	public boolean hasNoBids() {
		return winner == null;
//		return this.bidHistory.isEmpty();
	}

	public double getPopularity() {
		return this.popularity;
	}
	
	/**
	 * @return User with the highest bid currently, or the winner, if the
	 *         auction has ended, or <code>null</code> if no one has bid.
	 */
	public SimpleUserI getWinner() {
		return this.winner;
	}

	@Override
	public String toString() {
		return "(id:" + id + ", start:" + this.startTime + ", " +
//				"cat:" + category.getName() + ", " +
				"item:" + item + ", sellerId:" + seller.getId() + ", bidCount:" + this.bidCount + ")";
	}

	public boolean idAndStartTimeSet() {
		return this.id != -1 && this.startTime != -1;
	}
	
	public enum AuctionLength {
		ONE_DAY(288),
		SEVEN_DAYS(2016), 
		TEN_DAYS(2880);
		private final int timeUnits;
		AuctionLength(int timeUnits) {
			this.timeUnits = timeUnits;
		}
		public int timeUnits() {
			return timeUnits;
		}
	}
	
	/*
	 * % time elapsed since the auction was accepted by AH
	 */
	public double percentageElapsed(long currentTime) {
		return (double)(currentTime - this.getStartTime()) / (this.getEndTime() - this.getStartTime());
	}
	
	public int minimumBid() {
		if (this.bidCount == 0)
			return this.currentPrice;
		else 
			return this.currentPrice + Util.minIncrement(this.currentPrice);
	}
	
	/**
	 * Current price as a proportion of true item valuation
	 */
	public double proportionOfTrueValuation() {
		return (double) this.currentPrice / trueValue(); 
	}
	/**
	 * Next minimum bid as a proportion of true item valuation
	 */
	public double nextBidProportionOfTrueValuation() {
		return (double) minimumBid() / trueValue(); 
	}
	
	public double trueValue() {
		return this.getItem().getType().getTrueValuation();
	}
}
