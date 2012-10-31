package dataAnalysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.peterbloem.powerlaws.Discrete;

import simulator.database.DBConnection;

public class TestAuctionData {
	
	public static void main(String[] args) {
		TestAuctionData test = new TestAuctionData();
		test.test();
	}
	
	private void test() {
		List<Integer> data = getData();
		
		Discrete model = Discrete.fit(data).fit();
		double exponent = model.exponent();
		double xMin = model.xMin();
		System.out.println("exponent: " + exponent + ", xMin: " + xMin);
		
		double significance = model.significance(data, 10);
		System.out.println("significance: " + significance);
	}
	
	private List<Integer> getData() {
		Connection conn = DBConnection.getTrademeConnection();
		try {
			ResultSet rs = conn.createStatement().executeQuery(
					"SELECT u.userId, COUNT(DISTINCT winnerId) distinctSoldTo FROM users u " +
					"JOIN auctions a ON u.userId=a.sellerId " +
					"WHERE a.winnerId IS NOT NULL AND purchasedWithBuyNow=0 " +
					"GROUP BY u.userId;");
			
			List<Integer> data = new ArrayList<Integer>();
			while(rs.next()) {
				data.add(rs.getInt("distinctSoldTo"));
			}
			
			return data;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
