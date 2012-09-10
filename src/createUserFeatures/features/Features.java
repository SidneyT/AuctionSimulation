package createUserFeatures.features;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import util.IncrementalMean;

import createUserFeatures.UserFeatures;

/**
 * Per-user features of bidders 
 */
public enum Features implements Feature {
	
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
			return uf.getAvgBid();
		}
	},
	/**
	 * Bid amount, averaged over all bids, ln-ed
	 */
	AvgBid3Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid());
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
	/**
	 * Fraction of auction time elapsed when auction bids were made, averaged over all bids
	 */
	BidTimesElapsed9 {
		@Override
		public double value(UserFeatures uf) {
			IncrementalMean ia = new IncrementalMean();
			for (double mins : uf.getBidTimesFractionToEnd()) {
				ia.addNext(mins);
			}
			return ia.getAverage();
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
	/**
	 * Number of bids in auctions as a proportion of the total, averaged over all auctions.
	 */
	AvgBidProp11 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidProp().getAverage();
		}
	},
	/**
	 * Number of minutes before the end of the auction bids were made, averaged over all bids. 
	 * Similar to BidTimesUntilEnd9
	 */
	BidTimesMinsBeforeEnd12 { 
		@Override
		public double value(UserFeatures uf) {
			IncrementalMean ia = new IncrementalMean();
			for (long mins : uf.getBidTimesMinsBeforeEnd()) {
				ia.addNext(mins);
			}
			return Math.log(ia.getAverage());
		}
	},
	/**
	 * Minutes before the end of the auction the FIRST bid was made, averaged over all auctions, ln-ed.
	 */
	FirstBidTimes13 {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getFirstBidTime().getAverage());
		}
	},
	/**
	 * Amount of the last bid submitted as a proportion of the highest bid, averaged across all auctions. 
	 */
	AvgFinalBidAmountPropMax14 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgFinalBidComparedToMax();
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
	;
	
	public String label() {
		return this.name();
	}
	
	public static String labels(Collection<Feature> features) {
		StringBuilder sb = new StringBuilder();
		for (Feature feature : features)
			sb.append(feature.label()).append(",");
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public static String fileLabels(Collection<Feature> features) {
		StringBuilder sb = new StringBuilder();
		for (Feature feature : features)
			sb.append(feature.label().replaceAll("^[A-za-z]+", "")).append(",");
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	/**
	 * 
	 * Values for features where Double.isNaN() == true is not printed.
	 * 
	 * @param features
	 * @param userFeatures
	 * @return
	 */
	public static String values(Collection<Feature> features, UserFeatures userFeatures) {
		StringBuilder sb = new StringBuilder();
		for (Feature feature : features) {
			double value = feature.value(userFeatures);
			if (!Double.isNaN(value))
				sb.append(value);
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
	
	public static final List<Feature> defaultFeatures = Arrays.<Feature>asList(
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
}
