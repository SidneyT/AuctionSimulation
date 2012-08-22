package simulator.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import simulator.categories.CategoryNode;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;

import agents.SimpleUser;

public class SaveObjects {

	private static final Connection conn;
	private static final PreparedStatement saveBidPstmt;
	private static final PreparedStatement saveExpiredAuctionPstmt;
	private static final PreparedStatement saveAuctionPstmt;
//	private static final PreparedStatement saveExpiredPstmt;
	private static final PreparedStatement saveUserPstmt;
	private static final PreparedStatement saveItemTypePstmt;
	private static final PreparedStatement saveFeedbackPstmt;
	static {
		try {
			conn = DatabaseConnection.getSimulationConnection();
			
			saveBidPstmt = conn.prepareStatement("INSERT INTO bids " +
					"(bidId, time, bidderId, amount, listingId) " +
					"values (?, ?, ?, ?, ?)");
			saveAuctionPstmt = conn.prepareStatement("INSERT INTO auctions " +
					"(listingId, startTime, sellerId, listingName, itemTypeId, duration, startPrice, reservePrice) " +
					"values (?, ?, ?, ?, ?, ?, ?, ?)");
//			saveExpiredPstmt = conn.prepareStatement("UPDATE auctions " +
//					"SET endTime=?, winnerId=? " +
//					"WHERE listingId=?");
			saveExpiredAuctionPstmt = conn.prepareStatement("INSERT INTO auctions " +
					"(listingId, startTime, sellerId, listingName, itemTypeId, duration, startPrice, reservePrice, endTime, winnerId) " +
					"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			saveUserPstmt = conn.prepareStatement("INSERT INTO users (userId, userType, posRep, neuRep, negRep, posUnique, negUnique) " +
					"values (?, ?, ?, ?, ?, ?, ?)");
			saveItemTypePstmt = conn.prepareStatement("INSERT INTO itemtypes (id, name, weight, trueValuation, categoryId) " +
					"values (?, ?, ?, ?, ?)");
			saveFeedbackPstmt = conn.prepareStatement("INSERT INTO feedback " +
					"(listingId, forSeller, fromUserId, toUserId, score, time) " +
					"values (?, ?, ?, ?, ?, ?)");
		} catch (SQLException e) {
			throw new RuntimeException("Problem with SQL statements.", e);
		}
	}

