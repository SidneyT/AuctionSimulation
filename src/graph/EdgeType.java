package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;

public enum EdgeType implements EdgeTypeI {
	/**
	 * Edge from winner to seller.
	 */
	WIN {
		@Override
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			return Collections.singletonList(new int[]{bids.get(bids.size() - 1).bidderId, auction.sellerId});
		}
	},
	/**
	 * Edge from losses to seller.
	 * Maximum 1 edge between seller/loser pair (per auction).
	 */
	LOSS {
		@Override
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
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
	}, 
	/**
	 * Edges are between all bidders of the auction.
	 * Note edges are undirected (i.e. 2 directed edges), so don't use {@link EdgeType#reverse(EdgeTypeI)}.
	 */
	IN_SAME_AUCTION {
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			Set<Integer> users = new HashSet<>();
			for (BidObject bid : bids) {
				users.add(bid.bidderId);
			}
			
			List<int[]> edges = new ArrayList<>();
			for (int userA : users) {
				for (int userB : users) {
					if (userA != userB) {
						edges.add(new int[]{userA, userB});
					}
				}
			}
			return edges;
		}
	},
	/**
	 * An edge goes from each bidder to the seller for one auction. 
	 * Maximum of 1 edge per auction between seller/bidder per auction.
	 */
	PARTICIPATE {
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
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
	},
	/**
	 * An edge goes from each bidder to the seller for EACH bid the bidder made. 
	 */
	BID_IN {
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			ArrayList<int[]> tuples = new ArrayList<>();
			for (BidObject bid : bids) {
				tuples.add(new int[]{bid.bidderId, auction.sellerId});
			}
			return tuples;
		}
	}

	;


	public static Reverse reverse(EdgeTypeI edgeType) {
		return new Reverse(edgeType);
	}
	
	/**
	 * Reverse the direction of edges.
	 * Modifies the given list.
	 */
	private static class Reverse implements EdgeTypeI {
		private final EdgeTypeI edgeType;
		
		Reverse(EdgeTypeI edgeType) {
			this.edgeType = edgeType;
		}
		
		@Override
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			List<int[]> edges = edgeType.getTuples(auction, bids);
			int temp;
			for (int[] edge : edges) {
				temp = edge[0];
				edge[0] = edge[1];
				edge[1] = temp;
			}
			return edges;
		}
	
		@Override
		public String toString() {
			return "r_" + edgeType.toString();
		}
		
	}
	
	public static SellerEdges sellerEdges() {
		return new SellerEdges();
	}
	/**
	 * A marker class 
	 */
	public static class SellerEdges implements EdgeTypeI {
		@Override
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
			return "sellerEdges";
		}
	}
	
	public static Undirected undirected(EdgeTypeI edgeType) {
		return new Undirected(edgeType);
	}
	
	public static class Undirected implements EdgeTypeI {
		private final EdgeTypeI edgeType;
		
		Undirected(EdgeTypeI edgeType) {
			this.edgeType = edgeType;
		}
		
		@Override
		public List<int[]> getTuples(AuctionObject auction, List<BidObject> bids) {
			List<int[]> edges = edgeType.getTuples(auction, bids);
			List<int[]> edgesCopy = new ArrayList<>(edgeType.getTuples(auction, bids));
			for (int[] edge : edges) {
				edgesCopy.add(new int[]{edge[1], edge[0]});
			}
			return edgesCopy;
		}
	
		@Override
		public String toString() {
			return "u_" + edgeType.toString();
		}
	}
}