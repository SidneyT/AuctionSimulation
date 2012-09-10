package simulator.buffers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import agents.SimpleUser;

import simulator.objects.Auction;

public class PaymentSender implements Runnable {

	private static final Logger logger = Logger.getLogger(PaymentSender.class);
	
	// Map<Delay finish time, PaymentHolder>
	private final ConcurrentHashMap<Long, Set<PaymentHolder>> delayedPayments;
	private final ConcurrentHashMap<SimpleUser, Set<Payment>> readyPayments;
	
	public PaymentSender() {
		this.time = 0;
		
		this.delayedPayments = new ConcurrentHashMap<Long, Set<PaymentHolder>>();
		this.readyPayments = new ConcurrentHashMap<SimpleUser, Set<Payment>>();
	}
	
	public void send(long delay, Auction auction, long amount, SimpleUser sender, SimpleUser recipient) {
		logger.debug("Payment of " + amount + " received from " + sender + " to " + recipient + " for " + auction + ".");
		
		final long target = this.time + delay;
		if (!this.delayedPayments.containsKey(target)) {
			Set<PaymentHolder> newPaymentHolderSet = Collections.synchronizedSet(new HashSet<PaymentHolder>());
			delayedPayments.putIfAbsent(target, newPaymentHolderSet);
		}
		this.delayedPayments.get(target).add(new PaymentHolder(recipient, new Payment(auction, amount, sender)));
	}
	
	/**
	 * Returns synchronised set of payments made to the user.
	 */
	public Set<Payment> receive(SimpleUser recipient) {
		return this.readyPayments.remove(recipient);
	}
	
	private long time;
	
	@Override
	public void run() {
		// move payments in delayedPayments to readyPayments 
		Set<PaymentHolder> paymentHolderSet = this.delayedPayments.get(this.time);
		if (paymentHolderSet != null && !paymentHolderSet.isEmpty()) {
//			synchronized(paymentHolderSet) {
				for (PaymentHolder ph : paymentHolderSet) {
					Set<Payment> paymentSet;
//					synchronized (this.readyPayments) {
						paymentSet = this.readyPayments.get(ph.getRecipient());
						if (paymentSet == null ) {
							paymentSet = Collections.synchronizedSet(new HashSet<Payment>());
							this.readyPayments.put(ph.getRecipient(), paymentSet);
						}
//					}
					paymentSet.add(ph.getPayment());
				}
//			}
		}
		this.time++;
	}
	
	private static class PaymentHolder {
		private final SimpleUser recipient; 
		private final Payment payment;
		
		public PaymentHolder(SimpleUser recipient, Payment payment) {
			this.recipient = recipient;
			this.payment = payment;
		}

		public SimpleUser getRecipient() {
			return recipient;
		}

		public Payment getPayment() {
			return payment;
		}
	}

	public static class Payment {
		private final Auction auction;
		private final long amount;
		private final SimpleUser sender;
		
		private Payment(Auction auction, long amount, SimpleUser sender) {
			this.auction = auction;
			this.amount = amount;
			this.sender = sender;
		}
		
		public Auction getAuction() {
			return this.auction;
		}
		
		public long getAmount() {
			return this.amount;
		}
		
		public SimpleUser getSender() {
			return this.sender;
		}
		
		@Override
		public String toString() {
			return "(" + auction + ", " + amount + ", " + sender + ")";
		}
	}

}
