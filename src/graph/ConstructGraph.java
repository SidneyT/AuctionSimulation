package graph;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.HashMultiset;

import simulator.database.DBConnection;
import temporary.Chart;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionGroupIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.TMAuction;

public class ConstructGraph {
	public static void main(String[] args) {
		run();
	}
	
	enum TMIterator {
		INSTANCE;
		
		private final TMAuctionGroupIterator tmagi;
		TMIterator() {
			Connection conn = DBConnection.getTrademeConnection();
			tmagi = new TMAuctionGroupIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		}
		
		Iterator<Pair<TMAuction, List<BidObject>>> getIterator() {
			return tmagi.getIterator();
		}
	}
	
//	private static Iterator<Pair<TMAuction, List<BidObject>>> getAuctionIterator() {
//		TMAuctionGroupIterator auctionGroupIteratorCreator = new TMAuctionGroupIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
//		Iterator<Pair<TMAuction, List<BidObject>>> auctionIterator = auctionGroupIteratorCreator.iterator();
//		return auctionIterator;
//	}

	private static void run() {

		HashMap<Integer, HashMultiset<Integer>> bidsInEdges = constructAdjacencyList(new SellerBidderUniqueTuples());
//		charting(adjacencyList1);
		HashMap<Integer, HashMultiset<Integer>> winFromEdges = constructAdjacencyList(new WinFromTuples());
//		charting(adjacencyList2);
		HashMap<Integer, HashMultiset<Integer>> adjacencyList3 = constructAdjacencyList(new LossFromTuples());
//		charting(adjacencyList3);
		
		List<int[]> xVsY = xVsY(winFromEdges, bidsInEdges);
		
		Chart chart = new Chart();
		chart.addSeries2(xVsY, "xVsY");
		chart.build();

		Chart chart2 = new Chart();
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		logBins.addValues(xVsY);
		logBins.addValue(1.0, 1.0);
		ArrayList<Double> binStarts = logBins.getBins();
		ArrayList<Double> medians = logBins.binMeans();
		
		chart2.start("binned");
//		chart2.addPoint(1,1);
		for (int i = 0; i < medians.size(); i++) {
			double median = medians.get(i);
			if (!Double.isNaN(median))
				chart2.addPoint(binStarts.get(i), medians.get(i) + 1);
		}
		chart2.done();
		chart2.build();
	}
	
	private static List<int[]> xVsY(HashMap<Integer, HashMultiset<Integer>> adjacencyList1, HashMap<Integer, HashMultiset<Integer>> adjacencyList2) {
		List<int[]> xyPairs = new ArrayList<>();
		
		Set<Integer> allIds = new HashSet<Integer>(adjacencyList1.keySet());
		allIds.addAll(adjacencyList2.keySet());
		
		for (Integer id : allIds) {
			int x = 0;
			if (adjacencyList1.containsKey(id)) {
				x = adjacencyList1.get(id).size();
			}
			
			int y = 0;
			if (adjacencyList2.containsKey(id)) {
				y = adjacencyList2.get(id).size();
			}
			
//			if (x == 1 && y > 40)
//				System.out.println("pause");
			
			if (x > 0 && y > 0)
				xyPairs.add(new int[]{x, y});
			
		}
		
		return xyPairs;
	}
	
	private static HashMap<Integer, HashMultiset<Integer>> constructAdjacencyList(EdgeExtractor edgeExtractor) {
//		Iterator<Pair<TMAuction, List<BidObject>>> auctionIterator = getAuctionIterator();
		Iterator<Pair<TMAuction, List<BidObject>>> auctionIterator = TMIterator.INSTANCE.getIterator();
		
		ArrayList<int[]> allTuples = new ArrayList<>();
		while(auctionIterator.hasNext()) {
			Pair<TMAuction, List<BidObject>> pair = auctionIterator.next();
			
			TMAuction auction = pair.getKey();
			List<BidObject> bids = pair.getValue();
			
			List<int[]> tuples = edgeExtractor.getTuples(auction, bids);
			
			allTuples.addAll(tuples);
		}
		
		// construct adjacency list
		HashMap<Integer, HashMultiset<Integer>> adjacencyList = adjacencyList(allTuples);
		
		return adjacencyList;
	}
	
	interface EdgeExtractor {
		List<int[]> getTuples(TMAuction auction, List<BidObject> bids);
	}
	
	private static void charting(HashMap<Integer, HashMultiset<Integer>> adjacencyList) {
		Frequency outgoingEdgesFreq = new Frequency();
		for (int bidderId : adjacencyList.keySet()) {
			int outEdges = adjacencyList.get(bidderId).size();
			outgoingEdgesFreq.addValue(outEdges);
		}
		
		Chart<Integer> chart = new Chart<>();
		
		List<Integer> series = new ArrayList<Integer>();
		for (int i = 1; i < 100; i++) {
			int freq = (int) outgoingEdgesFreq.getCount(i);
			if (freq > 0)
			series.add(freq);
		}
			
		chart.addSeries(series, "blabhalhb");
		chart.build();
	}

	private static HashMap<Integer, HashMultiset<Integer>> adjacencyList(List<int[]> allTuples) {
		HashMap<Integer, HashMultiset<Integer>> adjacencyList = new HashMap<>();

		for (int[] tuple : allTuples) {
			int from = tuple[0];
			int to = tuple[1];
			
			if (!adjacencyList.containsKey(from)) {
				adjacencyList.put(from, HashMultiset.<Integer>create());
			}
			
			adjacencyList.get(from).add(to);
		}
		
		return adjacencyList;
	}
	
	/**
	 * An edge goes from each bidder to the seller for one auction. 
	 * Maximum of 1 edge per auction between seller/bidder.
	 */
	private static class SellerBidderUniqueTuples implements EdgeExtractor {
		public List<int[]> getTuples(TMAuction auction, List<BidObject> bids) {
			HashSet<Integer> uniqueBidders = new HashSet<>();
			for (BidObject bid : bids) {
				uniqueBidders.add(bid.bidderId);
			}
			
			ArrayList<int[]> tuples = new ArrayList<>();
			
			for (int bidder : uniqueBidders) {
				tuples.add(new int[]{bidder, auction.sellerId});
			}
			return tuples;
		}
	}

	/**
	 * Edge from winner to seller.
	 */
	private static class WinFromTuples implements EdgeExtractor {
		@Override
		public List<int[]> getTuples(TMAuction auction, List<BidObject> bids) {
			return Collections.singletonList(new int[]{bids.get(bids.size() - 1).bidderId, auction.sellerId});
		}
	}
	
	/**
	 * Edge from losses to seller.
	 * Maximum 1 edge per auction between seller/loser.
	 */
	private static class LossFromTuples implements EdgeExtractor {
		@Override
		public List<int[]> getTuples(TMAuction auction, List<BidObject> bids) {
			HashSet<Integer> uniqueBidders = new HashSet<>();
			for (BidObject bid : bids) {
				uniqueBidders.add(bid.bidderId);
			}
			uniqueBidders.remove(bids.get(bids.size() - 1).bidderId); // remove the winner
			
			ArrayList<int[]> tuples = new ArrayList<>();
			
			for (int bidder : uniqueBidders) {
				tuples.add(new int[]{bidder, auction.sellerId});
			}
			return tuples;
		}
	}
}
