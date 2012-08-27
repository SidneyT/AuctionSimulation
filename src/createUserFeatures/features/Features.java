package createUserFeatures.features;

import java.util.Collection;

import org.apache.commons.math3.util.FastMath;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Util;

import createUserFeatures.UserFeatures;

public enum Features implements Feature {
	AuctionCount1  {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAuctionCount();
		}
	},
	AuctionCount1Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAuctionCount());
		}
	},
	Rep2  {
		@Override
		public double value(UserFeatures uf) {
			return uf.getPos() - uf.getNeg();
		}
	},
	Rep2Ln {
		@Override
		public double value(UserFeatures uf) {
			double value = Rep2.value(uf);
			if (value > 0)
				return -FastMath.log1p(-value);
			else 
				return FastMath.log1p(value);
		}
	},
	AvgBid3 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBid();
		}
	},
	AvgBid3Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid());
		}
	},
	AvgBidIncMinusMinInc4 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidIncMinusMinInc();
		}
	},
	AvgBidIncMinusMinInc4Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAvgBidIncMinusMinInc());
		}
	},
	PropWin5 {
		@Override
		public double value(UserFeatures uf) {
			return ((double) uf.getAuctionsWon())/uf.auctionCount();
		}
	},
	BidsPerAuc6 {
		@Override
		public double value(UserFeatures uf) {
			return uf.getBidsPerAuc();
		}
	},
	BidsPerAuc6Ln {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getBidsPerAuc());
		}
	},
	BidTimesUntilEnd9 { // average number of minutes before the end of the auction each bid was made 
		@Override
		public double value(UserFeatures uf) {
			double result = 0;
			int count = 0;
			for (int i = 0; i < uf.getBidTimesBeforeEnd().size(); i++) {
				result = Util.incrementalAvg(result, count, uf.getBidTimesBeforeEnd().get(i));
				count++;
			}
			return result;
		}
	},
	AvgBidPropMax10 { // average bid amount as a proportion of the final bid
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidAmountComparedToMax();
		}
	},
	AvgBidProp11 { // average number of bids in auctions as a proportion of total
		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidProp();
		}
	},
	FirstBidTimes13 {
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getFirstBidTimes());
		}
	},
	BidMinsBeforeEnd12 {
		@Override
		public double value(UserFeatures uf) {
			double result = 0;
			int count = 0;
			for (int i = 0; i < uf.getBidMinsBeforeEnd().size(); i++) {
				// result = Util.incrementalAvg(result, count, Math.log1p(bidMinsBeforeEnd.get(i)));
				result = Util.incrementalAvg(result, count, uf.getBidMinsBeforeEnd().get(i));
				count++;
			}
			return Math.log(result);
		}
	},
	BidTimesUntilEnd16 { // average number of minutes before the end of the auction each bid was made 
		@Override
		public double value(UserFeatures uf) {
			double result = 0;
			int count = 0;
			for (int i = 0; i < uf.getBidTimesBeforeEnd().size(); i++) {
				result = Util.incrementalAvg(result, count, uf.getBidTimesBeforeEnd().get(i));
				count++;
			}
			return result;
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
	
	public static String values(Collection<Feature> features, UserFeatures userFeatures) {
		StringBuilder sb = new StringBuilder();
		for (Feature feature : features)
			sb.append(feature.value(userFeatures)).append(",");
		if (!features.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException();
	}
}
