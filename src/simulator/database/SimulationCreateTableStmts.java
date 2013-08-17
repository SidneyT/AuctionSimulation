package simulator.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SimulationCreateTableStmts {
	
	public final static String users = "CREATE TABLE `users` ( " + 
		"`userId` int(11) NOT NULL, " + 
		"`userType` varchar(45) DEFAULT NULL, " + 
		"`posRep` int(11) DEFAULT NULL, " + 
		"`neuRep` int(11) DEFAULT NULL, " + 
		"`negRep` int(11) DEFAULT NULL, " + 
		"`posUnique` int(11) DEFAULT NULL, " + 
		"`negUnique` int(11) DEFAULT NULL, " + 
		"PRIMARY KEY (`userId`), " + 
		"UNIQUE KEY `id_UNIQUE` (`userId`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	public final static String categories = "CREATE TABLE `categories` ( " + 
		"`id` int(11) NOT NULL, " + 
		"`name` varchar(45) DEFAULT NULL, " + 
		"`parentId` int(11) NULL, " + 
		"PRIMARY KEY (`id`), " + 
		"UNIQUE KEY `id_UNIQUE` (`id`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	public final static String auctions = "CREATE TABLE `auctions` ( " + 
		"`listingId` int(11) NOT NULL, " + 
		"`startTime` bigint(20) NOT NULL, " + 
		"`sellerId` int(11) NOT NULL, " + 
		"`listingName` varchar(45) NOT NULL, " + 
		"`itemTypeId` int(11) NOT NULL, " + 
		"`duration` int(11) NOT NULL, " + 
		"`startPrice` bigint(20) NOT NULL, " + 
		"`reservePrice` bigint(20) NOT NULL, " + 
		"`endTime` bigint(20) NOT NULL, " + 
		"`winnerId` int(11) DEFAULT NULL, " +
		"`popularity` double DEFAULT NULL, " +
		"PRIMARY KEY (`listingId`), " + 
		"UNIQUE KEY `id_UNIQUE` (`listingId`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	public final static String bids = "CREATE TABLE `bids` ( " + 
		"`bidId` int(11) NOT NULL, " + 
		"`time` bigint(20) NOT NULL, " + 
		"`bidderId` int(11) NOT NULL, " + 
		"`amount` bigint(20) NOT NULL, " + 
		"`listingId` int(11) NOT NULL, " + 
		"PRIMARY KEY (`bidId`), " + 
		"UNIQUE KEY `id_UNIQUE` (`bidId`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	public final static String feedback = "CREATE TABLE `feedback` ( " + 
		"`listingId` int(11) NOT NULL, " + 
		"`forSeller` binary(1) NOT NULL, " + 
		"`fromUserId` int(11) NOT NULL, " + 
		"`toUserId` int(11) NOT NULL, " + 
		"`score` tinyint(3) UNSIGNED NOT NULL, " + 
		"`time` bigint(20) NOT NULL, " + 
		"PRIMARY KEY (`listingId`,`forseller`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	public final static String itemtypes = "CREATE TABLE `itemtypes` ( " + 
		"`id` int(11) NOT NULL, " + 
		"`name` varchar(45) NOT NULL, " + 
		"`weight` DOUBLE NOT NULL, " + 
		"`trueValuation` DOUBLE NOT NULL, " + 
		"`categoryId` int(11) NOT NULL, " + 
		"PRIMARY KEY (`id`), " + 
		"UNIQUE KEY `id_UNIQUE` (`id`) " +
		") ENGINE=InnoDB DEFAULT CHARSET=latin1;";
	
	public static void main(String[] args) {
		createSimulationTables("auction_simulation1");
	}
	
	public final static void createSimulationTables(String databaseName) {
		try {
			Connection conn = DBConnection.getConnection(databaseName);
			Statement stmt = conn.createStatement();
			stmt.addBatch(users);
			stmt.addBatch(categories);
			stmt.addBatch(auctions);
			stmt.addBatch(bids);
			stmt.addBatch(feedback);
			stmt.addBatch(itemtypes);
			stmt.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
}
