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
			printUserId,
			printNumAuctionsBidIn,printNumAuctionsBidInLn, 
			printRep, printRepLn,
			printAvgBid, printAvgBidLn,
			printBidInc, printBidIncLn,
			printPropWin,
			printBidsPerAuc, printBidsPerAucLn,
			printAvgBidPropMax,
			printAvgBidProp,
			printBidPeriod,
			printBidPeriodAlt,
			printBidMinsBeforeEnd,
			printAucPerCat,
			printFirstBidTimes,
			printSelfBidInterval,
			printAnyBidInterval
		;
		public FeaturesToUse(String features) {
			String featuresNoSpace = features.replaceAll(" ", "");
			List<String> splits = Arrays.asList(featuresNoSpace.split("-"));
			printUserId = splits.contains("0");
			printNumAuctionsBidIn = splits.contains("1");
			printNumAuctionsBidInLn = splits.contains("1ln");
			printRep = splits.contains("2");
			printRepLn = splits.contains("2ln");
			printAvgBid = splits.contains("3");
			printAvgBidLn = splits.contains("3ln");
			printAvgBidPropMax = splits.contains("10");
			printBidInc = splits.contains("4");
			printBidIncLn = splits.contains("4ln");
			printPropWin = splits.contains("5");
			printBidsPerAuc = splits.contains("6");
			printBidsPerAucLn = splits.contains("6ln");
			printAvgBidProp = splits.contains("11");
			printBidPeriod = splits.contains("7");
			printBidPeriodAlt = splits.contains("9");
			printBidMinsBeforeEnd = splits.contains("12");
			printAucPerCat = splits.contains("8");
			printFirstBidTimes = splits.contains("13");
			printSelfBidInterval = splits.contains("14");
			printAnyBidInterval = splits.contains("15");
			//-3ln-10-6ln-11-5-12
		}

	}
}