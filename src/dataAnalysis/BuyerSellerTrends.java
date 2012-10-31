package dataAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import simulator.database.DBConnection;
import util.IncrementalSD;

/**
 * Queries for the analysis of seller behaviour
 */
public class BuyerSellerTrends {
	public static void main(String[] args) throws SQLException {
		new BuyerSellerTrends().buyerCategorySpread();
	}
	
	private void countSuccessfulAuctions() {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			Statement stmt = conn.createStatement();

			ResultSet rs1 = stmt.executeQuery("SELECT sellerId, COUNT(*) as soldWithAuction FROM auctions a " +
					"JOIN users u ON a.sellerId=u.userId " +
					"WHERE a.winnerId IS NOT NULL AND a.purchasedWithBuyNow=0 " +
					"GROUP BY a.sellerId;");
			HashMap<Integer, Integer> soldWithAuctionCounts = new HashMap<>();
			while (rs1.next()) {
				int userId = rs1.getInt("sellerId");
				int soldWithAuction = rs1.getInt("soldWithAuction");
				soldWithAuctionCounts.put(userId, soldWithAuction);
			}

			ResultSet rs2 = stmt.executeQuery("SELECT sellerId, COUNT(*) as soldWithBuyNow FROM auctions a " +
					"JOIN users u ON a.sellerId=u.userId " +
					"WHERE a.winnerId IS NOT NULL AND a.purchasedWithBuyNow=1 " +
					"GROUP BY a.sellerId;");
			HashMap<Integer, Integer> soldWithBuyNowCounts = new HashMap<>();
			while (rs2.next()) {
				int userId = rs2.getInt("sellerId");
				int soldWithBuyNow = rs2.getInt("soldWithBuyNow");
				soldWithBuyNowCounts.put(userId, soldWithBuyNow);
			}

			
			Set<Integer> allUserIds = new HashSet<>();
			allUserIds.addAll(soldWithBuyNowCounts.keySet());
			allUserIds.addAll(soldWithAuctionCounts.keySet());
			
			try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("sellerAuctionCounts.csv"), Charset.defaultCharset())) {
				bw.append("userId, soldWithAuction, soldWithBuyNow");
				bw.newLine();
				for (Integer userId : allUserIds) {
					bw.append(userId + ",");
					if (soldWithAuctionCounts.containsKey(userId))
						bw.append(soldWithAuctionCounts.get(userId) + ",");
					else
						bw.append(0 + ",");
					if (soldWithBuyNowCounts.containsKey(userId))
						bw.append(soldWithBuyNowCounts.get(userId) + "");
					else
						bw.append(0 + "");
					bw.newLine();
				}
				bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Finds (sellerId, auctions, averagePrice, priceSD, categories) for each seller.
	 * For finding trends in category compared to number of auctions or average auction price.
	 * @throws SQLException
	 */
	private void sellerCategorySpread() throws SQLException {
		Connection conn = DBConnection.getTrademeConnection();
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT u.userId, a.listingId, a.winnerId, a.category, b1.amount " +
				"FROM users u " +
				"JOIN auctions a ON u.userId=a.sellerId " +
				"JOIN bids b1 ON a.listingId=b1.listingId " +
				"LEFT JOIN bids b2 ON b1.listingId=b2.listingId AND b1.amount < b2.amount " +
				"WHERE a.winnerId IS NOT NULL AND purchasedWithBuyNow=0 AND b2.amount IS NULL;");
		
		Multimap<Integer, String> categoryCount = TreeMultimap.create();
		HashMap<Integer, IncrementalSD> averagePrices = new HashMap<>();
		
		while (rs.next()) {
			int sellerId = rs.getInt("userId");
			int finalPrice = rs.getInt("amount");
			int winnerId = rs.getInt("winnerId");
			String category = rs.getString("category");
			
			if (!averagePrices.containsKey(sellerId))
				averagePrices.put(sellerId, new IncrementalSD());
			averagePrices.get(sellerId).addNext(finalPrice);
			
			categoryCount.put(sellerId, AnalyseAuctionCategories.shorten(category));
		}
		
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("sellerCategorySpread.csv"), Charset.defaultCharset())) {
			bw.append("userId,auctions,averagePrice,priceSD,categories");
			bw.newLine();
			
			for (Integer userId : averagePrices.keySet()) {
				bw.append(userId + ",");
				IncrementalSD averagePrice = averagePrices.get(userId);
				bw.append(averagePrice.getNumElements() + ",");
				bw.append(averagePrice.getAverage() + ",");
				bw.append(averagePrice.getSD() + ",");
				bw.append(categoryCount.get(userId).size() + "");
				
				bw.newLine();
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void buyerCategorySpread() throws SQLException {
		Connection conn = DBConnection.getTrademeConnection();
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT u.userId, a.listingId, a.winnerId, a.category, b1.amount " +
				"FROM users u " +
				"JOIN auctions a ON u.userId=a.sellerId " +
				"JOIN bids b1 ON a.listingId=b1.listingId " +
				"LEFT JOIN bids b2 ON b1.listingId=b2.listingId AND b1.amount < b2.amount " +
				"WHERE a.winnerId IS NOT NULL AND purchasedWithBuyNow=0 AND b2.amount IS NULL;");
		
		Multimap<Integer, String> categoryCount = TreeMultimap.create();
		HashMap<Integer, IncrementalSD> averagePrices = new HashMap<>();
		
		while (rs.next()) {
			int sellerId = rs.getInt("userId");
			int finalPrice = rs.getInt("amount");
			int winnerId = rs.getInt("winnerId");
			String category = rs.getString("category");
			
			if (!averagePrices.containsKey(winnerId))
				averagePrices.put(winnerId, new IncrementalSD());
			averagePrices.get(winnerId).addNext(finalPrice);
			
			categoryCount.put(winnerId, AnalyseAuctionCategories.shorten(category));
		}
		
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("winnerCategorySpread.csv"), Charset.defaultCharset())) {
			bw.append("userId,auctions,averagePrice,priceSD,categories");
			bw.newLine();
			
			for (Integer winnerId : averagePrices.keySet()) {
				bw.append(winnerId + ",");
				IncrementalSD averagePrice = averagePrices.get(winnerId);
				bw.append(averagePrice.getNumElements() + ",");
				bw.append(averagePrice.getAverage() + ",");
				bw.append(averagePrice.getSD() + ",");
				bw.append(categoryCount.get(winnerId).size() + "");
				
				bw.newLine();
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
