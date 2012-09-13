package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import createUserFeatures.BuildUserFeatures.BidObject;

import simulator.database.DBConnection;

/**
 *	Builds UserFeature object using scraped TradeMe data. 
 */
public class BuildTMFeatures extends BuildUserFeatures{
	
	public static void main(String[] args) {
//		String features = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15"; // all
//		String features = "-1ln-2ln-3ln-4ln-6ln-13-9-5-10-11";
		
		List<Feature> allFeatures = Arrays.<Feature>asList(Features.values());
		List<Feature> featureList = Arrays.<Feature>asList(
				Features.AuctionCount1Ln, 
				Features.Rep2Ln,
				Features.AvgBid3Ln,
				Features.AvgBidIncMinusMinInc4Ln,
				Features.BidsPerAuc6Ln,
				Features.FirstBidTimes13,
				Features.BidTimesElapsed9,
				Features.PropWin5,
				Features.AvgBidPropMax10,
				Features.AvgBidProp11
				);
		
		BuildTMFeatures bf = new BuildTMFeatures();
		
//		writeToFile(bf.build().values(), bf.getFeaturesToPrint(), Paths.get("TradeMeUserFeatures" + features + ".csv"));
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
//		reclustering(features, 4);
		int minimumFinalPrice = 2000;
		int maximumFinalPrice = 10000;
		writeToFile(bf.build(minimumFinalPrice, maximumFinalPrice).values(), featureList, 
				Paths.get("TradeMeUserFeatures" + Features.fileLabels(featureList) + "-" + minimumFinalPrice + "-" + maximumFinalPrice + ".csv"));
		System.out.println("Finished.");
	}
	
	/**
	 * Builds user features for users using only auctions that ended with a price over a certain value. 
	 * @param minimumPrice
	 * @param maximumPrice
	 * @return
	 */
	public Map<Integer, UserFeatures> build(int minimumPrice, int maximumPrice) {
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time " +
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"AND EXISTS (SELECT DISTINCT b.listingId FROM bids as b2 WHERE b2.listingId=a.listingId GROUP BY b2.listingId " +
				"HAVING MAX(b2.amount) > " + minimumPrice + " AND " +
				"MAX(b2.amount) <= " + maximumPrice + ") " +
				"ORDER BY a.listingId, amount ASC;";
		return constructUserFeatures(query);
	}
	
	public static void reclustering(List<Feature> features, int numClusters) {
		for (int clusterId = 0; clusterId < numClusters; clusterId++) {
			BuildTMFeatures buf = new BuildTMFeatures();
			writeToFile(buf.reclustering_build(clusterId).values(), features, Paths.get("recluster_" + clusterId + ".csv"));
		}
	}
	@Override
	public Map<Integer, UserFeatures> reclustering_build(int clusterId) {
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time, c.cluster " +  
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"JOIN cluster AS c ON b.bidderId=c.userId " + 
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"ORDER BY a.listingId, b.amount ASC;";
		
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
	 * calls constructUserFeatures with default query
	 */
	public Map<Integer, UserFeatures> build() {
		// get bids (and user and auction info) for auctions that are not purchased with buy now
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time " +
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"ORDER BY a.listingId ASC, amount ASC;";
		return constructUserFeatures(query);
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public Map<Integer, UserFeatures> constructUserFeatures(String query) {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			
			PreparedStatement bigQuery = conn.prepareStatement(query);
			ResultSet bigRS = bigQuery.executeQuery();

			int lastListingId = -1;
			TMAuction ao = null;
			
			// split group the bids by auctions, and put them into a list
			List<BuildUserFeatures.BidObject> bidList = new ArrayList<BuildUserFeatures.BidObject>();
			while (bigRS.next()) {
				int currentListingId = bigRS.getInt("listingId");
				if (lastListingId != currentListingId) { // new auction
					if (lastListingId != -1) { // test if there is a previous auction processed, if there is, then process the previous auction
						processAuction(ao, bidList);
						// clear the lists
						bidList.clear();
					}
					lastListingId = currentListingId;
					// record the auction information for the new row
					ao = new TMAuction(currentListingId, bigRS.getInt("winnerId"), bigRS.getInt("sellerId"), bigRS.getTimestamp("endTime"), simpleCategory(bigRS.getString("category")));
				}
				bidList.add(new BuildUserFeatures.BidObject(bigRS.getInt("bidderId"), currentListingId, bigRS.getTimestamp("time"), bigRS.getInt("amount")));
			}
			processAuction(ao, bidList); // process the bidList for the last remaining auction
			
			userRep(conn);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this.userFeaturesMap;
	}
	
	public static class SimAuctionGroupIterator {
		private int listingId = -1; // id of the current one being processed
		private final ResultSet rs;
		private boolean hasNext = true;
		public SimAuctionGroupIterator(Connection conn) {
			try {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery(
						"SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time " +
							"FROM auctions AS a " +
							"JOIN bids AS b ON a.listingId=b.listingId " +
							"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
							"ORDER BY a.listingId ASC, amount ASC;"
						);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Iterator<Pair<TMAuction, List<BuildUserFeatures.BidObject>>> iterator() {
			return new Iterator<Pair<TMAuction,List<BuildUserFeatures.BidObject>>>() {
				@Override
				public boolean hasNext() {
					return hasNext;
				}

				@Override
				public Pair<TMAuction, List<BuildUserFeatures.BidObject>> next() {
					try {
						List<BuildUserFeatures.BidObject> bids = new ArrayList<>();
						TMAuction auction = null;
							while (rs.next()) {
								int nextId = rs.getInt("listingId");
								if (listingId != nextId) {
									listingId = nextId;
									return new Pair<>(auction, bids);
								} else { // still going through bids from the same auction
									auction = new TMAuction(rs.getInt("listingId"), 
											rs.getInt("winnerId"), 
											rs.getInt("sellerId"), 
											BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), 
											BuildTMFeatures.simpleCategory(rs.getString("category"))
										);
								}
								BuildUserFeatures.BidObject bid = new BuildUserFeatures.BidObject(rs.getInt("bidderId"), 
										rs.getInt("listingId"), 
										BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), 
										rs.getInt("bidAmount"));
								bids.add(bid);
							}
						hasNext = false;
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
	}

	/**
	 * Finds the POS, NEU and NEG reputation for a user. Records those values
	 * into UserFeatures objects with the same userId.  If userFeaturesMap does
	 * not contain such an object, those values are discarded.
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
