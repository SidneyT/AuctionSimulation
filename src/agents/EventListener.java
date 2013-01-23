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
public abstract class EventListener implements Runnable {
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
			long time = bh.getTimeMessage().getTime();
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
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	protected void newAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.NEW + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void priceChangeAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.PRICE_CHANGE + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void lossAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.LOSS + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void winAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.WIN + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void expiredAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.EXPIRED + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void soldAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.SOLD + " at " + time);
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	protected void endSoonAction(Auction auction, long time) {
		logger.debug(this + " received " + auction + " " + MessageType.END_SOON + " at " + time);
	}
	
	/**
	 * For sellers
	 * @param auction
	 */
	protected void gotPaidAction(Collection<Payment> paymentSet) {
		logger.debug(this + " received payments " + paymentSet);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		logger.debug(this + " received items " + itemSet);
	}
	
	/**
	 * Runs after everything else is done.
	 */
	protected void action() {}
	
}