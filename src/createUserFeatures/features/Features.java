package createUserFeatures.features;

import org.apache.commons.math3.util.FastMath;

import util.Util;

import createUserFeatures.UserFeatures;

public enum Features implements Feature {
	AuctionCount1  {
		@Override
		public String label() {
			return "AuctionCount1";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAuctionCount();
		}
	},
	AuctionCount1Ln {
		@Override
		public String label() {
			return "AuctionCount1Ln";
		}

		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAuctionCount());
		}
	},
	Rep2  {
		@Override
		public String label() {
			return "Rep2";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getPos() - uf.getNeg();
		}
	},
	Rep2Ln {
		@Override
		public String label() {
			return "Rep2Ln";
		}
		
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
		public String label() {
			return "AvgBid3";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBid();
		}
	},
	AvgBid3Ln {
		@Override
		public String label() {
			return "AvgBid3LnLn";
		}
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getAvgBid());
		}
	},
	AvgBidIncMinusMinInc4 {
		@Override
		public String label() {
			return "AvgBidIncMinusMinInc4";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidIncMinusMinInc();
		}
	},
	AvgBidIncMinusMinInc4Ln {
		@Override
		public String label() {
			return "AvgBidMinusMinInc4Ln";
		}

		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAvgBidIncMinusMinInc());
		}
	},
	PropWin5 {
		@Override
		public String label() {
			return "PropWin5";
		}

		@Override
		public double value(UserFeatures uf) {
			return ((double) uf.getAuctionsWon())/uf.auctionCount();
		}
	},
	BidsPerAuc6 {
		@Override
		public String label() {
			return "BidsPerAuc6";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getBidsPerAuc();
		}
	},
	BidsPerAuc6Ln {
		@Override
		public String label() {
			return "BidsPerAuc6Ln";
		}

		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getBidsPerAuc());
		}
	},
	BidTimesUntilEnd9 {
		@Override
		public String label() {
			return "BidTimesUntilEnd9";
		}

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
	AvgBidPropMax10 {
		@Override
		public String label() {
			return "AvgBidPropMax10";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidAmountComparedToMax();
		}
	},
	AvgBidProp11 {
		@Override
		public String label() {
			return "AvgBidProp11";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidProp();
		}
	},
	FirstBidTimes13 {
		@Override
		public String label() {
			return "FirstBidTimes13";
		}

		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(uf.getFirstBidTimes());
		}
	},
}
