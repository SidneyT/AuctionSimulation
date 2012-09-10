package createUserFeatures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

import util.Util;

/**
 * Builds and updates UserFeatures objects using auction bidding information.
 */
public abstract class BuildUserFeatures {
	protected static final double BEG_MID_BOUNDARY = 0.5;
	protected static final double MID_END_BOUNDARY = 0.95;

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

	public static void writeToFile(Collection<UserFeatures> userFeaturesCollection, List<Feature> featuresToPrint, Path path) {
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
	 * @param bidList list of bids for this auction in ascending price order
	 */
	protected void processAuction(AuctionObject auction, List<BidObject> bidList) {
		// convert bidList into a multimap
		ArrayListMultimap<Integer, BidObject> bidsByUser = ArrayListMultimap.create();
		for (BidObject bo : bidList) {
			boolean unique = bidsByUser.put(bo.bidderId, bo);
			if (!unique)
				throw new AssertionError("Every bidObject's price should be different.");
		}
		
		// create a new UserFeatures the bidder if there is no UserFeatures object for them yet
		for (int bidderId : bidsByUser.keySet()) {
			if (!userFeaturesMap.containsKey(bidderId)) {
				UserFeatures uf = new UserFeatures();
				uf.setUserId(bidderId);
				userFeaturesMap.put(bidderId, uf);
			}
		}
		
		// record when bids are made
		recordBidPeriods(auction.endTime, bidsByUser);
		recordUserBidPeriods(auction.endTime, bidList);
		updateSelfBidInterval(bidsByUser);
		updateAnyBidInterval(bidList);
		
		recordBidProportions(bidsByUser);
		
		// record bid counts, amounts and increments
		recordBidAmounts(bidList);
		recordLastBidAmountProportionOfMax(bidList.get(bidList.size() - 1).amount, bidsByUser);

		// record who won the auction
		if (userFeaturesMap.containsKey(auction.winnerId))
			userFeaturesMap.get(auction.winnerId).auctionCount++;
		
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			uf.categories.add(null); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject
			uf.auctionCount++; // *** should be last in this method because of auctionCount increment *** 
		}
	}
	
