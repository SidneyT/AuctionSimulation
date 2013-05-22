package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import simulator.database.DBConnection;

/**
 *	Builds UserFeature object using scraped TradeMe data. 
 */
public class BuildTMFeatures extends BuildUserFeatures {
	
	public static void main(String[] args) {
//		run();
		moreThanTwo();
		System.out.println("Finished.");
	}

	private static void moreThanTwo() {
		List<Features> featureList = Features.ALL_FEATURES_MINUS_TYPE;
		BuildTMFeatures bf = new BuildTMFeatures();
		
		KeepTest moreThanTwo = new KeepTest() {
			@Override
			public boolean keep(UserFeatures uf) {
				return uf.auctionCount > 2 && uf.auctionCount != uf.auctionsWon;
			}
		};
		
		writeToFile(bf.build().values(), featureList, moreThanTwo, Paths.get("TradeMeUserFeatures_" + Features.fileLabels(featureList) + "_gt2.csv"));
	}
	
	private static void run() {
		List<Features> featureList = Features.ALL_FEATURES_MINUS_TYPE;
		BuildTMFeatures bf = new BuildTMFeatures();
		
//		int minimumFinalPrice = 10000;
//		int maximumFinalPrice = 999999999;
//		writeToFile(bf.build(minimumFinalPrice, maximumFinalPrice).values(), featureList, 
//				Paths.get("TradeMeUserFeatures_" + Features.fileLabels(featureList) + "-" + minimumFinalPrice + "-" + maximumFinalPrice + ".csv"));
		
		writeToFile(bf.build().values(), featureList, Paths.get("TradeMeUserFeatures_" + Features.fileLabels(featureList) + ".csv"));
		
	}
	
	
	public static final String DEFAULT_QUERY = 
			"SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount bidAmount, b.time bidTime " +
			"FROM auctions AS a " +
			"JOIN bids AS b ON a.listingId=b.listingId " +
			"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
			"ORDER BY a.listingId ASC, amount ASC;";
	/**
	 * calls constructUserFeatures with default query
	 */
	public Map<Integer, UserFeatures> build() {
		// get bids (and user and auction info) for auctions that are not purchased with buy now
		return constructUserFeatures(DEFAULT_QUERY);
	}
	
	/**
	 * Builds user features for users using only auctions that ended with a price over a certain value. 
	 * @param minimumPrice
	 * @param maximumPrice
	 * @return
	 */
	public Map<Integer, UserFeatures> build(int minimumPrice, int maximumPrice) {
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount bidAmount, b.time bidTime " +
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"AND EXISTS (SELECT DISTINCT b.listingId FROM bids as b2 WHERE b2.listingId=a.listingId GROUP BY b2.listingId " +
				"HAVING MAX(b2.amount) > " + minimumPrice + " AND " +
				"MAX(b2.amount) <= " + maximumPrice + ") " +
				"ORDER BY a.listingId, amount ASC;";
		return constructUserFeatures(query);
	}
	
