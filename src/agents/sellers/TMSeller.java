package agents.sellers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Item;
import util.IncrementalMean;

/**
 * Models behaviour of sellers found in TradeMe.
 */
public class TMSeller extends TimedSeller {
	
	private static final Logger logger = Logger.getLogger(TMSeller.class);

	private final ExponentialDistribution exponentialDistribution; // gives next time to submit an auction
	private long nextSubmission; // remembers the time to submit an auction next
	
	private final List<ItemType> itemTypesUsed;
	
	public TMSeller(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> types) {
		super(bh, ps, is, ah, types);
		
		int numberOfAuctions = SellerAuctionListingFrequency.numberOfAuctions(); // number of auctions per 60 days
		
		// number of auctions over 60 days worth of time units. so * (60 * 24 * 60 * 5)
		exponentialDistribution = new ExponentialDistribution((double) (60 * 24 * 60 / 5) / numberOfAuctions);
		nextSubmission = Math.round(exponentialDistribution.sample());

		itemTypesUsed = new ArrayList<>();
	}

	public void action() {
		long currentTime = this.bh.getTime();
		if (currentTime == nextSubmission) {
			do {
				submitAuction();
				nextSubmission = Math.round(exponentialDistribution.sample()) + currentTime;
			} while (nextSubmission == currentTime);
		}
	}
	
	private int auctionsSubmitted = 0;
	private void submitAuction() {
		
		double logParam = 2.5; // from TM data. See countCategorySpread.xlsx
		
		ItemType type;
		if (useNewAuctionCategory(++auctionsSubmitted, logParam, r.nextDouble())) { // pick a new type
			type = CreateItemTypes.pickType(itemTypes, r.nextDouble());
			itemTypesUsed.add(type);
		} else { // use an previously used type
			type = chooseOldItemType(logParam, itemTypesUsed, r.nextDouble());
		}
		
		Item newItem = new Item(type, "item" + r.nextInt(100000));
		double popularity;
		if (r.nextDouble() < 0.5)
			popularity = 0.7;
		else
			popularity = 1;
		Auction auction = new Auction(this, newItem, 2016, 100, 0, popularity);
		this.bh.getAuctionMessagesToAh().put(auction);
	}
	
	/**
	 * Calculate the probability of a new category being used for the item being sold.
	 * See CategoryCountSpread.xlsx
	 * @param auctionNumber Count of auctions to submit, including this. Starts from 1.
	 * @param logParam
	 * @return
	 */
	public static boolean useNewAuctionCategory(int auctionNumber, double logParam, double rand) {
		if (auctionNumber < 1 || logParam < 1)
			throw new IllegalArgumentException();

		if (auctionNumber == 1)
			return true;
		
		// log( (x + 1) / x), log-base to be given
		double probNewCat = FastMath.log(1 + (double) 1 / auctionNumber) / FastMath.log(logParam);
		return rand < probNewCat; // new category
	}
	
	public static ItemType chooseOldItemType(double logParam, List<ItemType> itemTypes, double rand) {
		// sum of log values for each category in itemTypesUsed
		// log( (x + 1) / x), log-base to be given
		double totalWeight = FastMath.log(logParam, itemTypes.size() + 1);
		// x = 2 * k ^ (y - 1), where k is the log base, y is a random number between 1 and totalWeight
		// basically given a random number, decides which category to reuse.
		double scaledRand = rand * totalWeight;
		int index = (int) (FastMath.pow(logParam, scaledRand)) - 1; // gives a value between 0 and itemTypesUsed.size() exclusive
		return itemTypes.get(index);
	}

	public static int sellersRequired(double targetPerDay) {
		// 5.539 is the number of auctions on average that each TMSeller submits every 60 days.
		return (int) (targetPerDay/5.539 * 60 + 0.5);
	}
}
