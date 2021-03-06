package createUserFeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import com.google.common.collect.ImmutableList;

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
			return FastMath.log(uf.getAuctionCount());
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
			return uf.getAvgBid().average();
		}
	},
	/**
	 * Bid amount, averaged over all bids, ln-ed
	 */
	AvgBid3Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid().average());
		}
	},
	AvgBid3SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBid().getSD();
		}
	},
	/**
	 * Bid increment, minus minimum required, averaged over all bids
	 */
	AvgBidIncMinusMinInc4 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidIncMinusMinInc().average();
		}
	},
	/**
	 * Bid increment, minus minimum required, averaged over all bids, ln-ed
	 */
	AvgBidIncMinusMinInc4Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAvgBidIncMinusMinInc().average());
		}
	},
	AvgBidIncMinusMinInc4SD {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAvgBidIncMinusMinInc().getSD());
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
			return uf.getBidsPerAuc().average();
		}
	},
	/**
	 * Number of bids in each auction, averaged over all auctions, ln-ed
	 */
	BidsPerAuc6Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getBidsPerAuc().average());
		}
	},
	BidsPerAuc6SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidsPerAuc().getSD();
		}
	},
	/**
	 * Fraction of auction time elapsed when auction bids were made, averaged over all bids
	 */
	BidTimesElapsed9 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidTimesFractionToEnd().average();
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
			return uf.getAvgBidAmountComparedToMax().average();
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
			return uf.getAvgBidProp().average();
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
			return uf.getBidTimesMinsBeforeEnd().average();
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
			return uf.getFirstBidTime().average();
		}
	},
	FirstBidTimes13Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getFirstBidTime().average());
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
			return uf.getAvgFinalBidComparedToMax().average();
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
	/**
	 * Average interval, in minutes, between bids made by the SAME USER
	 */
	SelfBidInterval16 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getSelfBidInterval().average();
		}
	},
	SelfBidInterval16SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getSelfBidInterval().getSD();
		}
	},
	/**
	 * Average interval, in minutes, between bids made by this user and the previous bid
	 */
	AnyBidInterval17 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAnyBidInterval().average();
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
//	},
	/**
	 * Final bid amount for this user, averaged across all auctions.
	 */
	AvgLastBidAmount19 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgFinalBidAmount().average();
		}
	},
	AvgLastBidAmount19Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgFinalBidAmount().average());
		}
	},
	AvgFinalBidAmount19SD {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgFinalBidAmount().getSD();
		}
	},

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
		if (features == ALL_FEATURES_MINUS_TYPE)
			return "ALL_FEATURES_MINUS_TYPE";
		if (features == DEFAULT_FEATURES_NOID)
			return "DEFAULT_FEATURES_NOID";
		if (features == MANY_FEATURES)
			return "MANY_FEATURES";

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
	
//	@Override
//	public String toString() {
//		throw new UnsupportedOperationException();
//	}
	
	public static final List<Features> CLUSTERING_FEATURES = Arrays.<Features>asList(
			Features.PropWin5,
			Features.BidsPerAuc6Ln,
			Features.AvgBidPropMax10,
			Features.AvgBidProp11
			);
	public static final List<Features> DEFAULT_FEATURES_NOID = Arrays.<Features>asList(
			Features.AvgBid3Ln,
			Features.AvgBidIncMinusMinInc4Ln,
			Features.PropWin5,
			Features.BidsPerAuc6Ln,
			Features.BidTimesElapsed9,
			Features.AvgBidPropMax10,
			Features.AvgBidProp11,
			Features.AuctionCount1Ln, 
			Features.Rep2Ln,
			Features.FirstBidTimes13Ln
			);
