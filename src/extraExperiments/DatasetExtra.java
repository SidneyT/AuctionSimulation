package extraExperiments;

import java.sql.Connection;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import simulator.database.DBConnection;
import util.Util;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Doubles;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.TMAuction;
import createUserFeatures.Features;
import createUserFeatures.UserFeatures;

public class DatasetExtra {
	public static void main(String[] args) {
		compare();
	}

	private static void compare() {
		ListMultimap<Features, Double> small = datasetCompare("trademe_small");
		ListMultimap<Features, Double> big = datasetCompare("trademe");
		
		EnumMap<Features, Double> maxs = new EnumMap<>(Features.class);
		EnumMap<Features, Double> mins = new EnumMap<>(Features.class);
		for (Features f : small.keySet()) {
			double max = small.get(f).stream().filter(val -> !Double.isNaN(val)).max(Double::compare).get();
			double min = small.get(f).stream().filter(val -> !Double.isNaN(val)).max(Double::compare).get();
			maxs.put(f, max);
			mins.put(f, min);
		}
		for (Features f : big.keySet()) {
			double max = small.get(f).stream().filter(val -> !Double.isNaN(val)).max(Double::compare).get();
			double min = small.get(f).stream().filter(val -> !Double.isNaN(val)).min(Double::compare).get();
			if (maxs.get(f) < max)
				maxs.put(f, max);
			if (mins.get(f) > min)
				mins.put(f, min);
		}
		
		System.out.println(maxs);
		System.out.println(mins);
		
		EnumMap<Features, int[]> smallCount = new EnumMap<>(Features.class);
		for (Features f : small.keySet()) {
			for (Double val : small.get(f)) {
				if (smallCount.get(f) == null) {
					smallCount.put(f, new int[20]);
				}
				smallCount.get(f)[bin(maxs.get(f), mins.get(f), val)]++;
			}
		}
		EnumMap<Features, int[]> bigCount = new EnumMap<>(Features.class);
		for (Features f : big.keySet()) {
			for (Double val : big.get(f)) {
				if (bigCount.get(f) == null) {
					bigCount.put(f, new int[20]);
				}
				bigCount.get(f)[bin(maxs.get(f), mins.get(f), val)]++;
			}
		}
		
		for (Features f : smallCount.keySet()) {
			System.out.println(f + ":" + Arrays.toString(smallCount.get(f)));
		}
		for (Features f : bigCount.keySet()) {
			System.out.println(f + ":" + Arrays.toString(bigCount.get(f)));
		}
	}
	
	static int bin(double max, double min, double val) {
		return FastMath.min((int) ((val - min) / (max - min) * 20), 19);
	}
	
	private static ListMultimap<Features, Double> datasetCompare(String dbName) {
		Connection conn = DBConnection.getConnection(dbName);
		
		BuildTMFeatures builder = new BuildTMFeatures();
		Map<Integer, UserFeatures> userFeatures = builder.constructUserFeatures(dbName, BuildTMFeatures.DEFAULT_QUERY);
		
		ListMultimap<Features, Double> featureValues = Multimaps.newListMultimap(new EnumMap<>(Features.class), () -> Lists.newArrayList());
		for (UserFeatures uf : userFeatures.values()) {
			for (Features f : Features.DEFAULT_FEATURES) {
				double val = f.value(uf);
				featureValues.put(f, val);
			}
		}
		
		return featureValues;
	}

	
}
