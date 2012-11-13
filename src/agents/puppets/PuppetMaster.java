package agents;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;

public interface PuppetMaster {

	void newAction(Puppet puppet, Auction auction);

	void priceChangeAction(Puppet puppet, Auction auction);

	void expiredAuction(Puppet puppet, Auction auction);

	void loseAuction(Puppet puppet, Auction auction);

	void winAuction(Puppet puppet, Auction auction);

	void soldAuction(Puppet puppet, Auction auction);

	void gotPaidAction(Puppet puppet, Collection<Payment> paymentSet);

	void itemReceivedAction(Puppet puppet, Set<ItemSold> itemSet);

}
