package createUserFeatures;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import simulator.database.DBConnection;
import util.IncrementalMean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.TMAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public class tmStats {
	public static void main(String[] args) {
		bothBidderAndSellerStats();
	}
	
	public static void repCounts(HashSet<Integer> allUsers, TMAuctionIterator it) {
		Map<Integer, UserObject> users = BuildTMFeatures.users(DBConnection.getTrademeConnection());
		
		int netPos = 0;
		int neutral = 0;
		int netNeg = 0;
		
		int totalPos = 0;
		int totalNeg = 0;
		
		for (Integer wantedUserId : allUsers) {
			if (!users.containsKey(wantedUserId))
				continue;
			
			UserObject wantedUser = users.get(wantedUserId);
			totalPos += wantedUser.posUnique;
			totalNeg += wantedUser.negUnique;
			if (wantedUser.posUnique > wantedUser.negUnique)
				netPos++;
			else if (wantedUser.posUnique == wantedUser.negUnique)
				neutral++;
			else if (wantedUser.posUnique < wantedUser.negUnique)
				netNeg++;
			else
				assert false;
		}
		
		System.out.println("netPos,neutral,netNeg: " + netPos + "," + neutral + "," + netNeg);
		System.out.println("totalPos,totalNeg:" + totalPos + "," + totalNeg);
	}
	
	/**
	 * Reports counts about users acting as both bidders and sellers.
	 */
	public static void bothBidderAndSellerStats() {
//		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getConnection("trademe_small"), BuildTMFeatures.DEFAULT_QUERY);
		TMAuctionIterator it = new TMAuctionIterator(DBConnection.getTrademeConnection(), BuildTMFeatures.DEFAULT_QUERY);
		
//		Multimap<Integer, Integer> bidInteraction = ArrayListMultimap.create();
		Multimap<Integer, Integer> bidInteraction = HashMultimap.create();
		Multimap<Integer, Integer> winInteraction = ArrayListMultimap.create();
		Multimap<Integer, Integer> sellInteraction = ArrayListMultimap.create();
		
		int bidCount = 0;
		int auctionCount = 0;
		
		for (Pair<TMAuction, List<BidObject>> auctionPair : it.list()) {
			HashSet<Integer> bidderSet = new HashSet<>();
			List<BidObject> bids = auctionPair.getValue();
			bidCount += bids.size();
			auctionCount ++;
			for (BidObject bid : bids) {
				bidderSet.add(bid.bidderId);
			}
			
			BidObject lastBid = bids.get(bids.size() - 1);
			winInteraction.put(lastBid.bidderId, lastBid.listingId);

			TMAuction auction = auctionPair.getKey();
			Integer sellerId = auction.sellerId;
			Integer listingId = auction.listingId;
			for (Integer user : bidderSet) {
				bidInteraction.put(user, listingId);
			}
			
			sellInteraction.put(sellerId, listingId);
		}
		
		HashSet<Integer> allUsers = new HashSet<>(sellInteraction.keySet());
		allUsers.addAll(bidInteraction.keySet());
		
		IncrementalMean onlySellerAvgSold = new IncrementalMean();
		IncrementalMean onlyBidderAvgBid = new IncrementalMean();
		IncrementalMean onlyBidderAvgWins = new IncrementalMean();
		IncrementalMean bothAvgSold = new IncrementalMean();
		IncrementalMean bothAvgBid = new IncrementalMean();
		IncrementalMean bothAvgWin = new IncrementalMean();
		
		for (Integer userId : allUsers) {
			boolean isSeller = sellInteraction.containsKey(userId);
			boolean isBidder = bidInteraction.containsKey(userId);
			boolean isWinner = winInteraction.containsKey(userId);
			
			if (isSeller && isBidder) {
				bothAvgSold.add(sellInteraction.get(userId).size());
				bothAvgBid.add(bidInteraction.get(userId).size());
				if (isWinner) bothAvgWin.add(winInteraction.get(userId).size());
			} else if (isSeller) {
				onlySellerAvgSold.add(sellInteraction.get(userId).size());
			} else { // isBidder
				onlyBidderAvgBid.add(bidInteraction.get(userId).size());
				if (isWinner) onlyBidderAvgWins.add(winInteraction.get(userId).size());
			}
		}
		
		System.out.println("sold: " + onlySellerAvgSold + "," + bothAvgSold);
		System.out.println("bids: " + onlyBidderAvgBid + "," + bothAvgBid);
		System.out.println("wins: " + onlyBidderAvgWins + "," + bothAvgWin);
		System.out.println("bidCount: " + bidCount + "| " + "auctionCount:" + auctionCount);
		
		repCounts(allUsers, it);
//		int bothCount2 = 0;
//		for (Integer userId : sellInteraction.keySet()) {
//			if (winInteraction.containsKey(userId)) {
//				bothCount2++;
//			}
//		}
//		System.out.println("sellers, bidders, both");
//		System.out.println(sellInteraction.keySet().size() + "," + winInteraction.keySet().size() + "," + bothCount2);
	}

}
