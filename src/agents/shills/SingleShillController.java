package agents.shills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import simulator.objects.ItemCondition;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Util;
import agents.EventListener;
import agents.SimpleUser;
import agents.shills.strategies.Strategy;

public class SingleShillController extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(SingleShillController.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	private final PuppetSeller ss;
	private final PuppetBidder sb;
	private final Set<Auction> shillAuctions;
	private final Set<Auction> expiredShillAuctions;
	List<ItemType> types;
	
//	private Random r;
	
	public SingleShillController(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy) {
		super(bh, ur.nextId());
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		

		// set up the shill seller
		PuppetSeller ss = new PuppetSeller(bh, ps, is, ah, ur.nextId(), this, types);
		ur.addUser(ss);
		this.ss = ss;
		
		// set up the shill bidder
		PuppetBidder sb = new PuppetBidder(bh, ps, is, ah, ur.nextId(), this);
		ur.addUser(sb);
		this.sb = sb;
	
		shillAuctions = new HashSet<>();
		expiredShillAuctions = new HashSet<>();
		
		setNumberOfAuctions(10);
		
//		r = new Random();
	}

	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTimeMessage().getTime();
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions) {
			// test each condition to bid
			if (ifIsLatePriceTooLowThenBid(shillAuction, 0.8, 0.5))
//				sb.makeBid(shillAuction, shillAuction.minimumBid() + 100);
				sb.makeBid(shillAuction);
			else if (ifSomeoneElseJustBidThenBid(shillAuction))
				sb.makeBid(shillAuction);
		}
		
		// make the bidders follow a normal behaviour pattern?
		
		
		// decide whether to submit a new auction
//		if (currentTime % 20 == 1) {
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) {
			Auction newShillAuction = ss.submitAuction();
			shillAuctions.add(newShillAuction);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	private List<Integer> auctionTimes;
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(Util.randomInt(Math.random(), 0, (int) (100 * 24 * 60 / 5 + 0.5)));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	/**
	 * @param shillAuction
	 * @param elapsed
	 * @param proportionOfTrueEvaluation
	 * @return true if the shill bidder make a bid
	 */
	private boolean ifIsLatePriceTooLowThenBid(Auction shillAuction, double elapsed, double proportionOfTrueEvaluation) {
		if (shillAuction.percentageElapsed(bh.getTimeMessage().getTime()) < elapsed)
			return false;
		if (shillAuction.proportionOfTrueValuation() < proportionOfTrueEvaluation) { // if price is less than 60%
			return true;
		}
		return false;
	}
	
	/**
	 * make a shill bid if someone else just bid, unless the price is > 0.7 of true valuation
	 * @param shillAuction
	 * @return true if the shill bidder should make a bid
	 */
	private boolean ifSomeoneElseJustBidThenBid(Auction shillAuction) {
		if (shillAuction.proportionOfTrueValuation() < 0.80) {
			if (shillAuction.getLastBid() != null) {
				SimpleUser bidder = shillAuction.getLastBid().getBidder();
				if (bidder != null && bidder != sb) {
//					sb.makeBid(shillAuction);
					return true;
				}
			}
		}
		return false;
	}
	
	public Set<Auction> getShillAuctions() {
		return this.shillAuctions;
	}

	@Override
	protected void newAction(Auction auction, long time) {
		super.newAction(auction, time);
		
		if (shillAuctions.contains(auction)) {
			ah.registerForAuction(this, auction); // have the controller register for the auction, instead of the shill bidder
		}
	}

	@Override
	protected void priceChangeAction(Auction auction, long time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	protected void loseAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void winAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	protected void expiredAction(Auction auction, long time) {
		super.expiredAction(auction, time);
	}

	@Override
	protected void soldAction(Auction auction, long time) {
		super.soldAction(auction, time);
		this.awaitingPayment.add(auction);
	}

	@Override
	// acts for the shill bidder. copied from simpleUser, replacing "this" with ss
	protected void gotPaidAction(Collection<Payment> paymentSet) {
		super.gotPaidAction(paymentSet);
		
		for (Payment payment : paymentSet) {
			boolean exists = this.awaitingPayment.remove(payment.getAuction());
			assert(exists); 
			
			this.is.send(2, payment.getAuction(), payment.getAuction().getItem(), ItemCondition.GOOD, ss, payment.getSender());
			
			bh.getFeedbackToAh().put(new Feedback(Val.POS, ss, payment.getAuction()));
		}
	}
	
	@Override
	// acts for the shill seller. copied from simpleUser, replacing "this" with ss
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		super.itemReceivedAction(itemSet);

		for (ItemSold item : itemSet) {
			boolean awaiting = awaitingItem.remove(item.getAuction());
			assert awaiting;
			
			bh.getFeedbackToAh().put(new Feedback(Val.POS, sb, item.getAuction()));
		}
	}

	public static void main(String[] args) {
		System.out.println(SingleShillController.proportionRemaining(1000, 2000, 1500));
		System.out.println(SingleShillController.proportionToTime(0.9, 1000, 2000));
	}
	
	/**
	 * Counts the number of bids that were made after the timeLimit by bidders
	 * who are not shill bidders from this controller.
	 * @param bidHistory
	 * @param time
	 */
	private static int numberOfBids(List<Bid> bidHistory, long timeLimit) {
		int count = 0;
		for (int i = bidHistory.size() - 1; i >= 0 && bidHistory.get(i).getTime() >= timeLimit; i--) {
			count++;
		}
		return count;
	}
	
	private static double proportionRemaining(long t0, long tc, double ti) {
		return ((double) ti - t0)/(tc - t0);
	}
	
	private static double proportionToTime(double proportion, long start, long end) {
		return proportion * (end - start) + start;  
	}
	
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					SingleShillController sc = new SingleShillController(bh, ps, is, ah, ur, types, strategy);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "AgentAdderTrevathanSimple:" + numberOfAgents;
			}
		};
	}
	
	@Override
	public void winAction(SimpleUser agent, Auction auction) {
		throw new NotImplementedException();
	}

	@Override
	public void lossAction(SimpleUser agent, Auction auction) {
		throw new NotImplementedException();
	}
	
}
