package agents.bidders;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import distributions.Exponential;
import distributions.Normal;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.records.ReputationRecord;
import util.Sample;
import util.Util;
import agents.SimpleUser;
import agents.SimpleUserI;
import agents.sellers.TMSeller;

/**
 * Not thread safe.
 */
public abstract class ClusterBidder extends SimpleUser {
	
//	private static final List<Integer> allClusterBidderIds = new ArrayList<>();
	
	// constants...
//	protected static final int ONE_DAY = 24*60/AuctionHouse.UNIT_LENGTH;
	// probability of bidding when auction has 1-7 days to go, adjusted by number of users 
	
	// parameters
	private static final Logger logger = Logger.getLogger(ClusterBidder.class);
	
	protected final List<Auction> newAuctionsUnprocessed; // should be empty at the beginning of each time unit
	protected final HashMultimap<Integer, Auction> auctionsToBidIn;
	
	protected double privateValuationProportion;

	protected final Random r;
	
	protected final List<ItemType> itemTypes;
	
	protected final Multiset<SimpleUserI> seenUsers; // records the set of users whose auctions this agent has bid in, and the frequency
	
	public ClusterBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes, int id) {
		super(bh, ps, is, ah, id);

		this.itemTypes = itemTypes;
		
		this.newAuctionsUnprocessed = new ArrayList<Auction>();
		this.auctionsToBidIn = HashMultimap.create();
		
		r = new Random();
		ReputationRecord.generateRep(rr, r);
		seenUsers = HashMultiset.create();
		
		// normal distribution, mean 1, std dev 0.15
		privateValuationProportion = Normal.nextDouble(r.nextDouble(), 1, 0.2);
		if (privateValuationProportion < 0)
			privateValuationProportion *= -1;
		
		maxAuctions = numberOfAuctionsPer100Days(r.nextDouble());
		List<Integer> times = Sample.randomSample(AuctionHouse.HUNDRED_DAYS - AuctionHouse.SEVEN_DAYS, maxAuctions, r); // equals 26784 time units which is (100 - 7) days
		Collections.sort(times, Collections.reverseOrder());
		interestTimes = new ArrayDeque<>(times);
		
		this.maxNumberOfInterestedCategories = numberOfInterestedCategories(interestTimes.size());
	}
	
	public ClusterBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes) {
		super(bh, ps, is, ah);

		this.itemTypes = itemTypes;
		
		this.newAuctionsUnprocessed = new ArrayList<Auction>();
		this.auctionsToBidIn = HashMultimap.create();
		
		r = new Random();
		ReputationRecord.generateRep(rr, r);
		seenUsers = HashMultiset.create();
		
		// normal distribution, mean 1, std dev 0.15
		privateValuationProportion = Normal.nextDouble(r.nextDouble(), 1, 0.2);
		if (privateValuationProportion < 0)
			privateValuationProportion *= -1;
		
		maxAuctions = numberOfAuctionsPer100Days(r.nextDouble());
		List<Integer> times = Sample.randomSample(AuctionHouse.HUNDRED_DAYS - AuctionHouse.SEVEN_DAYS, maxAuctions, r); // equals 26784 time units which is (100 - 7) days
		Collections.sort(times, Collections.reverseOrder());
		interestTimes = new ArrayDeque<>(times);
		
		this.maxNumberOfInterestedCategories = numberOfInterestedCategories(interestTimes.size());
	}
	
	protected abstract int firstBidTime();
	
	private final int maxNumberOfInterestedCategories; 
	
	private int numberOfInterestedCategories(int totalNumberOfAuctions) {
		int numberOfCategories = 0;
		
		for (int i = 0; i < totalNumberOfAuctions; i++) {
			if(TMSeller.useNewAuctionCategory(i + 1, logParam, r.nextDouble()))
				numberOfCategories++;
		}

		return numberOfCategories;
	}
	
	private Set<ItemType> itemTypesBidOn = new HashSet<>();
	private static final double logParam = 2.4;
	
	public static long earlyCount, sniperCount;
	
	/**
	 * Selects auctions to bid in, and the time to begin bidding in them.
	 */
	public void selectAuctionsToBidIn() {
		int currentTime = this.bh.getTime();

		Collections.shuffle(newAuctionsUnprocessed); // randomise order of the newAuctions
		for (Auction auction : newAuctionsUnprocessed) {
			
			if (timeToParticipateInAuction(currentTime)) { // standard path: time to make a bid in an auction
				
				// less likely to be interested in an auction if they have a low popularity
				if (r.nextDouble() > auction.getPopularity())
					continue; 
				
				// check if the auction is in a category you're interested in
				if (itemTypesBidOn.contains(auction.getItem().getType())) { // if yes, then bid.
					this.ah.registerForAuction(this, auction);
					int timeToMakeBid = firstBidTime() + currentTime;
					logger.debug(this + " is making first bid in the future at " + timeToMakeBid + " at time " + currentTime + ".");
//					if (this instanceof ClusterEarly) {
//						earlyCount++;
//					} else {
//						sniperCount++;
////						System.out.println("pause");
//					}
					scheduleBid(timeToMakeBid, auction);
					break;
				} else { // if not, check if you are willing to buy things from another category
					
					// check if the number of categories you participated in has reached the predetermined number
					if (maxNumberOfInterestedCategories < itemTypesBidOn.size()) {
						continue;
					}
					
					itemTypesBidOn.add(auction.getItem().getType()); // record the new category you'll bid in
					
					this.ah.registerForAuction(this, auction);
					int timeToMakeBid = firstBidTime() + currentTime;
					logger.debug(this + " is making first bid in the future at " + timeToMakeBid + " at time " + currentTime + ".");
//					if (this instanceof ClusterEarly) {
//						earlyCount++;
//					} else {
//						sniperCount++;
////						System.out.println("pause");
//					}
					scheduleBid(timeToMakeBid, auction);
					break;
				}
				
			} else if (seenUsers.contains(auction.getSeller())) { // non-standard path: auction belongs to someone seen before
				if (!itemTypesBidOn.contains(auction.getItem().getType())) { // if yes, then bid.
					continue;
				}
				
				int timesSeen = seenUsers.count(auction.getSeller());
//				double probBid = FastMath.pow(timesSeen - 1, 1.55) / 800;
				double probBid;
//				if (timesSeen >= 50)
//					probBid = 0.022;
//				else {
					probBid = 0.05 * (timesSeen) * maxAuctions / 2;
					probBid = Math.min(0.12, probBid);
//				}
				if (probBid > r.nextDouble()) {
//					itemTypesBidOn.add(auction.getItem().getType()); // record the new category you'll bid in
					int timeToMakeBid = firstBidTime() + currentTime;
//					if (this instanceof ClusterEarly) {
//						earlyCount++;
//					} else {
//						sniperCount++;
////						System.out.println("pause");
//					}
					scheduleBid(timeToMakeBid, auction);
					break;
				}
				
			} else {
				assert false: "Should be 1 of those if cases above. Should not reach here.";
			}
		}
		
		newAuctionsUnprocessed.clear();
	}
	
	private void scheduleBid(int timeToMakeBid, Auction auction) {
		auctionsToBidIn.put(timeToMakeBid, auction);
		seenUsers.add(auction.getSeller()); // record that this agent has interacted with this seller
		participated();
	}
	
