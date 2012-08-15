package simulator.buffers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import agent.SimpleUser;

import simulator.objects.Auction;

public class PaymentSender implements Runnable {

	private static final Logger logger = Logger.getLogger(PaymentSender.class);
	
	// Map<Delay finish time, PaymentHolder>
	private final Map<Long, Set<PaymentHolder>> delayedPayments;
	private final Map<SimpleUser, Set<Payment>> readyPayments;
	
	public PaymentSender() {
		this.time = 0;
		
		this.delayedPayments = Collections.synchronizedMap(new HashMap<Long, Set<PaymentHolder>>());
		this.readyPayments = Collections.synchronizedMap(new HashMap<SimpleUser, Set<Payment>>());
	}
	
	public void send(long delay, Auction auction, long amount, SimpleUser sender, SimpleUser recipient) {
		logger.debug("Payment of " + amount + " received from " + sender + " to " + recipient + " for " + auction + ".");
		
		Set<PaymentHolder> paymentHolderSet = this.delayedPayments.get(this.time + delay);
		synchronized (this.delayedPayments) {
			if (paymentHolderSet == null ) {
				paymentHolderSet = Collections.synchronizedSet(new HashSet<PaymentHolder>());
				this.delayedPayments.put(this.time + delay, paymentHolderSet);
			}
		}
		paymentHolderSet.add(new PaymentHolder(recipient, new Payment(auction, amount, sender)));
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
