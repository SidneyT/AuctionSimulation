package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

import shillScore.BuildShillScore.BidObject;
import simulator.database.DatabaseConnection;

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
	
	private static final long zeroTime = (long) 946684800 * 1000; // time since epoch at year 1/1/2000
	private static final long timeUnitMillis = 5 * 60 * 1000;
	public static Date convertTimeunitToTimestamp(long timeUnit) {
		return new Date(zeroTime + timeUnit * timeUnitMillis);
	}
	
	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public Map<Integer, UserFeatures> build() {
		try {
			Connection conn = DatabaseConnection.getSimulationConnection();
			
			Iterator<Pair<SimAuction, List<shillScore.BuildShillScore.BidObject>>> it = new SimAuctionGroupIterator(conn, trim).iterator();
			while (it.hasNext()) {
				Pair<SimAuction, List<shillScore.BuildShillScore.BidObject>> pair = it.next();
//				System.out.println("auction: " + pair.getKey() + ", bids: " + pair.getValue());
				processAuction(pair.getKey(), pair.getValue());
			}
			
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
		public SimAuctionGroupIterator(Connection conn, boolean trim) {
			try {
				Statement stmt = conn.createStatement();
				if (!trim)
					rs = stmt.executeQuery(
									"SELECT a.listingId, a.itemTypeId, a.sellerId, a.winnerId " +
		//							", u2.userType as sellerType" +
									", a.endTime, b.time as bidTime, b.amount as bidAmount, b.bidderId " +
//									", u1.userType as bidderType " +
									"FROM auctions as a " +
									"JOIN bids as b ON a.listingId=b.listingId " + 
//									"JOIN users as u1 ON b.bidderId=u1.userId " +
//									"JOIN users as u2 ON a.sellerId=u2.userId " +
									"WHERE endTime IS NOT NULL ORDER BY a.listingId, time ASC;"
							);
				else
					rs = stmt.executeQuery(
									"SELECT a.listingId, a.itemTypeId, a.sellerId, a.winnerId " +
//									", u2.userType sellerType, " +
									", a.endTime, b.time bidTime, b.amount as bidAmount, b.bidderId " +
//									", u1.userType as bidderType " +  
									"FROM bids b " +
									"JOIN auctions a ON a.listingId=b.listingId " +   
//									"JOIN users u1 ON b.bidderId=u1.userId " +
//									"JOIN users u2 ON a.sellerId=u2.userId " +
									"LEFT OUTER JOIN bids b2 ON (b.listingId = b2.listingId AND b.amount < b2.amount) " +
									"WHERE endTime IS NOT NULL " +
									"GROUP BY b.listingId, b.amount " + 
									"HAVING COUNT(*) < 20 " +
									"ORDER BY b.listingId ASC, b.amount ASC;"
							);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		private List<BidObject> bids = new ArrayList<>();
		private SimAuction auction = null;
		public Iterator<Pair<SimAuction, List<BidObject>>> iterator() {
			return new Iterator<Pair<SimAuction,List<BidObject>>>() {
				@Override
				public boolean hasNext() {
					return hasNext;
				}

				@Override
				public Pair<SimAuction, List<BidObject>> next() {
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
									Pair<SimAuction, List<BidObject>> resultPair = new Pair<>(auction, bids); 
									auction = new SimAuction(rs.getInt("listingId"), rs.getInt("winnerId"), rs.getInt("sellerId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("endTime")), rs.getInt("itemTypeId"));
									bids = new ArrayList<>();
									BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
									bids.add(bid);
									return resultPair;
								}
							} 
							// still going through bids from the same auction
							BidObject bid = new BidObject(rs.getInt("bidderId"), rs.getInt("listingId"), BuildSimFeatures.convertTimeunitToTimestamp(rs.getLong("bidTime")), rs.getInt("bidAmount"));
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
