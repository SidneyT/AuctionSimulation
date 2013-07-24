package agents.shills;

import agents.SimpleUserI;
import simulator.objects.Auction;

public interface PuppetI extends SimpleUserI {

	/**
	 * Makes a bid in the given auction for the given amount.
	 * @param auction
	 * @param bidPrice
	 */
	void makeBid(Auction auction, int bidPrice);

}