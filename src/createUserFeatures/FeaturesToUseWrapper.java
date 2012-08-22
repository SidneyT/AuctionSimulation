package createUserFeatures;

import java.util.Arrays;
import java.util.List;

public class FeaturesToUseWrapper {
	
	private FeaturesToUse featuresToUse;
	private String featuresToUseString;
	
	public FeaturesToUseWrapper(String featuresToUse) {
		if (featuresToUse == null)
			featuresToUse = "";
		this.featuresToUse = new FeaturesToUse(featuresToUse);
		this.featuresToUseString = featuresToUse;
	}
	
	public void setFeaturesToPrint(String featuresToUse) {
		this.featuresToUse = new FeaturesToUse(featuresToUse);
		this.featuresToUseString = featuresToUse;
	}
	
	public FeaturesToUse getFeaturesToUse() {
		return featuresToUse;
	}
	
	public String getFeaturesToUseString() {
		return featuresToUseString;
	}
	
	public static class FeaturesToUse {

		public final boolean
			userId0,
			auctionCount1,auctionCount1Ln, 
			rep2, rep2Ln,
			avgBid3, avgBid3Ln,
			bidInc4, bidInc4Ln,
			propWin5,
			bidsPer6Auc, bidsPerAuc6Ln,
			avgBidPropMax10,
			avgBidProp11,
			bidPeriod7,
			bidTimesUntilEnd9,
			bidMinsBeforeEnd12,
			aucPerCat8,
			firstBidTimes13,
			selfBidInterval14,
			anyBidInterval15
		;
		public FeaturesToUse(String features) {
			String featuresNoSpace = features.replaceAll(" ", "");
			List<String> splits = Arrays.asList(featuresNoSpace.split("-"));
			userId0 = splits.contains("0");
			auctionCount1 = splits.contains("1");
			auctionCount1Ln = splits.contains("1ln");
			rep2 = splits.contains("2");
			rep2Ln = splits.contains("2ln");
			avgBid3 = splits.contains("3");
			avgBid3Ln = splits.contains("3ln");
			avgBidPropMax10 = splits.contains("10");
			bidInc4 = splits.contains("4");
			bidInc4Ln = splits.contains("4ln");
			propWin5 = splits.contains("5");
			bidsPer6Auc = splits.contains("6");
			bidsPerAuc6Ln = splits.contains("6ln");
			avgBidProp11 = splits.contains("11");
			bidPeriod7 = splits.contains("7");
			bidTimesUntilEnd9 = splits.contains("9");
			bidMinsBeforeEnd12 = splits.contains("12");
			aucPerCat8 = splits.contains("8");
			firstBidTimes13 = splits.contains("13");
			selfBidInterval14 = splits.contains("14");
			anyBidInterval15 = splits.contains("15");
			//-3ln-10-6ln-11-5-12
		}

	}
}