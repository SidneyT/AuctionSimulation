package agents.sellers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import agents.SimpleUser;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Item;
import simulator.objects.Auction.AuctionLength;
import util.Sample;

/**
 * Models the distribution of auction submission frequency of sellers found in TradeMe.
 */
public class TMSeller extends SimpleUser {
	
	private static final Logger logger = Logger.getLogger(TMSeller.class);

	private final List<ItemType> itemTypesUsed;
	private final static Random r = new Random();
	private List<ItemType> itemTypes;

	public TMSeller(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes) {
		super(bh, ps, is, ah);
		
		int numberOfAuctions = numberOfAuctions(r.nextDouble());
		int numberOfTimeUnits = AuctionLength.ONE_DAY.length() * (100 - 7);

		List<Integer> listOfTimes = Sample.randomSample(numberOfTimeUnits, numberOfAuctions, r);
		Collections.sort(listOfTimes, Collections.reverseOrder());
		auctionSubmissionTimes = new ArrayDeque<>(listOfTimes);
//		System.out.println(auctionSubmissionTimes.size());
		itemTypesUsed = new ArrayList<>();
		this.itemTypes = itemTypes;
	}

	ArrayDeque<Integer> auctionSubmissionTimes; // the times at which this agent will submit an auction
	
	public void run() {
		long currentTime = this.bh.getTime();
		if (!auctionSubmissionTimes.isEmpty() && currentTime >= auctionSubmissionTimes.peekLast()) {
			auctionSubmissionTimes.removeLast();
			submitAuction();
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
		// 7.17892371446915 is the number of auctions on average that each TMSeller submits every 60 days.
		return (int) (targetPerDay/7.17892371446915 * 60 + 0.5);
	}
	
	protected int numberOfAuctions(double random) {
		// auction submission frequency modelled using a power law
		for (int i = 0; i < probabilities.size(); i++) {
			if (random < probabilities.get(i))
				return i + 1;
		}
		return probabilities.size() + 1;
	}
	
	@Override
	public void expiredAction(Auction auction, int time) {
		// *** THIS IS A BAD IDEA: you end up with more and more empty auctions towards the end of the simulation...
//		// since the auction failed, just re-submit the exact same auction again immediately
//		Auction nAuction = new Auction(this, auction.getItem(), auction.getDuration(), auction.getStartPrice(), auction.getReservePrice(), auction.getPopularity());
//		submitAuction(nAuction);
	}
	
	private static final ArrayList<Double> probabilities;
	static {
		probabilities = new ArrayList<>(373);
		double sum = 0;
		for (int x = 1;; x++) {
			double y = Math.round(17875 * Math.pow(x, -1.77)) / 34266d;
			sum += y;
			probabilities.add(sum);
//			System.out.println(sum);
			if (sum > 0.999999999999)
				break;
		}
	}
	public static void main(String[] args) {
		System.out.println(probabilities);
	}
}
