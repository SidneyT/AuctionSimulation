package shillScore.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.sql.CallableStatement;
import java.util.Date;

import simulator.database.DatabaseConnection;
import util.IncrementalAverage;


public class ShillWinLossPrice {

	/**
	 * Calculates the average final sale price of auctions with and without shills, and the
	 * number of wins and losses by shills.
	 * Used to measure see whether the shills actually do their job of raising the final price,
	 * and see how often the shills win the auctions.
	 * 
	 * Written to <code>shillingResults/comparisons/shillWinLossCounts.csv<code>. 
	 */
	public static void writeToFile(String label) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "comparisons", "winLossCounts.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)){

			bw.append(new Date().toString());
			bw.append(",");
			bw.append(label);
			bw.append(",");
			
			try {
				// number of auctions won by the shill, and the price
				IncrementalAverage shillWinAvg = findShillWinAuctions();
				bw.append(shillWinAvg.getNumElements() + "," + shillWinAvg.getAverage() + ",");

				// number of auctions lost by the shill, and the price
				IncrementalAverage shillLossAvg = findShillLossAuctions();
				bw.append(shillLossAvg.getNumElements() + "," + shillLossAvg.getAverage() + ",");

				// number of non-shill auctions lost by the shill, and the price
				IncrementalAverage nonShillShillWinAvg = findNonShillAuctionsWinByShills();
				bw.append(nonShillShillWinAvg.getNumElements() + "," + nonShillShillWinAvg.getAverage() + ",");

				// number of non-shill auctions won by a non-shill, and the price
				IncrementalAverage nonShillNonShillWinAvg = findNonShillAuctionsWinsByNonShills();
				bw.append(nonShillNonShillWinAvg.getNumElements() + "," + nonShillNonShillWinAvg.getAverage());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Find the number of shill auctions won by shills, and the average finalPrice/trueValuation ratio
	 * for those auctions.
	 */
	private static IncrementalAverage findShillWinAuctions() throws SQLException {
		IncrementalAverage incAvg = new IncrementalAverage();
		Connection conn = DatabaseConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice " +  
				"FROM auctions as a JOIN itemtypes as i ON a.itemTypeId=i.id JOIN bids as b ON a.listingId=b.listingId JOIN users as seller ON seller.userId=a.sellerId JOIN users as bidder ON bidder.userId=a.winnerId " +  
				"WHERE seller.userType LIKE '%Puppet%' AND bidder.userType LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");
			
			double ratio = (double) finalPrice / trueValuation;
			incAvg.incrementalAvg(ratio);
		}
		return incAvg;
	}
	
	/**
	 * Find the number of shill auctions lost by shills, and the average finalPrice/trueValuation ratio
	 * for those auctions.
	 */
	private static IncrementalAverage findShillLossAuctions() throws SQLException {
		IncrementalAverage incAvg = new IncrementalAverage();
		Connection conn = DatabaseConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice " +  
				"FROM auctions as a JOIN itemtypes as i ON a.itemTypeId=i.id JOIN bids as b ON a.listingId=b.listingId JOIN users as seller ON seller.userId=a.sellerId JOIN users as bidder ON bidder.userId=a.winnerId " +  
				"WHERE seller.userType LIKE '%Puppet%' AND bidder.userType NOT LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");
			
			double ratio = (double) finalPrice / trueValuation;
			incAvg.incrementalAvg(ratio);
		}
		return incAvg;
	}
	
	private static IncrementalAverage findNonShillAuctionsWinByShills() throws SQLException {
		IncrementalAverage incAvg = new IncrementalAverage();
		Connection conn = DatabaseConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice, seller.userType as sellerType, bidder.userType as winnerType " + 
				"FROM auctions as a JOIN itemtypes as i ON a.itemTypeId=i.id JOIN bids as b ON a.listingId=b.listingId JOIN users as seller ON seller.userId=a.sellerId JOIN users as bidder ON bidder.userId=a.winnerId " +
				"WHERE seller.userType NOT LIKE '%Puppet%' AND bidder.userType LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");

			double ratio = (double) finalPrice / trueValuation;
			incAvg.incrementalAvg(ratio);
		}
		return incAvg;
	}
	
	/**
	 * Find the number of non-shill auctions, and the average finalPrice/trueValuation ratio
	 * for those auctions.
	 */
	private static IncrementalAverage findNonShillAuctionsWinsByNonShills() throws SQLException {
		IncrementalAverage incAvg = new IncrementalAverage();
		Connection conn = DatabaseConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice, seller.userType as sellerType, bidder.userType as winnerType " +
				"FROM auctions as a JOIN itemtypes as i ON a.itemTypeId=i.id JOIN bids as b ON a.listingId=b.listingId JOIN users as seller ON seller.userId=a.sellerId JOIN users as bidder ON bidder.userId=a.winnerId " +
				"WHERE seller.userType NOT LIKE '%Puppet%' AND bidder.userType NOT LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");

			double ratio = (double) finalPrice / trueValuation;
			incAvg.incrementalAvg(ratio);
		}
		return incAvg;
	}
	
}
