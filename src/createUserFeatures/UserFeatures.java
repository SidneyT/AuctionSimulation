package createUserFeatures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.Util;

/**
 * Object storing information about a user's bidding information. Contains methods to convert that information into
 * features for clustering
 * 
 * @author SidTDesktop
 * 
 */
public class UserFeatures {
	private static String delimiter = ",";

	int userId;
	int pos, neg; // reputation

	int bidCount = 0; // number of bids made
	double avgBid; // average value of bids

	int bidIncCount; // number of bidIncrements made (does not count as incrementing a bid if user is first to bid)

	public static String getDelimiter() {
		return delimiter;
	}

	double avgBidInc; // average bid increment
	double avgBidIncMinusMinInc; // average bid increment minus minimum increment

	private int auctionCount; // number of auctions as a bidder
	private int auctionsWon; // number of auctions won
	double bidsPerAuc; // average number of bids made in all auctions
	// double bidCountVar; // variance in the number of bids made in an auction
	double lastBidTime; // average time until the end of the auction the last bid was made
	final double[] bidPeriods;
	final double[] bidPeriodsLogBins;
	double firstBidTimes; // average number of minutes from the end of the auction the first bid was made
	double selfBidInterval; // average bid interval
	double anyBidInterval; // average bid interval
	final List<Double> bidTimesFractionBeforeEnd; // bid time as fraction befor
	final List<Long> bidMinsBeforeEnd;

	double avgNumCategory; // number of categories per auction the user is in
	Set<String> categories;

	double avgBidAmountComparedToMax; // average of the bid amounts as fractions of the maximum bid in the same auction
	double avgBidProp;

	public UserFeatures() {
		this.bidPeriods = new double[4];
		this.bidPeriodsLogBins = new double[11];
		this.categories = new HashSet<>();

		this.bidTimesFractionBeforeEnd = new ArrayList<>();
		this.bidMinsBeforeEnd = new ArrayList<>();

		// uninitilised values. used to find which users do not have a feedback page, and so have no reputation
		pos = -1;
		neg = -1;
	}

	public int getAuctionsWon() {
		return auctionsWon;
	}

	public int getAuctionCount() {
		return auctionCount;
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
	 * Adds this new bid to the list of bids made by the user. If <code>previousBid < 0</code>, this bid is the first in
	 * the auction, and therefore has no increment or minIncrement.
	 * 
	 * @param bid
	 *            value of the bid
	 * @param previousBid
	 *            value of the previous bid
	 * @param maximumBid
	 *            the value of the last/highest bid in the auction
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
		avgBid = Util.incrementalAvg(avgBid, bidCount, bid);
		// update avgBidComparedToFinal
		double fractionOfMax = ((double) bid) / maximumBid;
		avgBidAmountComparedToMax = Util.incrementalAvg(avgBidAmountComparedToMax, bidCount, fractionOfMax);
		bidCount++;
	}

	public int getUserId() {
		return userId;
	}

	public int getPos() {
		return pos;
	}

	public int getNeg() {
		return neg;
	}

	public int getBidCount() {
		return bidCount;
	}

	public double getAvgBid() {
		return avgBid;
	}

	public int getBidIncCount() {
		return bidIncCount;
	}

	public double getAvgBidInc() {
		return avgBidInc;
	}

	public double getAvgBidIncMinusMinInc() {
		return avgBidIncMinusMinInc;
	}

	public double getBidsPerAuc() {
		return bidsPerAuc;
	}

	public double getLastBidTime() {
		return lastBidTime;
	}

	public double[] getBidPeriods() {
		return bidPeriods;
	}

	public double[] getBidPeriodsLogBins() {
		return bidPeriodsLogBins;
	}

	public double getFirstBidTimes() {
		return firstBidTimes;
	}

	public double getSelfBidInterval() {
		return selfBidInterval;
	}

	public double getAnyBidInterval() {
		return anyBidInterval;
	}

