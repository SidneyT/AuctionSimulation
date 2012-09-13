package createUserFeatures;

import java.util.HashSet;
import java.util.Set;


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
	String userType;

	int bidCount = 0; // number of bids made

	int bidIncCount; // number of bidIncrements made (does not count as incrementing a bid if user is first to bid)

	IncrementalSD avgBidInc = new IncrementalSD();
//	double avgBidInc; // average bid increment
	double avgBidIncMinusMinInc = Double.NaN; // average bid increment minus minimum increment; initialise to NaN to know when it has not been used

	int auctionCount; // number of auctions as a bidder
	int auctionsWon; // number of auctions won
	private final IncrementalSD avgBid = new IncrementalSD(); // average value of bids
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

	public IncrementalSD getAvgBid() {
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

	public boolean isComplete() {
		return pos != -1; // && neg != -1
	}

	public double numAuctionsBidInLn() {
		return Math.log(auctionCount);
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

	public IncrementalSD getBidTimesMinsBeforeEnd() {
		return bidTimesMinsBeforeEnd;
	}

	public IncrementalSD getAvgFinalBidComparedToMax() {
		return avgFinalBidComparedToMax;
	}

}