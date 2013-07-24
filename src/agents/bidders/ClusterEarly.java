package agents.bidders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;

public class ClusterEarly extends ClusterBidder {

	private static final Logger logger = Logger.getLogger(ClusterEarly.class);

	public ClusterEarly(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes) {
		super(bh, ps, is, ah, itemTypes);
	}

	// returns a value between .7-.85 of an item's private valuation, uniform distribution
	private int firstBidPrice(Auction auction) {
		return (int) ((r.nextDouble() * privateValuationProportion * 0.15 + privateValuationProportion*0.7) * auction.trueValue() + 0.5);
	}
	
	@Override
	public void run() {
		super.run();
		
		long currentTime = this.bh.getTime();
		selectAuctionsToBidIn();
		
		Set<Auction> alreadyBidOn = new HashSet<Auction>();
		if (this.auctionsToBidIn.containsKey(currentTime)) {
			for (Auction auction : this.auctionsToBidIn.removeAll(currentTime)) {
				if (!alreadyBidOn.contains(auction)) {
					if (r.nextDouble() < valuationEffect(auction.getCurrentPrice(), privateValuationProportion * auction.getCurrentPrice())) {
						// if item is under 50% value, made a bid greater than the minimum
//						if (auction.nextBidProportionOfTrueValuation() / privateValuationProportion < 0.5 && r.nextDouble() < 0.7) {
						if (auction.nextBidProportionOfTrueValuation() / privateValuationProportion < 0.4 && r.nextDouble() < 0.3) {
							int bidAmount = (int) (auction.trueValue() * 0.6);
							if (bidAmount < auction.minimumBid())
								bidAmount = auction.minimumBid();
							makeBid(auction, bidAmount);
						} else { // make the minimum possible bid
							makeBid(auction);
						}
						revisitLater(auction);
						alreadyBidOn.add(auction);
					}
				}
			}
		}
			
		if (revisitForRebids.containsKey(currentTime)) {
			for (Auction auction : revisitForRebids.removeAll(currentTime)) {
				if (!alreadyBidOn.contains(auction)) {
					boolean rebidMade = prepareRebid(auction);
					if (rebidMade) 
						alreadyBidOn.add(auction);
				}
			}
		}
	}
	
	
	private boolean prepareRebid(Auction auction) {
//		if (r.nextDouble() < 0.04) {
//			return false;
//		}
		
//		if (makeRebidAuctions.contains(auction) && auction.getWinner() != this) {
//			makeRebidAuctions.remove(auction);
//			if (r.nextDouble() < likelihoodToRebid(auction.getBidCount())) {
			int bidAmount = calculateBidAmount(auction);
			
//			if (r.nextDouble() < likelihoodOfRebid * valuationEffect(bidAmount, privateValuationProportion)) {
			double maximumBid = privateValuationProportion * auction.trueValue();
			if (r.nextDouble() < valuationEffect(bidAmount, maximumBid)) {
//				if (auction.percentageElapsed(this.currentTime) < 0.6) {
//					logger.debug(this + " making rebid for " + auction + " at " + this.currentTime);
					makeBid(auction, bidAmount);
					revisitLater(auction);
					return true;
//				}
			} else {
				return false;
			}
//		}
	}
	
