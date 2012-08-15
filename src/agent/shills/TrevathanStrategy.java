package agent.shills;

import java.util.List;

import simulator.objects.Auction;
import simulator.objects.Bid;

/**
 *	Strategy consistent with the behaviour described in <i>A Simple Shill Bidding Agent</i>
 *	by <i>Trevathan et al.</i>.
 *
 *	Does NOT keep track of who is winning an auction; and does NOT
 *	keep track of whether the auction should be bid on by, e.g.,
 *	a shill.
 */
public class TrevathanStrategy implements Strategy {

	private final double theta;
	private final double alpha;
	private final double mu;
	
	public TrevathanStrategy(double theta, double alpha, double mu) {
		this.theta = theta;
		this.alpha = alpha;
		this.mu = mu;
	}
	
	@Override
	/**
	 * Always bid immediately.
	 */
	public long wait(Auction auction) {
		return 0;
	}

	@Override
	public boolean shouldBid(Auction shillAuction, long currentTime) {
		boolean d3success = directive3(theta, shillAuction, currentTime);
		boolean d4success = directive4(alpha, shillAuction);
		boolean d5success = directive5(mu, theta, currentTime, shillAuction);
		return d3success && d4success && d5success;
	}

	@Override
	/**
	 * Bid the minimum amount possible.
	 */
	public long bidAmount(Auction auction) {
		return auction.minimumBid();
	}
	
	

	/**
	 * D3 of Simple Shilling Agent by Trevathan; don't bid too close to auction end
	 */
	private static boolean directive3(double theta, Auction auction, long time) {
		double proportionRemaining = proportionRemaining(auction.getStartTime(), auction.getEndTime(), time);
//		System.out.println("proportionRemaining: " + proportionRemaining);
		return proportionRemaining <= theta;
	}
	
	/**
	 * D4 of Simple Shilling Agent by Trevathan; bid until target price is reached
	 */
	private static boolean directive4(double alpha, Auction auction) {
		double proportionPrice = auction.getCurrentPrice() / auction.trueValue();
//		System.out.println("proportionPrice: " + proportionPrice);
		return alpha > proportionPrice;
	}
	
	/**
	 * D5 of Simple Shilling Agent by Trevathan; bid when bid volume is high
	 * @param mu
	 * @param auction
	 * @return true if should bid, else false
	 */
	private static boolean directive5(double mu, double theta, long currentTime, Auction auction) {
		if (auction.getBidCount() == 0)
			return true;
		
		double muTime = proportionToTime(1 - mu, auction.getStartTime(), currentTime); // D5, how far back to look when counting bids
		double thetaTime = proportionToTime(theta, auction.getStartTime(), auction.getEndTime()); // D3, don't bid too close to end
		int numberOfBids = numberOfBids(auction.getBidHistory(), (long) (muTime + 0.5));
//		System.out.println("numberOfBids: " + numberOfBids);
		if (numberOfBids > 1) {
			return true;
		} else if (numberOfBids == 1) {
			// find the proportion of time time left available for the shill bidder to act
			double normalisedTime = proportionRemaining(auction.getStartTime(), thetaTime, currentTime);
//			System.out.println("normalisedTime: " + normalisedTime);
			if (normalisedTime < 0.85)
				return true;
		}	
		return false;
	}

	/**
	 * Counts the number of bids that were made after the timeLimit by bidders
	 * who are not shill bidders from this controller.
	 * @param bidHistory
	 * @param time
	 */
	private static int numberOfBids(List<Bid> bidHistory, long timeLimit) {
		int count = 0;
		for (int i = bidHistory.size() - 1; i >= 0 && bidHistory.get(i).getTime() >= timeLimit; i--) {
			count++;
		}
		return count;
	}
	
	private static double proportionRemaining(long start, double end, long current) {
		return ((double) current - start)/(end - start);
	}
	
	private static double proportionToTime(double proportion, long start, long end) {
		return proportion * (end - start) + start;  
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "." + theta + "." + alpha + "." + mu;
	}

}
