package createUserFeatures;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import createUserFeatures.FeaturesToUseWrapper.FeaturesToUse;

import util.Util;


/**
 * Object storing information about a user's bidding information.
 * Contains methods to convert that information into features for clustering 
 * @author SidTDesktop
 *
 */
public class UserFeatures {
	private static String delimiter = ",";
	
	int userId;
	int pos, neg; // reputation
	
	int bidCount = 0; // number of bids made
	double bidAvg; // average value of bids
	
	int bidIncCount; // number of bidIncrements made (does not count as incrementing a bid if user is first to bid)
	double avgBidInc; // average bid increment
	double avgBidIncMinusMinInc; // average bid increment minus minimum increment

	int auctionCount; // number of auctions as a bidder
	int auctionsWon; // number of auctions won
	double bidCountAvg; // average number of bids made in all auctions
//	double bidCountVar; // variance in the number of bids made in an auction
	double lastBidTime; // average time until the end of the auction the last bid was made
	final double[] bidPeriods;
	final double[] bidPeriodsLogBins;
	double firstBidTimes; // average number of minutes from the end of the auction the first bid was made
	double selfBidInterval; // average bid interval
	double anyBidInterval; // average bid interval
	final List<Double> bidTimesBeforeEnd;
	final List<Long> bidMinsBeforeEnd;

	double avgNumCategory; // number of categories per auction the user is in
	Set<String> categories;
	
	public String cluster;
	public final FeaturesToUseWrapper fpWrapper;
	
	double avgBidAmountComparedToFinal; // average of the bid amounts as fractions of the maximum bid in the same auction
	double avgBidProp;
	
