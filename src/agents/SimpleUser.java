package agents;

import java.util.Collection;
import java.util.Set;


import org.apache.log4j.Logger;

import agents.puppets.Puppet;

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
public abstract class SimpleUser extends EventListener {
	
	private static final Logger logger = Logger.getLogger(SimpleUser.class);

	protected final AuctionHouse ah;
	protected final PaymentSender ps;
	protected final ItemSender is;
	protected final ReputationRecord rr;

	public SimpleUser(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah) {
		super(bh);
		this.ah = ah;
		this.ps = ps;
		this.is = is;
//		System.out.println("setting id as " + id);

		this.rr = new ReputationRecord();
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
		Collection<Payment> paymentSet = ps.receive(this);
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
	protected void winAction(Auction auction, long time) {
		super.winAction(auction, time);
		this.ps.send(2, auction, auction.getCurrentPrice(), this, auction.getSeller());
		this.awaitingItem.add(auction);
	}

	/**
	 * For sellers.  Will record that this user is waiting for 
	 * @param auction
	 */
	@Override
	protected void soldAction(Auction auction, long time) {
		super.soldAction(auction, time);
		this.awaitingPayment.add(auction);
	}
	
	/**
	 * For sellers.  Will receive payment, then post a positive feedback.
	 * @param auction
	 */
	@Override
	protected void gotPaidAction(Collection<Payment> paymentSet) {
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

	public void submitBid(Auction auction, int amount) {
		this.bh.getBidMessageToAh().put(auction, new Bid(this, amount));
	}

	public void submitAuction(Auction auction) {
		this.bh.getAuctionMessagesToAh().put(auction);
	}
	
}