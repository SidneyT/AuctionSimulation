package shillScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import util.IncrementalMean;
import util.Util;

public class ShillScore {
	
	private final int id; // bidderId
	public final String userType;
	int winCount;
	int lossCount;
	final Map<Integer, Integer> lossCounts; // number of auctions lost by this bidder for each seller (sellerId, count)
	final IncrementalMean bidProportion; // proportion of bids compared to maximum
	final IncrementalMean interBidTime; // number of time units since previous bid
	final IncrementalMean bidIncrement; // amount increment compared to previous bid
	final IncrementalMean firstBidTime; // number of minutes before auction end
	
	ShillScore(int id, String userType) {
		this.id = id;
		this.userType = userType;
		this.lossCounts = new HashMap<>();
		this.bidProportion = new IncrementalMean();
		this.interBidTime = new IncrementalMean(); 
		this.bidIncrement = new IncrementalMean(); 
		this.firstBidTime = new IncrementalMean(); 
	}
	
	public int getId() {
		return this.id;
	}
	
	public int getWinCount() {
		return this.winCount;
	}
	
	public int getLossCount() {
		return this.lossCount;
	}
	
	@Override
	public String toString() {
		return "(" + id + ", "+ winCount + ", " + lossCount + ", " + lossCounts + ", " + bidProportion + ", " + interBidTime + ", " + bidIncrement + ", " + firstBidTime + ")";
	}
	
	/**
	 * Returns sellerId-alpha pairs
	 * @param auctionCounts
	 * @return
	 */
	public Map<Integer, Double> getAlphas(Map<Integer, Integer> auctionCounts) {
		// Map<sellerId, alpha>
		Map<Integer, Double> values = new HashMap<>();
		for (Entry<Integer, Integer> countEntry : auctionCounts.entrySet()) {
			values.put(countEntry.getKey(), (double) countEntry.getValue() / auctionCounts.get(countEntry.getKey()));
		}
		
		return values;
	}
	
	/**
	 * Returns the highest alpha value, and all sellerIds associated with this max value.
	 * @param auctionCounts the number of auctions submitted by a seller (sellerId, number of auctions)
	 * @return
	 */
	public Thing getAlpha(Map<Integer, Integer> auctionCounts) {
		double maxAlpha = Double.MIN_VALUE;
		List<Integer> maxSellerIds = new ArrayList<Integer>(1);
		for (Entry<Integer, Integer> countEntry : lossCounts.entrySet()) {
			int sellerId = countEntry.getKey();
			double alpha = (double) countEntry.getValue() / auctionCounts.get(sellerId);
			if (alpha > maxAlpha) {
				maxAlpha = alpha;
				if (alpha == maxAlpha)
					maxSellerIds.add(sellerId);
				else
					maxSellerIds.clear();
			}
		}
		return new Thing(maxSellerIds, maxAlpha);
	}
	
	public static class Thing {
		final List<Integer> sellerIds;
		final double maxAlpha;
		public Thing(List<Integer> sellerIds, double maxAlpha) {
			this.sellerIds = sellerIds;
			this.maxAlpha = maxAlpha;
		}
	}

	/**
	 * Returns alpha for the sellerId given.
	 * @param auctionCounts the number of auctions submitted by a seller (sellerId, number of auctions)
	 * @param sellerId
	 * @return
	 */
	public double getAlpha(Map<Integer, Integer> auctionCounts, int sellerId) {
		return (double) lossCounts.get(sellerId) / auctionCounts.get(sellerId);
	}
	
	public double getBeta() {
		return bidProportion.getAverage();
	}
	
	public double getGamma() {
		if (lossCount == 0)
			return 0;
		double value = 1 - (5 * (winCount + 0.2) / lossCount); 
		return value < 0 ? 0 : value;
	}
	
	public double getDelta() {
		return 1 - interBidTime.getAverage();
	}
	
	public double getEpsilon() {
		return 1 - bidIncrement.getAverage();
	}
	
	public double getZeta() {
		return firstBidTime.getAverage();
	}
	
	/**
	 * Get the shill score. The alpha score used is the highest one for all
	 * bidder/seller pairs, given this bidder.
	 */
	public static final double[] DEFAULT_WEIGHTS = {9, 2, 5, 2, 2, 2}; //22
	public double getShillScore(Map<Integer, Integer> auctionCounts, double[] weights) {
		double score = 0;
		score += this.getAlpha(auctionCounts).maxAlpha * weights[0];
		score += this.getBeta() * weights[1];
		score += this.getGamma() * weights[2];
		score += this.getDelta() * weights[3];
		score += this.getEpsilon() * weights[4];
		score += this.getZeta() * weights[5];

		double weightSum = 0;
		for (int i = 0; i < weights.length; i++) {
			weightSum += weights[i];
		}
		
		return score / weightSum * 10;
	}
	/**
	 * Get the shill score. The alpha score used is the one calculated for
	 * the sellerId given.
	 */
	public double getShillScore(Map<Integer, Integer> auctionCounts, int sellerId, double[] weights) {
		double score = 0;
		score += this.getAlpha(auctionCounts, sellerId) * weights[0];
		score += this.getBeta() * weights[1];
		score += this.getGamma() * weights[2];
		score += this.getDelta() * weights[3];
		score += this.getEpsilon() * weights[4];
		score += this.getZeta() * weights[5];

		int weightSum = 0;
		for (int i = 0; i < weights.length; i++) {
			weightSum += weights[i];
		}
		
		return score / weightSum * 10;
	}
	
	public double getShillScore(Map<Integer, Integer> auctionCounts) {
		return getShillScore(auctionCounts, DEFAULT_WEIGHTS);
	}
	public double getShillScore(Map<Integer, Integer> auctionCounts, int sellerId) {
		return getShillScore(auctionCounts, sellerId, DEFAULT_WEIGHTS);
	}
	
	public double bayseanSS(double groupSize, double mean, Map<Integer, Integer> auctionCounts, int sellerId) {
		return Util.bayseanAverage(groupSize, mean, lossCount, this.getShillScore(auctionCounts, sellerId));
	}
	
	public double bayseanSS(double groupSize, double mean, Map<Integer, Integer> auctionCounts) {
		return Util.bayseanAverage(groupSize, mean, lossCount, this.getShillScore(auctionCounts));
	}
	
}
