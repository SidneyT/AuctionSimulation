package temporary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import simulator.database.DBConnection;


public class PrintQueryResults {
	
	public static void main(String[] args) {
		printColumn("SELECT MAX(b.amount) as finalPrice FROM bids as b GROUP BY b.listingId;", Paths.get("auctionPrices.csv"));
	}
	
	private static void printColumn(String query, Path path) {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			
			Statement stmt = conn.createStatement();
			
			
			BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset());
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				bw.append(rs.getObject(1).toString());
				bw.newLine();
			}
			bw.close();
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
