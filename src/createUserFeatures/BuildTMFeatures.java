package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

import simulator.database.DatabaseConnection;
import util.Util;

/**
 *	Builds UserFeature object using scraped TradeMe data. 
 */
public class BuildTMFeatures extends BuildUserFeatures{
	
	public static void main(String[] args) {
//		String features = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15"; // all
		String features = "-1ln-2ln-3ln-4ln-6ln-13-9-5-10-11";
		
		List<Feature> allFeatures = Arrays.<Feature>asList(Features.values());
		List<Feature> featureList = Arrays.<Feature>asList(
				Features.AuctionCount1, 
				Features.Rep2,
				Features.Rep2Ln,
				Features.AvgBid3Ln,
				Features.AvgBidIncMinusMinInc4Ln,
				Features.BidsPerAuc6Ln,
				Features.FirstBidTimes13,
				Features.BidTimesUntilEnd9,
				Features.PropWin5,
				Features.AvgBidPropMax10,
				Features.AvgBidProp11);
		
		BuildTMFeatures bf = new BuildTMFeatures();
		
		System.out.println(Features.Rep2.name());
		
//		writeToFile(bf.build().values(), bf.getFeaturesToPrint(), Paths.get("TradeMeUserFeatures" + features + ".csv"));
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
//		reclustering(features, 4);
		int minimumFinalPrice = 10000;
		int maximumFinalPrice = 100000000;
		writeToFile(bf.build(minimumFinalPrice, maximumFinalPrice).values(), featureList, 
				Paths.get("TradeMeUserFeatures" + features + "-" + minimumFinalPrice + "-" + maximumFinalPrice + ".csv"));
		System.out.println("Finished.");
	}
	
