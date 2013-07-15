package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import util.Util;

import com.google.common.collect.ArrayListMultimap;
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
	static <T extends AuctionObject> HashMap<Integer, Set<Integer>> adjacencySet(Iterator<Pair<T, List<BidObject>>> auctionIterator, EdgeTypeI edgeType) {
		
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
	static <T extends AuctionObject> HashMap<Integer, HashMultiset<Integer>> duplicateAdjacencyList(Iterator<Pair<T, List<BidObject>>> auctionIterator, EdgeTypeI edgeType) {
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
		
		return adjacencyList;
	}
	
}
