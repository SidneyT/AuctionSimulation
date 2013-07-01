package shillScore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import createUserFeatures.BuildTMFeatures.TMAuctionGroupIterator;
import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.TMAuction;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.SimAuctionDBIterator;
import createUserFeatures.SimAuctionIterator;


import shillScore.ShillScore;
import simulator.database.DBConnection;
import util.IncrementalMean;
import util.Util;


public class BuildShillScore {

	private static final Logger logger = Logger.getLogger(BuildShillScore.class);
	
	public static class ShillScoreInfo {
		public final Map<Integer, ShillScore> shillScores; // Map(bidderId, shill score object)
		public final Map<? extends AuctionObject, List<Integer>> auctionBidders; // Map(seller, bidderlist)
		public final Multiset<Integer> auctionCounts; // Multiset<sellerIds>
		
		public ShillScoreInfo(Map<Integer, ShillScore> shillScores,
				Map<? extends AuctionObject, List<Integer>> auctionBidders, 
						Multiset<Integer> auctionCounts) {
			this.shillScores = shillScores;
			this.auctionBidders = auctionBidders;
			this.auctionCounts = auctionCounts;
		}
	}
	
	public static ShillScoreInfo build(SimAuctionIterator simAuctionIterator) {
		
		Iterator<Pair<SimAuction, List<BidObject>>> it = simAuctionIterator.getAuctionIterator();

		Map<Integer, ShillScore> shillScores = new HashMap<>();
		Map<SimAuction, List<Integer>> auctionBidders = new HashMap<>(); // Map(seller, bidderlist)
		Multiset<Integer> auctionCounts = HashMultiset.create();
		Map<Integer, UserObject> users = simAuctionIterator.users();
		while (it.hasNext()) {
			Pair<SimAuction, List<BidObject>> pair = it.next();
			processBids(shillScores, auctionBidders, auctionCounts, users, pair.getKey(), pair.getValue());
		}
		
		return new ShillScoreInfo(shillScores, auctionBidders, auctionCounts);
	}
	
	public static ShillScoreInfo buildTM() {
		return buildTM(BuildTMFeatures.DEFAULT_QUERY);
	}
	public static ShillScoreInfo buildTM(String query) {
		
		TMAuctionGroupIterator tmIterator = new BuildTMFeatures.TMAuctionGroupIterator(DBConnection.getTrademeConnection(), query);
		Iterator<Pair<TMAuction, List<BidObject>>> it = tmIterator.getIterator();
		
		Map<Integer, ShillScore> shillScores = new HashMap<>();
		Map<TMAuction, List<Integer>> auctionBidders = new HashMap<>(); // Map(seller, bidderlist)
		Multiset<Integer> auctionCounts = HashMultiset.create();
		Map<Integer, UserObject> users = BuildTMFeatures.users(DBConnection.getTrademeConnection());
		while (it.hasNext()) {
			Pair<TMAuction, List<BidObject>> pair = it.next();
			if (pair.getKey().listingId == 487782655)
				System.out.println();
			processBids(shillScores, auctionBidders, auctionCounts, users, pair.getKey(), pair.getValue());
		}
		
		Iterator<Entry<Integer, ShillScore>> ssIter = shillScores.entrySet().iterator();
		while (ssIter.hasNext()) {
			Entry<Integer, ShillScore> ssEntry = ssIter.next();
//			if ("phantom".equals(ssEntry.getValue().userType))
			if (!users.containsKey(ssEntry.getValue().getId()))
				ssIter.remove();
		}
		
		return new ShillScoreInfo(shillScores, auctionBidders, auctionCounts);
	}
	
	public static ShillScoreInfo build() {
		return build(new SimAuctionDBIterator(DBConnection.getSimulationConnection(), true));
	}
	
