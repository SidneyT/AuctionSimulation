package agents.puppets;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;

public interface PuppetMaster {

	void puppetNewAction(Puppet puppet, Auction auction);

	void puppetPriceChangeAction(Puppet puppet, Auction auction);

	void puppetExpiredAction(Puppet puppet, Auction auction);

	void puppetLossAction(Puppet puppet, Auction auction);

	void puppetWinAction(Puppet puppet, Auction auction);

	void puppetSoldAction(Puppet puppet, Auction auction);

	void puppetGotPaidAction(Puppet puppet, Collection<Payment> paymentSet);

	void puppetItemReceivedAction(Puppet puppet, Set<ItemSold> itemSet);

	void puppetEndSoonAction(Puppet puppet, Auction auctions);
}
