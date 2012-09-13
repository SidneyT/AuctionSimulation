package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import createUserFeatures.BuildUserFeatures.BidObject;

import simulator.database.DBConnection;

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
	
	public Map<Integer, UserFeatures> build(SimAuctionIterator simAuctionIterator) {
		Iterator<Pair<SimAuction, List<createUserFeatures.BuildUserFeatures.BidObject>>> it = simAuctionIterator.iterator();
		while (it.hasNext()) {
			Pair<SimAuction, List<createUserFeatures.BuildUserFeatures.BidObject>> pair = it.next();
//			System.out.println("auction: " + pair.getKey() + ", bids: " + pair.getValue());
			processAuction(pair.getKey(), pair.getValue());
		}
		
		for (UserObject user : simAuctionIterator.userRep()) {
			UserFeatures uf = this.userFeaturesMap.get(user.userId);
			if (uf == null)
				continue;
			uf.pos = user.posUnique;
			uf.neg = user.negUnique;
			uf.userType = user.userType;
		}
		
		return this.userFeaturesMap;
	}
//	private void userRep(Connection conn) throws SQLException {
//		PreparedStatement usersQuery = conn.prepareStatement(
//				"SELECT DISTINCT userId, posUnique, negUnique " +
//				"FROM users as u " +
//				";"
//				); 
//		ResultSet usersResultSet = usersQuery.executeQuery();
//		while (usersResultSet.next()) {
//			if (userFeaturesMap.containsKey(usersResultSet.getInt("userId")))
//				userFeaturesMap.get(usersResultSet.getInt("userId")).setRep(usersResultSet.getInt("posUnique"), usersResultSet.getInt("negUnique"));
//		}
//	}

	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public Map<Integer, UserFeatures> build() {
		try {
			Connection conn = DBConnection.getSimulationConnection();
			Map<Integer, UserFeatures> result = build(new SimAuctionDBIterator(conn, trim));
			conn.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
}
