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

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import createUserFeatures.BuildUserFeatures.BidObject.BidObjectComparator;
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
//		String features = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15"; // all
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
//		String features = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
//		String features = "-3ln-10-5-6ln-11";
		
		List<Feature> features = Arrays.<Feature>asList(Features.values());
		
		boolean trim = true;
		BuildSimFeatures bf = new BuildSimFeatures(trim);
		writeToFile(bf.build().values(), features, Paths.get("BuildTrimmedSimFeatures" + Features.fileLabels(features) + ".csv"));
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
						processAuction(ao.endTime, ao.winnerId, trim ? trimTo20(bidList) : bidList);
						// clear the lists
						bidList.clear();
					}

					lastListingId = currentListingId; // remember id of the new auction

					// record the auction information for the new row
					ao = new SimAuctionObject(currentListingId, bigRS.getInt("itemTypeId"), convertTimeunitToTimestamp(bigRS.getLong("endTime")), bigRS.getInt("winnerId"));
				}
				bidList.add(new BidObject(bigRS.getInt("bidderId"), bigRS.getInt("amount"), convertTimeunitToTimestamp(bigRS.getLong("time"))));
			}
			processAuction(ao.endTime, ao.winnerId, trim ? trimTo20(bidList) : bidList); // process the bidList for the last remaining auction
			
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
	
}
