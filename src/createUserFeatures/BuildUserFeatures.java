package createUserFeatures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import agents.SimpleUser;

import com.google.common.collect.ArrayListMultimap;

import simulator.objects.Auction;
import simulator.objects.Bid;
import util.Util;

/**
 * Builds and updates UserFeatures objects using auction bidding information.
 */
public abstract class BuildUserFeatures {
	protected Map<Integer, UserFeatures> userFeaturesMap;
	public boolean trim; // trim auction bid list lengths to 20

	public BuildUserFeatures() {
		this.userFeaturesMap = new TreeMap<Integer, UserFeatures>();
//		this.userFeaturesMap = new HashMap<Integer, UserFeatures>();
		this.trim = false;
	}

	public boolean trim() {
		return this.trim;
	}

	public static void writeToFile(Collection<UserFeatures> userFeaturesCollection, List<Features> featuresToPrint, Path path) {
		try (BufferedWriter w = Files.newBufferedWriter(path, Charset.defaultCharset())) {
			// print headings
			w.append(Features.labels(featuresToPrint));
			w.newLine();
	
			for (UserFeatures uf : userFeaturesCollection) { // for each set of user features
				if (uf.isComplete()) {
					w.append(Features.values(featuresToPrint, uf));
					w.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printResultSet(ResultSet rs) throws SQLException {
		// for printing out the rows
		rs.beforeFirst();
		while (rs.next()) {
			for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
				System.out.print(rs.getObject(i + 1) + ", ");
			}
			System.out.println();
		}
		rs.beforeFirst();
	}

	/**
	 * @param auction
	 * @param list list of bids for this auction in ascending price order
	 */
	protected void processAuction(AuctionObject auction, List<BidObject> list) {
		// convert bidList into a multimap
		ArrayListMultimap<Integer, BidObject> bidsByUser = ArrayListMultimap.create();
		for (BidObject bo : list) {
			boolean unique = bidsByUser.put(bo.bidderId, bo);
			if (!unique)
				throw new AssertionError("Every bidObject's price should be different.");
		}
		
		// create a new UserFeatures the bidder if there is no UserFeatures object for them yet
		for (int bidderId : bidsByUser.keySet()) {
			if (!userFeaturesMap.containsKey(bidderId)) {
				UserFeatures uf = new UserFeatures(bidderId);
				userFeaturesMap.put(bidderId, uf);
			}
		}
		
		// record when bids are made
		recordFirstLastBidTimes(auction.endTime, bidsByUser);
		recordBidTimes(auction.endTime, list.get(0).time, bidsByUser);
		recordBidProportions(bidsByUser);
		updateSelfBidInterval(bidsByUser);
		updateAnyBidInterval(list);
		
		// record bid counts, amounts and increments
		recordBidAmounts(list);
		recordLastBidAmount(list.get(list.size() - 1).amount, bidsByUser);

		// record who won the auction
		if (userFeaturesMap.containsKey(auction.winnerId))
			userFeaturesMap.get(auction.winnerId).auctionsWon++;
		
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			uf.categories.add(null); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject
			uf.auctionCount++; // *** should be last in this method because of auctionCount increment *** 
		}
	}
	
	/**
	 * record bid counts, amounts and increments
	 * @param list
	 */
	protected void recordBidAmounts(List<BidObject> list) {
		BidObject firstBid = list.get(0);
		BidObject lastBid = list.get(list.size() - 1);
		addBid(userFeaturesMap.get(firstBid.bidderId), firstBid.amount, -1, lastBid.amount);
		if (list.size() >= 1) {
			// record bid increments
			for (int i = 1; i < list.size(); i++) {
				BidObject bo = list.get(i);
				addBid(userFeaturesMap.get(bo.bidderId), bo.amount, list.get(i - 1).amount, lastBid.amount);
			}
		}
	}
	
	/**
	 * 
	 * Adds this new bid to the list of bids made by the user. If <code>previousBid < 0</code>, this bid is the first in
	 * the auction, and therefore has no increment or minIncrement.
	 * 
	 * @param uf the user to update
	 * @param bid value of the bid
	 * @param previousBid value of the previous bid
	 * @param maximumBid the value of the last/highest bid in the auction
	 */
//	BufferedWriter bw2; 
//	{
//		try {
//			bw2 = Files.newBufferedWriter(Paths.get("C:/asdf.txt"), Charset.defaultCharset());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	public void addBid(UserFeatures uf, int bid, int previousBid, int maximumBid) {
		if (previousBid > 0) { // test whether there's a previous bid
			int increment = bid - previousBid; // find the difference between this and the previous bid amount
//			uf.avgBidInc = Util.incrementalAvg(uf.avgBidInc, uf.bidIncCount, increment);
			uf.getAvgBidInc().addNext(increment);
			

			int incMinusMin = increment - Util.minIncrement(previousBid);
			if (incMinusMin < 0)
				incMinusMin = 0;
			uf.getAvgBidIncMinusMinInc().addNext(incMinusMin);
		}
		// update average bid value
		uf.getAvgBid().addNext(bid);
		// update avgBidComparedToFinal
		double fractionOfMax = ((double) bid) / maximumBid;
//		try {
//		bw2.write(fractionOfMax + "");
//		bw2.newLine();
//		bw2.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		uf.getAvgBidAmountComparedToMax().addNext(fractionOfMax);
	}

	/**
	 * calculate & record the interval of time between bids made by anyone
	 * 
	 * @param list
	 */
	private void updateAnyBidInterval(List<BidObject> list) {
		// if there's only 1 bid in the auction, there is no interval.
		if (list.size() <= 1)
			return;
		
		for (int i = 1; i < list.size(); i++) {
			int bidderId = list.get(i).bidderId;
			long interval = Util.timeDiffInMin(list.get(i).time, list.get(i - 1).time);
			assert (interval >= 0);
			UserFeatures user = userFeaturesMap.get(bidderId);
			user.getAnyBidInterval().addNext(interval);
		}
	}

	/**
	 * calculate & record the interval of time, in minutes, between bids made by the SAME USER
	 * 
	 * @param bidsByUser
	 */
	protected void updateSelfBidInterval(ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			List<BidObject> userBidList = bidsByUser.get(bidderId);
			// if there is only 1 bid in the list, there is no interval.
			UserFeatures user = userFeaturesMap.get(bidderId);
			for (int i = 1; i < userBidList.size(); i++) {
				long interval = Util.timeDiffInMin(userBidList.get(i).time, userBidList.get(i - 1).time);
				assert (interval >= 0);
				user.getSelfBidInterval().addNext(interval);
			}
		}
	}

