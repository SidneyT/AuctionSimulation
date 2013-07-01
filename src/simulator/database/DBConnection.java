package simulator.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

	public static void main(String[] args) {
		createDatabase("auction_simulation1");
	}
	
	public static Connection getConnection(String database) {
		try {
			// Load the JDBC driver
			String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
			Class.forName(driverName);

			// Create a connection to the database
			String serverName = "localhost";
			String url = "jdbc:mysql://" + serverName + "/" + database; // a JDBC url
			String username = "root";
			String password = "triangle";
			Connection connection = DriverManager.getConnection(url, username, password);
			return connection;

		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static Connection getTrademeConnection() {
		return getConnection("trademe");
	}

	public static Connection getSimulationConnection() {
		return getConnection("auction_simulation");
	}

	public static void createDatabase(String database) {
		try {
			// Load the JDBC driver
			String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
			Class.forName(driverName);

			// Create a connection to the database
			String serverName = "localhost";
			String url = "jdbc:mysql://" + serverName + "/"; // a JDBC url
			String username = "root";
			String password = "triangle";
			Connection connection = DriverManager.getConnection(url, username, password);
			Statement st = connection.createStatement();
			st.executeUpdate("CREATE DATABASE " + database);
			st.close();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
}
