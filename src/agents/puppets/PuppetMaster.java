package agents.puppets;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;

public interface PuppetMaster {

	void puppetNewAction(Puppet_old puppet, Auction auction);

	void puppetPriceChangeAction(Puppet_old puppet, Auction auction);

	void puppetExpiredAction(Puppet_old puppet, Auction auction);

	void puppetLossAction(Puppet_old puppet, Auction auction);

	void puppetWinAction(Puppet_old puppet, Auction auction);

	void puppetSoldAction(Puppet_old puppet, Auction auction);

	void puppetGotPaidAction(Puppet_old puppet, Collection<Payment> paymentSet);

	void puppetItemReceivedAction(Puppet_old puppet, Set<ItemSold> itemSet);

	void puppetEndSoonAction(Puppet_old puppet, Auction auctions);
}