	private void recordLastBidAmount(int finalPrice, ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			int highestAmount = bidsByUser.get(bidderId).get(bidsByUser.get(bidderId).size() - 1).amount;
			
			double finalBidComparedToMax = (double) highestAmount / finalPrice;
			uf.getAvgFinalBidComparedToMax().addNext(finalBidComparedToMax);
			
			uf.getAvgFinalBidAmount().addNext(highestAmount);
		}
	}

	/** record proportion of bids in the auction are made by each user */
	protected void recordBidProportions(ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			double bidProp = ((double) bidsByUser.get(bidderId).size() / bidsByUser.size());
			uf.getAvgBidProp().addNext(bidProp);
			uf.getBidsPerAuc().addNext(bidsByUser.get(bidderId).size());
		}
	}
	
	private void recordFirstLastBidTimes(Date auctionEnd, ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures user = userFeaturesMap.get(bidderId);
			// record how many minutes before the end of the auction the FIRST bid was made by each user
			long firstBidTime = Util.timeDiffInMin(auctionEnd, bidsByUser.get(bidderId).get(0).time);
			user.getFirstBidTime().addNext(firstBidTime);
			
			// record how many minutes before the end of the auction the LAST bid was made by each user
			long lastBidTime = Util.timeDiffInMin(auctionEnd, bidsByUser.get(bidderId).get(bidsByUser.get(bidderId).size() - 1).time);
			user.getLastBidTime().addNext(lastBidTime);
			
			// System.out.println("firstBidTimes: " + user.firstBidTimes);
		}

		// time from first bid to the end of the auction, and the number of bids
		// System.out.print((auctionEnd.getTime() - bidList.get(0).time.getTime()) / 60000 + "," + bidList.size() +
		// ";");
		// System.out.print(userBidPeriods.size() + ",");
	}

	private void recordBidTimes(Date auctionEnd, Date firstBidTime, ArrayListMultimap<Integer, BidObject> bidsByUser) {
		long length = Util.timeDiffInMin(auctionEnd, firstBidTime);
		// System.out.println("auction length: " + length);

		for (int userId : bidsByUser.keySet()) {
			UserFeatures userFeature = userFeaturesMap.get(userId);
			for (BidObject bo : bidsByUser.get(userId)) {
				// long bidMinsBeforeEnd = timeDiffInMin(bidList.get(bidList.size() - 1).time, bo.time);
				long bidMinsBeforeEnd = Util.timeDiffInMin(auctionEnd, bo.time);
				userFeature.getBidTimesMinsBeforeEnd().addNext(bidMinsBeforeEnd);
	
				// time of bid as a fraction from the time of the first bid in the auction to the end time
				double fractionElapsed = ((double) Util.timeDiffInMin(bo.time, firstBidTime)) / length;
				userFeature.getBidTimesFractionToEnd().addNext(fractionElapsed);
			}
		}
	}
	
	protected static void removeIncompleteUserFeatures(Map<Integer, UserFeatures> userFeatures) {
		Iterator<UserFeatures> it = userFeatures.values().iterator();
		while (it.hasNext()) {
			UserFeatures uf = it.next();
			if (!uf.isComplete())
				it.remove();
		}
	}

	public abstract Map<Integer, UserFeatures> build();

	public Map<Integer, UserFeatures> reclustering_build(int clusterId) {
		throw new UnsupportedOperationException();
	}
	
	public abstract static class AuctionObject {
		public final int listingId;
		public final int sellerId;
		public final int winnerId;
		public final Date endTime;
		public AuctionObject(int listingId, int winnerId, int sellerId, Date endTime) {
			this.listingId = listingId;
			this.winnerId = winnerId;
			this.sellerId = sellerId;
			this.endTime = endTime;
		}
		
		@Override
		public String toString() {
			return "(sellerId: " + sellerId + ", endTime: " + endTime + ")";
		}
	}

	public static class TMAuction extends AuctionObject {
		final String category;

		public TMAuction(int listingId, int winnerId, int sellerId, Date endTime, String category) {
			super(listingId, winnerId, sellerId, endTime);
			this.category = category;
		}

		@Override
		public String toString() {
			return "(" + listingId + ", " + category + ", " + endTime + ")";
		}
	}

	public static class SimAuction extends AuctionObject {
		public final int itemTypeId;

		public SimAuction(int listingId, int winnerId, int sellerId, Date endTime, int itemTypeId) {
			super(listingId, winnerId, sellerId, endTime);
			this.itemTypeId = itemTypeId;
		}

		public SimAuction(Auction auction) {
			super(auction.getId(), 
					auction.getWinner().getId(), 
					auction.getSeller().getId(), 
					BuildSimFeatures.convertTimeunitToTimestamp(auction.getEndTime()));
			this.itemTypeId = auction.getItem().getType().getId();
		}
		
		@Override
		public String toString() {
			return "(" + listingId + ", " + itemTypeId + ", " + endTime + ")";
		}
	}

	public static class BidObject {
		public final int bidderId;
		public final int listingId;
		public final Date time;
		public final int amount;
		public BidObject(int bidderId, int listingId, Date time, int amount) {
			this.bidderId = bidderId;
			this.listingId = listingId;
			this.time = time;
			this.amount = amount;
		}
		
		public BidObject(Auction auction, Bid bid) {
			this.bidderId = bid.getBidder().getId();
			this.listingId = auction.getId();
			this.time = BuildSimFeatures.convertTimeunitToTimestamp(bid.getTime());
			this.amount = bid.getPrice();
		}
		
		@Override
		public String toString() {
			return "(bidderId:" + bidderId + ", listingId: " + listingId + ", time: " + time + ", amount: " + amount + ")";
		}
	}
	
	public static class UserObject {
		public final int userId;
		public final int posUnique;
		public final int negUnique;
		public final String userType;
		public UserObject(SimpleUser user) {
			this.userId = user.getId();
			this.posUnique = user.getReputationRecord().getPosUnique();
			this.negUnique = user.getReputationRecord().getNegUnique();
			this.userType = user.getClass().getSimpleName();
		}
		public UserObject(int userId, int posUnique, int negUnique, String userType) {
			this.userId = userId;
			this.posUnique = posUnique;
			this.negUnique = negUnique;
			this.userType = userType;
		}
		
	}

}
