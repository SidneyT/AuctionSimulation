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
		return build(query);
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
		
		TreeMap<Integer, UserFeatures> userFeaturesCol = build(query); // contains userFeatures from all clusters
		
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
		return build(query);
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public TreeMap<Integer, UserFeatures> build(String query) {
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
				if (lastListingId == -1) {
					lastListingId = currentListingId;
					ao = new TMAuctionObject(currentListingId, this.simpleCategory(bigRS.getString("category")), bigRS.getTimestamp("endTime"), bigRS.getInt("winnerId"));
				}
				if (lastListingId != currentListingId) { // new auction
					lastListingId = currentListingId;
					// process the stuff from the previous auction
					processAuction(ao, bidList);
					// record the auction information for the new row
					ao = new TMAuctionObject(currentListingId, this.simpleCategory(bigRS.getString("category")), bigRS.getTimestamp("endTime"), bigRS.getInt("winnerId"));
					
					// clear the lists
					bidList.clear();
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
	
	private void processAuction(TMAuctionObject auctionObject, List<BidObject> bidList) {
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
			// *** should be last in this method because of auctionCount ***
			uf.addAuction(null, bidderBidCount.get(bidderId), (int)untilEndMin); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject  
		}
		
		// record who won the auction
		if (userFeaturesMap.containsKey(auctionObject.winnerId))
			userFeaturesMap.get(auctionObject.winnerId).addWonAuction();
		
		// record bid counts, amounts and increments
		recordBids(bidList);
	}
	
	private void recordBids(List<BidObject> bidList) {
		BidObject firstBid = bidList.get(0);
		BidObject lastBid = bidList.get(bidList.size() - 1);
		userFeaturesMap.get(firstBid.bidderId).addBid(firstBid.amount, -1, lastBid.amount);
		if (bidList.size() >= 1) {
			// record bid increments
			for (int i = 1; i < bidList.size(); i++) {
				BidObject bo = bidList.get(i);
				userFeaturesMap.get(bo.bidderId).addBid(bo.amount, bidList.get(i-1).amount, lastBid.amount);
			}
		}
	}
	
	private void recordBidPeriods(Date auctionEnd, List<BidObject> bidList) {
		// seperate the bidList into a list for each user
		Map<Integer, List<BidObject>> bidListsByUser = seperateBidListByUser(bidList);
		
		// record when the first bid was made by each user
		for (int bidderId : bidListsByUser.keySet()) {
			BidObject firstBid = bidListsByUser.get(bidderId).get(0); // first bid in the list must be first, since it's in order
			long firstBidTime = Util.timeDiffInMin(auctionEnd, firstBid.time);
			UserFeatures user = userFeaturesMap.get(bidderId);
			user.firstBidTimes = Util.incrementalAvg(user.firstBidTimes, user.getAuctionCount(), firstBidTime);
//			System.out.println("firstBidTimes: " + user.firstBidTimes);
		}
		
		updateSelfBidInterval(bidListsByUser);
		updateAnyBidInterval(bidList, bidListsByUser);
		
		// find the proportion of bids made in the Beginning, Middle and End of an auction
		Map<Integer, double[]> userBidPeriods = findBidPeriods(auctionEnd, bidList);
		// record the proportion in the UserFeatures object
		for (int bidderId : userBidPeriods.keySet()) {
//			UserFeatures.incrementalAvg(currAvg, currNumElements, nextValue)
			double[] bidPeriod = userFeaturesMap.get(bidderId).bidPeriods; // get the stored bidPeriod
			double[] newBidPeriod = userBidPeriods.get(bidderId); // get the new bidPeriod
			for (int i = 0; i < 3; i++) {
				// update stored bidPeriod using new BidPeriod
				// averages are weighted using the number of bids made in each auction (in index 3)
				bidPeriod[i] = Util.incrementalAvg(bidPeriod[i], (int) bidPeriod[3], newBidPeriod[i], (int) newBidPeriod[3]); 
			}
			bidPeriod[3] += newBidPeriod[3];
		}
		
		// time from first bid to the end of the auction, and the number of bids
//		System.out.print((auctionEnd.getTime() - bidList.get(0).time.getTime()) / 60000 + "," + bidList.size() + ";");
//		System.out.print(userBidPeriods.size() + ",");
	}
	
	/**
	 * calculate & record the interval of time between bids made by anyone
	 * @param bidList
	 * @param bidListsByUser
	 */
	private void updateAnyBidInterval(List<BidObject> bidList, Map<Integer, List<BidObject>> bidListsByUser) {
		// if there's only 1 bid in the auction, there is no interval.
		if (bidList.size() > 1) {
			// Map<bidderId, average>
			Map<Integer, Integer> intervalTotal = new HashMap<>();
			for (int bidderId : bidListsByUser.keySet()) { // initialise map with all bidders from the auction
				intervalTotal.put(bidderId, (int) 0);
			}
			
			for (int i = 1; i < bidList.size(); i++) {
				int bidderId = bidList.get(i).bidderId;
				long interval = Util.timeDiffInMin(bidList.get(i).time, bidList.get(i-1).time);
				assert (interval >= 0);
				intervalTotal.put(bidderId, (int) (intervalTotal.get(bidderId) + interval));
			}
			
			// update userFeature objects with intervalAverage and weights
			int bidderWhoBidFirst = bidList.get(0).bidderId;
			for (int bidderId : bidListsByUser.keySet()) {
				UserFeatures user = userFeaturesMap.get(bidderId); 
				int numberOfIntervals;
				// the bidder who bid first does not have a bid interval for that bid, since there is no preceeding bid
				if (bidderId == bidderWhoBidFirst) {
					numberOfIntervals = bidListsByUser.get(bidderId).size() - 1;
				} else {
					numberOfIntervals = bidListsByUser.get(bidderId).size();
				}
				
				if (numberOfIntervals >= 1) // if the only bid made was the first bid for the auction, there is no bid interval for this user
					user.anyBidInterval = Util.incrementalAvg(user.anyBidInterval, user.bidCount, (double) intervalTotal.get(bidderId) / numberOfIntervals, numberOfIntervals);
			}
			
		}
		
	}

	/**
	 * calculate & record the interval of time between bids made by the SAME USER
	 * @param bidListsByUser
	 */
	private void updateSelfBidInterval(Map<Integer, List<BidObject>> bidListsByUser) {
		for (int bidderId : bidListsByUser.keySet()) {
			List<BidObject> userBidList = bidListsByUser.get(bidderId);
			// if there is only 1 bid in the list, there is no interval.
			if (userBidList.size() > 1) {
				long intervalTotal = 0;
				UserFeatures user = userFeaturesMap.get(bidderId);
				for (int i = 1; i < userBidList.size(); i++) {
					long interval = Util.timeDiffInMin(userBidList.get(i).time, userBidList.get(i-1).time);
					intervalTotal += interval;
					assert (interval >= 0);
				}
				
				user.selfBidInterval = Util.incrementalAvg(user.selfBidInterval, user.bidCount, (double) intervalTotal / (userBidList.size() - 1), userBidList.size() - 1);
//				System.out.println("selfBidInterval: " + user.selfBidInterval);
			}
		}
	}

	/**
	 * Find the number of bids that fall into the bid periods BEG, MID, END for the bidList.
	 * 
	 * @param auctionEnd 
	 * @param bidList
	 * 
	 * @return Map containing mapping between userId and a 4 element array of doubles.  
	 * The first 3 elements are the number of bids made in BEG, MID and END of the auction.
	 * The 4th element counts the number of bids that user has in bidList.
	 * 
	 */
	private Map<Integer, double[]> findBidPeriods(Date auctionEnd, List<BidObject> bidList) {
		Map<Integer, double[]> dist = new HashMap<Integer, double[]>();
		long length = Util.timeDiffInMin(auctionEnd, bidList.get(0).time);
//		System.out.println("auction length: " + length);
		
		for (BidObject bo : bidList) {
//			long bidMinsBeforeEnd = timeDiffInMin(bidList.get(bidList.size() - 1).time, bo.time);
			long bidMinsBeforeEnd = Util.timeDiffInMin(auctionEnd, bo.time);
			userFeaturesMap.get(bo.bidderId).bidMinsBeforeEnd.add(bidMinsBeforeEnd);
			
			// time of bid as a fraction from the time of the first bid in the auction to the end time
			double fractionToEnd = ((double) Util.timeDiffInMin(bo.time, bidList.get(0).time))/length;
			userFeaturesMap.get(bo.bidderId).bidTimesBeforeEnd.add(fractionToEnd);
			
			// store the bidPeriod this fractionToEnd corresponds to
			double[] userDist = dist.get(bo.bidderId);
			if (userDist == null) {
				userDist = new double[4];
				Arrays.fill(userDist, 0);
				dist.put(bo.bidderId, userDist);
			}
			
			BidPeriod period = findBidPeriod(fractionToEnd);
			for (int i = 0; i < 3; i++) { // increment the correct bin: beg, mid or end
				if (i == period.getI()) {
					userDist[i] = Util.incrementalAvg(userDist[i], (int) userDist[3], 1);
				} else {
					userDist[i] = Util.incrementalAvg(userDist[i], (int) userDist[3], 0);
				}
			}
			userDist[3]++; // increment count
		}
		
		// print out the proportions found for each user for this bidList
//		for (int id : dist.keySet()) {
//			System.out.println(id + ":" + Arrays.toString(dist.get(id)));
//		}
		return dist;
	}

	/**
	 * @param bidList list of bids from one auction in chronological order by bid submission date
	 * @return Map of lists with keys as bidderIds and values as a list of bids made by that bidder for this auction. Each list
	 * 			is in chronological order.
	 */
	private Map<Integer, List<BidObject>> seperateBidListByUser(List<BidObject> bidList) {
		// Map<bidderId, bidObjects>
		Map<Integer, List<BidObject>> bidLists = new HashMap<>();
		
		for (BidObject bid : bidList) {
			Util.mapListAdd(bidLists, bid.bidderId, bid);
		}
		
		return bidLists;
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