	public List<Double> getBidTimesBeforeEnd() {
		return bidTimesFractionBeforeEnd;
	}

	public List<Long> getBidMinsBeforeEnd() {
		return bidMinsBeforeEnd;
	}

	public double getAvgNumCategory() {
		return avgNumCategory;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public double getAvgBidAmountComparedToMax() {
		return avgBidAmountComparedToMax;
	}

	public double getAvgBidProp() {
		return avgBidProp;
	}

	public static double getBidinthreshold() {
		return bidInThreshold;
	}

	public boolean isComplete() {
		return pos != -1; // && neg != -1
	}

	// public void addAuction(String category) {
	// categories.add(category);
	// auctionCount++;
	// }

	public void addWonAuction() {
		auctionsWon++;
		assert propWin() <= 1 : "propWin > 1";
	}

	public void addAuction(String category, int numberOfBids, int timeUntilEnd
	// boolean won
	) {
		categories.add(category);
		bidsPerAuc = Util.incrementalAvg(bidsPerAuc, auctionCount, numberOfBids);
		lastBidTime = Util.incrementalAvg(lastBidTime, auctionCount, timeUntilEnd);
		// if (won) { auctionsWon++; }
		auctionCount++;
	}

	public int id() {
		return userId;
	}

	public int auctionCount() {
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
		// return Math.log1p(pos) - Math.log1p(neg);
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
		return avgBid;
	}

	public double avgBidLn() {
		return Math.log(avgBid);
	}

	public double bidAvgOrdinal() {
		if (avgBid < 800)
			return 1;
		else if (avgBid < 2500)
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
		return ((double) auctionsWon) / auctionCount;
	}

	public int propWinOrdinal() {
		return discritiseEvenBins(3, propWin());
	}

	public double bidsPerAucLn() {
		return Math.log(bidsPerAuc);
	}

	public int avgBidPerAucOrdinal() {
		if (bidsPerAuc == 1)
			return 1;
		else if (bidsPerAuc <= 4)
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

//	public static String headings(FeaturesToUse ufs) {
//		StringBuilder sb = new StringBuilder();
//		if (ufs.userId0)
//			sb.append("0userId").append(delimiter);
//		if (ufs.auctionCount1)
//			sb.append("1AuctionCount").append(delimiter);
//		if (ufs.auctionCount1Ln)
//			sb.append("1lnAuctionCount").append(delimiter);
//		if (ufs.rep2)
//			sb.append("2Rep").append(delimiter);
//		if (ufs.rep2Ln)
//			sb.append("2lnRep").append(delimiter);
//		if (ufs.avgBid3)
//			sb.append("3AvgBidAmount").append(delimiter);
//		if (ufs.avgBid3Ln)
//			sb.append("3lnAvgBidAmount").append(delimiter);
//		if (ufs.bidInc4)
//			sb.append("4AvgBidInc").append(delimiter);
//		if (ufs.bidInc4Ln)
//			sb.append("4lnAvgBidInc").append(delimiter);
//		if (ufs.bidsPer6Auc)
//			sb.append("6BidsPerAuc").append(delimiter);
//		if (ufs.bidsPerAuc6Ln)
//			sb.append("6lnBidsPerAuc").append(delimiter);
//		if (ufs.firstBidTimes13)
//			sb.append("13FirstBidTimes").append(delimiter);
//		if (ufs.bidTimesUntilEnd9)
//			sb.append("9AvgBidTimesUntilEnd").append(delimiter);
//		if (ufs.propWin5)
//			sb.append("5PropWin").append(delimiter);
//		if (ufs.avgBidPropMax10)
//			sb.append("10AvgBidPropMax").append(delimiter);
//		if (ufs.avgBidProp11)
//			sb.append("11AvgBidProp").append(delimiter);
//		if (ufs.bidPeriod7) {
//			sb.append("7BidBeg").append(delimiter);
//			sb.append("7BidMid").append(delimiter);
//			sb.append("7BidEnd").append(delimiter);
//		}
//		if (ufs.bidMinsBeforeEnd12)
//			sb.append("12BidMinsBeforeEnd").append(delimiter);
//		if (ufs.aucPerCat8)
//			sb.append("8AuctionsPerCat").append(delimiter);
//		if (ufs.selfBidInterval14)
//			sb.append("14SelfBidInterval").append(delimiter);
//		if (ufs.anyBidInterval15)
//			sb.append("15AnyBidInterval").append(delimiter);
//
//		sb.deleteCharAt(sb.length() - delimiter.length());
//		return sb.toString();
//	}

//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//
//		FeaturesToUse featuresToPrint = this.fpWrapper.getFeaturesToUse();
//		if (featuresToPrint.userId0) {
//			sb.append(userId);
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.auctionCount1) {
//			sb.append(auctionCount());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.auctionCount1Ln) {
//			sb.append(numAuctionsBidInLn());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.rep2) {
//			if (pos == -1)
//				throw new RuntimeException("Rep not present for " + userId + ".");
//			sb.append(rep());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.rep2Ln) {
//			sb.append(repLn());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.avgBid3) {
//			sb.append(avgBid());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.avgBid3Ln) {
//			sb.append(avgBidLn());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidInc4) {
//			if (bidIncCount != 0) {
//				// sb.append(avgBidInc());
//				sb.append(avgBidIncMinusMinInc());
//			}
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidInc4Ln) {
//			if (bidIncCount != 0)
//				sb.append(avgBidIncMinusMinIncLn());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidsPer6Auc) {
//			sb.append(getBidsPerAuc());
//			// sb.append(avgBidPerAucOrdinal());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidsPerAuc6Ln) {
//			sb.append(bidsPerAucLn());
//			// sb.append(avgBidPerAucOrdinal());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.firstBidTimes13) {
//			sb.append(Math.log(firstBidTimes));
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidTimesUntilEnd9) {
//			sb.append(bidTimeBeforeEndAvg());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.propWin5) {
//			sb.append(propWin());
//			// sb.append(propWinOrdinal());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.avgBidPropMax10) {
//			sb.append(getAvgBidAmountComparedToMax());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidPeriod7) {
//			// sb.append(avgMinsToEnd());
//			// sb.append(delimiter);
//			sb.append(bidPropBeg()); // beg
//			sb.append(delimiter);
//			sb.append(bidPropMid()); // mid
//			sb.append(delimiter);
//			sb.append(bidPropEnd()); // end
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.avgBidProp11) {
//			sb.append(avgBidProp);
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.bidMinsBeforeEnd12) {
//			sb.append(bidMinsBeforeEnd());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.aucPerCat8) {
//			// sb.append(auctionsPerCat());
//			sb.append(auctionsPerCatLn());
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.selfBidInterval14) {
//			if (selfBidInterval != 0)
//				sb.append(Math.log1p(selfBidInterval));
//			// sb.append(selfBidInterval);
//			sb.append(delimiter);
//		}
//		if (featuresToPrint.anyBidInterval15) {
//			if (anyBidInterval != 0)
//				sb.append(Math.log1p(anyBidInterval));
//			// sb.append(anyBidInterval);
//			sb.append(delimiter);
//		}
//		sb.deleteCharAt(sb.length() - delimiter.length());
//		return sb.toString();
//	}

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
		return (double) auctionCount / categories.size();
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
		for (int i = 0; i < bidTimesFractionBeforeEnd.size(); i++) {
			result = Util.incrementalAvg(result, count, bidTimesFractionBeforeEnd.get(i));
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
			// result = Util.incrementalAvg(result, count, Math.log1p(bidMinsBeforeEnd.get(i)));
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
	 * Returns the bin number the value should be in. Value must be between [0-1).
	 */
	private int discritiseEvenBins(int numberOfBins, double value) {
		return (int) (value * numberOfBins) + 1;
	}

}