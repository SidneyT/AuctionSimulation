package dataAnalysis;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.AtomicDouble;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.TMAuction;

import simulator.database.DBConnection;

public class FrequencyByPrice {
	public static void main(String[] args) {
		new FrequencyByPrice().countAuctionFrequenciesByPrice();
	}
	
	/**
	 * Counts the number of auctions sold at every price
	 */
	public void countAuctionFrequenciesByPrice() {
		ArrayListMultimap<Integer, Integer> amountsById = ArrayListMultimap.create();
		
		Connection conn = DBConnection.getTrademeConnection();
		Iterator<Pair<TMAuction, List<BidObject>>> iterator = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY).iterator();
		
		while(iterator.hasNext()) {
			Pair<TMAuction, List<BidObject>> pair = iterator.next();
			
			HashMap<Integer, Integer> highestPrice = new HashMap<>();
			for (BidObject bidObject : pair.getValue()) {
				highestPrice.put(bidObject.bidderId, bidObject.amount);
			}
			
			for (Integer bidderId : highestPrice.keySet()) {
				amountsById.put(bidderId, highestPrice.get(bidderId));
			}
		}
		
		EnumMap<Price, AtomicInteger> binnedAmounts = new EnumMap<Price, AtomicInteger>(Price.class);
		EnumMap<Price, AtomicDouble> binnedAmounts2 = new EnumMap<Price, AtomicDouble>(Price.class);
		EnumMap<Price, AtomicDouble> binnedAmounts3 = new EnumMap<Price, AtomicDouble>(Price.class);
		for (Price keys : Price.values()) { // fill up the map
			binnedAmounts.put(keys, new AtomicInteger());
			binnedAmounts2.put(keys, new AtomicDouble());
			binnedAmounts3.put(keys, new AtomicDouble());
		}
		
		for (Collection<Integer> list : amountsById.asMap().values()) {
			for (int amount : list) {
				binnedAmounts.get(Price.binPrices(amount)).incrementAndGet();
				binnedAmounts2.get(Price.binPrices(amount)).addAndGet(1.0/list.size());
				binnedAmounts3.get(Price.binPrices(amount)).addAndGet(FastMath.pow(1.0/list.size(), 2));
			}
		}
		System.out.println(binnedAmounts);
		System.out.println(binnedAmounts2);
		System.out.println(binnedAmounts3);
		
		
	}
	
	private enum Price {
		t1(100),
		t10(1000),
		t20(2000),
		t50(5000),
		t100(10000),
		REST(Integer.MAX_VALUE);
		
		public final int threshold;
		Price(int threshold) {
			this.threshold = threshold;
		}

		private static Price binPrices(int amount) {
			for(Price price : Price.values()) {
				if (amount <= price.threshold)
					return price;
			}
			throw new RuntimeException("Should not reach here.");
		}
	}
	
}
