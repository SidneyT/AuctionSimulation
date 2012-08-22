package shillScore;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import simulator.database.DatabaseConnection;
import util.IncrementalAverage;
import util.Util;


public class BuildShillScore {

	private static final Logger logger = Logger.getLogger(BuildShillScore.class);
	
	public static class ShillScoreInfo {
		public final Map<Integer, ShillScore> shillScores; // Map(bidderId, shill score object)
		public final Map<Auction, List<User>> auctionBidders; // Map(seller, bidderlist)
		public final Map<Integer, Integer> auctionCounts; // Map(sellerId, number of auctions)
		
		public ShillScoreInfo(Map<Integer, ShillScore> shillScores,
				Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts) {
			this.shillScores = shillScores;
			this.auctionBidders = auctionBidders;
			this.auctionCounts = auctionCounts;
		}
	}
	
	/**
	 * Calculates the shill score for users.
	 */
	public static ShillScoreInfo build() {
		try {
			Connection conn = DatabaseConnection.getSimulationConnection();
			CallableStatement stmt = conn.prepareCall(
					"SELECT a.listingId, a.sellerId, a.winnerId, u2.userType as sellerType, a.endTime, b.time as bidTime, b.amount as bidAmount, b.bidderId, u1.userType as bidderType " +
					"FROM auctions as a " +
					"JOIN bids as b ON a.listingId=b.listingId " + 
					"JOIN users as u1 ON b.bidderId=u1.userId " +
					"JOIN users as u2 ON a.sellerId=u2.userId " +
					"WHERE endTime IS NOT NULL ORDER BY a.listingId, time ASC;");
			ResultSet bidsResult = stmt.executeQuery();

			int listingId = -1;
			List<Bid> bids = new ArrayList<>();
			Map<Integer, ShillScore> shillScores = new HashMap<>();
			Map<Auction, List<User>> auctionBidders = new HashMap<>(); // Map(seller, bidderlist)
			Map<Integer, Integer> auctionCounts = new HashMap<>();

			Auction auction = null;
			while (bidsResult.next()) {
				int newListingId = bidsResult.getInt("listingId");
				if (listingId != newListingId) {
					listingId = newListingId;
					if (!bids.isEmpty()) {
						processBids(shillScores, auctionBidders, auctionCounts, auction, bids);
						bids.clear();
					}
					auction = new Auction(bidsResult.getInt("listingId"), bidsResult.getInt("winnerId"), bidsResult.getInt("sellerId"), bidsResult.getString("sellerType"), bidsResult.getLong("endTime"));
				}

				Bid bid = new Bid(bidsResult.getInt("bidderId"), bidsResult.getString("bidderType"), bidsResult.getInt("listingId"), bidsResult.getLong("bidTime"), bidsResult.getInt("bidAmount"));
				bids.add(bid);
			}

			processBids(shillScores, auctionBidders, auctionCounts, auction, bids);
			return new ShillScoreInfo(shillScores, auctionBidders, auctionCounts);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * Updates shill scores of users using the information given about the auction and bids.
	 * 
	 * @param shillScores stores the shillscores of bidders (userId, ShillScore)
	 * @param auctionCounts used to store the number of auctions made by each seller
	 * @param auction the auction associated with the bids being processed
	 * @param bids bids of the same auction ordered by ascending time (or amount)
	 */
	static void processBids(Map<Integer, ShillScore> shillScores, Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts, Auction auction, List<Bid> bids) {
		if (bids.isEmpty()) {
			logger.warn("bids list is empty in processBids. It shouldn't be.");
			return;
		}
		
		for (Bid bid : bids) {
			int bidder = bid.user.id;
			String bidderType = bid.user.userType;
			ShillScore ss;
			if (!shillScores.containsKey(bidder)) {
				ss = new ShillScore(bidder, bidderType);
				shillScores.put(bidder, ss);
			} else {
				ss = shillScores.get(bidder);
			}
		}
		
//		System.out.println("auction: " + auction + ", bids: " + bids);
		
		// record the bidders for each auction
		Set<User> bidders = new HashSet<>();
		for (Bid bid : bids) {
			bidders.add(bid.user);
		}
		auctionBidders.put(auction, new ArrayList<User>(bidders));
		
		// record number of auctions for each seller
		omegaOp(auctionCounts, auction.seller.id);
		
		// record information for all ss measures
		alphaOp(shillScores, auction.seller.id, bids);
		betaOp(shillScores, auction.seller.id, bids);
		gammaOp(shillScores, bids);
		deltaOp(shillScores, bids);
		epsilonOp(shillScores, bids);
		zetaOp(shillScores, auction.endTime, bids);

//		System.out.println("shillScores: " + shillScores);
	}
	
	private static void omegaOp(Map<Integer, Integer> sellerCounts, int sellerId) {
		Integer count = sellerCounts.get(sellerId);
		if (count == null) {
			sellerCounts.put(sellerId, 1);
		} else {
			sellerCounts.put(sellerId, count + 1);
		}
	}
	
	// Record the number of losses by the bidder for each seller.
	// Alpha is different for each bidder/seller pair.
	private static void alphaOp(Map<Integer, ShillScore> shillScores, int sellerId, List<Bid> bids) {
		Set<Integer> seen = new HashSet<>();
		seen.add(bids.get(bids.size() - 1).user.id); // add the winner to the seen list, so it won't get a value for this auction
		for (int i = 0; i < bids.size(); i++) {
			Bid bid = bids.get(i);
			if (!seen.add(bid.user.id)) // already seen this user
				continue;
			ShillScore ss = shillScores.get(bid.user.id);
			Integer count = ss.lossCounts.get(sellerId);
			if (count == null) {
				ss.lossCounts.put(sellerId, 1);
			} else {
				ss.lossCounts.put(sellerId, count + 1);
			}
		}
	}
	
	// bid proportion
	private static void betaOp(Map<Integer, ShillScore> shillScores, int sellerId, List<Bid> bids) {
		// count the number of bids by each bidder
		Map<Integer, Integer> bidCounts = new HashMap<>(); // Map<bidderId, count>
		for (Bid bid : bids) {
			Integer count = bidCounts.get(bid.user.id);
			if (count == null)
				bidCounts.put(bid.user.id, 1);
			else
				bidCounts.put(bid.user.id, count + 1);
		}
		
		double worstCase = Math.floor(bids.size() / 2);
		
		// winner does not have the auction contribute to their beta value, as defined
		Integer removed = bidCounts.remove(bids.get(bids.size() - 1).user.id);
		assert removed != null;
		
		// calculate average, and store it
		for (Entry<Integer, Integer> countEntry : bidCounts.entrySet()) {
//			System.out.println(countEntry.getKey() + " normalised bidProportion: " + countEntry.getValue() / worstCase);
			shillScores.get(countEntry.getKey()).bidProportion.incrementalAvg(countEntry.getValue() / worstCase);
		}
	}
	
	// win-loss counts
	private static void gammaOp(Map<Integer, ShillScore> shillScores, List<Bid> bids) {
		Set<Integer> seen = new HashSet<>();
		for (Bid bid : bids) {
			seen.add(bid.user.id);
		}
		int winnerId = bids.get(bids.size() - 1).user.id;
		boolean exists = seen.remove(winnerId);
		assert exists : "The winner must exist in the bidder list.";
		
		shillScores.get(winnerId).winCount++;
		for (int bidderId : seen) {
			shillScores.get(bidderId).lossCount++;
		}
	}
	
	// inter bid time
	private static void deltaOp(Map<Integer, ShillScore> shillScores, List<Bid> bids) {
		Map<Integer, IncrementalAverage> timeDiffs = new HashMap<>(); // Map<bidderId, avg time diff>
		
		// find the inter-bid times
		long previousTime = bids.get(0).time; // first bid's time difference is defined as 0
		for (int i = 0; i < bids.size(); i++) {
			int bidderId = bids.get(i).user.id;
			IncrementalAverage incAvg = timeDiffs.get(bidderId);
			if (incAvg == null) {
				incAvg = new IncrementalAverage();
				timeDiffs.put(bidderId, incAvg);
			}
			
			incAvg.incrementalAvg(bids.get(i).time - previousTime);
			previousTime = bids.get(i).time;
		}
		
		// find the max and min for normalising
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (IncrementalAverage incAvg : timeDiffs.values()) {
			if (incAvg.getAverage() < min)
				min = incAvg.getAverage();
			if (incAvg.getAverage() > max)
				max = incAvg.getAverage();
		}
		
		// remove the winner: definition is that winner is skipped
		IncrementalAverage removed = timeDiffs.remove(bids.get(bids.size() - 1).user.id);
		assert removed != null;
		
		// normalise, then average the interbid times with previous interbid times for the users
		for (Entry<Integer, IncrementalAverage> timeDiffsEntry : timeDiffs.entrySet()) {
//			System.out.println(timeDiffsEntry.getKey() + " normalised interBidTime: " + normalise((timeDiffsEntry.getValue().getAverage()), min, max));
			shillScores.get(timeDiffsEntry.getKey()).interBidTime.incrementalAvg(normalise((timeDiffsEntry.getValue().getAverage()), min, max));
		}
	}
	
	// inter bid increment
	private static void epsilonOp(Map<Integer, ShillScore> shillScores, List<Bid> bids) {
		Map<Integer, IncrementalAverage> increments = new HashMap<>();
		
		int previousAmount = bids.get(0).amount; // first bid's amount difference is defined as 0
		for (int i = 0; i < bids.size(); i++) {
			int bidderId = bids.get(i).user.id;
			IncrementalAverage incAvg = increments.get(bidderId);
			if (incAvg == null) {
				incAvg = new IncrementalAverage();
				increments.put(bidderId, incAvg);
			}
			
			incAvg.incrementalAvg(bids.get(i).amount - previousAmount);
			previousAmount = bids.get(i).amount;
		}
		
		// remove the winner: definition is that winner is skipped
		IncrementalAverage removed = increments.remove(bids.get(bids.size() - 1).user.id);
		assert removed != null;
		
		// find the max and min for normalising
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (IncrementalAverage incAvg : increments.values()) {
			if (incAvg.getAverage() < min)
				min = incAvg.getAverage();
			if (incAvg.getAverage() > max)
				max = incAvg.getAverage();
		}
		
		for (Entry<Integer, IncrementalAverage> incrementEntry : increments.entrySet()) {
//			System.out.println(incrementEntry.getKey() + " normalised bidIncrement: " + normalise((incrementEntry.getValue().getAverage()), min, max));
			shillScores.get(incrementEntry.getKey()).bidIncrement.incrementalAvg(normalise((incrementEntry.getValue().getAverage()), min, max));
		}
	}
	
	// first bid time
	private static void zetaOp(Map<Integer, ShillScore> shillScores, long endTime, List<Bid> bids) {
		Map<Integer, Long> firstBidTimes = new HashMap<>();
		
		// traverse bids in reverse order. firstBidTimes will store the oldest, i.e., first bids for each user.
		for (int i = bids.size() - 1; i >= 0; i--) {
			Bid bid = bids.get(i);
			firstBidTimes.put(bid.user.id, endTime - bid.time);
		}
		
		// find the max and min for normalising
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (long incAvg : firstBidTimes.values()) {
			if (incAvg < min)
				min = incAvg;
			if (incAvg > max)
				max = incAvg;
		}
		
		// remove the winner: definition is that score calc is skipped for the winner
		Long removed = firstBidTimes.remove(bids.get(bids.size() - 1).user.id);
		assert removed != null;
		
		for (Entry<Integer, Long> firstBidTimeEntry : firstBidTimes.entrySet()) {
//			System.out.println(firstBidTimeEntry.getKey() + " normalised firstBidTime:" + normalise((firstBidTimeEntry.getValue()), min, max));
			shillScores.get(firstBidTimeEntry.getKey()).firstBidTime.incrementalAvg(Util.normalise((firstBidTimeEntry.getValue()), min, max));
		}
	}
	
	private static double normalise(double value, double min, double max) {
		if (value == min && min == max)
			return 0;
		return (value - min)/(max - min);
	}
	
	private static class Bid {
		User user;
		int listingId;
		long time;
		int amount;
		Bid(int bidderId, String bidderType, int listingId, long time, int amount) {
			this.user = new User(bidderId, bidderType);
			this.listingId = listingId;
			this.time = time;
			this.amount = amount;
		}
		
		@Override
		public String toString() {
			return "(user:" + user + ", listingId: " + listingId + ", time: " + time + ", amount: " + amount + ")";
		}
	}
	
	public static class User {
		public int id;
		public String userType;
		User(int id, String bidderType) {
			this.id = id;
			this.userType = bidderType;
		}
		@Override
		public String toString() {
			return "(" + id + ":" + userType + ")";
		}
	}
	
	public static class Auction {
		public int listingId;
		public User seller;
		public int winnerId;
		public long endTime;
		Auction(int listingId, int winnerId, int sellerId, String sellerType, long endTime) {
			this.listingId = listingId;
			this.winnerId = winnerId;
			this.seller = new User(sellerId, sellerType);
			this.endTime = endTime;
		}
		
		@Override
		public String toString() {
			return "(sellerId: " + seller + ", endTime: " + endTime + ")";
		}
	}
}
