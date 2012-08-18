package simulator.records;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import agents.SimpleUser;


import simulator.database.SaveObjects;
import simulator.objects.Auction;
import simulator.objects.Bid;
import util.Util;

public class BidRecord {
	
	private static final Logger logger = Logger.getLogger(BidRecord.class);

	private int bidCount;
	private final Random r;
	
	public BidRecord() {
		bidCount = 0;
		r = new Random();
	}
	
	public void processAuctionBids(Auction auction, List<Bid> auctionBids, long time) {
		List<Bid> highestBids = sortBidsAndReturnHighest(auctionBids);
		
		// assert that all bids are higher than the minimum increment by testing the lowest in the ordered list
		assert auctionBids.isEmpty() || auction.hasNoBids() || auction.getCurrentPrice() + Util.minIncrement(auction.getCurrentPrice()) <= auctionBids.get(0).getPrice() :
			"brokenBids:" + auctionBids;
		
		Bid winningBid;
		if (highestBids.size() == 1) {
			winningBid = highestBids.get(0);
		} else {
			// randomly pick a highest bid if there is more than 1
			winningBid = highestBids.get(r.nextInt(highestBids.size()));
		}
		
		winningBid.setId(this.bidCount);
		this.bidCount++;
		winningBid.setTime(time);
		
		// update the state of the auction with the winning bid
		auction.addBid(winningBid);
		
		// save to database
		SaveObjects.saveBid(auction, winningBid);
	}

	/**
	 * Sorts the list of bids into increasing order.  Removes the highest bid on the list
	 * and returns it. If there's more than 1 bidder making the same highest bid, all of
	 * the highest bids are returned.
	 */
	private static List<Bid> sortBidsAndReturnHighest(List<Bid> bids) {
		if (bids.isEmpty())
			return Collections.emptyList();
		else if (bids.size() == 1) {
			return Collections.singletonList(bids.get(0));
		}
		
		{ // asserts no user submitted two bids for the same auction
			boolean assertOn = false;
			assert assertOn = true;
			if (assertOn) {
				Set<SimpleUser> users = new HashSet<SimpleUser>();
				for (Bid bid : bids) {
					boolean exists = !users.add(bid.getBidder());
					if (exists)
						logger.error("Bids are: " + bids);
					assert !exists : "More than 1 bid by " + bid.getBidder() + " for the same auction.";
				}
			}
		}
		
		Collections.sort(bids);
		for (int i = 0; i + 1 < bids.size(); i++) {
			assert(bids.get(i).getPrice() <= bids.get(i+1).getPrice());
		}
		
		int i = numberOfSameMaxBids(bids);
		assert(i > 0) : "There must be 1 or more highest bid";
		
//		int elementFromEnd = (int) (Math.random() * i);
		
		return bids.subList(bids.size() - i, bids.size());
		
//		System.out.println("Same number of max bids: " + i);
//		System.out.println("winner is: " + (bids.size() - 1 - elementFromEnd));
//		return bids.get(bids.size() - 1 - elementFromEnd);
	}

	private static int numberOfSameMaxBids(List<Bid> bids) {
		Bid previousBid = null;
		int i = 0;
		for (; i < bids.size(); i++) {
			Bid currentBid = bids.get(bids.size() - i - 1);
			if (previousBid != null && currentBid.getPrice() != previousBid.getPrice()) { // test if next biggest bid is the same
				break;
			} else { 
				previousBid = currentBid;
			}
		}
		return i;
	}

}
