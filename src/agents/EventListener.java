package agent;

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
			switch (message.getType()) {
				case NEW:
					newAction(auction);
					break;
				case PRICE_CHANGE:
					priceChangeAction(auction);
					break;
				case LOSS:
					loseAction(auction);
					break;
				case WIN:
					winAction(auction);
					break;
				case EXPIRED:
					expiredAction(auction);
					break;
				case SOLD:
					soldAction(auction);
					break;
				case END_SOON:
					endSoonAction(auction);
					break;
			}
		}
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	protected void newAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.NEW + " at " + bh.getTimeMessage().getTime());
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void priceChangeAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.PRICE_CHANGE + " at " + bh.getTimeMessage().getTime());
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void loseAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.LOSS + " at " + bh.getTimeMessage().getTime());
	}

	/**
	 * For bidders
	 * @param auction
	 */
	protected void winAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.WIN + " at " + bh.getTimeMessage().getTime());
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void expiredAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.EXPIRED + " at " + bh.getTimeMessage().getTime());
	}

	/**
	 * For sellers
	 * @param auction
	 */
	protected void soldAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.SOLD + " at " + bh.getTimeMessage().getTime());
	}
	
	/**
	 * For bidders
	 * @param auction
	 */
	protected void endSoonAction(Auction auction) {
		logger.debug(id + " received " + auction + " " + MessageType.END_SOON + " at " + bh.getTimeMessage().getTime());
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