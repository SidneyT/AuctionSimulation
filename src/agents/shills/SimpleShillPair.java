package agents.shills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.objects.ItemCondition;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Util;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.strategies.Strategy;

/**
 *	Shill bidder that bids using the strategy given.
 *	Contains 1 seller working with 1 bidder only.
 */
public class SimpleShillPair extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(SimpleShillPair.class); 
	
	protected final BufferHolder bh;
	protected final PaymentSender ps;
	protected final ItemSender is;
	protected final AuctionHouse ah;
	private final List<ItemType> types;
	
	private final PuppetSeller ss;
	private final PuppetBidder sb;
	// Map<Auction, Registered>
	private final Map<Auction, Boolean> shillAuctions;
	private final Set<Auction> expiredShillAuctions;

	private final Strategy strategy;
	
	public final Random r;
	
	public SimpleShillPair(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		// set up the shill seller
		PuppetSeller ss = new PuppetSeller(bh, ps, is, ah, this, types);
		ur.addUser(ss);
		this.ss = ss;
		
		// set up the shill bidder
		PuppetBidder sb = new PuppetBidder(bh, ps, is, ah, this);
		ur.addUser(sb);
		this.sb = sb;
	
		shillAuctions = new HashMap<>();
		expiredShillAuctions = new HashSet<>();
		
		setNumberOfAuctions(10);

		this.strategy = strategy;
		this.r = new Random();
	}
	
	private final Set<Auction> waiting = new HashSet<>();
	private final ArrayListMultimap<Long, Auction> futureBid = ArrayListMultimap.create();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTime();
		
		// TODO: can make another set "toCheckShillAuctions" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions.keySet()) {
			if (shillAuctions.get(shillAuction) == false || shillAuction.getWinner() == sb)
				continue;

			if (waiting.contains(shillAuction))
				continue;
			
			if (this.strategy.shouldBid(shillAuction, currentTime)) {
				long wait = strategy.wait(shillAuction); 
				if (wait > 0) {
					// record when to bid in the future
					waiting.add(shillAuction);
					futureBid.put(currentTime + wait, shillAuction);
				} else {
					sb.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.removeAll(currentTime);
		if (finishedWaiting != null) {
			for (Auction shillAuction : finishedWaiting) {
				sb.makeBid(shillAuction, this.strategy.bidAmount(shillAuction));
			}
			this.waiting.removeAll(finishedWaiting);
		}
		
		// submit a new auction
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) {
			Auction newShillAuction = ss.submitAuction();
			shillAuctions.put(newShillAuction, false);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	private List<Integer> auctionTimes;
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		int latest = 100 * 24 * 60 / 5;
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(r.nextInt(latest));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	public Set<Auction> getShillAuctions() {
		return this.shillAuctions.keySet();
	}

	@Override
	public void newAction(Auction auction, long time) {
		super.newAction(auction, time);
		if (shillAuctions.containsKey(auction)) {
//			ah.registerForAuction(sb, auction);
			ah.registerForAuction(this, auction);
			shillAuctions.put(auction, true);
		}
	}

	@Override
	public void priceChangeAction(Auction auction, long time) {
		super.priceChangeAction(auction, time);
	}

	@Override
	public void winAction(SimpleUserI agent, Auction auction) {throw new NotImplementedException();}
	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {throw new NotImplementedException();}
	@Override
	public boolean isFraud(Auction auction) {throw new NotImplementedException();}
	
	@Override
	public void lossAction(Auction auction, long time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	@Override
	public void winAction(Auction auction, long time) {
		assert(false) : "This method should never be called, since this class can never bid/win.";
	}

	@Override
	public void expiredAction(Auction auction, long time) {
		super.expiredAction(auction, time);
	}

	@Override
	public void soldAction(Auction auction, long time) {
		super.soldAction(auction, time);
		this.awaitingPayment.add(auction);
	}

	@Override
	// acts for the shill bidder. copied from simpleUser, replacing "this" with ss
	public void gotPaidAction(Collection<Payment> paymentSet) {
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
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		super.itemReceivedAction(itemSet);

		for (ItemSold item : itemSet) {
			boolean awaiting = awaitingItem.remove(item.getAuction());
			assert awaiting;
			
			bh.getFeedbackToAh().put(new Feedback(Val.POS, sb, item.getAuction()));
		}
	}

	@Override
	public String toString() {
		return super.toString() + ":" + strategy.toString(); 
	}

	public static AgentAdder getAgentAdder(final int numberOfGroups, final Strategy strategy) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					SimpleShillPair sc = new SimpleShillPair(bh, ps, is, ah, ur, types, strategy);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "SimpleShillPair." + numberOfGroups + "." + strategy.toString();
			}
		};
	}

}