	/**
	 * 
	 * Updates shill scores of users using the information given about the auction and bids.
	 * 
	 * @param shillScores stores the shillscores of bidders (userId, ShillScore)
	 * @param auctionCounts used to store the number of auctions made by each seller
	 * @param map 
	 * @param auction the auction associated with the bids being processed
	 * @param bids bids of the same auction ordered by ascending time (or amount)
	 */
	static <T extends AuctionObject> void processBids(
			Map<Integer, ShillScore> shillScores, 
			Map<T, List<Integer>> auctionBidders, 
			Multiset<Integer> auctionCounts, 
			Map<Integer, UserObject> users, 
			T auction, List<BidObject> bids
			) {
		
		if (bids.isEmpty()) {
			logger.warn("bids list is empty in processBids. It shouldn't be.");
			System.out.println("bids list is empty in processBids. It shouldn't be.");
			return;
		}
		
		for (BidObject bid : bids) {
			int bidderId = bid.bidderId;
			
			String bidderType;
			if (users.containsKey(bidderId))
				bidderType = users.get(bidderId).userType;
			else // if this user does not have a user profile and a userType, make up one. 
				bidderType = "phantom"; // THESE USERS MUST BE REMOVED AFTERWARDS. they have not been completely crawled.
			
			if (!shillScores.containsKey(bidderId)) {
				ShillScore ss = new ShillScore(bidderId, bidderType);
				shillScores.put(bidderId, ss);
			}
		}
		
//		System.out.println("auction: " + auction + ", bids: " + bids);
		
		// record the bidders for each auction
		Set<Integer> bidders = new HashSet<>();
		for (BidObject bid : bids) {
			bidders.add(bid.bidderId);
		}
		auctionBidders.put(auction, new ArrayList<Integer>(bidders));
		
		// record number of auctions for each seller
		omegaOp(auctionCounts, auction.sellerId);
		
		// record information for all ss measures
		alphaOp(shillScores, auction.sellerId, bids);
		betaOp(shillScores, auction.sellerId, bids);
		gammaOp(shillScores, bids);
		deltaOp(shillScores, bids);
		epsilonOp(shillScores, bids);
		zetaOp(shillScores, auction.endTime, bids);

	}
	
	private static void omegaOp(Multiset<Integer> sellerCounts, int sellerId) {
		sellerCounts.add(sellerId);
	}
	
	// Record the number of losses by the bidder for each seller.
	// Alpha is different for each bidder/seller pair.
	private static void alphaOp(Map<Integer, ShillScore> shillScores, int sellerId, List<BidObject> bids) {
		Set<Integer> seen = new HashSet<>();
		seen.add(bids.get(bids.size() - 1).bidderId); // add the winner to the seen list, so it won't get a value for this auction
		for (int i = 0; i < bids.size(); i++) {
			BidObject bid = bids.get(i);
			if (!seen.add(bid.bidderId)) // already seen this user
				continue;
			ShillScore ss = shillScores.get(bid.bidderId);
			Integer count = ss.lossCounts.get(sellerId);
			if (count == null) {
				ss.lossCounts.put(sellerId, 1);
			} else {
				ss.lossCounts.put(sellerId, count + 1);
			}
		}
	}
	
	// bid proportion
	private static void betaOp(Map<Integer, ShillScore> shillScores, int sellerId, List<BidObject> bids) {
		// count the number of bids by each bidder
		Map<Integer, Integer> bidCounts = new HashMap<>(); // Map<bidderId, count>
		for (BidObject bid : bids) {
			Integer count = bidCounts.get(bid.bidderId);
			if (count == null)
				bidCounts.put(bid.bidderId, 1);
			else
				bidCounts.put(bid.bidderId, count + 1);
		}
		
		double worstCase = Math.floor(bids.size() / 2);
		
		// winner does not have the auction contribute to their beta value, as defined
		Integer removed = bidCounts.remove(bids.get(bids.size() - 1).bidderId);
		assert removed != null;
		
		// calculate average, and store it
		for (Entry<Integer, Integer> countEntry : bidCounts.entrySet()) {
//			System.out.println(countEntry.getKey() + " normalised bidProportion: " + countEntry.getValue() / worstCase);
			shillScores.get(countEntry.getKey()).bidProportion.addNext(countEntry.getValue() / worstCase);
		}
	}
	
	// win-loss counts
	private static void gammaOp(Map<Integer, ShillScore> shillScores, List<BidObject> bids) {
		Set<Integer> seen = new HashSet<>();
		for (BidObject bid : bids) {
			seen.add(bid.bidderId);
		}
		int winnerId = bids.get(bids.size() - 1).bidderId;
		boolean exists = seen.remove(winnerId);
		assert exists : "The winner must exist in the bidder list.";
		
		shillScores.get(winnerId).winCount++;
		for (int bidderId : seen) {
			shillScores.get(bidderId).lossCount++;
		}
	}
	