	public static void reclustering(List<Features> features, int numClusters) {
		for (int clusterId = 0; clusterId < numClusters; clusterId++) {
			BuildTMFeatures buf = new BuildTMFeatures();
			writeToFile(buf.reclustering_build(clusterId).values(), features, Paths.get("recluster_" + clusterId + ".csv"));
		}
	}
	@Override
	public Map<Integer, UserFeatures> reclustering_build(int clusterId) {
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount bidAmount, b.time bidTime, c.cluster " +  
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"JOIN cluster AS c ON b.bidderId=c.userId " + 
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"ORDER BY a.listingId, b.amount bidAmount ASC;";
		
		Map<Integer, UserFeatures> userFeaturesMap = constructUserFeatures(query); // contains userFeatures from all clusters
		
		Set<Integer> idsInCluster = new HashSet<Integer>();
		try {
			Connection conn = DBConnection.getTrademeConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT userId FROM cluster WHERE cluster=? AND algorithm='SimpleKMeans'");
			pstmt.setInt(1, clusterId);
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				idsInCluster.add(rs.getInt("userId"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		userFeaturesMap.keySet().retainAll(idsInCluster);
		
		return userFeaturesMap;
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public Map<Integer, UserFeatures> constructUserFeatures(String query) {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			Iterator<Pair<TMAuction, List<BidObject>>> auctionIterator = new TMAuctionGroupIterator(conn, query).iterator();
			
			while(auctionIterator.hasNext()) {
				Pair<TMAuction, List<BidObject>> pair = auctionIterator.next();
				processAuction(pair.getKey(), pair.getValue()); // process the bidList for the last remaining auction
			}
			
			userRep(conn);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this.userFeaturesMap;
	}
	
	/**
	 * Gives an iterator going through DB, giving a pair with Auction & Bids belonging to that auction.
	 *
	 */
	public static class TMAuctionGroupIterator {
		private int listingId = -1; // id of the current one being processed
		private final ResultSet rs;
		private boolean hasNext;
		private Connection conn;
		public TMAuctionGroupIterator(Connection conn, String query) {
			try {
				this.conn = conn;
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery(query);
				
				this.hasNext = rs.first(); // see if there's anything in result set
				rs.beforeFirst(); // put the cursor back
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		private List<BidObject> bids;
		private TMAuction auction;

		public Iterator<Pair<TMAuction, List<BidObject>>> iterator() {
			return new Iterator<Pair<TMAuction,List<BidObject>>>() {
				@Override
				public boolean hasNext() {
					return hasNext;
				}

				@Override
				public Pair<TMAuction, List<BidObject>> next() {
					try {
						while (rs.next()) {
							int nextId = rs.getInt("listingId");
							if (listingId != nextId) {
								if (listingId == -1) {
									listingId = nextId;
									auction = new TMAuction(rs.getInt("listingId"), rs.getInt("winnerId"),
											rs.getInt("sellerId"), rs.getTimestamp("endTime"),
											BuildTMFeatures.simpleCategory(rs.getString("category")));
									bids = new ArrayList<>();
								} else {
									listingId = nextId;
									Pair<TMAuction, List<BidObject>> result = new Pair<>(auction, bids);
									auction = new TMAuction(rs.getInt("listingId"), rs.getInt("winnerId"),
											rs.getInt("sellerId"), rs.getTimestamp("endTime"),
											BuildTMFeatures.simpleCategory(rs.getString("category")));
									bids = new ArrayList<>();
									BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"),
											rs.getTimestamp("bidTime"),
											rs.getInt("bidAmount"));
									bids.add(bid);
									
									if (auction == null) {
										throw new RuntimeException();
									}
									return result;
								}
							}
							// still going through bids from the same auction
							BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"),
									rs.getTimestamp("bidTime"),
									rs.getInt("bidAmount"));
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
			};
		}
		
		public Map<Integer, UserObject> users() {
			Builder<Integer, UserObject> userObjects = ImmutableMap.builder();
			try {
				PreparedStatement usersQuery = conn.prepareStatement(
						"SELECT DISTINCT userId, posUnique, negUnique " +
						"FROM users as u " +
						";"
				);
				ResultSet usersResultSet = usersQuery.executeQuery();
				while (usersResultSet.next()) {
					
					int userId = usersResultSet.getInt("userId");
					UserObject user = new UserObject(userId, usersResultSet.getInt("posUnique"), usersResultSet.getInt("negUnique"), "TMUser");
					userObjects.put(userId, user);
				}
				
				return userObjects.build();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} 
		}
	}

	/**
	 * Finds the POS, NEU and NEG reputation for a user. Records those values
	 * into UserFeatures objects with the same userId.  If userFeaturesMap does
	 * not contain such a user, those values are discarded.
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	private void userRep(Connection conn) throws SQLException {
		// get userId and Rep which bid on an auction that is not purchased with buynow
		PreparedStatement usersQuery = conn.prepareStatement(
				"SELECT DISTINCT userId, posUnique, negUnique " +
				"FROM users as u " +
				";"
				); 
		ResultSet usersResultSet = usersQuery.executeQuery();
		while (usersResultSet.next()) {
			if (userFeaturesMap.containsKey(usersResultSet.getInt("userId")))
				userFeaturesMap.get(usersResultSet.getInt("userId")).setRep(usersResultSet.getInt("posUnique"), usersResultSet.getInt("negUnique"));
		}
	}
	
	public static String simpleCategory(String category) {
		return category.split("/")[1];
	}
	
}
