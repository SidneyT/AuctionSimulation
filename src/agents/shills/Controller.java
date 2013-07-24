package agents.shills;

import simulator.objects.Auction;
import agents.SimpleUserI;

public interface Controller {
	public void winAction(SimpleUserI agent, Auction auction);
	public void lossAction(SimpleUserI agent, Auction auction);
	public boolean isFraud(Auction auction);
}
