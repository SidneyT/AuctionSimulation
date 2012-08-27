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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

import simulator.database.DatabaseConnection;
import util.Util;

/**
 *	Builds UserFeature object using scraped TradeMe data.
 */
public class BuildSimFeatures extends BuildUserFeatures{

	private static final Logger logger = Logger.getLogger(BuildSimFeatures.class);
	
	public static void main(String[] args) {
		String features = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15"; // all
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
//		String features = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
//		String features = "-3ln-10-5-6ln-11";
		
		List<Feature> allFeatures = Arrays.<Feature>asList(Features.values());
		
		boolean trim = true;
		BuildSimFeatures bf = new BuildSimFeatures(trim);
		writeToFile(bf.build().values(), allFeatures, Paths.get("BuildTrimmedSimFeatures" + features + ".csv"));
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
		System.out.println("Finished.");
	}
	
	public BuildSimFeatures(boolean trim) {
		this.trim = trim;
	}
	
	/**
	 * calls constructUserFeatures with default query
	 */
	public TreeMap<Integer, UserFeatures> build() {
		// order of bids will be in the order they were made in, (since amount is in ascending order)
		String query = "SELECT * FROM auctions as a JOIN bids as b ON a.listingId=b.listingId " +
				"WHERE endTime IS NOT NULL ORDER BY a.listingId, amount ASC;";
		return constructUserFeatures(query);
	}
	
	private static final long zeroTime = (long) 946684800 * 1000; // time since epoch at year 1/1/2000
	private static final long timeUnitMillis = 5 * 60 * 1000;
	private Date convertTimeunitToTimestamp(long timeUnit) {
		return new Date(zeroTime + timeUnit * timeUnitMillis);
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public TreeMap<Integer, UserFeatures> constructUserFeatures(String query) {
		try {
			Connection conn = DatabaseConnection.getSimulationConnection();
			
			PreparedStatement bigQuery = conn.prepareStatement(query);
			ResultSet bigRS = bigQuery.executeQuery();

			int lastListingId = -1;
			SimAuctionObject ao = null;
			
			// split group the bids by auctions, and put them into a list
			List<BidObject> bidList = new ArrayList<BidObject>();
			while (bigRS.next()) {
				int currentListingId = bigRS.getInt("listingId");
				if (lastListingId != currentListingId) { // new auction
					if (lastListingId != -1) { // if this is not the first auction,
						// process the stuff from the previous auction
						processAuction(ao, trim ? trimTo20(bidList) : bidList);
						// clear the lists
						bidList.clear();
					}

					lastListingId = currentListingId; // remember id of the new auction

					// record the auction information for the new row
					ao = new SimAuctionObject(currentListingId, bigRS.getInt("itemTypeId"), convertTimeunitToTimestamp(bigRS.getLong("endTime")), bigRS.getInt("winnerId"));
				}
				bidList.add(new BidObject(bigRS.getInt("bidderId"), bigRS.getInt("amount"), convertTimeunitToTimestamp(bigRS.getLong("time"))));
			}
			processAuction(ao, trim ? trimTo20(bidList) : bidList); // process the bidList for the last remaining auction
			
			userRep(conn);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this.userFeaturesMap;
	}
	
	private static List<BidObject> trimTo20(List<BidObject> bidList) {
		if (bidList.size() > 20)
			return new ArrayList<>(bidList.subList(bidList.size() - 20, bidList.size())); 
		else
			return bidList;
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
	
	private int trimmedCounter = 0;
	/**
	 * @param auctionObject
	 * @param bidList list of bids for this auction in chronological order
	 */
	private void processAuction(SimAuctionObject auctionObject, List<BidObject> bidList) {
//		System.out.println(auctionObject + ", " + bidObjects);

		// count the number of bids made for each user in the bidList
		// and record the time of the last bid made for each user in the bidList 
 		Map<Integer, Integer> bidderBidCount = new HashMap<Integer, Integer>(); // Map<UserId, Count>
		Map<Integer, Date> bidderLastBidTime = new HashMap<Integer, Date>(); // Map<UserId, Date>
		for (BidObject bo : bidList) {
			int bidderId = bo.bidderId;
			if (bidderBidCount.get(bidderId) == null) {
				bidderBidCount.put(bidderId, 0);
			}
			bidderBidCount.put(bidderId, bidderBidCount.get(bidderId) + 1);
			bidderLastBidTime.put(bidderId, bo.time);
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
			// *** should be last in this method because of auctionCount increment ***
			uf.addAuction(null, bidderBidCount.get(bidderId), (int)untilEndMin); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject  
		}
		
		// record who won the auction
		if (userFeaturesMap.containsKey(auctionObject.winnerId))
			userFeaturesMap.get(auctionObject.winnerId).addWonAuction();
		
		// record bid counts, amounts and increments
		recordBids(bidList);
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
