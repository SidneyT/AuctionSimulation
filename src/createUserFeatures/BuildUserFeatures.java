package createUserFeatures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import createUserFeatures.BuildUserFeatures.BidObject.BidObjectComparator;
import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

import util.Util;

/**
 * Builds and updates UserFeatures objects using auction bidding information.
 */
public abstract class BuildUserFeatures {
	protected static final double BEG_MID_BOUNDARY = 0.5;
	protected static final double MID_END_BOUNDARY = 0.95;

	protected TreeMap<Integer, UserFeatures> userFeaturesMap;
	public boolean trim; // trim auction bid list lengths to 20

	public BuildUserFeatures() {
		this.userFeaturesMap = new TreeMap<Integer, UserFeatures>();
		trim = false;
	}

	public boolean trim() {
		return trim;
	}

	public static void writeToFile(Collection<UserFeatures> userFeaturesCol, List<Feature> featuresToPrint, Path path) {
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeToFile(userFeaturesCol, featuresToPrint, bw);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeToFile(Collection<UserFeatures> userFeaturesCollection, List<Feature> featuresToPrint,
			BufferedWriter w) throws IOException {

		// print headings
		w.append(Features.labels(featuresToPrint));
		w.newLine();

		for (UserFeatures uf : userFeaturesCollection) { // for each set of user features
			if (uf.isComplete()) {
				w.append(Features.values(featuresToPrint, uf));
				w.newLine();
			}
		}
		w.newLine();
		w.close();
	}

	/**
	 * POJO
	 */
	protected class TMAuctionObject {
		final int auctionId;
		final String category;
		final Date endTime;
		final int winnerId;

		public TMAuctionObject(int auctionId, String category, Date endTime, int winnerId) {
			this.auctionId = auctionId;
			this.category = category;
			this.endTime = endTime;
			this.winnerId = winnerId;
		}

		@Override
		public String toString() {
			return "(" + auctionId + ", " + category + ", " + endTime + ")";
		}
	}

	/**
	 * POJO
	 */
	protected class SimAuctionObject {
		final int auctionId;
		final int itemTypeId;
		final Date endTime;
		final int winnerId;

		public SimAuctionObject(int auctionId, int itemTypeId, Date endTime, int winnerId) {
			this.auctionId = auctionId;
			this.itemTypeId = itemTypeId;
			this.endTime = endTime;
			this.winnerId = winnerId;
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
	 * record bid counts, amounts and increments
	 * @param bidList
	 */
	protected void recordBidAmounts(List<BidObject> bidList) {
		BidObject firstBid = bidList.get(0);
		BidObject lastBid = bidList.get(bidList.size() - 1);
		userFeaturesMap.get(firstBid.bidderId).addBid(firstBid.amount, -1, lastBid.amount);
		if (bidList.size() >= 1) {
			// record bid increments
			for (int i = 1; i < bidList.size(); i++) {
				BidObject bo = bidList.get(i);
				userFeaturesMap.get(bo.bidderId).addBid(bo.amount, bidList.get(i - 1).amount, lastBid.amount);
			}
		}
	}

	/**
	 * calculate & record the interval of time between bids made by anyone
	 * 
	 * @param bidList
	 * @param bidListsByUser
	 */
	private void updateAnyBidInterval(List<BidObject> bidList, ArrayListMultimap<Integer, BidObject> bidListsByUser) {
		// if there's only 1 bid in the auction, there is no interval.
		if (bidList.size() > 1) {
			// Map<bidderId, average>
			Map<Integer, Integer> intervalTotal = new HashMap<>();
			for (int bidderId : bidListsByUser.keySet()) { // initialise map with all bidders from the auction
				intervalTotal.put(bidderId, (int) 0);
			}

			for (int i = 1; i < bidList.size(); i++) {
				int bidderId = bidList.get(i).bidderId;
				long interval = Util.timeDiffInMin(bidList.get(i).time, bidList.get(i - 1).time);
				assert (interval >= 0);
				intervalTotal.put(bidderId, (int) (intervalTotal.get(bidderId) + interval));
			}

			// update userFeature objects with intervalAverage and weights
			int firstBidder = bidList.get(0).bidderId;
			for (int bidderId : bidListsByUser.keySet()) {
				UserFeatures user = userFeaturesMap.get(bidderId);
				int numberOfIntervals;
				// the bidder who bid first does not have a bid interval for that bid, since there is no preceeding bid
				if (bidderId == firstBidder) {
					numberOfIntervals = bidListsByUser.get(bidderId).size() - 1;
				} else {
					numberOfIntervals = bidListsByUser.get(bidderId).size();
				}

				if (numberOfIntervals >= 1) // if the only bid made was the first bid for the auction, there is no bid
											// interval for this user
					user.anyBidInterval = Util.incrementalAvg(user.anyBidInterval, user.bidCount,
							(double) intervalTotal.get(bidderId) / numberOfIntervals, numberOfIntervals);
			}
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
			if (userBidList.size() > 1) {
				long intervalTotal = 0;
				UserFeatures user = userFeaturesMap.get(bidderId);
				for (int i = 1; i < userBidList.size(); i++) {
					long interval = Util.timeDiffInMin(userBidList.get(i).time, userBidList.get(i - 1).time);
					intervalTotal += interval;
					assert (interval >= 0);
				}

				user.selfBidInterval = Util.incrementalAvg(user.selfBidInterval, user.bidCount, (double) intervalTotal
						/ (userBidList.size() - 1), userBidList.size() - 1);
				// System.out.println("selfBidInterval: " + user.selfBidInterval);
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
			userFeaturesMap.get(bo.bidderId).bidTimesMinsBeforeEnd.add(bidMinsBeforeEnd);

			// time of bid as a fraction from the time of the first bid in the auction to the end time
			double fractionElapsed = ((double) Util.timeDiffInMin(bo.time, bidList.get(0).time)) / length;
			assert fractionElapsed <= 1;
			userFeaturesMap.get(bo.bidderId).bidTimesFractionToEnd.add(fractionElapsed);

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

	/**
	 * @param ao
	 * @param bidList list of bids for this auction in ascending price order
	 */
	protected void processAuction(Date auctionEndtime, int winnerId, List<BidObject> bidList) {
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
		recordBidPeriods(auctionEndtime, bidsByUser);
		recordUserBidPeriods(auctionEndtime, bidList);
		updateSelfBidInterval(bidsByUser);
		updateAnyBidInterval(bidList, bidsByUser);
		

		recordLastBidAmountProportionOfMax(bidList.get(bidList.size() - 1).amount, bidsByUser);
		recordBidProportions(bidsByUser);
		
		// record bid counts, amounts and increments
		recordBidAmounts(bidList);

		// record who won the auction
		if (userFeaturesMap.containsKey(winnerId))
			userFeaturesMap.get(winnerId).addWonAuction();
		
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures uf = userFeaturesMap.get(bidderId);
			uf.categories.add(null); // TODO: fix the null category, because sim auctions have an item type, which has a category, but doesn't have a direct category..., in contrast to TMAuctionObject
			uf.auctionCount++; // *** should be last in this method because of auctionCount increment *** 
		}
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
			uf.avgBidProp = Util.incrementalAvg(uf.avgBidProp, uf.getAuctionCount(), bidProp);
			uf.bidsPerAuc = Util.incrementalAvg(uf.bidsPerAuc, uf.getAuctionCount(), bidsByUser.get(bidderId).size());
			
		}
	}
	
	protected void recordBidPeriods(Date auctionEnd, ArrayListMultimap<Integer, BidObject> bidsByUser) {
		for (int bidderId : bidsByUser.keySet()) {
			UserFeatures user = userFeaturesMap.get(bidderId);
			// record how many minutes before the end of the auction the FIRST bid was made by each user
			long firstBidTime = Util.timeDiffInMin(auctionEnd, bidsByUser.get(bidderId).get(0).time);
			user.firstBidTime = Util.incrementalAvg(user.firstBidTime, user.getAuctionCount(), firstBidTime);
			
			// record how many minutes before the end of the auction the LAST bid was made by each user
			long lastBidTime = Util.timeDiffInMin(auctionEnd, bidsByUser.get(bidderId).get(bidsByUser.get(bidderId).size() - 1).time);
			user.lastBidTime = Util.incrementalAvg(user.lastBidTime, user.getAuctionCount(), lastBidTime);
			
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

	public abstract TreeMap<Integer, UserFeatures> build();

	public TreeMap<Integer, UserFeatures> reclustering_build(int clusterId) {
		throw new UnsupportedOperationException();
	}
}
