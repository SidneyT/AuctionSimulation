package agents.repFraud;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;
import agents.puppets.Puppet;
import agents.puppets.PuppetMaster;

public class PuppetMasterAdapter implements PuppetMaster {	
	
	@Override
	public void puppetNewAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetPriceChangeAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetExpiredAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetLossAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetWinAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetSoldAction(Puppet puppet, Auction auction) {}

	@Override
	public void puppetGotPaidAction(Puppet puppet, Collection<Payment> paymentSet) {}

	@Override
	public void puppetItemReceivedAction(Puppet puppet, Set<ItemSold> itemSet) {}

	@Override
	public void puppetEndSoonAction(Puppet puppet, Auction auctions) {}

}
