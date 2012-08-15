package simulator.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimulationDbConn {
	public static void main(String[] args) {
		SimulationDbConn.getConnection();
	}

	private static Connection connection;
	
	public static Connection getConnection() {
		if (SimulationDbConn.connection != null)
			return connection;
		Connection conn = null;
		try {
		    // Load the JDBC driver
		    String driverName = "com.mysql.jdbc.Driver";
		    Class.forName(driverName);

		    // Create a connection to the database
		    String serverName = "localhost";
		    String databaseName = "auction_simulation";
		    String url = "jdbc:mysql://" + serverName +  "/" + databaseName;
		    String username = "root";
		    String password = "triangle";
		    conn = DriverManager.getConnection(url, username, password);
		    
		} catch (ClassNotFoundException e) {
		    // Could not find the database driver
			e.printStackTrace();
		} catch (SQLException e) {
		    // Could not connect to the database
			e.printStackTrace();
		}
		connection = conn;
		return conn;
	}
	
	public static void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
//	private void makePreparedStatement(Connection con) {
//		try {
//			PreparedStatement pstmt = con.prepareStatement("insert into test_table_1 (col1, col2) values (?, ?)");
//			
//			pstmt.setInt(1, 123);
//			pstmt.setInt(2, 12352);
//			
//			pstmt.execute();
//			
//			pstmt.close();
//			
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} 
//	}
//	
//	private void makePreparedStatement2(Connection con) {
//		try {
//			PreparedStatement pstmt = con.prepareStatement("insert into test_table_2 (col2, col3) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
//			
//			pstmt.setInt(1, 123);
//			pstmt.setInt(2, 112233);
//			
//			pstmt.execute();
//			ResultSet resultSet = pstmt.getGeneratedKeys();
//			while (resultSet.next())
//				System.out.println(resultSet.getInt(1));
//			
//			pstmt.close();
//			
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} 
//	}
	
}