	private static List<SimpleUser> userStore = new ArrayList<SimpleUser>();
	public static void saveUser(SimpleUser user) {
		userStore.add(user);
		if (userStore.size() >= 500) {
			flushSaveUser();
		}
	}
	private static void flushSaveUser() {
		try {
			conn.setAutoCommit(false);
			
			for (SimpleUser user : userStore) {
				saveUserPstmt.setInt(1, user.getId());
				saveUserPstmt.setString(2, user.getClass().getSimpleName());
				saveUserPstmt.setInt(3, user.getReputationRecord().getPos());
				saveUserPstmt.setInt(4, user.getReputationRecord().getNeu());
				saveUserPstmt.setInt(5, user.getReputationRecord().getNeg());
				saveUserPstmt.setInt(6, user.getReputationRecord().getPosUnique());
				saveUserPstmt.setInt(7, user.getReputationRecord().getNegUnique());
				saveUserPstmt.addBatch();
			}
			saveUserPstmt.executeBatch();
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
		userStore.clear();
	}
	
	public static void saveCategory(Collection<CategoryNode> categories) {
		try {
			for (CategoryNode category : categories) {
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO CATEGORIES (id, name, parentId) values (?, ?, ?)");
				pstmt.setInt(1, category.getId());
				pstmt.setString(2, category.getName());
				if (category.getParent() == null)
					pstmt.setNull(3, Types.INTEGER);
				else
					pstmt.setInt(3, category.getParent().getId());
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
	}
	
	public static void saveItemTypes(List<ItemType> types) {
		try {
			conn.setAutoCommit(false);
			
			for (ItemType type : types) {
				saveItemTypePstmt.setInt(1, type.getId());
				saveItemTypePstmt.setString(2, type.getName());
				saveItemTypePstmt.setDouble(3, type.getWeight());
				saveItemTypePstmt.setDouble(4, type.getTrueValuation());
				saveItemTypePstmt.setInt(5, type.getCategory().getId());
				saveItemTypePstmt.addBatch();
			}
			saveItemTypePstmt.executeBatch();
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
	}

	private static List<Auction> auctionStore = new ArrayList<Auction>();
	public static void saveAuction(Auction auction) {
		auctionStore.add(auction);
//		if (auctionStore.size() >= 200) {
//			flushSaveAuction();
//		}
	}
	private static void flushSaveAuction() {
		try {
			conn.setAutoCommit(false);

			for (Auction auction : auctionStore) {
				saveAuctionPstmt.setInt(1, auction.getId());
				saveAuctionPstmt.setLong(2, auction.getStartTime());
				saveAuctionPstmt.setInt(3, auction.getSeller().getId());
				saveAuctionPstmt.setString(4, auction.getItem().getName());
				saveAuctionPstmt.setInt(5, auction.getItem().getType().getId());
//				saveAuctionPstmt.setString(6, auction.getCategory().getName());
				saveAuctionPstmt.setLong(6, auction.getDuration());
				saveAuctionPstmt.setLong(7, auction.getStartPrice());
				saveAuctionPstmt.setLong(8, auction.getReservePrice());
				saveAuctionPstmt.addBatch();
			}
			saveAuctionPstmt.executeBatch();
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
		auctionStore.clear();
	}
//
	private static List<Auction> expiredStore = new ArrayList<Auction>();
	public static void saveExpiredAuction(Auction auction, boolean sold) {
		expiredStore.add(auction);
		if (expiredStore.size() >= 1000)
			flushExpiredAuction();
	}
	private static void flushExpiredAuction() {
			try {
			conn.setAutoCommit(false);
	
			for (Auction auction : expiredStore) {
				saveExpiredAuctionPstmt.setInt(1, auction.getId());
				saveExpiredAuctionPstmt.setLong(2, auction.getStartTime());
				saveExpiredAuctionPstmt.setInt(3, auction.getSeller().getId());
				saveExpiredAuctionPstmt.setString(4, auction.getItem().getName());
				saveExpiredAuctionPstmt.setInt(5, auction.getItem().getType().getId());
				saveExpiredAuctionPstmt.setLong(6, auction.getDuration());
				saveExpiredAuctionPstmt.setLong(7, auction.getStartPrice());
				saveExpiredAuctionPstmt.setLong(8, auction.getReservePrice());
				saveExpiredAuctionPstmt.setLong(9, auction.getEndTime());
				if (auction.getWinner() == null) // if there is no winner
					saveExpiredAuctionPstmt.setNull(10, Types.INTEGER);
				else
					saveExpiredAuctionPstmt.setLong(10, auction.getWinner().getId());
				saveExpiredAuctionPstmt.addBatch();
			}
			saveExpiredAuctionPstmt.executeBatch();
			
			auctionStore.removeAll(expiredStore);
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
		expiredStore.clear();
	}
//	public static void flushExpired() {
//		try {
//			conn.setAutoCommit(false);
//
//			for (Auction auction : expiredStore) {
//				saveExpiredPstmt.setLong(1, auction.getEndTime());
//				if (auction.getWinner() == null) // if there is no winner
//					saveExpiredPstmt.setNull(2, Types.INTEGER);
//				else
//					saveExpiredPstmt.setLong(2, auction.getWinner().getId());
//				saveExpiredPstmt.setLong(3, auction.getId());
//				saveExpiredPstmt.addBatch();
//			}
//			int[] updateResults = saveExpiredPstmt.executeBatch();
//			
//			// make sure each update changed 1 row/auction.
//			{
////				for (int i = 0; i < updateResults.length; i++) {
////					assert updateResults[i] == 1 : "got " + updateResults[i] + " instead of " + "1.";
////				}
//			}
//
//			conn.commit();
//			conn.setAutoCommit(true);
//		} catch (SQLException e) {
//			e.printStackTrace();
//			throw new RuntimeException("SQLException");
//		}
//		expiredStore.clear();
//	}

	private static List<Object[]> bidStore = new ArrayList<Object[]>();
	public static void saveBid(Auction auction, Bid bid) {
		bidStore.add(new Object[]{auction, bid});
		if (bidStore.size() >= 500) {
			flushSaveBid();
		}
	}
	private static void flushSaveBid() {
		try {
			conn.setAutoCommit(false);
			
			for (Object[] obj : bidStore) {
				Auction auction = (Auction) obj[0];
				Bid bid = (Bid) obj[1];
				saveBidPstmt.setInt(1, bid.getId());
				saveBidPstmt.setLong(2, bid.getTime());
				saveBidPstmt.setInt(3, bid.getBidder().getId());
				saveBidPstmt.setLong(4, bid.getPrice());
				saveBidPstmt.setInt(5, auction.getId());
				saveBidPstmt.addBatch();
			}
			saveBidPstmt.executeBatch();
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
		bidStore.clear();
	}
	
	public static void flush() {
		flushSaveUser();
		flushExpiredAuction();
		flushSaveAuction();
//		flushExpired();
		flushSaveBid();
		flushFeedback();
	}
	
	private static List<Feedback> feedbackStore = new ArrayList<>();
	public static void saveFeedback(Feedback feedback) {
		feedbackStore.add(feedback);
		if (feedbackStore.size() >= 500)
			flushFeedback();
	}
	public static void flushFeedback() {
		try {
			conn.setAutoCommit(false);
			
			for (Feedback feedback : feedbackStore) {
				saveFeedbackPstmt.setInt(1, feedback.getAuction().getId());
				saveFeedbackPstmt.setBoolean(2, feedback.forSeller());
				saveFeedbackPstmt.setInt(3, feedback.forSeller() ? feedback.getAuction().getWinner().getId() : feedback.getAuction().getSeller().getId());
				saveFeedbackPstmt.setInt(4, feedback.forSeller() ? feedback.getAuction().getSeller().getId() : feedback.getAuction().getWinner().getId());
				saveFeedbackPstmt.setInt(5, feedback.getVal().getInt());
				saveFeedbackPstmt.setLong(6, feedback.getTime());
				saveFeedbackPstmt.addBatch();
			}
			saveFeedbackPstmt.executeBatch();

			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException", e);
		}
		feedbackStore.clear();
	}

}
