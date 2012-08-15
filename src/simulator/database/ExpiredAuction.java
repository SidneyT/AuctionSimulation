package simulator.database;

import java.sql.Connection;

import simulator.objects.Auction;

public class ExpiredAuction {

	Connection conn;
	
	public ExpiredAuction(Connection conn) {
		this.conn = conn;
	}
	
	public void recordExpired(Auction auction) {
		
	}
	
}
