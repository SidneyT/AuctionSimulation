package agents.bidders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import distributions.Exponential;
import distributions.Normal;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.records.ReputationRecord;
import util.Util;
import agents.SimpleUser;
import agents.sellers.TMSeller;

/**
 * Not thread safe.
 */
public abstract class ClusterBidder extends SimpleUser {
	
//	private static final List<Integer> allClusterBidderIds = new ArrayList<>();
	
	// constants...
	private static final int ONE_DAY = 24*60/AuctionHouse.UNIT_LENGTH;
	protected static final int SEVEN_DAYS = ONE_DAY * 7;
	private static int num_users = -1;
	protected double probInterest;
	// probability of bidding when auction has 1-7 days to go, adjusted by number of users 
	
	// parameters
	private static final Logger logger = Logger.getLogger(ClusterBidder.class);
	
//	protected final Set<Auction> oneBidAuctionsUnprocessed; // auctions with more than 1 bid to be processed 
	protected final List<Auction> newAuctionsUnprocessed; // should be empty at the beginning of each time unit
	protected final HashMultimap<Long, Auction> auctionsToBidIn;
	
	// Auctions for which this bidder is much more likely to bid on.
	// Simulates motivation...
//	private final boolean willRebid;
//	protected final double likelihoodOfRebid;
	
	protected double numberOfAuctionsPer100Days;
//	protected Exponential nextInterestDist;
	
//	protected final Map<Long, Set<Auction>> nextBid;
	
	protected double privateValuationProportion;

	protected final Random r;
	
	protected final List<ItemType> itemTypes;
	
	public ClusterBidder(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes2) {
		super(bh, ps, is, ah);

		assert(num_users > 0);
		
		this.itemTypes = itemTypes2;
		
//		this.oneBidAuctionsUnprocessed = new HashSet<Auction>();
		this.newAuctionsUnprocessed = new ArrayList<Auction>();
		this.auctionsToBidIn = HashMultimap.create();
		
		r = new Random();
		ReputationRecord.generateRep(rr, r);

		
//		this.nextBid = new HashMap<Long, Set<Auction>>();
		
//		willRebid = willRebid();
//		if (willRebid)
//			likelihoodOfRebid = likelihoodOfRebid();
//		else
//			likelihoodOfRebid = 0;
		
//		probInterest = oneBidAuctionProb(num_users);
		probInterest = oneBidAuctionProb(num_users) * Exponential.nextDouble(r.nextDouble(), 1);
//		probInterest = oneBidAuctionProb(num_users)
		
		// normal distribution, mean 1, std dev 0.15
		privateValuationProportion = Normal.nextDouble(r.nextDouble(), 1, 0.2);
		if (privateValuationProportion < 0)
			privateValuationProportion *= -1;
//		privateValuationProportion = Normal.twoNormal(r.nextDouble(), r.nextDouble(), 1, 0.2, 0.6, 1.4, 0.1, 0.4);
		
		// these values are based on 100 days of auctions and for this number of users in TM.
		// if number of auctions or users changes, does this value change??  
//		numberOfAuctionsPer100Days = numberOfAuctionsPer100Days(r.nextDouble());
//		average+= numberOfAuctionsPer100Days;
////		System.out.println("numberOfAuctionsPer100Days: " + numberOfAuctionsPer100Days);
//		nextInterestDist = new Exponential(28800 / numberOfAuctionsPer100Days);
		
		int numAuctions = numberOfAuctionsPer100Days(r.nextDouble());
		interestTimes = new ArrayList<>(numAuctions);
//		debugAverage += numAuctions;
		for (int i = 0; i < numAuctions; i++) {
			interestTimes.add((long) r.nextInt(28800));
		}
		Collections.sort(interestTimes, Collections.reverseOrder());
		
		this.numberOfInterestedCategories = numberOfInterestedCategories(interestTimes.size());
//		System.out.println(interestTimes.size() + " vs " + numberOfInterestedCategories);
	}
	
	protected abstract long firstBidTime();
	
	private final int numberOfInterestedCategories; 
	
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
	/**
	 * Selects auctions to bid in, and the time to begin bidding in them.
	 */
	public void selectAuctionsToBidIn() {
		long currentTime = this.bh.getTime();

		// don't do anything if you're not scheduled to bid in an auction yet.
		if (!shouldParticipateInAuction(currentTime)) {
			newAuctionsUnprocessed.clear();
			return;
		}
		
		Collections.shuffle(newAuctionsUnprocessed); // randomise order of the newAuctions
		for (Auction auction : newAuctionsUnprocessed) {
			// less likely to be interested in an auction if they have a low popularity
			if (r.nextDouble() > auction.getPopularity())
				continue; 
			
			// check if the auction is in a category you're interested in
			if (itemTypesBidOn.contains(auction.getItem().getType())) { // if yes, then bid.
				this.ah.registerForAuction(this, auction);
				long timeToMakeBid = firstBidTime() + currentTime;
				logger.debug(this + " is making first bid in the future at " + timeToMakeBid + " at time " + currentTime + ".");
				auctionsToBidIn.put(timeToMakeBid, auction);
				participated();
				break;
			} else { // if not, check if you are willing to buy things from another category
				
				// check if the number of categories you participated in has reached the predetermined number
				if (numberOfInterestedCategories == itemTypesBidOn.size()) {
					continue;
				}
				
				itemTypesBidOn.add(auction.getItem().getType()); // record the new category you'll bid in
				
				this.ah.registerForAuction(this, auction);
				long timeToMakeBid = firstBidTime() + currentTime;
				logger.debug(this + " is making first bid in the future at " + timeToMakeBid + " at time " + currentTime + ".");
				auctionsToBidIn.put(timeToMakeBid, auction);
				participated();
				break;
			}
		}
		
		newAuctionsUnprocessed.clear();
	}
	
//	public static int debugAverage = 0;
	
