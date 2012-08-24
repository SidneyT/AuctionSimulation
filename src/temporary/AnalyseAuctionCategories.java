package temporary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.TreeSet;

import simulator.database.DatabaseConnection;

public class AnalyseAuctionCategories {
	public static void main(String[] args) throws SQLException {
		new AnalyseAuctionCategories().run();
	}
	
	public void run() throws SQLException {
		Connection conn = DatabaseConnection.getTrademeConnection();
		
		PreparedStatement pstmt = conn.prepareStatement("SELECT category, COUNT(*) FROM auctions as a JOIN bids as b ON a.listingId=b.listingId AND a.purchasedWithBuyNow=0 GROUP BY category;");
		
		ResultSet rs = pstmt.executeQuery();
		
		HashSet<String> fullCategoryNames = new HashSet<>();
		while(rs.next()) {
			String category = rs.getString("category");
			fullCategoryNames.add(category);
		}
		TreeSet<String> shortCategoryNames = new TreeSet<>();
		for (String categoryName : fullCategoryNames) {
			shortCategoryNames.add(shorten(categoryName));
//			System.out.println(categoryName + ", " + shorten(categoryName));
		}
		for (String categoryName : shortCategoryNames) {
			PreparedStatement stmt = conn.prepareStatement("SELECT category, COUNT(DISTINCT a.listingId) FROM auctions as a JOIN bids as b ON a.listingId=b.listingId AND a.purchasedWithBuyNow=0 WHERE category LIKE ? OR category LIKE ?");
			stmt.setString(1, categoryName + "/%"); // e.g. Home/Mobile-phones/blahblah
			stmt.setString(2, categoryName); // e.g. Trade-Me-Motors/Trucks
			ResultSet result = stmt.executeQuery();
			result.next();
			int count = result.getInt("COUNT(DISTINCT a.listingId)");
			System.out.println(categoryName + ":" + count );
		}
	}
	
	private String shorten(String fullCategoryName) {
		String[] parts = fullCategoryName.replace("//", "/").split("/");
		assert(parts.length >= 2);
		
		if (parts[0].equals("Home"))
			return parts[0] + "/" + parts[1]
				//+ parts[2]
				;
		else
			return parts[0] + "/" + parts[1];
	}
	
}
