package simulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import simulator.database.SimulationDbConn;


public class CalculateStats {
	public static void main(String[] args) {
		calculateStats();
	}
	
	public static void calculateStats() {
		try {
			Connection conn = SimulationDbConn.getConnection();
			int auctionCount = getAuctionCount(conn);
			int[] bidFrequencies = getUserBidFrequencies(conn);
			
			System.out.println("number of auctions: " + auctionCount);
			int bidCount = getBidCount(conn);
			System.out.println("number of bids: " + bidCount);
			int bidCountTrimmed = getBidCountTrimmed(conn);
			System.out.println("number of bids trimmed: " + bidCountTrimmed);
			System.out.println("number of of bidders created: " + getBiddersCreatedCount(conn));
			int userCount = getBidderCount(conn);
			System.out.println("number that bid: " + userCount);
			int userCountTrimmed = getBidderCountTrimmed(conn);
			System.out.println("number that bid trimmed: " + getBidderCountTrimmed(conn));
			System.out.println("bids per auction: " + (double) bidCount/auctionCount);
			System.out.println("unique bidder per auction: " + getAverageNumberOfBiddersPerAuction(conn));
			System.out.println("unique bidder per auction trimmed: " + getAverageNumberOfBiddersPerAuctionTrimmed(conn));
			System.out.println("bids per user: " + (double) bidCount/userCount);
			System.out.println("bids per user trimmed: " + (double) bidCountTrimmed/userCountTrimmed);
			System.out.println("user bid frequencies: " + Arrays.toString(bidFrequencies));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static double getAverageNumberOfBiddersPerAuction(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT AVG(sub.c) FROM (SELECT COUNT(DISTINCT b.bidderId) as c FROM bids as b JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL GROUP BY a.listingId) as sub;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		double numAuctions = results.getDouble("AVG(sub.c)");
		return numAuctions;
	}

	private static double getAverageNumberOfBiddersPerAuctionTrimmed(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT AVG(sub.c) FROM (SELECT COUNT(DISTINCT b.bidderId) as c FROM (SELECT c.* FROM bids AS c " +
				"LEFT JOIN bids AS d " +
    			"ON c.listingId=d.listingId AND c.amount <= d.amount " +
    			"GROUP BY c.bidId " +
				"HAVING COUNT(*) <= 20) " +
				"as b JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL GROUP BY a.listingId) as sub;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		double numAuctions = results.getDouble("AVG(sub.c)");
		return numAuctions;
	}

	private static double getAverageNumberOfAuctionsPerUser(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT AVG(sub.c) FROM (SELECT COUNT(DISTINCT(a.listingId)) as c FROM bids as b JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL GROUP BY bidderId) as sub;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numAuctions = results.getInt("COUNT(*)");
		return numAuctions;
	}
	
	private static int getBiddersCreatedCount(Connection conn) throws SQLException {
		// number of users that have made a bid in an auction that has completed
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users as u WHERE userType LIKE '%bidders%';");
//		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(DISTINCT bidderId) FROM bids;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numUsers = results.getInt("COUNT(*)");
		return numUsers;
	}
	
	private static int getBidderCount(Connection conn) throws SQLException {
		// number of users that have made a bid in an auction that has completed
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users as u WHERE EXISTS (SELECT DISTINCT(bidderId) FROM bids as b INNER JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL AND u.userId=b.bidderId);");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numUsers = results.getInt("COUNT(*)");
		return numUsers;
	}
	
	private static int getBidderCountTrimmed(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(DISTINCT bidderId) FROM (SELECT a.* FROM bids AS a " + 
				"LEFT JOIN bids AS a2 " +
				"ON a.listingId=a2.listingId AND a.amount <= a2.amount " + 
				"GROUP BY a.bidId " +
				"HAVING COUNT(*) <= 20) as c; ");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numUsers = results.getInt("COUNT(DISTINCT bidderId)");
		return numUsers;
	}
	
	private static int[] getUserBidFrequencies(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT f, COUNT(*) FROM (SELECT COUNT(*) as f FROM bids GROUP BY bidderId) as b GROUP BY f ORDER BY f DESC;");
		ResultSet results = pstmt.executeQuery();

		int[] result = null;
		
		while (results.next()) {
			int bidFrequency = results.getInt("f");
			int userFrequency = results.getInt("COUNT(*)");
			if (result == null) {
				result = new int[bidFrequency + 1];
			}
			
			result[bidFrequency] = userFrequency; 
		}
		return result;
	}
	
	private static int getAuctionCount(Connection conn) throws SQLException {
		// number of auctions that have ended
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM auctions WHERE endTime IS NOT NULL AND winnerId IS NOT NULL ;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numAuctions = results.getInt("COUNT(*)");
		return numAuctions;
	}
	
	private static int getBidCount(Connection conn) throws SQLException {
		// number of bids for auctions that have ended (i.e. have an endTime)
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM bids as b INNER JOIN auctions as a ON b.listingId=a.listingId WHERE a.endTime IS NOT NULL;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numBids = results.getInt("COUNT(*)");
		return numBids;
	}
	
	private static int getBidCountTrimmed(Connection conn) throws SQLException {
		// number of bids for auctions that have ended (i.e. have an endTime)
		PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM (SELECT a.* FROM bids AS a " + 
				"LEFT JOIN bids AS a2 " +
				"ON a.listingId=a2.listingId AND a.amount <= a2.amount " + 
				"GROUP BY a.bidId " + 
				"HAVING COUNT(*) <= 20) as c JOIN auctions as auc ON c.listingId=auc.listingId WHERE auc.endTime IS NOT NULL;");
		ResultSet results = pstmt.executeQuery();
		results.next();
		int numBids = results.getInt("COUNT(*)");
		return numBids;
	}

}
