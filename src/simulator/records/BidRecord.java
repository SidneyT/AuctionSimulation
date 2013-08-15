package simulator.records;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import agents.SimpleUserI;

import simulator.objects.Auction;
import simulator.objects.Bid;

public class BidRecord {
	
	private static final Logger logger = Logger.getLogger(BidRecord.class);

//	private final AtomicInteger bidCount;
	private final Random r;
	
	public BidRecord() {
//		bidCount = new AtomicInteger();
		r = new Random();
	}
	public static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			if (i == 5)
				break;
			System.out.println(i);
		}
	}
	
	public Bid processAuctionBids(Auction auction, List<Bid> auctionBids, long time) {
		List<Bid> highestBids = sortBidsAndReturnHighest(auctionBids);
		
		// assert that all bids are higher than the minimum increment by testing the lowest in the ordered list
		assert auctionBids.get(0).getPrice() >= auction.minimumBid() : "bid lower than minimum required: " + auctionBids.get(0).getPrice() + " vs " + auction.minimumBid() + ".";  
		
		Bid winningBid;
		if (highestBids.size() == 1) {
			winningBid = highestBids.get(0);
		} else {
			// randomly pick a highest bid if there is more than 1
			winningBid = highestBids.get(r.nextInt(highestBids.size()));
		}
		
//		winningBid.setId(bidCount.getAndIncrement());
		winningBid.setTime(time);
		
		// update the state of the auction with the winning bid
		auction.addBid(winningBid);
		
		return winningBid;
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
		
		Collections.sort(bids);

		{ // asserts no user submitted two bids for the same auction
			boolean assertOn = false;
			assert assertOn = true; // assertOn only becomes true if assertions are actually on.
			if (assertOn) {
				Set<SimpleUserI> users = new HashSet<>();
				for (Bid bid : bids) {
					boolean exists = !users.add(bid.getBidder());
					if (exists)
						logger.error("Bids are: " + bids);
					assert !exists : "More than 1 bid by " + bid.getBidder() + " for the same auction.";
				}
			}
			
			// assert each bid is sorted so that each is strictly greater than the previous
			if (assertOn) {
				for (int i = 0; i < bids.size() - 1; i++) {
					assert(bids.get(i).getPrice() <= bids.get(i+1).getPrice());
				}
			}
		}
		
		int i = numberOfSameMaxBids(bids);
		assert i > 0 : "There must be 1 or more highest bid";
		assert bids.get(bids.size() - 1).getPrice() == bids.get(bids.size() - i).getPrice() : "The two bid values should be the same. Might be bug in \"numberOfSameMaxBids\"";
		
		return bids.subList(bids.size() - i, bids.size());
	}

	/**
	 * Bids must be sorted in ascending value.
	 * @param bids
	 * @return
	 */
	private static int numberOfSameMaxBids(List<Bid> bids) {
		if (bids.size() < 1)
			return 1;
		
		int highestBidValue = bids.get(bids.size() - 1).getPrice();
		int i = 1;
		for (; i < bids.size(); i++) {
			Bid currentBid = bids.get(bids.size() - i - 1);
			if (currentBid.getPrice() != highestBidValue) { // test if next biggest bid is the same
				return i;
			}
		}
		return i;
	}

}