	public UserFeatures(FeaturesToUseWrapper featuresToPrintWrapper) {
		this.fpWrapper = featuresToPrintWrapper;

		this.bidPeriods = new double[4];
		this.bidPeriodsLogBins = new double[11];
		this.categories = new HashSet<>();

		this.bidTimesBeforeEnd = new ArrayList<>();
		this.bidMinsBeforeEnd = new ArrayList<>();
		
		// uninitilised values. used to find which users do not have a feedback page, and so have no reputation
		pos = -1;
		neg = -1;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public void setRep(int pos, int neg) {
		this.pos = pos;
		this.neg = neg;
	}
	
	/**
	 * 
	 * Adds this new bid to the list of bids made by the user.
	 * If <code>previousBid < 0</code>, this bid is the first in
	 * the auction, and therefore has no increment or minIncrement.
	 * 
	 * @param bid value of the bid
	 * @param previousBid value of the previous bid
	 * @param maximumBid the value of the last/highest bid in the auction
	 */
	public void addBid(int bid, int previousBid, int maximumBid) {
		if (previousBid > 0) { // test whether there's a previous bid
			int increment = bid - previousBid; // find the difference between this and the previous bid amount
			avgBidInc = Util.incrementalAvg(avgBidInc, bidIncCount, increment);
			
			int incMinusMin = increment - Util.minIncrement(previousBid);
			if (incMinusMin < 0)
				incMinusMin = 0;
			avgBidIncMinusMinInc = Util.incrementalAvg(avgBidIncMinusMinInc(), bidIncCount, incMinusMin);
			bidIncCount++;
		}
		// update average bid value
		bidAvg = Util.incrementalAvg(bidAvg, bidCount, bid);
		// update avgBidComparedToFinal
		double fractionOfMax = ((double) bid) / maximumBid;
		avgBidAmountComparedToFinal = Util.incrementalAvg(avgBidAmountComparedToFinal, bidCount, fractionOfMax);
		bidCount++;
	}
	
	public boolean isComplete() {
		return pos != -1; //&& neg != -1
	}
	
//	public void addAuction(String category) {
//		categories.add(category);
//		auctionCount++;
//	}
	
	public void addWonAuction() {
		auctionsWon++;
		assert propWin() <= 1 : "propWin > 1";
	}
	
	public void addAuction(String category, 
			int numberOfBids, 
			int timeUntilEnd
//			boolean won
			) {
		categories.add(category);
		bidCountAvg = Util.incrementalAvg(bidCountAvg, auctionCount, numberOfBids);
		lastBidTime = Util.incrementalAvg(lastBidTime, auctionCount, timeUntilEnd);
//		if (won) { auctionsWon++; }
		auctionCount++;
	}
	
	public int id() {
		return userId;
	}
	
	public int numAuctionsBidIn() {
		return auctionCount;
	}

	public double numAuctionsBidInLn() {
		return Math.log(auctionCount);
	}
	
	public int rep() {
		return pos - neg;
	}

	public double repLn() {
		if (rep() < 0)
			return -Math.log1p(-rep());
		else
			return Math.log1p(rep());
//		return Math.log1p(pos) - Math.log1p(neg);
	}
	
	public String repOrdinal() {
		if (neg > pos)
			return "NEG";
		else if (pos - neg < 5)
			return "NEU";
		else
			return "POS";
	}
	
	public double avgBid() {
		return bidAvg;
	}
	public double avgBidLn() {
		return Math.log(bidAvg);
	}
	public double bidAvgOrdinal() {
		if (bidAvg < 800)
			return 1;
		else if (bidAvg < 2500)
			return 2;
		else
			return 3;
	}
	
	public double avgBidInc() {
		assert avgBidInc >= 0;
		return avgBidInc;
	}

	public double avgBidIncMinusMinInc() {
		assert avgBidIncMinusMinInc >= 0;
		return avgBidIncMinusMinInc;
	}
	public double avgBidIncMinusMinIncLn() {
		return Math.log1p(avgBidIncMinusMinInc());
	}
	
	public double propWin() {
		return ((double) auctionsWon)/auctionCount;
	}
	public int propWinOrdinal() {
		return discritiseEvenBins(3, propWin());
	}
	
	public double bidsPerAuc() {
		return bidCountAvg;
	}
	public double bidsPerAucLn() {
		return Math.log(bidCountAvg);
	}
	public int avgBidPerAucOrdinal() {
		if (bidCountAvg == 1)
			return 1;
		else if (bidCountAvg <= 4)
			return 2;
		else
			return 3;
	}
	
	public double avgMinsToEnd() {
		return lastBidTime;
	}
	
	public double avgCatPerAuc() {
		return avgNumCategory;
	}
	
	public boolean hasBids() {
		return bidCount != 0 && auctionCount != 0;
	}
	
	public static String headings(FeaturesToUse ufs) {
		StringBuilder sb = new StringBuilder();
		if (ufs.printUserId)
			sb.append("0userId").append(delimiter);
		if (ufs.printNumAuctionsBidIn)
			sb.append("1AuctionCount").append(delimiter);
		if (ufs.printNumAuctionsBidInLn)
			sb.append("1lnAuctionCount").append(delimiter);
		if (ufs.printRep)
			sb.append("2Rep").append(delimiter);
		if (ufs.printRepLn)
			sb.append("2lnRep").append(delimiter);
		if (ufs.printAvgBid)
			sb.append("3AvgBidAmount").append(delimiter);
		if (ufs.printAvgBidLn)
			sb.append("3lnAvgBidAmount").append(delimiter);
		if (ufs.printAvgBidPropMax)
			sb.append("10AvgBidPropMax").append(delimiter);
		if (ufs.printBidInc)
			sb.append("4AvgBidInc").append(delimiter);
		if (ufs.printBidIncLn)
			sb.append("4lnAvgBidInc").append(delimiter);
		if (ufs.printPropWin)
			sb.append("5PropWin").append(delimiter);
		if (ufs.printBidsPerAuc)
			sb.append("6BidsPerAuc").append(delimiter);
		if (ufs.printBidsPerAucLn)
			sb.append("6lnBidsPerAuc").append(delimiter);
		if (ufs.printAvgBidProp)
			sb.append("11AvgBidProp").append(delimiter);
		if (ufs.printBidPeriod) {
			sb.append("7BidBeg").append(delimiter);
			sb.append("7BidMid").append(delimiter);
			sb.append("7BidEnd").append(delimiter);
		}
		if (ufs.printBidPeriodAlt)
			sb.append("9AvgBidTimesUntilEnd").append(delimiter);
		if (ufs.printBidMinsBeforeEnd)
			sb.append("12BidMinsBeforeEnd").append(delimiter);
		if (ufs.printAucPerCat)
			sb.append("8AuctionsPerCat").append(delimiter);
		if (ufs.printFirstBidTimes)
			sb.append("13FirstBidTimes").append(delimiter);
		if (ufs.printSelfBidInterval)
			sb.append("14SelfBidInterval").append(delimiter);
		if (ufs.printAnyBidInterval)
			sb.append("15AnyBidInterval").append(delimiter);
		

		sb.deleteCharAt(sb.length() - delimiter.length());
		return sb.toString();
	}
	public static String headingsWithCluster(FeaturesToUse ufs) {
		return headings(ufs) + delimiter + "Cluster";   
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		FeaturesToUse featuresToPrint = this.fpWrapper.getFeaturesToUse();
		if (featuresToPrint.printUserId) {
			sb.append(userId);
			sb.append(delimiter);
		}
		if (featuresToPrint.printNumAuctionsBidIn) {
			sb.append(numAuctionsBidIn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printNumAuctionsBidInLn) {
			sb.append(numAuctionsBidInLn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printRep) {
			if (pos == -1)
				throw new RuntimeException("Rep not present for " + userId + ".");
			sb.append(rep());
			sb.append(delimiter);
		}
		if (featuresToPrint.printRepLn) {
			sb.append(repLn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printAvgBid) {
			sb.append(avgBid());
			sb.append(delimiter);
		}
		if (featuresToPrint.printAvgBidLn) {
			sb.append(avgBidLn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printAvgBidPropMax) {
			sb.append(avgBidAmountComparedToFinal);
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidInc) {
			if (bidIncCount != 0) {
//				sb.append(avgBidInc());
				sb.append(avgBidIncMinusMinInc());
			}
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidIncLn) {
			if (bidIncCount != 0)
				sb.append(avgBidIncMinusMinIncLn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printPropWin) {
			sb.append(propWin());
//			sb.append(propWinOrdinal());
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidsPerAuc) {
			sb.append(bidsPerAuc());
//			sb.append(avgBidPerAucOrdinal());
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidsPerAucLn) {
			sb.append(bidsPerAucLn());
//			sb.append(avgBidPerAucOrdinal());
			sb.append(delimiter);
		}
		if (featuresToPrint.printAvgBidProp) {
			sb.append(avgBidProp);
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidPeriod) {
//			sb.append(avgMinsToEnd());
//			sb.append(delimiter);
			sb.append(bidPropBeg()); // beg
			sb.append(delimiter);
			sb.append(bidPropMid()); // mid
			sb.append(delimiter);
			sb.append(bidPropEnd()); // end
			sb.append(delimiter);
//			sb.append(delimiter);
//			sb.append(bidsInBeginning()); // beg
//			sb.append(delimiter);
//			sb.append(bidsInMiddle()); // mid
//			sb.append(delimiter);
//			sb.append(bidsInEnd()); // end
//			sb.append(delimiter);
//			sb.append(mostBidPeriod());
//			sb.append(delimiter);
		}
		if (featuresToPrint.printBidPeriodAlt) {
//			sb.append(bidAtEnd());
			sb.append(bidTimeBeforeEndAvg());
			sb.append(delimiter);
		}
		if (featuresToPrint.printBidMinsBeforeEnd) {
			sb.append(bidMinsBeforeEnd());
			sb.append(delimiter);
		}
		if (featuresToPrint.printAucPerCat) {
//			sb.append(auctionsPerCat());
			sb.append(auctionsPerCatLn());
			sb.append(delimiter);
		}
		if (featuresToPrint.printFirstBidTimes) {
			sb.append(Math.log(firstBidTimes));
			sb.append(delimiter);
		}
		if (featuresToPrint.printSelfBidInterval) {
			if (selfBidInterval != 0)
				sb.append(Math.log1p(selfBidInterval));
//				sb.append(selfBidInterval);
			sb.append(delimiter);
		}
		if (featuresToPrint.printAnyBidInterval) {
			if (anyBidInterval != 0)
				sb.append(Math.log1p(anyBidInterval));
//				sb.append(anyBidInterval);
			sb.append(delimiter);
		}
		sb.deleteCharAt(sb.length() - delimiter.length());
		return sb.toString();
	}
	
	public String toStringWithCluster() {
		return this.toString() + delimiter + "c" + cluster;
	}
	
	public double bidPropBeg() {
		return bidPeriods[0];
	}
	public double bidPropMid() {
		return bidPeriods[1];
	}
	public double bidPropEnd() {
		return bidPeriods[2];
	}
	
	public double auctionsPerCat() {
		return (double) auctionCount/categories.size();
	}
	private double auctionsPerCatLn() {
		return Math.log(auctionsPerCat());
	}

	// boolean attributes for whether a user bids in beg, mid or end of an auction
	private static final double bidInThreshold = 0.3;
	private boolean bidsInBeginning() {
		return bidPeriods[0] > bidInThreshold;
	}
	private boolean bidsInMiddle() {
		return bidPeriods[1] > bidInThreshold;
	}
	private boolean bidsInEnd() {
		return bidPeriods[2] > bidInThreshold;
	}
	
	private boolean bidAtEnd() {
		return bidPeriods[2] >= bidPeriods[1] && bidPeriods[2] >= bidPeriods[0];
	}
	public double bidTimeBeforeEndAvg() {
		double result = 0;
		int count = 0;
		for (int i = 0; i < bidTimesBeforeEnd.size(); i++) {
			result = Util.incrementalAvg(result, count, bidTimesBeforeEnd.get(i));
			count++;
		}
		return result;
	}
	private BidPeriod mostBidPeriod() {
		if (bidPeriods[2] >= bidPeriods[1]) {
			if (bidPeriods[2] >= bidPeriods[0])
				return BidPeriod.END;
			else
				return BidPeriod.BEGINNING;
		} else {
			if (bidPeriods[1] >= bidPeriods[0])
				return BidPeriod.MIDDLE;
			else
				return BidPeriod.BEGINNING;
		}
	}
	
	public double bidMinsBeforeEnd() {
		double result = 0;
		int count = 0;
		for (int i = 0; i < bidMinsBeforeEnd.size(); i++) {
//			result = Util.incrementalAvg(result, count, Math.log1p(bidMinsBeforeEnd.get(i)));
			result = Util.incrementalAvg(result, count, bidMinsBeforeEnd.get(i));
			count++;
		}
		return Math.log(result);
	}
	
	// returns values 0-9
	private int bidTimeBin(double fractionToEnd) {
		return (int) Math.pow(10, fractionToEnd) - 1;
	}

	/**
	 * Returns the bin number the value should be in.  Value must be between [0-1).
	 */
	private int discritiseEvenBins(int numberOfBins, double value) {
		return (int) (value * numberOfBins) + 1;
	}
	
}