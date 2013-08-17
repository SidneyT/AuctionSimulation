package graph;

import graph.EdgeType.SellerEdges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import util.Util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;

import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;

public class GraphOperations {

	/**
	 * Given a list of edges, converts them to an adjacency list.
	 */
	static HashMap<Integer, HashMultiset<Integer>> adjacencyList(List<int[]> allTuples) {
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
	 * Finds the list of edges from each auction as defined by <code>edgeType</code>, then unions all the lists together into a SET, without duplication.
	 * I.e. there is only a weight of 1 between each user. 
	 * This is different from {@link GraphOperations#duplicateAdjacencyList(Iterator, EdgeTypeI)}, which gives duplicates in the returned datastructure (i.e. edges have weights).
	 */
	public static <T extends AuctionObject> HashMap<Integer, Set<Integer>> adjacencySet(Iterator<Pair<T, List<BidObject>>> auctionIterator, EdgeTypeI edgeType) {
		
		HashMap<Integer, Set<Integer>> adjacencyMap = new HashMap<>();
		while(auctionIterator.hasNext()) {
			Pair<T, List<BidObject>> pair = auctionIterator.next();
			
			T auction = pair.getKey();
			List<BidObject> bids = pair.getValue();
			
			List<int[]> tuples = edgeType.getTuples(auction, bids);
			
			for (int i = 0; i < tuples.size(); i++) {
				int[] tuple = tuples.get(i);
				Util.mapSetAdd(adjacencyMap, tuple[0], tuple[1]);
			}
		}
		
		return adjacencyMap;
	}

	/**
	 * Unions the set of edges obtained from each auction using <code>edgeType</code> then just adds them together, with duplication.
	 * @param auctionIterator
	 * @param edgeType
	 * @return
	 */
	public static <T extends AuctionObject> HashMap<Integer, HashMultiset<Integer>> duplicateAdjacencyList(Iterator<Pair<T, List<BidObject>>> auctionIterator, EdgeTypeI edgeType) {
		ArrayList<int[]> allTuples = new ArrayList<>();
		while(auctionIterator.hasNext()) {
			Pair<T, List<BidObject>> pair = auctionIterator.next();
			
			T auction = pair.getKey();
			List<BidObject> bids = pair.getValue();
			
			List<int[]> tuples = edgeType.getTuples(auction, bids);
			
			allTuples.addAll(tuples);
		}
		
		// construct adjacency list
		HashMap<Integer, HashMultiset<Integer>> adjacencyList = adjacencyList(allTuples);
//		for (Integer key : adjacencyList.keySet()) {
//			System.out.println(key + " || " + adjacencyList.get(key));
//		}
		return adjacencyList;
	}
	
	/**
	 * Sellers that have shared bidders will have an edge between them. The weight of the edge reflects the number of bidders shared.
	 * A single seller bids in the same seller's auction once only, or more than once, makes no difference to the resulting adjacencyList.
	 * @param auctionIterator
	 * @param edgeType
	 * @return
	 */
	public static <T extends AuctionObject> HashMap<Integer, HashMultiset<Integer>> duplicateAdjacencyList(Iterator<Pair<T, List<BidObject>>> auctionIterator, SellerEdges edgeType) {
		HashMultimap<Integer, Integer> sellerList = HashMultimap.create(); //stores the list of sellers each bidder has interacted with Multimap<bidder, sellers>
		while (auctionIterator.hasNext()) {
			Pair<T, List<BidObject>> pair = auctionIterator.next();
			
			AuctionObject auction = pair.getKey();
			List<BidObject> bids = pair.getValue();

			int sellerId = auction.sellerId;
			
			for (BidObject bid : bids) {
				sellerList.put(bid.bidderId, sellerId);
			}
			
		}
		
		HashMap<Integer, HashMultiset<Integer>> adjacencyList = new HashMap<>();
		
		// convert seller list into an adjacency list
		for (Integer bidderId : sellerList.keySet()) {
			Set<Integer> linkedSellers = sellerList.get(bidderId);
			
			for (Integer seller : linkedSellers) {
				if (adjacencyList.containsKey(seller)) {
					adjacencyList.put(seller, HashMultiset.<Integer>create());
				}
				
				HashMultiset<Integer> neighbourSellerList = adjacencyList.get(seller);
				neighbourSellerList.addAll(linkedSellers); 
				neighbourSellerList.remove(seller); // remove the seller itself from it's own edge list
			}
			
		}

		boolean assertOn = false;
		assert assertOn = true;
		if (assertOn) {
			for (Integer seller : adjacencyList.keySet()) {
				assert !adjacencyList.get(seller).contains(seller) : "A seller should not have an edge to itself.";
			}
		}
		
		return adjacencyList;
	}
	
}
