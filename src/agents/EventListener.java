package agents;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import simulator.buffers.BufferHolder;
import simulator.buffers.Message;
import simulator.buffers.MessageType;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;


/**
 * Receives all events from AuctionHouse and ItemSender/PaymentSender and logs them.
 * Doesn't do anything.
 */
public abstract class EventListener implements Runnable, EventListenerI {
	private static final Logger logger = Logger.getLogger(EventListener.class);

	private final static AtomicInteger userIdCount = new AtomicInteger(); // for assigning unique ids
	
	public final BufferHolder bh;
	protected final int id;

	protected final Set<Auction> awaitingPayment;
	protected final Set<Auction> awaitingItem;
	
	public EventListener(BufferHolder bh) {
		this.id = userIdCount.getAndIncrement();
		
		this.bh = bh;
		this.awaitingPayment = new HashSet<Auction>();
		this.awaitingItem = new HashSet<Auction>();
	}

	/**
	 * Use this to define the id of the agent. Use carefully. 
	 * Used by {@link PuppetClusterBidderCombined}.
	 * @param bh
	 * @param id
	 */
	public EventListener(BufferHolder bh, int id) {
		this.id = id;
		
		this.bh = bh;
		this.awaitingPayment = new HashSet<Auction>();
		this.awaitingItem = new HashSet<Auction>();
	}
	
	public final int getId() {
		return id;
	}

	public String toString() {
		return this.getClass().getSimpleName() + ":" + this.getId();
	}

	/**
	 * All subclasses must call this method if overriding.
	 */
	public void run() {
		List<Message> messages = this.bh.getMessagesToUsers().getMessages(id);
		for (Message message : messages) {
			Auction auction = message.getAuction();
			int time = bh.getTime();
			switch (message.getType()) {
				case NEW:
					newAction(auction, time);
					break;
				case PRICE_CHANGE:
					priceChangeAction(auction, time);
					break;
				case LOSS:
					lossAction(auction, time);
					break;
				case WIN:
					winAction(auction, time);
					break;
				case EXPIRED:
					expiredAction(auction, time);
					break;
				case SOLD:
					soldAction(auction, time);
					break;
				case END_SOON:
					endSoonAction(auction, time);
					break;
			}
		}
	
		run2();
		
	}
	
	public void run2() {}
	
	/**
	 * For bidders
	 * @param auction
	 */
	public void newAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.NEW + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	public void priceChangeAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.PRICE_CHANGE + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	public void lossAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.LOSS + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	public void winAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.WIN + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	public void expiredAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.EXPIRED + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	public void soldAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.SOLD + " at " + time);
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	public void endSoonAction(Auction auction, int time) {
		logger.debug(this + " received " + auction + " " + MessageType.END_SOON + " at " + time);
	}
	
	/**
	 * For sellers
	 * @param auction
	 */
	public void gotPaidAction(Collection<Payment> paymentSet) {
		logger.debug(this + " received payments " + paymentSet);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		logger.debug(this + " received items " + itemSet);
	}
	
//	/**
//	 * Runs after everything else is done.
//	 */
//	protected void action() {}
	
}