//	public static int debugAverage = 0;
	
	public final Deque<Integer> interestTimes; // times at which to bid in auctions, sorted in decreasing order
	public final int maxAuctions;
	/**
	 * If the scheduled time for participating for an auction has passed,
	 * return true.
	 * @param currentTime
	 * @return
	 */
	protected boolean timeToParticipateInAuction(int currentTime) {
		if (interestTimes.isEmpty()) {
			return false;
		}
		return interestTimes.peekLast() <= currentTime;
	}
	public void participated() {
		interestTimes.removeLast();
	}
	
	/**
	 * Effect of the item's price on probability of bidding.
	 * If the item price is below privateValuation, returns a number > 1.
	 * As itemPrice goes further above privateValuation, return value decreases.
	 * 
	 * @param bidAmount
	 * @param maximumBid
	 * @return
	 */
	protected static double valuationEffect(int bidAmount, double maximumBid) {
//		if (bidAmount <= maximumBid)
//			return 100000000;
//		else
//			return 0;
		return Util.sigmoid(bidAmount/maximumBid);
	}
	
//	@Override
//	public void priceChangeAction(Auction auction, long time) {
//		super.newAction(auction, time);
////		if (auction.getBidCount() == 1)
////			this.oneBidAuctionsUnprocessed.add(auction);
//		
////		if (makeRebidAuctions.contains(auction) && auction.getWinner() != this) {
////			makeRebidAuctions.remove(auction);
////			long bidAmount = calculateBidAmount(auction);
//////			if (r.nextDouble() < likelihoodToRebid(auction.getBidCount()) * valuationEffect(bidAmount, privateValuationProportion)) {
////			if (r.nextDouble() < likelihoodOfRebid * valuationEffect(bidAmount, privateValuationProportion)) {
//////				logger.debug(this + " making rebid for " + auction + " at " + this.currentTime);
////				makeBid(auction);
////			}
////		}
//		
//	}
	
	
	@Override
	public void newAction(Auction auction, int time) {
		super.newAction(auction, time);

		if (interestTimes.isEmpty())
			return;
		
		if (timeToParticipateInAuction(time)) { // add the auction only if agent is scheduled to bid in an auction.
			this.newAuctionsUnprocessed.add(auction);
		} else if (seenUsers.contains(auction.getSeller())) { // if not, add the auction if the agent has seen this seller before
			this.newAuctionsUnprocessed.add(auction);
		}
	}
	
