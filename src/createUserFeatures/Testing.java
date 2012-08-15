package createUserFeatures;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Testing {
	public static void main(String[] args) {
		
		new Testing().go();
		
	}

	private void go() {
		Connection connection = null;
		try {
		    // Load the JDBC driver
		    String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
		    Class.forName(driverName);

		    // Create a connection to the database
		    String serverName = "localhost";
		    String mydatabase = "test";
		    String url = "jdbc:mysql://" + serverName +  "/" + mydatabase; // a JDBC url
		    String username = "root";
		    String password = "triangle";
		    connection = DriverManager.getConnection(url, username, password);
		    
		    makePreparedStatement2(connection);
		    
		} catch (ClassNotFoundException e) {
		    // Could not find the database driver
			e.printStackTrace();
		} catch (SQLException e) {
		    // Could not connect to the database
			e.printStackTrace();
		}
	}
	
	private void makePreparedStatement(Connection con) {
		try {
			PreparedStatement pstmt = con.prepareStatement("insert into test_table_1 (col1, col2) values (?, ?)");
			
			pstmt.setInt(1, 123);
			pstmt.setInt(2, 12352);
			
			pstmt.execute();
			
			pstmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	
	private void makePreparedStatement2(Connection con) {
		try {
			PreparedStatement pstmt = con.prepareStatement("insert into test_table_2 (col2, col3) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
			
			pstmt.setInt(1, 123);
			pstmt.setInt(2, 112233);
			
			pstmt.execute();
			ResultSet resultSet = pstmt.getGeneratedKeys();
			while (resultSet.next())
				System.out.println(resultSet.getInt(1));
			
			pstmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
}
