package temporary;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.primitives.Doubles;

import simulator.database.DBConnection;
import util.Util;

public class AnalyseAuctionCategories {
	public static void main(String[] args) throws SQLException {
		new AnalyseAuctionCategories().run();
	}
	
	public void run() throws SQLException {
		Connection conn = DBConnection.getTrademeConnection();
		
		ArrayListMultimap<String, Integer> map = ArrayListMultimap.create();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT category, MAX(b.amount) as price FROM auctions as a JOIN bids as b ON a.listingId=b.listingId AND a.purchasedWithBuyNow=0 GROUP BY a.listingId;");
		while (rs.next()) {
			String shortened = shorten(rs.getString("category"));
			int price = rs.getInt("price");
			map.put(shortened, price);
		}
		
		Chart<Integer> chart = new Chart<>();
		StandardDeviation sd = new StandardDeviation(true);
		Mean mean = new Mean();
		for (String category : new TreeSet<>(map.keySet())) {
			List<Integer> prices = map.get(category);
			Collections.sort(prices);
			
			double[] pricesArray = Doubles.toArray(prices);
			

			if (prices.size() > 1000) {
				sd.setData(pricesArray);
				mean.setData(pricesArray);
				System.out.println(category + ", " + prices.size() + ", " + mean.evaluate() + ", " + sd.evaluate());

				//				List<Double> loggedPrices = new ArrayList<>(100);
//				for (double price : Util.getSample(prices.iterator(), 100)) {
//					loggedPrices.add(FastMath.log(price));
//				}
				List<Integer> loggedPrices = Util.getSample(prices.iterator(), 100);
				Collections.sort(loggedPrices);
				chart.addSeries(loggedPrices, category);
			}
		}
		chart.build();
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