	/**
	 * Builds user features for users using only auctions that ended with a price over a certain value. 
	 * @param minimumPrice
	 * @param maximumPrice
	 * @return
	 */
	public TreeMap<Integer, UserFeatures> build(int minimumPrice, int maximumPrice) {
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
	public TreeMap<Integer, UserFeatures> reclustering_build(int clusterId) {
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time, c.cluster " +  
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"JOIN cluster AS c ON b.bidderId=c.userId " + 
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"ORDER BY a.listingId, b.amount ASC;";
		
		TreeMap<Integer, UserFeatures> userFeaturesCol = constructUserFeatures(query); // contains userFeatures from all clusters
		
		Set<Integer> idsInCluster = new HashSet<Integer>();
		try {
			Connection conn = DatabaseConnection.getTrademeConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT userId FROM cluster WHERE cluster=? AND algorithm='SimpleKMeans'");
			pstmt.setInt(1, clusterId);
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				idsInCluster.add(rs.getInt("userId"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		userFeaturesCol.keySet().retainAll(idsInCluster);
		
		return userFeaturesCol;
	}
	
	/**
	 * calls constructUserFeatures with default query
	 */
	public TreeMap<Integer, UserFeatures> build() {
		// get bids (and user and auction info) for auctions that are not purchased with buy now
		String query = "SELECT a.listingId, a.category, a.sellerId, a.endTime, a.winnerId, b.bidderId, b.amount, b.time " +
				"FROM auctions AS a " +
				"JOIN bids AS b ON a.listingId=b.listingId " +
				"WHERE a.purchasedWithBuyNow=0 " + // for no buynow
				"ORDER BY a.listingId, amount ASC;";
		return constructUserFeatures(query);
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public TreeMap<Integer, UserFeatures> constructUserFeatures(String query) {
		try {
			Connection conn = DatabaseConnection.getTrademeConnection();
			
			PreparedStatement bigQuery = conn.prepareStatement(query);
			ResultSet bigRS = bigQuery.executeQuery();

			int lastListingId = -1;
			TMAuctionObject ao = null;
			
			// split group the bids by auctions, and put them into a list
			List<BidObject> bidList = new ArrayList<BidObject>();
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
					ao = new TMAuctionObject(currentListingId, this.simpleCategory(bigRS.getString("category")), bigRS.getTimestamp("endTime"), bigRS.getInt("winnerId"));
				}
				bidList.add(new BidObject(bigRS.getInt("bidderId"), bigRS.getInt("amount"), bigRS.getTimestamp("time")));
			}
			processAuction(ao, bidList); // process the bidList for the last remaining auction
			
			userRep(conn);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this.userFeaturesMap;
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
	
	/**
	 * @param auctionObject
	 * @param bidList list of bids for this auction in chronological order
	 */
	private void processAuction(TMAuctionObject auctionObject, List<BidObject> bidList) {
//		System.out.println(auctionObject + ", " + bidObjects);

		// count the number of bids made for each user in the bidList
		// and record the time of the last bid made for each user in the bidList 
 		Map<Integer, Integer> bidderBidCount = new HashMap<Integer, Integer>(); // Map<UserId, Count>
		Map<Integer, Date> bidderLastBidTime = new HashMap<Integer, Date>(); // Map<UserId, Date>
		for (BidObject bo : bidList) {
			int bidderId = bo.bidderId;
			if (bidderBidCount.get(bidderId) == null) { // counting
				bidderBidCount.put(bidderId, 1);
			} else {
				bidderBidCount.put(bidderId, bidderBidCount.get(bidderId) + 1);
			}
			bidderLastBidTime.put(bidderId, bo.time); // last bid time
		}
		 
		// create a new UserFeatures the bidder if there is no UserFeatures object for them yet
		for (int bidderId : bidderBidCount.keySet()) {
			if (!userFeaturesMap.containsKey(bidderId)) {
				UserFeatures uf = new UserFeatures();
				uf.setUserId(bidderId);
				userFeaturesMap.put(bidderId, uf);
			}
		}
		
		// record when bids are made
		recordBidPeriods(auctionObject.endTime, bidList);
		
		for (int bidderId : bidderBidCount.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			
			// calculate the number of minutes until the end the last bid was made
			long untilEndMin = Util.timeDiffInMin(auctionObject.endTime, bidderLastBidTime.get(bidderId));
//			System.out.println(auctionObject.endTime + " - " + bidderLastBidTime.get(bidderId));
//			System.out.println(auctionObject.endTime.getTime() + " - " + bidderLastBidTime.get(bidderId).getTime());
//			System.out.println("untilEnd: " + untilEndMin);
			
			// record proportion of bids in the auction are made by this user
			double bidProp = ((double) bidderBidCount.get(bidderId) / bidList.size());
			uf.avgBidProp = Util.incrementalAvg(uf.avgBidProp, uf.getAuctionCount(), bidProp);

			// add this auction's information to this bidder's UserFeatures object
			// *** should be last in this method because of auctionCount ***
			uf.addAuction(null, bidderBidCount.get(bidderId), (int)untilEndMin); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject  
		}
		
		// record who won the auction
		if (userFeaturesMap.containsKey(auctionObject.winnerId))
			userFeaturesMap.get(auctionObject.winnerId).addWonAuction();
		
		// record bid counts, amounts and increments
		recordBids(bidList);
	}
	
	private String simpleCategory(String category) {
		return category.split("/")[1];
	}
	
//	private void go() {
//		try {
//
//			Connection connection = DatabaseConn.getConnection();
//			
////		    auctionBidFrequencies(connection, 0);
//		    avgBidPerUser(connection);
//		    
//		} catch (SQLException e) {
//		    // Could not connect to the database
//			e.printStackTrace();
//		}
//	}
//	
//	private void avgBidPerUser(Connection conn) throws SQLException {
//		PreparedStatement pstmt = conn.prepareStatement("SELECT a.bidderId, count(a.listingId), AVG(bidCounts) FROM " +  
//			    "(SELECT listingId, bidderId, count(*) bidCounts FROM bids GROUP BY bidderId, listingId) as a " + 
//			    "GROUP BY a.bidderId ORDER BY AVG(bidCounts) DESC;");
//    	ResultSet rs = pstmt.executeQuery();
//    	
//    	// bins of size 0.5
//    	int[] bins = new int[40];
//    	while (rs.next()) {
//    		double avgBids = rs.getDouble("AVG(bidCounts)");
//    		int binNum = (int)(avgBids/0.5);
//    		bins[binNum]++;
//    	}
//
//    	Integer[] binNames=  new Integer[40];
//    	for (int i = 0; i < 40; i++) {
//    		double binNum = (double)(i);
//    		binNames[i] = i;
//    	}
//    	
//    	makeChart(bins, binNames, "Bids Per 2 Auctions", "Number of Bids", "Frequency");
//	}
	
}
