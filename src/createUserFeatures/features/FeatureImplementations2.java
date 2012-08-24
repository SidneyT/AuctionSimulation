package createUserFeatures.features;

import java.util.ArrayList;

import org.apache.commons.math3.util.FastMath;

import createUserFeatures.UserFeatures;

public class FeatureImplementations2 {
	public static void main(String[] args) {
		for (Features enumThing : new ArrayList<Features>()) {
		}
	}
	public class AuctionCount1 implements Feature {
		@Override
		public String label() {
			return "AuctionCount1";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAuctionCount();
		}
	}
	
	public class AuctionCount1Ln extends AuctionCount1 {
		@Override
		public String label() {
			return super.label() + "Ln";
		}

		@Override
		public double value(UserFeatures uf) {
			return FastMath.log1p(uf.getAuctionCount());
		}
	}
	
	public class Rep2 implements Feature {
		@Override
		public String label() {
			return "Rep2";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getPos() - uf.getNeg();
		}
	}
	
	public class Rep2Ln extends Rep2 {
		@Override
		public String label() {
			return super.label() + "Ln";
		}
		
		@Override
		public double value(UserFeatures uf) {
			double value = super.value(uf);
			if (value > 0)
				return -FastMath.log1p(-value);
			else 
				return FastMath.log1p(value);
		}
	}
	
	public class AvgBid3 implements Feature {
		@Override
		public String label() {
			return "AvgBid3";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBid();
		}
	}
	
	public class AvgBid3Ln extends AvgBid3 {
		@Override
		public String label() {
			return super.label() + "Ln";
		}
		@Override
		public double value(UserFeatures uf) {
			return FastMath.log(super.value(uf));
		}
	}
	
	public class AvgBidIncMinusMinInc4 implements Feature {
		@Override
		public String label() {
			return "AvgBidIncMinusMinInc4";
		}

		@Override
		public double value(UserFeatures uf) {
			return uf.getAvgBidIncMinusMinInc();
		}
	}
	

}
