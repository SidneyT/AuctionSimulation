package agents.shills;

import java.util.Collection;
import java.util.Set;

import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.objects.Auction;
import agents.SimpleUserI;
import agents.shills.puppets.PuppetI;

public interface Controller {
	// actions by bidding puppets
	public void winAction(SimpleUserI agent, Auction auction);
	public void lossAction(SimpleUserI agent, Auction auction);
	public boolean isFraud(Auction auction);
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet);
	public void endSoonAction(PuppetI agent, Auction auction);
	
	// actions by selling puppets
	public void soldAction(SimpleUserI agent, Auction auction);
	public void expiredAction(SimpleUserI agent, Auction auction);
	void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet);
}