	// inter bid time
	private static void deltaOp(Map<Integer, ShillScore> shillScores, List<BidObject> bids) {
		Map<Integer, IncrementalMean> timeDiffs = new HashMap<>(); // Map<bidderId, avg time diff>
		
		// find the inter-bid times
		Date previousTime = bids.get(0).time; // first bid's time difference is defined as 0
		for (int i = 0; i < bids.size(); i++) {
			int bidderId = bids.get(i).bidderId;
			IncrementalMean incAvg = timeDiffs.get(bidderId);
			if (incAvg == null) {
				incAvg = new IncrementalMean();
				timeDiffs.put(bidderId, incAvg);
			}
			
			incAvg.addNext(bids.get(i).time.getTime() - previousTime.getTime());
			previousTime = bids.get(i).time;
		}
		
		// find the max and min for normalising
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (IncrementalMean incAvg : timeDiffs.values()) {
			if (incAvg.average() < min)
				min = incAvg.average();
			if (incAvg.average() > max)
				max = incAvg.average();
		}
		
		// remove the winner: definition is that winner is skipped
		IncrementalMean removed = timeDiffs.remove(bids.get(bids.size() - 1).bidderId);
		assert removed != null;
		
		// normalise, then average the interbid times with previous interbid times for the users
		for (Entry<Integer, IncrementalMean> timeDiffsEntry : timeDiffs.entrySet()) {
//			System.out.println(timeDiffsEntry.getKey() + " normalised interBidTime: " + normalise((timeDiffsEntry.getValue().getAverage()), min, max));
			shillScores.get(timeDiffsEntry.getKey()).interBidTime.addNext(normalise((timeDiffsEntry.getValue().average()), min, max));
		}
	}
	
	// inter bid increment
	private static void epsilonOp(Map<Integer, ShillScore> shillScores, List<BidObject> bids) {
		Map<Integer, IncrementalMean> increments = new HashMap<>();
		
		int previousAmount = bids.get(0).amount; // first bid's amount difference is defined as 0
		for (int i = 0; i < bids.size(); i++) {
			int bidderId = bids.get(i).bidderId;
			IncrementalMean incAvg = increments.get(bidderId);
			if (incAvg == null) {
				incAvg = new IncrementalMean();
				increments.put(bidderId, incAvg);
			}
			
			incAvg.addNext(bids.get(i).amount - previousAmount);
			previousAmount = bids.get(i).amount;
		}
		
		// remove the winner: definition is that winner is skipped
		IncrementalMean removed = increments.remove(bids.get(bids.size() - 1).bidderId);
		assert removed != null;
		
		// find the max and min for normalising
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (IncrementalMean incAvg : increments.values()) {
			if (incAvg.average() < min)
				min = incAvg.average();
			if (incAvg.average() > max)
				max = incAvg.average();
		}
		
		for (Entry<Integer, IncrementalMean> incrementEntry : increments.entrySet()) {
//			System.out.println(incrementEntry.getKey() + " normalised bidIncrement: " + normalise((incrementEntry.getValue().getAverage()), min, max));
			shillScores.get(incrementEntry.getKey()).bidIncrement.addNext(normalise((incrementEntry.getValue().average()), min, max));
		}
	}
	
	// first bid time
	private static void zetaOp(Map<Integer, ShillScore> shillScores, Date endTime, List<BidObject> bids) {
		Map<Integer, Long> firstBidTimes = new HashMap<>();
		
		// traverse bids in reverse order. firstBidTimes will store the oldest, i.e., first bids for each user.
		for (int i = bids.size() - 1; i >= 0; i--) {
			BidObject bid = bids.get(i);
			firstBidTimes.put(bid.bidderId, endTime.getTime() - bid.time.getTime());
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
		Long removed = firstBidTimes.remove(bids.get(bids.size() - 1).bidderId);
		assert removed != null;
		
		for (Entry<Integer, Long> firstBidTimeEntry : firstBidTimes.entrySet()) {
//			System.out.println(firstBidTimeEntry.getKey() + " normalised firstBidTime:" + normalise((firstBidTimeEntry.getValue()), min, max));
			double zeta = normalise((firstBidTimeEntry.getValue()), min, max);
//			if (Double.isNaN(zeta))
//				Util.normalise((firstBidTimeEntry.getValue()), min, max);
			shillScores.get(firstBidTimeEntry.getKey()).firstBidTime.addNext(zeta);
		}
	}
	
	private static double normalise(double value, double min, double max) {
		if (value == min && min == max)
			return 0;
		return (value - min)/(max - min);
	}
	
//	public static class User {
//		public int id;
//		public String userType;
//		public User(int id, String bidderType) {
//			this = id;
//			this.userType = bidderType;
//		}
//		@Override
//		public String toString() {
//			return "(" + id + ":" + userType + ")";
//		}
//	}
}