//	@Override
//	public void lossAction(Auction auction, long time) {
//		super.lossAction(auction, time);
//		
////		this.oneBidAuctionsUnprocessed.remove(auction);
//	}
//	
//	@Override
//	public void winAction(Auction auction, long time) {
//		super.winAction(auction, time);
//		
////		this.oneBidAuctionsUnprocessed.remove(auction);
//	}
//	
//	@Override
//	public void expiredAction(Auction auction, long time) {
//		super.expiredAction(auction, time);
////		assert(newAuctionsUnprocessed.contains(auction) == false);
//	}
	
	public void makeBid(Auction auction, int bidAmount) {
		if (auction.getEndTime() < this.bh.getTime()) // don't bid if auction finished
			return;
		if (auction.getWinner() == this) // don't bid if already winning
			return;
		bh.getBidMessageToAh().put(auction, createBid(auction, bidAmount));
		
		logger.debug(this + " is making bid now at time " + this.bh.getTime() + " for " + auction + ".");
	}
	public void makeBid(Auction auction) {
		if (auction.getEndTime() < this.bh.getTime()) // don't bid if auction finished
			return;
		if (auction.getWinner() == this) // don't bid if already winning
			return;
		bh.getBidMessageToAh().put(auction, createBid(auction, calculateBidAmount(auction)));
		
		logger.debug(this + " is making bid now at time " + this.bh.getTime() + " for " + auction + ".");
	}

	protected int calculateBidAmount(Auction auction) {
		int bidAmount = auction.minimumBid();
		
		if (r.nextDouble() < pIncreaseIncrement) {
//			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * 3;
			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * (r.nextInt(4) + 2);
		}
		
		return bidAmount;
	}
	
	protected final static double pIncreaseIncrement = 0.39505 / 15; // proportion of users that make more than the minimum increment
//	private Bid createBid(Auction auction) {
//		long bidAmount = auction.minimumBid();
//		
//		if (r.nextDouble() < pIncreaseIncrement) {
////			amountToBid += Math.log(auction.getCurrentPrice() * 0.05);
//			bidAmount += Util.minIncrement(auction.getCurrentPrice() * 2);
//		}
//		
//		return new Bid(this, bidAmount);
//	}
	private Bid createBid(Auction auction, int amount) {
		int min = auction.minimumBid();
		assert amount >= min : "The bid of " + amount + " by " + this + " is less than the minimum allowed for " + auction + ".";
		return new Bid(this, amount);
	}

//	// 1 bid: 0.333, 2 bids: 0.5, 3 bids: 0.6, 4 bids: 0.667
//	protected double factor(int numBids) {
////		return -((double) 1)/(0.5 * numBids + 1) + 1;
//		return 1; // TODO: modified. change back?
//	}
	
	protected int numberOfAuctionsPer100Days(double random) {
		if (random < 0.576816606) return 1;
		else if (random < 0.758013403) return 2; 
		else if (random < 0.839887786) return 3; 
		else if (random < 0.884832607) return 4; 
		else if (random < 0.912960228) return 5; 
		else if (random < 0.93186288) return 6; 
		else if (random < 0.945281016) return 7; 
		else if (random < 0.954995807) return 8; 
		else if (random < 0.961964629) return 9; 
		else if (random < 0.967590153) return 10; 
		else if (random < 0.972035653) return 11; 
		else if (random < 0.975590569) return 12; 
		else if (random < 0.978744721) return 13; 
		else if (random < 0.981193828) return 14; 
		else if (random < 0.983116006) return 15; 
		else if (random < 0.984949125) return 16; 
		else if (random < 0.986351796) return 17; 
		
		// model the rest with power law
		for (int i = 0; i < probabilities.size(); i++) {
			if (random < probabilities.get(i))
				return 18 + i;
		}
		return probabilities.size();
	}
	static final ArrayList<Double> probabilities;
	static {
		probabilities = new ArrayList<>();
		double sum = 0.987665407479424;
		for (int x = 18;; x++) {
			double y = Math.round(800000 * Math.pow(x, -2.99)) / 107421.465102286;
			sum += y;
			probabilities.add(sum);
	//		System.out.println(sum);
			if (sum > 0.9999999999)
				break;
		}
	}
}
