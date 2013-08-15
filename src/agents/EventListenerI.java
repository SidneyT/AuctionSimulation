package agents;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;

public interface EventListenerI extends Runnable {
	int getId();
	void newAction(Auction auction, int time);
	void priceChangeAction(Auction auction, int time);
	void lossAction(Auction auction, int time);
	void winAction(Auction auction, int time);
	void expiredAction(Auction auction, int time);
	void soldAction(Auction auction, int time);
	void endSoonAction(Auction auction, int time);
	void gotPaidAction(Collection<Payment> paymentSet);
	void itemReceivedAction(Set<ItemSold> itemSet);
}