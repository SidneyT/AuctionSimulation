package simulator.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

	public static Connection getConnection(String database) throws SQLException {
		Connection connection = null;
		try {
			// Load the JDBC driver
			String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
			Class.forName(driverName);

			// Create a connection to the database
			String serverName = "localhost";
			String url = "jdbc:mysql://" + serverName + "/" + database; // a JDBC url
			String username = "root";
			String password = "triangle";
			connection = DriverManager.getConnection(url, username, password);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public static Connection getTrademeConnection() throws SQLException {
		return getConnection("trademe");
	}

	public static Connection getSimulationConnection() throws SQLException {
		return getConnection("auction_simulation");
	}

}
