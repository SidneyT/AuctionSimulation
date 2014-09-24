package createUserFeatures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import simulator.categories.ItemType;
import simulator.database.DBConnection;
import util.IncrementalMean;

import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.TMAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public class SimDBAuctionIterator implements SimAuctionIterator {
	private Connection conn;
	private final boolean trim;

	public static void main(String[] args) {
		tmStats();
//		synStats();
	}

	/**
	 * Find the number of total and unique interactions for each bidder/seller.
	 */
	public static void tmStats() {
		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		
		Iterator<Pair<TMAuction, List<BidObject>>> auctions = it.iterator();
		
		HashMap<Integer, Multiset<Integer>> interactions = new HashMap<>();		
		while (auctions.hasNext()) {
			Pair<TMAuction, List<BidObject>> auctionPair = auctions.next();
			HashSet<Integer> bidderSet = new HashSet<>();
			List<BidObject> bids = auctionPair.getValue();
			for (BidObject bid : bids) {
				bidderSet.add(bid.bidderId);
			}

			TMAuction auction = auctionPair.getKey();
			Integer sellerId = auction.sellerId;
			
//			for (Integer user : bidderSet) {
//				if (!interactions.containsKey(user))
//					interactions.put(user, HashMultiset.<Integer>create());
//				interactions.get(user).add(sellerId); // link from each bidder to the seller
//			}
			
			if (!interactions.containsKey(sellerId))
				interactions.put(sellerId, HashMultiset.<Integer>create());
			
			interactions.get(sellerId).addAll(bidderSet); // link from seller to every bidder
		}
		
//		try {
//	//		BufferedWriter bw = Files.newBufferedWriter(Paths.get("BidderUniqueCounts_TM.csv"), Charset.defaultCharset());
//			BufferedWriter bw = Files.newBufferedWriter(Paths.get("SellerUniqueCounts_TM.csv"), Charset.defaultCharset());
//			for (Integer user : interactions.keySet()) {
//		//			System.out.println(user + "," + interactions.get(user).size() + "," + interactions.get(user).elementSet().size());
//					bw.write(user + "," + interactions.get(user).size() + "," + interactions.get(user).elementSet().size());
//					bw.newLine();
//			}
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
	}
	
	/**
	 * Find the number of total and unique interactions each bidder/seller.
	 */
	public static void synStats() {
		SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_20k_0"), false);
		
		Iterator<Pair<SimAuction, List<BidObject>>> auctions = it.iterator();
		
		HashMap<Integer, Multiset<Integer>> interactions = new HashMap<>();		
		while (auctions.hasNext()) {
			Pair<SimAuction, List<BidObject>> auctionPair = auctions.next();
			HashSet<Integer> bidderSet = new HashSet<>();
			List<BidObject> bids = auctionPair.getValue();
			for (BidObject bid : bids) {
				bidderSet.add(bid.bidderId);
			}

			SimAuction auction = auctionPair.getKey();
			Integer sellerId = auction.sellerId;
			
//			for (Integer user : bidderSet) {
//				if (!interactions.containsKey(user))
//					interactions.put(user, HashMultiset.<Integer>create());
//				interactions.get(user).add(sellerId); // link from each bidder to the seller
//			}
			
			if (!interactions.containsKey(sellerId))
				interactions.put(sellerId, HashMultiset.<Integer>create());
			
			interactions.get(sellerId).addAll(bidderSet); // link from seller to every bidder
		}
		
		try {
//		BufferedWriter bw = Files.newBufferedWriter(Paths.get("BidderUniqueCounts.csv"), Charset.defaultCharset());
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("SellerUniqueCounts.csv"), Charset.defaultCharset());
		for (Integer user : interactions.keySet()) {
	//			System.out.println(user + "," + interactions.get(user).size() + "," + interactions.get(user).elementSet().size());
				bw.write(user + "," + interactions.get(user).size() + "," + interactions.get(user).elementSet().size());
				bw.newLine();
		}
		bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param conn
	 * @param trim true if truncate auction bids to 20, similar to TM.
	 */
	public SimDBAuctionIterator(Connection conn, boolean trim) {
		this.conn = conn;
		this.trim = trim;
	}
	
	private List<BidObject> bids;
	private SimAuction auction = null;
	private ArrayList<Pair<SimAuction, List<BidObject>>> auctionGroups = null;
	/**
	 * @see createUserFeatures.SimAuctionIterator#iterator()
	 */
	@Override
	public Iterator<Pair<SimAuction, List<BidObject>>> iterator() {
		if (auctionGroups == null) {
			AuctionIterator it = new AuctionIterator();
			
			ArrayList<Pair<SimAuction, List<BidObject>>> all = new ArrayList<>();
			while (it.hasNext()) {
				Pair<SimAuction, List<BidObject>> pair = it.next();
				all.add(pair);
			}
			
//			try {
//				conn.close();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
			
			auctionGroups = all;
			
			return all.iterator();
			
		} else {
			return auctionGroups.iterator();
		}
		
	}
	
	private class AuctionIterator implements Iterator<Pair<SimAuction, List<BidObject>>> {
		private int listingId = -1; // id of the current one being processed
		private final ResultSet rs;
		private boolean hasNext = true;
			private AuctionIterator() {
				try {
				Statement stmt = conn.createStatement();
				this.rs = stmt.executeQuery(
									"SELECT a.listingId, a.itemTypeId, a.sellerId, a.winnerId " +
									", a.endTime, b.time as bidTime, b.amount as bidAmount, b.bidderId " +
									"FROM auctions as a " +
									"JOIN bids as b ON a.listingId=b.listingId " + 
									"WHERE endTime IS NOT NULL ORDER BY a.listingId, time ASC;"
							);
				this.hasNext = rs.first(); // see if there's anything in result set
				rs.beforeFirst(); // put the cursor back
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public boolean hasNext() {
			return hasNext;
			}
			@Override
		public Pair<SimAuction, List<BidObject>> next() {
			try {
				while (rs.next()) {
					int nextId = rs.getInt("listingId");
					if (listingId != nextId) { // if not equal, then this is the first bid from a different auction
						if (listingId == -1) { // first auction, so don't return anything.
							listingId = nextId;
							auction = new SimAuction(rs.getInt("listingId"), rs.getInt("winnerId"), rs.getInt("sellerId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), rs.getInt("itemTypeId"));
							bids = new ArrayList<>();
						} else {
							// process the auction and bids list for the last auction
							Pair<SimAuction, List<BidObject>> resultPair;
							int resultPairBidCount = bids.size();
							if (trim && resultPairBidCount > 20) { // logic for keeping the last 20 bids in the auction, like in TradeMe
								List<BidObject> trimmedBids = new ArrayList<>(bids.subList(resultPairBidCount - 20, resultPairBidCount));
								resultPair = new Pair<>(auction, trimmedBids);
							} else {
								resultPair = new Pair<>(auction, bids); 
							}
							
							listingId = nextId;
							auction = new SimAuction(rs.getInt("listingId"), rs.getInt("winnerId"), rs.getInt("sellerId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), rs.getInt("itemTypeId"));
							bids = new ArrayList<>();
							BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
							bids.add(bid);
							
							
							assert auction != null : "auction should never be null here.";
							assert resultPair.getValue().size() <= 20;
							return resultPair;
						}
					} 
					// still going through bids from the same auction
					BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
					bids.add(bid);
				}
				hasNext = false;
				if (auction == null) {
					throw new RuntimeException("To reach here, rs must've been empty to begin with.");
				}
				
				int lastResultPairBidCount = bids.size();
				if (trim && lastResultPairBidCount > 20) { // logic for keeping the last 20 bids in the auction, like in TradeMe
					List<BidObject> trimmedBids = new ArrayList<>(bids.subList(lastResultPairBidCount - 20, lastResultPairBidCount));
					return new Pair<>(auction, trimmedBids);
				} else {
					return new Pair<>(auction, bids);
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	Map<Integer, UserObject> users = null;
	@Override
	public Map<Integer, UserObject> users() {
		if (users != null) {
			return users;
		}
		try {
			PreparedStatement usersQuery = conn.prepareStatement(
					"SELECT DISTINCT userId, userType, posUnique, negUnique " +
					"FROM users as u " +
					";"
					); 
			ResultSet usersResultSet = usersQuery.executeQuery();
			Builder<Integer, UserObject> b = ImmutableMap.builder();
			while (usersResultSet.next()) {
				b.put(usersResultSet.getInt("userId"), 
						new UserObject(
						usersResultSet.getInt("userId"),
						usersResultSet.getInt("posUnique"),
						usersResultSet.getInt("negUnique"),
						usersResultSet.getString("userType")
						)
				);
				}
			return users = b.build();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Map<Integer, ItemType> itemTypes() {
		try {
			PreparedStatement itemtypesQuery = conn.prepareStatement(
					"SELECT id, weight, name, trueValuation, categoryId FROM itemtypes;"
					); 
			ResultSet itemtypesRS = itemtypesQuery.executeQuery();
			Builder<Integer, ItemType> itemtypesBuilder = ImmutableMap.builder();
			while (itemtypesRS.next()) {
				itemtypesBuilder.put(itemtypesRS.getInt("id"),
						new ItemType(
						itemtypesRS.getInt("id"),
						itemtypesRS.getDouble("weight"),
						itemtypesRS.getString("name"),
						itemtypesRS.getInt("trueValuation"),
						itemtypesRS.getInt("categoryId")
						)
				);
			}
			return itemtypesBuilder.build();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
}