package agents.puppets;

import java.util.Collection;
import java.util.Set;


import org.apache.log4j.Logger;

import agents.SimpleUser;

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
	
	public Puppet(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, PuppetMaster master) {
		super(bh, ps, is, ah);
		this.master = master;
	}
	
	@Override
	public void run() {
		super.run();
	}

	@Override
	public void newAction(Auction auction, long time) {
		master.puppetNewAction(this, auction);
	}

	@Override
	public void priceChangeAction(Auction auction, long time) {
		master.puppetPriceChangeAction(this, auction);
	}

	@Override
	public void lossAction(Auction auction, long time) {
		master.puppetLossAction(this, auction);
	}

	@Override
	public void winAction(Auction auction, long time) {
		master.puppetWinAction(this, auction);
	}

	@Override
	public void expiredAction(Auction auction, long time) {
		master.puppetExpiredAction(this, auction);
	}

	@Override
	public void soldAction(Auction auction, long time) {
		master.puppetSoldAction(this, auction);
	}
	
	@Override
	public void gotPaidAction(Collection<Payment> paymentSet) {
		master.puppetGotPaidAction(this, paymentSet);
	}

	@Override
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		master.puppetItemReceivedAction(this, itemSet);
	}
	
	@Override
	public void endSoonAction(Auction auction, long time) {
		master.puppetEndSoonAction(this, auction);
	}
}