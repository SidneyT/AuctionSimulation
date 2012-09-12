package createUserFeatures.util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import simulator.database.DBConnection;

import createUserFeatures.BuildUserFeatures;



public class RowFixes {

	public static void main(String[] args) {
//		new RowFixes().stringTest();
		new RowFixes().fixCategoryColumn();
	}

	private void stringTest() {
		String s1 = "Cars-bikes-boats/Car-parts-accessories/Performance/Gauges";
		s1 = s1.replaceFirst("Cars-bikes-boats/", "Trade-Me-Motors/");
		System.out.println(s1);
	}

	/**
	 * Change "Cars-bikes-boats/" prefix in category column in the Auctions table to "Trade-Me-Motors/"
	 */
	private void fixCategoryColumn() {
		
		try {
			Connection conn = DBConnection.getTrademeConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM auctions WHERE category LIKE '%Cars-bikes-boats%';");
			ResultSet rs = pstmt.executeQuery();

			BuildUserFeatures.printResultSet(rs);
			
			Map<Integer, String> idsOfRowsWithWrongName = new HashMap<Integer, String>();
			while (rs.next()) {
				idsOfRowsWithWrongName.put(rs.getInt("listingId"), rs.getString("category"));
			}
			System.out.println(idsOfRowsWithWrongName);
			
			PreparedStatement pstmt2 = conn.prepareStatement("UPDATE auctions SET category=? WHERE listingId=?;");
			for (int id : idsOfRowsWithWrongName.keySet()) {
				pstmt2.setString(1, idsOfRowsWithWrongName.get(id).replaceFirst("Cars-bikes-boats/", "Trade-Me-Motors/"));
				pstmt2.setInt(2, id);
				pstmt2.execute();
			}
			
			conn.prepareStatement("");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
}
