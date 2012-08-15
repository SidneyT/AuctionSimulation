package simulator.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TradeMeDbConn {
	public static Connection getConnection() throws SQLException {
		Connection connection = null;
		try {
		    // Load the JDBC driver
		    String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
		    Class.forName(driverName);

		    // Create a connection to the database
		    String serverName = "localhost";
		    String mydatabase = "trade_me";
		    String url = "jdbc:mysql://" + serverName +  "/" + mydatabase; // a JDBC url
		    String username = "root";
		    String password = "triangle";
		    connection = DriverManager.getConnection(url, username, password);
		    
		} catch (ClassNotFoundException e) {
		    // Could not find the database driver
			e.printStackTrace();
		}
		return connection;
	}
	
	public static void main(String[] args) throws Exception {
		Connection conn = TradeMeDbConn.getConnection();
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM auctions as a JOIN bids as b ON a.listingId=b.listingId WHERE a.purchasedWithBuyNow=0 GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			System.out.println(rs.getInt("amount"));
		}
	}

}
