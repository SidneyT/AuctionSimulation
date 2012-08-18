package agent.bidders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import distributions.Uniform;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.objects.Auction;
import util.Util;

public class ClusterSnipe extends ClusterBidder {

	private static final Logger logger = Logger.getLogger(ClusterSnipe.class);

	public ClusterSnipe(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId) {
		super(bh, ps, is, ah, uniqueId);
		
		probInterest *= 1.5;
	}

	private int firstBidPrice(Auction auction) {
		return (int) ((r.nextDouble() * privateValuationProportion * 0.15 + privateValuationProportion*0.7) * auction.trueValue() + 0.5);
	}
	
	@Override
	protected void action() {
		Set<Auction> alreadyBidOn = new HashSet<Auction>();
		
		long currentTime = this.bh.getTimeMessage().getTime();
				
		for (Auction auction : this.newAuctionsUnprocessed) {
			// randomly pick 1 auction to participate in
			if (shouldParticipateInAuction(currentTime)) {
				int index = Uniform.nextInt(r.nextDouble(), 0, this.newAuctionsUnprocessed.size());
				int count = 0;
					if (count == index) {
						this.ah.registerForAuction(this, auction);
			
						long timeToMakeBid = firstBidTime() + currentTime;
						logger.debug(this + " is making first bid in the future at " + timeToMakeBid + " at time " + currentTime + ".");
						Util.mapListAdd(auctionsToBidIn, timeToMakeBid, auction);
						participated();
						break;
					}
					count++;
			}
		}
		newAuctionsUnprocessed.clear();

		if (this.auctionsToBidIn.containsKey(currentTime)) {
			for (Auction auction : this.auctionsToBidIn.remove(currentTime)) {
				if (!alreadyBidOn.contains(auction)) {
					// if item is under 50% value, made a bid greater than the minimum
					if (auction.nextBidProportionOfTrueValuation() / privateValuationProportion < 0.5 && r.nextDouble() < 0.6) {
						long bidAmount = (long) (auction.trueValue() * 0.80);
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
		
		if (revisitForRebids.containsKey(currentTime)) {
			for (Auction auction : this.revisitForRebids.remove(currentTime)) {
				if (!alreadyBidOn.contains(auction)) {
					boolean rebidMade = prepareRebid(auction);
					if (rebidMade) 
						alreadyBidOn.add(auction);
				}
			}
		}
	
	}
	
	Map<Long, Set<Auction>> revisitForRebids = new HashMap<Long, Set<Auction>>();
	private void revisitLater(Auction auction) {
		long currentTime = this.bh.getTimeMessage().getTime();
		int delayForRevisit = 1; // since this runs before action(), if delay is zero, auction will be revisited immediately 
		
		Util.mapSetAdd(this.revisitForRebids, currentTime + delayForRevisit, auction);
	}

	/**
	 * If a re-bid is made, return true, else return false.
	 * @param auction
	 * @return
	 */
	private boolean prepareRebid(Auction auction) {
//		if (r.nextDouble() < 0.04) {
//			return false;
//		}

//		if (makeRebidAuctions.contains(auction) && auction.getWinner() != this) {
//			makeRebidAuctions.remove(auction);
//			if (r.nextDouble() < likelihoodToRebid(auction.getBidCount())) {
			long bidAmount = calculateBidAmount(auction);
			
//			if (r.nextDouble() < likelihoodOfRebid * valuationEffect(bidAmount, privateValuationProportion)) {
			double maximumBid = privateValuationProportion * auction.getItem().getType().getTrueValuation();
			if (r.nextDouble() < valuationEffect(bidAmount, maximumBid)) {
//				if (auction.percentageElapsed(bh.getTimeMessage().getTime()) < 0.6) {
//					logger.debug(this + " making rebid for " + auction + " at " + bh.getTimeMessage().getTime());
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
	 * 80.0% of bids are made in the last 60 minutes of an auction.
	 * @param auction
	 */
//	private void scheduleNextSnipeBid(Auction auction) {
//		scheduleNextSnipeBid(auction, 1);
//	}
//	private void scheduleNextSnipeBid(Auction auction, double factor) {
//		double param = 0.026824; // -ln0.2/60
//		// CDF: y = 1 - e^(-kx); y = probability, x = time; k = param
//		double minuteToBidFromEnd = -Math.log(1 - r.nextDouble()) / param;
//		double timeUnitsFromEnd = minuteToBidFromEnd/AuctionHouse.UNIT_LENGTH; 
//		// modify timeUnitToBid by factor
//		timeUnitsFromEnd /= factor;
//		
//		long timeUnitToBid = auction.getEndTime() - (long) (timeUnitsFromEnd + 0.5);
////		timeUnitToBid = (long) (factor * (timeUnitToBid - bh.getTimeMessage().getTime()) + 0.5) + bh.getTimeMessage().getTime();
//		assert timeUnitToBid > auction.getStartTime() && timeUnitToBid <= auction.getEndTime();
//		Util.mapListAdd(this.auctionsToBidIn, timeUnitToBid, auction);
//		
////		if (r.nextDouble() < ((double) 1) - factor) {
////			scheduleNextSnipeBid(auction, factor);
////		}
//		
////		System.out.println("SnipeBid: " + ((double) (timeUnitToBid - auction.getStartTime()) / (auction.getEndTime() - auction.getStartTime())));
//		logger.debug(this + " is making a snipe bid in the future at " + timeUnitToBid + " at time " + bh.getTimeMessage().getTime() + ".");
//	}
	
	// [0-960] minutes before the end
//	private long firstBidTime() {
//		long unitsBeforeEnd = 0; 
//		while(unitsBeforeEnd == 0) {
//			double minBeforeEnd = (long) (Math.exp(2.8555 * Math.log(11.0762 * (1 - r.nextDouble()))) + 0.5);
//			unitsBeforeEnd = (long) (minBeforeEnd/AuctionHouse.UNIT_LENGTH + 0.5);
//		}
//		return SEVEN_DAYS - unitsBeforeEnd;
//	}
	private long firstBidTime() {
		double bidTimeBeforeEnd;
		do {
			double random = r.nextDouble();
			if (random < 0.222)
				bidTimeBeforeEnd = 1.720952 * Math.pow(88753792, random) / 5;
			else if (random < 0.4633)
				bidTimeBeforeEnd = 12.46477 * Math.pow(16778.98, random) / 5;
			else
				bidTimeBeforeEnd = 169.6812 * Math.pow(66.27293, random) / 5;
		} while (bidTimeBeforeEnd > 260 || bidTimeBeforeEnd < 1);
		return SEVEN_DAYS - (long) bidTimeBeforeEnd;
	}

	@Override
	protected long calculateBidAmount(Auction auction) {
		long bidAmount = auction.minimumBid();
		
		if (r.nextDouble() < pIncreaseIncrement) {
//			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * 3;
			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * Uniform.nextInt(r.nextDouble(), 1, 4);
		}
		
		return bidAmount;
	}
	

}