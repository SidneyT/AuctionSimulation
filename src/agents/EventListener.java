package agents;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.Message;
import simulator.buffers.MessageType;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;


/**
 * Receives all events from AuctionHouse and ItemSender/PaymentSender and logs them.
 * Doesn't do anything.
 */
public abstract class EventListener implements Runnable {
	
	private static final Logger logger = Logger.getLogger(EventListener.class);

	public BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	protected final int id;

	protected final Set<Auction> awaitingPayment;
	protected final Set<Auction> awaitingItem;
	
	public EventListener(BufferHolder bh, int uniqueId) {
		this.bh = bh;
		this.id = uniqueId;
//		System.out.println("setting id as " + id);
		this.awaitingPayment = new HashSet<Auction>();
		this.awaitingItem = new HashSet<Auction>();
	}
	
	public int getId() {
		assert(this.id != -1); // id must have been set
		return id;
	}

//	@Override
//	public void setId(int id) {
//		assert(this.id == -1); // id can only be set once
//		this.id = id;
//	}
	
	public String toString() {
		return this.getClass().getSimpleName() + ":" + this.getId();
	}

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
					loseAction(auction, time);
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
		logger.debug(id + " received " + auction + " " + MessageType.NEW + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void priceChangeAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.PRICE_CHANGE + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void loseAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.LOSS + " at " + time);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void winAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.WIN + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void expiredAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.EXPIRED + " at " + time);
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void soldAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.SOLD + " at " + time);
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	protected void endSoonAction(Auction auction, long time) {
		logger.debug(id + " received " + auction + " " + MessageType.END_SOON + " at " + time);
	}
	
	/**
	 * For sellers
	 * @param auction
	 */
	protected void gotPaidAction(Set<Payment> paymentSet) {
		logger.debug(id + " received payments " + paymentSet);
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		logger.debug(id + " received items " + itemSet);
	}
	
	/**
	 * Runs after everything else is done.
	 */
	protected void action() {}
	
}