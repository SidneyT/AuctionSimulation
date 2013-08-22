package graph;

import graph.EdgeType.SellerEdges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import util.Util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;

public class GraphOperations {

	/**
	 * Given a list of edges, converts them to an adjacency list.
	 */
	static ImmutableMap<Integer, Multiset<Integer>> adjacencyList(List<int[]> allTuples) {
		Map<Integer, Multiset<Integer>> adjacencyList = new HashMap<>();
	
		for (int[] tuple : allTuples) {
			int from = tuple[0];
			int to = tuple[1];
			
			if (!adjacencyList.containsKey(from)) {
				adjacencyList.put(from, HashMultiset.<Integer>create());
			}
			
			adjacencyList.get(from).add(to);
		}
		
		Map<Integer, Multiset<Integer>> adjacencyList2 = new HashMap<>();
		for (Integer key : adjacencyList.keySet()) {
			adjacencyList2.put(key, ImmutableMultiset.copyOf(adjacencyList.get(key)));
		}
		
		ImmutableMap<Integer, Multiset<Integer>> immutableAdjacencyList = ImmutableMap.copyOf(adjacencyList2);
		
		return immutableAdjacencyList;
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
	public static <T extends AuctionObject> ImmutableMap<Integer, Multiset<Integer>> duplicateAdjacencyList(Iterator<Pair<T, List<BidObject>>> auctionIterator, EdgeTypeI edgeType) {
		ArrayList<int[]> allTuples = new ArrayList<>();
		while(auctionIterator.hasNext()) {
			Pair<T, List<BidObject>> pair = auctionIterator.next();
			
			T from = pair.getKey();
			List<BidObject> to = pair.getValue();
			
			List<int[]> tuples = edgeType.getTuples(from, to);
			
			allTuples.addAll(tuples);
		}
		
		// construct adjacency list
		ImmutableMap<Integer, Multiset<Integer>> adjacencyList = adjacencyList(allTuples);
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
	public static <T extends AuctionObject> HashMap<Integer, Multiset<Integer>> duplicateAdjacencyList(Iterator<Pair<T, List<BidObject>>> auctionIterator, SellerEdges edgeType) {
		HashMultimap<Integer, Integer> sellerList = HashMultimap.create(); //stores the list of sellers each bidder has interacted with Multimap<bidder, sellers>
		
//		HashSet<Integer> temp = new HashSet<>();
		
		while (auctionIterator.hasNext()) {
			Pair<T, List<BidObject>> pair = auctionIterator.next();
			
			AuctionObject auction = pair.getKey();
			List<BidObject> bids = pair.getValue();

			int sellerId = auction.sellerId;
			
			for (BidObject bid : bids) {
				sellerList.put(bid.bidderId, sellerId);
//				if (sellerId == 107683) {
//					System.out.println(bid.bidderId + " was in auction " + auction.listingId + " with " + bids);
//					temp.add(bid.bidderId);
//				}
			}
			
		}
		
		HashMap<Integer, Multiset<Integer>> adjacencyList = new HashMap<>();
		
		// convert seller list into an adjacency list
		for (Integer bidderId : sellerList.keySet()) {
			Set<Integer> linkedSellers = sellerList.get(bidderId);
			
			
			for (Integer seller : linkedSellers) {
//				if (temp.contains(bidderId) && bidderId != 107683) {
//					System.out.println("shared seller: " + seller);
//				}
				if (!adjacencyList.containsKey(seller)) {
					adjacencyList.put(seller, HashMultiset.<Integer>create());
				}
				
				Multiset<Integer> neighbourSellerList = adjacencyList.get(seller);
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

	private static final HashBasedTable<Map<Integer, Multiset<Integer>>, Integer, Map<Integer, Multiset<Integer>>> table = HashBasedTable.create();

	/**
	 * Wrapper for caching egonets.
	 * @param adjacencyList
	 * @param user
	 * @return
	 */
	public static Map<Integer, Multiset<Integer>> egonetAdjacencyMatrix(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
		Map<Integer, Multiset<Integer>> egonet;
		
		if (table.contains(adjacencyList, user)) {
			egonet = table.get(adjacencyList, user);
			assert egonet.equals(GraphOperations.egonetAdjacencyMatrixInner(adjacencyList, user));
		} else {
			egonet = GraphOperations.egonetAdjacencyMatrixInner(adjacencyList, user);
			table.put(adjacencyList, user, egonet);
		}
		
		return egonet;
	}

	/**
	 * Returns the adjacency list of <code>user</code>'s egonet. Frequency in the multiset represents the weight of that edge.
	 * @param adjacencyList
	 * @param user
	 * @return
	 */
	public static ImmutableMap<Integer, Multiset<Integer>> egonetAdjacencyMatrixInner(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
		Builder<Integer, Multiset<Integer>> egonetAdjacencyBuilder = ImmutableMap.builder();
		Multiset<Integer> neighbours = adjacencyList.get(user);

		// construct the adjacency list for the egonet: the user and its neighbours
		egonetAdjacencyBuilder.put(user, ImmutableMultiset.copyOf(neighbours));
		for (Integer neighbour : neighbours.elementSet()) {
			egonetAdjacencyBuilder.put(neighbour, HashMultiset.<Integer> create(neighbours.size()));
		}

		ImmutableMap<Integer, Multiset<Integer>> immutableEgonetAdjacency = egonetAdjacencyBuilder.build();
		for (Integer neighbour : neighbours.elementSet()) {
			
			if (!adjacencyList.containsKey(neighbour))
				continue; // a neighbour may incoming edges, i.e. user -> neighbour, but have no out-going edges, since graph is directed 
			
			Multiset<Integer> nns = adjacencyList.get(neighbour);
			
			Set<Integer> usersInEgonet; 
			if (nns.elementSet().size() < immutableEgonetAdjacency.keySet().size()) {
				usersInEgonet = Sets.intersection(nns.elementSet(), immutableEgonetAdjacency.keySet());
			} else {
				usersInEgonet = Sets.intersection(immutableEgonetAdjacency.keySet(), nns.elementSet());
			}
			
			Multiset<Integer> egonetNeighbours = immutableEgonetAdjacency.get(neighbour);

			for (Integer nn : usersInEgonet) {
				egonetNeighbours.add(nn, nns.count(nn));
			}
		}

		return immutableEgonetAdjacency;
	}

}
