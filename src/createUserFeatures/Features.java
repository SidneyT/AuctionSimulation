package createUserFeatures;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

/**
 * Per-user features of bidders 
 */
public enum Features {
	
	UserType0b {
		@Override
		public double value(UserFeatures uf) {
			throw new UnsupportedOperationException();
		}
	},
	/**
	 * User Id
	 */
	UserId0 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getUserId();
		}
	},
	/**
	 * Number of auction participations
	 */
	AuctionCount1  {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAuctionCount();
		}
	},
	/**
	 * Number of auction participations, ln-ed
	 */
	AuctionCount1Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAuctionCount());
		}
	},
	/**
	 * Positive reputation - negative reputation
	 */
	Rep2  {
		@Override
		public double value(UserFeatures uf) {
			return uf.getPos() - uf.getNeg();
		}
	},
	/**
	 * Positive reputation - negative reputation, ln-ed
	 */
	Rep2Ln {
		@Override
		public double value(UserFeatures uf) {
			double value = Rep2.value(uf);
			if (value < 0)
				return -FastMath.log1p(-value);
			else 
				return FastMath.log1p(value);
		}
	},
	/**
	 * Bid amount, averaged over all bids
	 */
	AvgBid3 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBid().getAverage();
		}
	},
	/**
	 * Bid amount, averaged over all bids, ln-ed
	 */
	AvgBid3Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid().getAverage());
		}
	},
	AvgBid3SD {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid().getSD());
		}
	},
	/**
	 * Bid increment, minus minimum required, averaged over all bids
	 */
	AvgBidIncMinusMinInc4 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidIncMinusMinInc();
		}
	},
	/**
	 * Bid increment, minus minimum required, averaged over all bids, ln-ed
	 */
	AvgBidIncMinusMinInc4Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAvgBidIncMinusMinInc());
		}
	},
	/**
	 * Proportion of auctions won
	 */
	PropWin5 {
		@Override
		public double value(UserFeatures uf) {
			return ((double) uf.getAuctionsWon())/uf.getAuctionCount();
		}
	},
	/**
	 * Number of bids in each auction, averaged over all auctions
	 */
	BidsPerAuc6 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidsPerAuc().getAverage();
		}
	},
	/**
	 * Number of bids in each auction, averaged over all auctions, ln-ed
	 */
	BidsPerAuc6Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getBidsPerAuc().getAverage());
		}
	},
	BidsPerAuc6SD {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getBidsPerAuc().getSD());
		}
	},
	/**
	 * Fraction of auction time elapsed when auction bids were made, averaged over all bids
	 */
	BidTimesElapsed9 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidTimesFractionToEnd().getAverage();
		}
	},
	BidTimesElapsed9SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidTimesFractionToEnd().getSD();
		}
	},
	/**
	 * Bid amount as a proportion of the final bid, averaged over all bids.
	 */
	AvgBidPropMax10 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidAmountComparedToMax().getAverage();
		}
	},
	AvgBidPropMax10SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidAmountComparedToMax().getSD();
		}
	},	/**
	 * Number of bids in auctions as a proportion of the total, averaged over all auctions.
	 */
	AvgBidProp11 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidProp().getAverage();
		}
	},
	AvgBidProp11SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidProp().getSD();
		}
	},
	/**
	 * Number of minutes before the end of the auction bids were made, averaged over all bids. 
	 * Similar to BidTimesUntilEnd9
	 */
	BidTimesMinsBeforeEnd12 { 
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidTimesMinsBeforeEnd().getAverage();
		}
	},
	BidTimesMinsBeforeEnd12SD { 
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidTimesMinsBeforeEnd().getSD();
		}
	},
	/**
	 * Minutes before the end of the auction the FIRST bid was made, averaged over all auctions, ln-ed.
	 */
	FirstBidTimes13 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getFirstBidTime().getAverage();
		}
	},
	FirstBidTimes13Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getFirstBidTime().getAverage());
		}
	},
	FirstBidTimes13SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getFirstBidTime().getSD();
		}
	},
	/**
	 * Amount of the last bid submitted as a proportion of the highest bid, averaged across all auctions. 
	 */
	AvgFinalBidAmountPropMax14 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgFinalBidComparedToMax().getAverage();
		}
	},
	AvgFinalBidAmountPropMax14SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgFinalBidComparedToMax().getSD();
		}
	},
	/**
	 * Proportion of evaluations that are positive.
	 */
	PositiveRepProportion15 {
		@Override
		public double value(UserFeatures uf) {
			return (double) uf.getPos() / (uf.getPos() + uf.getNeg());
		}
	},
	SelfBidInterval16 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getSelfBidInterval().getAverage();
		}
	},
	SelfBidInterval16SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getSelfBidInterval().getSD();
		}
	},
	AnyBidInterval17 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAnyBidInterval().getAverage();
		}
	},
	AnyBidInterval17SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAnyBidInterval().getSD();
		}
	},
	// not using because TM doesn't have this. But not sure.
//	BidAmountPropValuation18 {
//		@Override
//		public double value(UserFeatures uf) {
//			return uf.getBidAmountComparedToValuation().getAverage();
//		}
//	},
//	BidAmountPropValuation18SD {
//		@Override
//		public double value(UserFeatures uf) {
//			return uf.getBidAmountComparedToValuation().getSD();
//		}
//	}
	;
	
	public String label() {
		return this.name();
	}
	
	public abstract double value(UserFeatures uf);

	public static String labels(Collection<Features> features) {
		StringBuilder sb = new StringBuilder();
		for (Features feature : features)
			sb.append(feature.label()).append(",");
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public static String fileLabels(Collection<Features> features) {
		if (features == ALL_FEATURES)
			return "ALL_FEATURES";

		StringBuilder sb = new StringBuilder();
		for (Features feature : features)
			sb.append(feature.label().replaceAll("^[A-za-z]+", "")).append(",");
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	/**
	 * 
	 * Values for features where if <code>Double.isNaN()</code> is not printed.
	 * 
	 * @param features
	 * @param userFeatures
	 * @return
	 */
	public static String values(Collection<? extends Features> features, UserFeatures userFeatures) {
		StringBuilder sb = new StringBuilder();
		for (Features feature : features) {
			if (feature == UserType0b) {
				sb.append(userFeatures.userType);
			} else {
				double value = feature.value(userFeatures);
				if (!Double.isNaN(value))
					sb.append(value);
			}
			sb.append(",");
		}
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException();
	}
	
	public static final List<Features> DEFAULT_FEATURES = Arrays.<Features>asList(
			Features.UserId0,
			Features.AuctionCount1Ln, 
			Features.Rep2Ln,
			Features.AvgBid3Ln,
			Features.AvgBidIncMinusMinInc4Ln,
			Features.BidsPerAuc6Ln,
			Features.FirstBidTimes13,
			Features.BidTimesElapsed9,
			Features.PropWin5,
			Features.AvgBidPropMax10,
			Features.AvgBidProp11
		);
	
	public static final List<Features> ALL_FEATURES = Arrays.asList(Features.values());
}