	/**
	 * 76.0% of bids are made in the first half of the auction, where first half means the 
	 * time since the first bid in the auction until halfway to auction end.
	 */
//	private void scheduleNextEarlyBid(Auction auction) {
//		scheduleNextEarlyBid(auction, 1);
//	}
//	private void scheduleNextEarlyBid(Auction auction, double factor) {
//		// CDF: y = 1 - e^(-kx); y = probability, x = time; k = param
//		// Problem: propFromStart > 1 if ran > 0.9. So discard all ran's > 0.9.
//		long lengthOfAuction;
//		if (auction.hasNoBids()) {
//			lengthOfAuction = auction.getEndTime() - auction.getStartTime();
//		} else {
//			lengthOfAuction = auction.getEndTime() - auction.getFirstBid().getTime();
//		}
//		
//		double timeUnitToBid = 0;
//		double propFromStart = 0;
//		
//		// keep looking for a "timeUnitToBid" if the time picked is earlier than the current time
//		while (timeUnitToBid == 0 || timeUnitToBid < this.currentTime) {
//			propFromStart = propFromStart();
////			System.out.println("propFromStart: " + propFromStart);
//			timeUnitToBid = factor * propFromStart;
//			timeUnitToBid = propFromStart * lengthOfAuction + (auction.getEndTime() - lengthOfAuction);
//			assert propFromStart <= 1 && propFromStart >= 0;
//			assert timeUnitToBid <= auction.getEndTime() : timeUnitToBid + ":" + auction.getEndTime();
//			
//		}
////		if (timeUnitToBid >= this.currentTime) {
//			assert(timeUnitToBid >= this.currentTime);
//		
//			Util.mapListAdd(this.auctionsToBidIn, (long) (timeUnitToBid + 0.5), auction);
////			logger.info(this + " is making an early bid in the future at " + timeUnitToBid + " at time " + this.currentTime + ".");
//			logger.debug("difference is: " + (timeUnitToBid - this.currentTime));
//			
////		}
//		
////		double propTime = (((double) timeUnitToBid - auction.getFirstBid().getTime()) / (auction.getEndTime() - auction.getFirstBid().getTime()));
////		if (propTime > 1) {
////			System.out.println("paaaaause");
////		}
////		System.out.println("EarlyBid: " + propTime);
//		
//	}
	
	HashMultimap<Long, Auction> revisitForRebids = HashMultimap.create();
	private void revisitLater(Auction auction) {
		long currentTime = this.bh.getTimeMessage().getTime();
//		int delayForRevisit = 288; // since this runs before action(), if delay is zero, auction will be revisited immediately
		int delayForRevisit = 188 + r.nextInt(200);
		
		revisitForRebids.put(currentTime + delayForRevisit, auction);
	}
	
//	private double propFromStart() {
//		double param = -Math.log(0.1);
//		double random;
//		do {
//			random = r.nextDouble();
//		} while (random > 0.9);
//		double propFromStart = -Math.log(1 - random) / param;
////		propFromStart *= 0.7;
//		return propFromStart;
//	}
	private double propFromStart() {
		return r.nextDouble();
	}

//	public static void main(String[] args) {
//		ClusterEarly cb = new ClusterEarly(null, null, null, null);
//		for (int i = 0; i < 30000; i++) {
////			System.out.print(cb.firstBidTime()+",");
//			cb.firstBidTime();
//		}
//	}
	
	// (960 -10080] minutes before the end
//	private long firstBidTime() {
//		double propBeforeEnd = 1 - Math.pow(r.nextDouble(), 0.1);
//		double minBeforeEnd = propBeforeEnd * (10080 - 960) + 960;
//		return SEVEN_DAYS - (long) (minBeforeEnd/AuctionHouse.UNIT_LENGTH + 0.5);
//	}

	@Override
	//y = 4.5727*e^(8.2147* x); y is bid-time, x is U[0-1]
	protected long firstBidTime() {
		double bidTimeBeforeEnd;
		do {
			double random = r.nextDouble();
			if (random < 0.222)
				bidTimeBeforeEnd = 1.720952 * Math.pow(88753792, random) / 5;
			else if (random < 0.4633)
				bidTimeBeforeEnd = 12.46477 * Math.pow(16778.98, random) / 5;
			else
				bidTimeBeforeEnd = 169.6812 * Math.pow(66.27293, random) / 5;
		} while (bidTimeBeforeEnd <= 260 || bidTimeBeforeEnd >= 2016);
		return SEVEN_DAYS - (long) bidTimeBeforeEnd;
	}
	

}
