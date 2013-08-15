package simulator.buffers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import agents.SimpleUser;
import agents.SimpleUserI;

import simulator.objects.Auction;
import simulator.objects.Item;
import simulator.objects.ItemCondition;

public class ItemSender implements Runnable {

	// Map<Delay finish time, PaymentHolder>
	private final Map<Long, Set<ItemHolder>> delayedItems;
	private final Map<SimpleUserI, Set<ItemSold>> readyItems;

	public ItemSender() {
		this.time = 0;

		this.delayedItems = Collections.synchronizedMap(new HashMap<Long, Set<ItemHolder>>());
		this.readyItems = Collections.synchronizedMap(new HashMap<SimpleUserI, Set<ItemSold>>());
	}

	public void send(long delay, Auction auction, Item item, ItemCondition cond,
			SimpleUserI sender, SimpleUserI recipient) {
		Set<ItemHolder> paymentHolderSet = this.delayedItems.get(this.time + delay);
		synchronized (this.delayedItems) {
			if (paymentHolderSet == null) {
				paymentHolderSet = Collections.synchronizedSet(new HashSet<ItemHolder>());
				this.delayedItems.put(this.time + delay, paymentHolderSet);
			}
		}
		paymentHolderSet.add(new ItemHolder(recipient, new ItemSold(auction, item, cond, sender)));
	}

	/**
	 * Returns synchronised set of payments made to the user.
	 */
	public Set<ItemSold> receive(SimpleUserI recipient) {
		return this.readyItems.remove(recipient);
	}

	private long time;

	@Override
	public void run() {
		// move payments in delayedPayments to readyPayments
		Set<ItemHolder> paymentHolderSet = this.delayedItems.get(this.time);
		if (paymentHolderSet != null && !paymentHolderSet.isEmpty()) {
			// synchronized(paymentHolderSet) {
			for (ItemHolder ph : paymentHolderSet) {
				Set<ItemSold> paymentSet;
				// synchronized (this.readyPayments) {
				paymentSet = this.readyItems.get(ph.getRecipient());
				if (paymentSet == null) {
					paymentSet = Collections.synchronizedSet(new HashSet<ItemSold>());
					this.readyItems.put(ph.getRecipient(), paymentSet);
				}
				// }
				paymentSet.add(ph.getPayment());
			}
			// }
		}
		this.time++;
	}

	private static class ItemHolder {
		private final SimpleUserI recipient;
		private final ItemSold itemSold;

		public ItemHolder(SimpleUserI recipient, ItemSold itemSold) {
			this.recipient = recipient;
			this.itemSold = itemSold;
		}

		public SimpleUserI getRecipient() {
			return recipient;
		}

		public ItemSold getPayment() {
			return itemSold;
		}
	}

	public static class ItemSold {
		private final Auction auction;
		private final Item item;
		private final ItemCondition cond;
		private final SimpleUserI sender;

		private ItemSold(Auction auction, Item item, ItemCondition cond, SimpleUserI sender) {
			this.auction = auction;
			this.item = item;
			this.cond = cond;
			this.sender = sender;
		}

		public Auction getAuction() {
			return this.auction;
		}
		
		public Item getItem() {
			return this.item;
		}

		public SimpleUserI getSender() {
			return this.sender;
		}
		
		@Override
		public String toString() {
			return "(" + auction + ", " + item + ", " + sender + ")";
		}
	}

}