	/**
	 * record bid counts, amounts and increments
	 * @param bidList
	 */
	protected void recordBidAmounts(List<BidObject> bidList) {
		BidObject firstBid = bidList.get(0);
		BidObject lastBid = bidList.get(bidList.size() - 1);
		addBid(userFeaturesMap.get(firstBid.bidderId), firstBid.amount, -1, lastBid.amount);
		if (bidList.size() >= 1) {
			// record bid increments
			for (int i = 1; i < bidList.size(); i++) {
				BidObject bo = bidList.get(i);
				addBid(userFeaturesMap.get(bo.bidderId), bo.amount, bidList.get(i - 1).amount, lastBid.amount);
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
	public void addBid(UserFeatures uf, int bid, int previousBid, int maximumBid) {
		if (previousBid > 0) { // test whether there's a previous bid
			int increment = bid - previousBid; // find the difference between this and the previous bid amount
//			uf.avgBidInc = Util.incrementalAvg(uf.avgBidInc, uf.bidIncCount, increment);
			uf.avgBidInc.addNext(increment);
			

			int incMinusMin = increment - Util.minIncrement(previousBid);
			if (incMinusMin < 0)
				incMinusMin = 0;
			if (Double.isNaN(uf.avgBidIncMinusMinInc))
				uf.avgBidIncMinusMinInc = 0;
			uf.avgBidIncMinusMinInc = Util.incrementalAvg(uf.avgBidIncMinusMinInc(), uf.bidIncCount, incMinusMin);
			uf.bidIncCount++;
		}
		// update average bid value
		uf.avgBid = Util.incrementalAvg(uf.avgBid, uf.bidCount, bid);
		// update avgBidComparedToFinal
		double fractionOfMax = ((double) bid) / maximumBid;
		uf.getAvgBidAmountComparedToMax().addNext(fractionOfMax);
		uf.bidCount++;
	}

	/**
	 * calculate & record the interval of time between bids made by anyone
	 * 
	 * @param bidList
	 */
	private void updateAnyBidInterval(List<BidObject> bidList) {
		// if there's only 1 bid in the auction, there is no interval.
		if (bidList.size() <= 1)
			return;
		
		for (int i = 1; i < bidList.size(); i++) {
			int bidderId = bidList.get(i).bidderId;
			long interval = Util.timeDiffInMin(bidList.get(i).time, bidList.get(i - 1).time);
			assert (interval >= 0);
			UserFeatures user = userFeaturesMap.get(bidderId);
			user.getAnyBidInterval().addNext(interval);
		}
	}

	/**
	 * calculate & record the interval of time between bids made by the SAME USER
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

	/**
	 * @param auctionEnd
	 * @param bidList
	 * 
	 * @return Map containing mapping between userId and a 4 element array of doubles. The first 3 elements are the
	 *         number of bids made in BEG, MID and END of the auction. The 4th element counts the number of bids that
	 *         user has in bidList.
	 * 
	 */
	protected Map<Integer, double[]> findBidPeriods(Date auctionEnd, List<BidObject> bidList) {
		Map<Integer, double[]> dist = new HashMap<Integer, double[]>();
		long length = Util.timeDiffInMin(auctionEnd, bidList.get(0).time);
		// System.out.println("auction length: " + length);

		for (BidObject bo : bidList) {
			// long bidMinsBeforeEnd = timeDiffInMin(bidList.get(bidList.size() - 1).time, bo.time);
			long bidMinsBeforeEnd = Util.timeDiffInMin(auctionEnd, bo.time);
			assert bidMinsBeforeEnd >= 0;
			userFeaturesMap.get(bo.bidderId).getBidTimesMinsBeforeEnd().add(bidMinsBeforeEnd);

			// time of bid as a fraction from the time of the first bid in the auction to the end time
			double fractionElapsed = ((double) Util.timeDiffInMin(bo.time, bidList.get(0).time)) / length;
			assert fractionElapsed <= 1;
			userFeaturesMap.get(bo.bidderId).getBidTimesFractionToEnd().add(fractionElapsed);

			// store the bidPeriod this fractionToEnd corresponds to
			double[] userDist = dist.get(bo.bidderId);
			if (userDist == null) {
				userDist = new double[4];
				Arrays.fill(userDist, 0);
				dist.put(bo.bidderId, userDist);
			}

			BidPeriod period = findBidPeriod(fractionElapsed);
			for (int i = 0; i < 3; i++) { // increment the correct bin: beg, mid or end
				if (i == period.getI()) {
					userDist[i] = Util.incrementalAvg(userDist[i], (int) userDist[3], 1);
				} else {
					userDist[i] = Util.incrementalAvg(userDist[i], (int) userDist[3], 0);
				}
			}
			userDist[3]++; // increment count
		}

		// print out the proportions found for each user for this bidList
		// for (int id : dist.keySet()) {
		// System.out.println(id + ":" + Arrays.toString(dist.get(id)));
		// }
		return dist;
	}

	private void recordLastBidAmountProportionOfMax(int finalPrice, ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			double finalBidComparedToMax = (double) bidsByUser.get(bidderId).get(bidsByUser.get(bidderId).size() - 1).amount / finalPrice;
			uf.avgFinalBidComparedToMax = Util.incrementalAvg(uf.avgFinalBidComparedToMax, uf.auctionCount, finalBidComparedToMax); 
		}
	}

	/** record proportion of bids in the auction are made by each user */
	protected void recordBidProportions(Multimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			double bidProp = ((double) bidsByUser.get(bidderId).size() / bidsByUser.size());
			uf.getAvgBidProp().addNext(bidProp);
			uf.getBidsPerAuc().addNext(bidsByUser.get(bidderId).size());
		}
	}
	
	protected void recordBidPeriods(Date auctionEnd, ArrayListMultimap<Integer, BidObject> bidsByUser) {
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

	private void recordUserBidPeriods(Date auctionEndtime, List<BidObject> bidList) {
		// find the proportion of bids made in the Beginning, Middle and End of an auction
		Map<Integer, double[]> userBidPeriods = findBidPeriods(auctionEndtime, bidList);
		// record the proportion in the UserFeatures object
		for (int bidderId : userBidPeriods.keySet()) {
			// UserFeatures.incrementalAvg(currAvg, currNumElements, nextValue)
			double[] bidPeriod = userFeaturesMap.get(bidderId).bidPeriods; // get the stored bidPeriod
			double[] newBidPeriod = userBidPeriods.get(bidderId); // get the new bidPeriod
			for (int i = 0; i < 3; i++) {
				// update stored bidPeriod using new BidPeriod
				// averages are weighted using the number of bids made in each auction (in index 3)
				bidPeriod[i] = Util.incrementalAvg(bidPeriod[i], (int) bidPeriod[3], newBidPeriod[i],
						(int) newBidPeriod[3]);
			}
			bidPeriod[3] += newBidPeriod[3];
		}
	}
	
	/**
	 * @param fractionToEnd
	 *            proportion of time elapsed from the first bid to the end of the auction
	 * @return the BidPeriod the number falls in
	 */
	protected BidPeriod findBidPeriod(double fractionToEnd) {
		if (fractionToEnd < BEG_MID_BOUNDARY) {
			return BidPeriod.BEGINNING;
		} else if (fractionToEnd < MID_END_BOUNDARY) {
			return BidPeriod.MIDDLE;
		} else {
			return BidPeriod.END;
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
	
	protected abstract class AuctionObject {
		final int auctionId;
		final Date endTime;
		final int winnerId;

		public AuctionObject(int auctionId, Date endTime, int winnerId) {
			this.auctionId = auctionId;
			this.endTime = endTime;
			this.winnerId = winnerId;
		}
	}
	
	/**
	 * POJO
	 */
	protected class TMAuctionObject extends AuctionObject{
		final String category;

		public TMAuctionObject(int auctionId, String category, Date endTime, int winnerId) {
			super(auctionId, endTime, winnerId);
			this.category = category;
		}

		@Override
		public String toString() {
			return "(" + auctionId + ", " + category + ", " + endTime + ")";
		}
	}

	/**
	 * POJO
	 */
	protected class SimAuctionObject extends AuctionObject{
		final int itemTypeId;

		public SimAuctionObject(int auctionId, int itemTypeId, Date endTime, int winnerId) {
			super(auctionId, endTime, winnerId);
			this.itemTypeId = itemTypeId;
		}

		@Override
		public String toString() {
			return "(" + auctionId + ", " + itemTypeId + ", " + endTime + ")";
		}
	}

	/**
	 * POJO
	 */
	protected static class BidObject {
		final int bidderId;
		final int amount;
		final Date time;

		public BidObject(int bidderId, int amount, Date time) {
			this.bidderId = bidderId;
			this.amount = amount;
			this.time = time;
		}

		@Override
		public String toString() {
			return "(" + bidderId + ", " + amount + ", " + time + ")";
		}
		
		public enum BidObjectComparator implements Comparator<BidObject> {
			PRICE_SORT_ASC {
				@Override
				public int compare(BidObject o1, BidObject o2) {
					if (o1.amount == o2.amount)
						throw new AssertionError("Can't be used to sort lists with bids with equal amounts.");
					return o1.amount - o2.amount;
				}
			}
		}
	}
}