	public final ArrayList<Long> interestTimes; // sorted in decreasing order
	// returns number of auctions this user should bid in at this time
//	protected int shouldBid(int currentTime) {
//		int result = 0;
//		for (int i = bidTimes.size() - 1; i >= 0; i--) {
//			if (bidTimes.get(i) < currentTime) {
//				bidTimes.remove(i);
//				result++;
//			} else {
//				break;
//			}
//		}
//		return result;
//	}
	/**
	 * If the scheduled time for participating for an auction has passed,
	 * return true.
	 * @param currentTime
	 * @return
	 */
	protected boolean shouldParticipateInAuction(long currentTime) {
		if (interestTimes.isEmpty()) {
			return false;
		}
		long earliestBidTime = interestTimes.get(interestTimes.size() - 1);
		if (earliestBidTime < currentTime) {
//			interestTimes.remove(interestTimes.size() - 1);
			return true;
		} else {
			return false;
		}
	}
	public void participated() {
		interestTimes.remove(interestTimes.size() - 1);
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
//	protected static double valuationEffect(long bidAmount, double privateValuation) {
//		int param = 4;
//		return 1/Math.pow(bidAmount/privateValuation, param);
//	}
	protected static double valuationEffect(long bidAmount, double maximumBid) {
//		if (bidAmount <= maximumBid)
//			return 100000000;
//		else
//			return 0;
		return Util.sigmoid(bidAmount/maximumBid);
	}
	
	public static void setNumUsers(int numUsers) {
		num_users = numUsers;
//		probInterest = oneBidAuctionProb(num_users);
	}
	
	/**
	 * 
	 * Finds the probability for a binomial distribution, given the
	 * number of trials, such that the probability for 0 successes
	 * is 0.474758.
	 * 
	 * This is used to determine the probability that a user has 
	 * interest in a particular auction, so that 47.47% auctions
	 * will have no users interested in them, so that they will only
	 * have 1 bid.
	 * 
	 * @param numberOfUsers
	 * @return
	 */
	private static double oneBidAuctionProb(int numberOfUsers) {
//		double target = 0.474758; // TODO: modified. change back?
		double target = 0.3;
		double p = 1 - Math.exp(Math.log(target) / numberOfUsers);
		return p;
	}

//	@Override
//	public void cleanUp() {
//		alreadyBidThisTurn.clear();
//	}
	
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
	public void newAction(Auction auction, long time) {
		super.newAction(auction, time);

		this.newAuctionsUnprocessed.add(auction);
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
		if (auction.getEndTime() < this.bh.getTimeMessage().getTime()) // don't bid if auction finished
			return;
		if (auction.getWinner() == this) // don't bid if already winning
			return;
		bh.getBidMessageToAh().put(auction, createBid(auction, bidAmount));
		
		logger.debug(this + " is making bid now at time " + this.bh.getTimeMessage().getTime() + " for " + auction + ".");
	}
	public void makeBid(Auction auction) {
		if (auction.getEndTime() < this.bh.getTimeMessage().getTime()) // don't bid if auction finished
			return;
		if (auction.getWinner() == this) // don't bid if already winning
			return;
		bh.getBidMessageToAh().put(auction, createBid(auction, calculateBidAmount(auction)));
		
		logger.debug(this + " is making bid now at time " + this.bh.getTimeMessage().getTime() + " for " + auction + ".");
	}

	protected int calculateBidAmount(Auction auction) {
		int bidAmount = auction.minimumBid();
		
		if (r.nextDouble() < pIncreaseIncrement) {
//			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * 3;
			bidAmount += Util.minIncrement(auction.getCurrentPrice()) * (r.nextInt(4) + 2);
		}
		
		return bidAmount;
	}
	
	protected final static double pIncreaseIncrement = 0.39505 / 20; // proportion of users that make more than the minimum increment
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
	
	//	protected static double numberOfAuctionsPer100Days(double random) {
	//		// x = [(x1^(n+1) - x0^(n+1))*y + x0^(n+1)]^(1/(n+1))
	//		// modified to x = x0 - [(x1^(n+1) - x0^(n+1))*y + x0^(n+1)]^(1/(n+1))
	//		double param = 60; 
	//		
	//		double number = 101 - Math.pow((Math.pow(1, param) - Math.pow(100, param)) * random + Math.pow(100, param), 1/param) - 5;
	//		if (number < 1) {
	//			return 1;
	//		} else if (number > 8) {
	//			return 8;
	//		} else {
	//			return number;
	//		}
	//	}
	protected int numberOfAuctionsPer100Days(double random) {
		if (random < 0.6) return 1;
		else if (random < 0.78) return 2;
		else if (random < 0.86) return 3;
		else if (random < 0.91) return 4;
		else if (random < 0.94) return 5;
		else if (random < 0.956273) return 6;
		else if (random < 0.964617) return 7;
		else if (random < 0.970626) return 8;
		else if (random < 0.975345) return 9;
		else if (random < 0.979018) return 10;
		else if (random < 0.981647) return 11;
		else if (random < 0.984031) return 12;
		else if (random < 0.986350) return 13;
		else if (random < 0.987999) return 14;
		else if (random < 0.989501) return 15;
		else if (random < 0.990497) return 16;
		else if (random < 0.991460) return 17;
		else if (random < 0.992293) return 18;
		else if (random < 0.993126) return 19;
		else if (random < 0.993779) return 20;
		else if (random < 0.994416) return 21;
		else if (random < 0.994987) return 22;
		else if (random < 0.995542) return 23;
		else if (random < 0.995869) return 24;
		else if (random < 0.996277) return 25;
		else return 26;
	}

}
