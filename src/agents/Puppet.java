package agents;

import java.util.Collection;
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
	protected void newAction(Auction auction, long time) {
		master.newAction(this, auction);
	}

	@Override
	protected void priceChangeAction(Auction auction, long time) {
		master.priceChangeAction(this, auction);
	}

	@Override
	protected void loseAction(Auction auction, long time) {
		master.loseAuction(this, auction);
	}

	@Override
	protected void winAction(Auction auction, long time) {
		master.winAuction(this, auction);
	}

	@Override
	protected void expiredAction(Auction auction, long time) {
		master.expiredAuction(this, auction);
	}

	@Override
	protected void soldAction(Auction auction, long time) {
		master.soldAuction(this, auction);
	}
	
	@Override
	protected void gotPaidAction(Collection<Payment> paymentSet) {
		master.gotPaidAction(this, paymentSet);
	}

	@Override
	protected void itemReceivedAction(Set<ItemSold> itemSet) {
		master.itemReceivedAction(this, itemSet);
	}
	
}
