package agents.repFraud;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;
import agents.puppets.Puppet_old;
import agents.puppets.PuppetMaster;

public class PuppetMasterAdapter implements PuppetMaster {	
	
	@Override
	public void puppetNewAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetPriceChangeAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetExpiredAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetLossAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetWinAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetSoldAction(Puppet_old puppet, Auction auction) {}

	@Override
	public void puppetGotPaidAction(Puppet_old puppet, Collection<Payment> paymentSet) {}

	@Override
	public void puppetItemReceivedAction(Puppet_old puppet, Set<ItemSold> itemSet) {}

	@Override
	public void puppetEndSoonAction(Puppet_old puppet, Auction auctions) {}

}
