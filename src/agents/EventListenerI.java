package agents;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;

public interface EventListenerI extends Runnable {
	
	int getId();
	void newAction(Auction auction, long time);
	void priceChangeAction(Auction auction, long time);
	void lossAction(Auction auction, long time);
	void winAction(Auction auction, long time);
	void expiredAction(Auction auction, long time);
	void soldAction(Auction auction, long time);
	void endSoonAction(Auction auction, long time);
	void gotPaidAction(Collection<Payment> paymentSet);
	void itemReceivedAction(Set<ItemSold> itemSet);
}