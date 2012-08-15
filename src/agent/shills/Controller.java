package agent.shills;

import simulator.objects.Auction;
import agent.SimpleUser;

public interface Controller {
	public void winAction(SimpleUser agent, Auction auction);
	public void lossAction(SimpleUser agent, Auction auction);
}
