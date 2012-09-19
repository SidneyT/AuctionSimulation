package createUserFeatures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import simulator.categories.ItemType;
import simulator.database.DBConnection;

import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public class SimAuctionDBIterator implements SimAuctionIterator {
		private Connection conn;
		private final boolean trim;
		public SimAuctionDBIterator(Connection conn, boolean trim) {
			this.conn = conn;
			this.trim = trim;
		}
		
		private List<BuildUserFeatures.BidObject> bids = new ArrayList<>();
		private SimAuction auction = null;
		/* (non-Javadoc)
		 * @see createUserFeatures.SimAuctionIterator#iterator()
		 */
		@Override
		public Iterator<Pair<SimAuction, List<BuildUserFeatures.BidObject>>> getAuctionIterator() {
			return new AuctionIterator();
		}
		
		private class AuctionIterator implements Iterator<Pair<SimAuction, List<BuildUserFeatures.BidObject>>> {
			private int listingId = -1; // id of the current one being processed
			private final ResultSet rs;
			private boolean hasNext = true;

			private AuctionIterator() {
				try {
					Statement stmt = conn.createStatement();
					if (!trim)
						this.rs = stmt.executeQuery(
										"SELECT a.listingId, a.itemTypeId, a.sellerId, a.winnerId " +
			//							", u2.userType as sellerType" +
										", a.endTime, b.time as bidTime, b.amount as bidAmount, b.bidderId " +
//										", u1.userType as bidderType " +
										"FROM auctions as a " +
										"JOIN bids as b ON a.listingId=b.listingId " + 
//										"JOIN users as u1 ON b.bidderId=u1.userId " +
//										"JOIN users as u2 ON a.sellerId=u2.userId " +
										"WHERE endTime IS NOT NULL ORDER BY a.listingId, time ASC;"
								);
					else
						this.rs = stmt.executeQuery(
										"SELECT a.listingId, a.itemTypeId, a.sellerId, a.winnerId " +
//										", u2.userType sellerType, " +
										", a.endTime, b.time bidTime, b.amount as bidAmount, b.bidderId " +
//										", u1.userType as bidderType " +  
										"FROM bids b " +
										"JOIN auctions a ON a.listingId=b.listingId " +   
//										"JOIN users u1 ON b.bidderId=u1.userId " +
//										"JOIN users u2 ON a.sellerId=u2.userId " +
										"LEFT OUTER JOIN bids b2 ON (b.listingId = b2.listingId AND b.amount < b2.amount) " +
										"WHERE endTime IS NOT NULL " +
										"GROUP BY b.listingId, b.amount " + 
										"HAVING COUNT(*) < 20 " +
										"ORDER BY b.listingId ASC, b.amount ASC;"
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
			public Pair<SimAuction, List<BuildUserFeatures.BidObject>> next() {
				try {
					while (rs.next()) {
						int nextId = rs.getInt("listingId");
						if (listingId != nextId) {
							if (listingId == -1) {
								listingId = nextId;
								auction = new SimAuction(rs.getInt("listingId"), rs.getInt("winnerId"), rs.getInt("sellerId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), rs.getInt("itemTypeId"));
								bids = new ArrayList<>();
							} else {
								listingId = nextId;
								Pair<SimAuction, List<BuildUserFeatures.BidObject>> resultPair = new Pair<>(auction, bids); 
								auction = new SimAuction(rs.getInt("listingId"), rs.getInt("winnerId"), rs.getInt("sellerId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), rs.getInt("itemTypeId"));
								bids = new ArrayList<>();
								BuildUserFeatures.BidObject bid = new BuildUserFeatures.BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
								bids.add(bid);
								return resultPair;
							}
						} 
						// still going through bids from the same auction
						BuildUserFeatures.BidObject bid = new BuildUserFeatures.BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
						bids.add(bid);
					}
					hasNext = false;
					if (auction == null) {
						throw new RuntimeException();
					}
					return new Pair<>(auction, bids);
					
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		
		@Override
		public Map<Integer, UserObject> users() {
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
				return b.build();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Collection<ItemType> itemTypes() {
			try {
				PreparedStatement itemtypesQuery = conn.prepareStatement(
						"SELECT id, weight, name, trueValuation, categoryId FROM itemtypes;"
						); 
				ResultSet itemtypesRS = itemtypesQuery.executeQuery();
				ArrayList<ItemType> itemtypes = new ArrayList<>();
				while (itemtypesRS.next()) {
					itemtypes.add(new ItemType(
							itemtypesRS.getInt("id"),
							itemtypesRS.getDouble("weight"),
							itemtypesRS.getString("name"),
							itemtypesRS.getInt("trueValuation"),
							itemtypesRS.getInt("categoryId")
							)
					);
				}
				return itemtypes;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
	}