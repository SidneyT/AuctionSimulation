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


public class Puppet extends SimpleUser {

	private static final Logger logger = Logger.getLogger(SimpleUser.class);
	private final PuppetMaster master;
	
	public Puppet(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, int uniqueId, PuppetMaster master) {
		super(bh, ps, is, ah, uniqueId);
		this.master = master;
	}

	@Override
	protected void newAction(Auction auction) {
		master.newAction(this, auction);
	}

	@Override
	protected void priceChangeAction(Auction auction) {
		master.priceChangeAction(this, auction);
	}

	@Override
	protected void loseAction(Auction auction) {
		master.loseAuction(this, auction);
	}

	@Override
	protected void winAction(Auction auction) {
		master.winAuction(this, auction);
	}

	@Override
	protected void expiredAction(Auction auction) {
		master.expiredAuction(this, auction);
	}

	@Override
	protected void soldAction(Auction auction) {
		master.soldAuction(this, auction);
	}
	
	@Override
	protected void gotPaidAction(Set<Payment> paymentSet) {
		master.gotPaidAction(this, paymentSet);
	}

	@Override
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		master.itemReceivedAction(this, itemSet);
	}
	
}
