package createUserFeatures;

import java.util.HashSet;
import java.util.Set;

import createUserFeatures.features.Features;

import util.IncrementalMean;
import util.IncrementalSD;

/**
 * Object storing information about a user's bidding information. Contains methods to convert that information into
 * features for clustering
 * 
 * @author SidTDesktop
 * 
 */
public class UserFeatures {
	int userId;
	int pos, neg; // reputation

	int bidCount = 0; // number of bids made
	double avgBid; // average value of bids

	int bidIncCount; // number of bidIncrements made (does not count as incrementing a bid if user is first to bid)

	IncrementalSD avgBidInc = new IncrementalSD();
//	double avgBidInc; // average bid increment
	double avgBidIncMinusMinInc = Double.NaN; // average bid increment minus minimum increment; initialise to NaN to know when it has not been used

	int auctionCount; // number of auctions as a bidder
	int auctionsWon; // number of auctions won
	private final IncrementalSD bidsPerAuc = new IncrementalSD(); // average number of bids made in all auctions
	private final IncrementalSD firstBidTime = new IncrementalSD(); // average number of minutes from the end of the auction the FIRST bid in the auction by this user was made
	private final IncrementalSD lastBidTime = new IncrementalSD(); // average number of minutes from the end of the auction the LAST bid in the auction by this user was made
	private final IncrementalSD selfBidInterval = new IncrementalSD(); // average bid interval
	private final IncrementalSD anyBidInterval = new IncrementalSD(); // average bid interval
	private final IncrementalSD bidTimesFractionToEnd = new IncrementalSD(); // bid time as fraction of auction time elapsed
	private final IncrementalSD bidTimesMinsBeforeEnd = new IncrementalSD();// number of minutes before the end of the auction

	final double[] bidPeriods;
	final double[] bidPeriodsLogBins;
	double avgNumCategory; // number of categories per auction the user is in
	Set<String> categories;

	private final IncrementalSD avgBidAmountComparedToMax = new IncrementalSD(); // average of the bid amounts as fractions of the maximum bid in the same auction
	private final IncrementalSD avgFinalBidComparedToMax = new IncrementalSD(); // average of the last bid as fraction of the maximum
	private final IncrementalSD avgBidProp = new IncrementalSD();
	
	public UserFeatures() {
		this.bidPeriods = new double[4];
		this.bidPeriodsLogBins = new double[11];
		this.categories = new HashSet<>();

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

	/**
	 * @return Double.NaN if no value for this user exists (i.e., never bid after another bidder).
	 */
	public double getAvgBidIncMinusMinInc() {
		return avgBidIncMinusMinInc;
	}

	public IncrementalSD getBidsPerAuc() {
		return bidsPerAuc;
	}

	public IncrementalSD getLastBidTime() {
		return lastBidTime;
	}

	public double[] getBidPeriods() {
		return bidPeriods;
	}

	public double[] getBidPeriodsLogBins() {
		return bidPeriodsLogBins;
	}

	public IncrementalSD getFirstBidTime() {
		return firstBidTime;
	}

	public IncrementalSD getSelfBidInterval() {
		return selfBidInterval;
	}

	public IncrementalSD getAnyBidInterval() {
		return anyBidInterval;
	}

	public IncrementalSD getBidTimesFractionToEnd() {
		return bidTimesFractionToEnd;
	}

	public double getAvgNumCategory() {
		return avgNumCategory;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public IncrementalSD getAvgBidAmountComparedToMax() {
		return avgBidAmountComparedToMax;
	}

	public IncrementalSD getAvgBidProp() {
		return avgBidProp;
	}

	public static double getBidinthreshold() {
		return bidInThreshold;
	}

	public boolean isComplete() {
		return pos != -1; // && neg != -1
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

	public IncrementalMean getAvgBidInc() {
		assert avgBidInc.getAverage() >= 0;
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
		return discritiseEvenBins(3, Features.PropWin5.value(this));
	}

	public int avgBidPerAucOrdinal() {
		if (bidsPerAuc.getAverage() == 1)
			return 1;
		else if (bidsPerAuc.getAverage() <= 4)
			return 2;
		else
			return 3;
	}

	public boolean hasBids() {
		return bidCount != 0 && auctionCount != 0;
	}

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

	public IncrementalSD getBidTimesMinsBeforeEnd() {
		return bidTimesMinsBeforeEnd;
	}

	public double bidPropMid() {
		return bidPeriods[1];
	}

	public double bidPropEnd() {
		return bidPeriods[2];
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

	public IncrementalSD getAvgFinalBidComparedToMax() {
		return avgFinalBidComparedToMax;
	}

}