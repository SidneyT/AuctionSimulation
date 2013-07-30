package agents.sellers;

import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.CreateItemTypes;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Item;
import simulator.records.ReputationRecord;
import agents.SimpleUser;

/**
 * Simple seller class. Simulates the average frequency of auctions seen from sellers in TM.
 */
public class TimedSeller extends SimpleUser {

	private static final Logger logger = Logger.getLogger(TimedSeller.class);
	protected final Random r;
	private long nextSubmission;
	private final ExponentialDistribution exp;
	protected final List<ItemType> itemTypes;
	
	public TimedSeller(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes) {
		super(bh, ps, is, ah);
		r = new Random();
		ReputationRecord.generateRep(rr, r);
		
		this.itemTypes = itemTypes;

		exp = createExpDist();
		nextSubmission = nextAuctionSubmission();
		logger.debug(" submitting auction at " + this.nextSubmission + " at time " + 0 + ".");
	}
	
	private ExponentialDistribution createExpDist() {
		double ran = r.nextDouble();
		double mean; // average number of auctions in 181418 minutes
		if (ran < 0.5825) { // 1 auction
			mean = Math.log1p(1);
		} else {
			mean = 371.11 * Math.pow(ran, 4) - 1104.6 * Math.pow(ran, 3) 
				+ 1240.4 * Math.pow(ran, 2) - 619.35 * ran + 116.66;
		}
		mean = Math.expm1(mean);
		mean /= 36283.6;
		mean = ((double) 1) / mean;
//		System.out.println("mean: " + mean);
		return new ExponentialDistribution(mean);
	}

	/**
	 * Number of sellers for the targeted number of auctions per day.
	 * @return
	 */
	public static int sellersRequired(double targetPerDay) {
		// with 38290 sellers, there are 303.925 auctions per day.
		return (int) (targetPerDay / 1285.04 * 38290 + 0.5);
	}
	
	private double getPrice() {
		// y = 81.952 * e^(4.8083*x)
//		return 81.952 * Math.pow(Math.exp(1), 4.8083 * r.nextDouble());
		return 100;
	}

	@Override
	public void run() {
		super.run();
		long currentTime = this.bh.getTime();
		assert (currentTime <= nextSubmission) : "Time for next auction submission must be in the future: " + currentTime + "," + nextSubmission + ".";
		if (currentTime == nextSubmission) {
			do {
				submitAuction();
				nextSubmission = nextAuctionSubmission() + currentTime;
				logger.debug(this + " submitting auction at " + this.nextSubmission + " at time " + currentTime + ".");
			} while (nextSubmission == currentTime);
		}
	}

	private long nextAuctionSubmission() {
		return Math.round(exp.sample());
	}
	
	private void submitAuction() {
		ItemType type = CreateItemTypes.pickType(itemTypes, r.nextDouble());
		Item newItem = new Item(type, "item" + (int) (r.nextDouble() * 100000));
		double popularity;
		if (r.nextDouble() < 0.5)
			popularity = 0.7;
		else
			popularity = 1;
		Auction auction = new Auction(this, newItem, 2016, (int) getPrice(), 0, popularity);
		logger.debug(this + " submitting auction " + auction + " at " + this.bh.getTime() + ".");
		this.bh.getAuctionMessagesToAh().put(auction);
	}

}
