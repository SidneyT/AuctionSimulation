package util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Util {
	/**
	 * Finds the new average after including the new value
	 */
	public static double incrementalAvg(double currAvg, int currNumElements, int newValue) {
		return currAvg + (newValue - currAvg)/(currNumElements + 1);
	}
	public static double incrementalAvg(double currAvg, int currNumElements, double newValue) {
		return currAvg + (newValue - currAvg)/(currNumElements + 1);
	}
	
	/**
	 * Averages 2 averages. Takes into account the number of elements in each average.
	 */
	public static double incrementalAvg(double avg1, int numElements1, double avg2, int numElements2) {
		return avg1 + (avg2 - avg1)*numElements2/(numElements1 + numElements2);
	}
	
	/**
	 * @param currBid the current bid
	 * @return minimum bid increment as specified by TradeMe in cents
	 */
	public static int minIncrement(long currBid) {
		if (currBid >= 0 && currBid < 2000) 
			return 50;
		else if (currBid >= 2000 && currBid < 20000)
			return 100;
		else if (currBid >= 20000 && currBid < 100000)
			return 500;
		else if (currBid >= 100000 && currBid < 500000)
			return 1000;
		else if (currBid >= 500000 && currBid < 2500000)
			return 5000;
		else // if (currBid >= 2500000)
			return 10000;
	}
	
	/**
	 * http://www.trademe.co.nz/help/18/fees-and-fee-calculator
	 * @param highestBid
	 * @return fee charged by Trade Me for successful sale
	 */
	public static int successFee(long highestBid) {
		if (highestBid < 667)
			return 50;
		else if (highestBid <= 20000)
			return (int) (0.075 * highestBid + 0.5);
		else if (highestBid <= 150000)
			return 1500 + (int) (0.045 * (highestBid - 20000) + 0.5);
		else if (highestBid <= 397368)
			return 7350 + (int) (0.019 * (highestBid - 150000) + 0.5);
		else
			return 14900;
	}

	/**
	 * d1 - d2
	 */
	public static long timeDiffInMin(Date d1, Date d2) {
//		System.out.println(d1.getTime() + ", " + d2.getTime());
//		System.out.println("result: " + (d1.getTime() - d2.getTime() + 30000)/60000);
		return (d1.getTime() - d2.getTime() + 30000)/60000;
	}
	
	/**
	 * Add an item into a set, where the set is a value in a map.
	 * If there is no mapping for the key, creates a new set and
	 * adds the item to it.
	 * 
	 * Not thread safe.
	 */
	public static <K, T> boolean mapSetAdd(Map<K, Set<T>> map, K k, T v) {
		Set<T> set;
		if (!map.containsKey(k)) {
			set = new HashSet<T>();
			map.put(k, set);
		} else {
			set = map.get(k);
		}
		return set.add(v);
	}
	public static <K, T> boolean mapListAdd(Map<K, List<T>> map, K k, T v) {
		List<T> list;
		if (!map.containsKey(k)) {
			list = new ArrayList<T>();
			map.put(k, list);
		} else {
			list = map.get(k);
		}
		return list.add(v);
	}
	
	/**
	 * Sigmoid curve.
	 * 0.7 returns 0.9933; 1 returns 0.880797; 1.2 returns 0.5; 1.5 returns 0.047 
	 */
	public static double sigmoid(double x) {
		return 1 / (1 + Math.exp((x - 1.2) * 10));
	}
	
	/**
	 * Random int; min inclusive, max exclusive.
	 * @param random
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randomInt(double random, int min, int max) {
		return (int) (random * (max - min) + min);
	}
	
	public static double normalise(double value, double min, double max) {
		return (value - min)/(max - min);
	}
	
	public static double bayseanAverage(double c, double mean, int weight, double value) {
		double average = (c * mean + weight * value)/(c + weight);
		return average;
	}
	
	public static <T> List<T> getSample(Iterator<T> it, int sampleSize) {
		List<T> sample = new ArrayList<>(sampleSize);
		int seen = 0;
		Random r = new Random();
		while(it.hasNext()) {
			T item = it.next();
			seen++;
			if (seen <= sampleSize) {
				sample.add(item);
			} else if (r.nextInt(seen) < sampleSize) { // element kept with p=1/seen
				// remove a random element
				sample.set(r.nextInt(sampleSize), item);
			}
		}
		return sample;
	}

}
