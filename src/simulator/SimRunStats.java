package simulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import simulator.database.DatabaseConnection;


public class SimRunStats {

	public static void main(String[] args) throws SQLException {
		go();
	}
	
	private static void go() throws SQLException {
		Connection conn = DatabaseConnection.getSimulationConnection();
		
		int numberOfAuctions = singleIntQuery(conn, "SELECT COUNT(*) FROM auctions WHERE endTime IS NOT NULL;");
		System.out.println("Number of auctions: " + numberOfAuctions);
		
		int numberOfBids = singleIntQuery(conn, "SELECT COUNT(*) FROM bids;");
		System.out.println("Number of bids: " + numberOfBids);
		
		System.out.println("Bids per Auction = " + (double) numberOfBids/numberOfAuctions);
	}
	
	private static int singleIntQuery(Connection conn, String query) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(query);
		ResultSet rs = pstmt.executeQuery();
		rs.next();
		return rs.getInt(1);
	}
}