//	public static final List<Features> MANY_FEATURES = Arrays.<Features>asList(
//			Features.AvgBid3Ln,
//			Features.AvgBidIncMinusMinInc4Ln,
//			Features.PropWin5,
//			Features.BidsPerAuc6Ln,
//			Features.BidTimesElapsed9,
//			Features.AvgBidPropMax10,
//			Features.AvgBidProp11,
//			Features.AuctionCount1Ln, 
//			Features.Rep2Ln,
//			Features.FirstBidTimes13Ln,
//			Features.AvgBid3SD, // phi1sd
//			Features.AvgBidIncMinusMinInc4SD,
//			Features.BidsPerAuc6SD,
//			Features.BidTimesElapsed9SD,
//			Features.AvgBidPropMax10SD,
//			Features.AvgBidProp11SD,
//			Features.SelfBidInterval16, // phi12
//			Features.SelfBidInterval16SD,
//			Features.AnyBidInterval17,
//			Features.AnyBidInterval17SD,
//			Features.BidTimesMinsBeforeEnd12, // phi14
//			Features.BidTimesMinsBeforeEnd12SD,
//			Features.AvgLastBidAmount19Ln, // phi15
//			Features.AvgFinalBidAmount19SD,
//			Features.AvgFinalBidAmountPropMax14, //phi16
//			Features.AvgFinalBidAmountPropMax14SD
//			);
	public static final List<Features> MANY_FEATURES = Arrays.<Features>asList(
			AvgBid3Ln, // 1
			AvgBid3SD, // 1sd
			AvgBidIncMinusMinInc4Ln, // 2
			AvgBidIncMinusMinInc4SD, // 2sd
			PropWin5, // 3
			BidsPerAuc6Ln, // 4
			BidsPerAuc6SD, // 4sd
			BidTimesElapsed9, // 5
			BidTimesElapsed9SD, // 5sd
			AvgBidPropMax10, // 6
			AvgBidPropMax10SD, // 6sd
			AvgBidProp11, // 7 
			AvgBidProp11SD, // 7sd
			AuctionCount1Ln, // 8
			Rep2Ln, // 9
			FirstBidTimes13Ln, // 10
			FirstBidTimes13SD, // 10sd
			SelfBidInterval16, // 12
			SelfBidInterval16SD, // 12sd
			AnyBidInterval17, // 13
			AnyBidInterval17SD, // 13sd
			BidTimesMinsBeforeEnd12, // 14
			BidTimesMinsBeforeEnd12SD, // 14sd
			AvgLastBidAmount19Ln, // 15
			AvgFinalBidAmount19SD, // 15sd
			AvgFinalBidAmountPropMax14, // 16
			AvgFinalBidAmountPropMax14SD // 16sd
			);
	
	public static final List<Features> DEFAULT_FEATURES = Arrays.<Features>asList(
			Features.UserId0,
			Features.AuctionCount1Ln, 
			Features.Rep2Ln,
			Features.AvgBid3Ln,
			Features.AvgBidIncMinusMinInc4Ln,
			Features.BidsPerAuc6Ln,
			Features.FirstBidTimes13Ln,
			Features.BidTimesElapsed9,
			Features.PropWin5,
			Features.AvgBidPropMax10,
			Features.AvgBidProp11
			);
	
	public static final List<Features> ALL_FEATURES = Arrays.asList(Features.values());
	public static final List<Features> ALL_FEATURES_MINUS_TYPE;
	static {
		List<Features> allFeaturesMinusType = new ArrayList<>(Arrays.asList(Features.values()));
		allFeaturesMinusType.remove(UserType0b);
		ALL_FEATURES_MINUS_TYPE = ImmutableList.copyOf(allFeaturesMinusType);
	}
	public static final List<Features> FEATURES_FOR_DT = Arrays.<Features>asList(
			Features.UserType0b,
//			Features.AuctionCount1Ln,
			Features.AvgBid3Ln,
			Features.AvgBidIncMinusMinInc4Ln,
			Features.PropWin5,
			Features.BidsPerAuc6Ln,
			Features.BidTimesElapsed9,
			Features.AvgBidPropMax10,
			Features.AvgBidProp11,
			Features.BidTimesMinsBeforeEnd12,
			Features.AvgFinalBidAmountPropMax14,
			Features.AvgLastBidAmount19Ln
		);
}
