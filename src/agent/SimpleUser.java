package agent;

import java.util.Set;


import org.apache.log4j.Logger;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import simulator.objects.ItemCondition;
import simulator.objects.Feedback.Val;
import simulator.records.ReputationRecord;


/**
 * Will not make bids or create new auctions.
 * Will respond if it wins an auction, receives a payment or receives an item and
 * gives feedback accordingly.
 */
public class SimpleUser extends EventListener {
	
	private static final Logger logger = Logger.getLogger(SimpleUser.class);

	protected final ReputationRecord rr;

	public SimpleUser(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId) {
		super(bh, uniqueId);
		this.ps = ps;
		this.is = is;
		this.ah = ah;
//		System.out.println("setting id as " + id);

		this.rr = new ReputationRecord();
	}
	
	public int getId() {
		assert(this.id != -1); // id must have been set
		return id;
	}
	
	public void addFeedback(Feedback feedback) {
		logger.debug(this + " received feedback " + feedback);
		this.rr.addFeedback(this.getId(), feedback);
	}
	
	public ReputationRecord getReputationRecord() {
		return this.rr;
	}
	
	@Override
	public void run() {
		super.run();
		
		// pick up payment messages from other users
		Set<Payment> paymentSet = ps.receive(this);
		if (paymentSet != null && !paymentSet.isEmpty())
			gotPaidAction(paymentSet);
		
		// pick up item messages from other users
		Set<ItemSold> itemSet = is.receive(this);
		if (itemSet != null && !itemSet.isEmpty())
			itemReceivedAction(itemSet);
		
		this.action();
//		long time = this.bh.getTimeMessage().getMessage(this);
//		logger.debug("Received time message: " + time);
	}
	

	@Override
	protected void winAction(Auction auction) {
		super.winAction(auction);
		this.ps.send(2, auction, auction.getCurrentPrice(), this, auction.getSeller());
		this.awaitingItem.add(auction);
	}

	/**
	 * For sellers.  Will record that this user is waiting for 
	 * @param auction
	 */
	@Override
	protected void soldAction(Auction auction) {
		super.soldAction(auction);
		this.awaitingPayment.add(auction);
	}
	
	/**
	 * For sellers.  Will receive payment, then post a positive feedback.
	 * @param auction
	 */
	@Override
	protected void gotPaidAction(Set<Payment> paymentSet) {
		super.gotPaidAction(paymentSet);
		
		for (Payment payment : paymentSet) {
			boolean exists = this.awaitingPayment.remove(payment.getAuction());
			assert(exists); 
			
			this.is.send(2, payment.getAuction(), payment.getAuction().getItem(), ItemCondition.GOOD, this, payment.getSender());
			bh.getFeedbackToAh().put(new Feedback(Val.POS, this, payment.getAuction()));
		}
	}

	/**
	 * For bidders.  Will receive an item, then post a positive feedback.
	 * @param itemSet synchronised set
	 */
	@Override
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		super.itemReceivedAction(itemSet);

		for (ItemSold item : itemSet) {
			boolean awaiting = awaitingItem.remove(item.getAuction());
			assert awaiting;
			
			bh.getFeedbackToAh().put(new Feedback(Val.POS, this, item.getAuction()));
		}
	}

	public void submitBid(Auction auction, long amount) {
		this.bh.getBidMessageToAh().put(auction, new Bid(this, amount));
	}

	public void submitAuction(Auction auction) {
		this.bh.getAuctionMessagesToAh().put(auction);
	}
	
}