package agent.shills;

import simulator.objects.Auction;

public interface Strategy {
	/**
	 * Whether a bid should be made according to the strategy.
	 * @param auction
	 * @param currentTime
	 * @return
	 */
	boolean shouldBid(Auction auction, long currentTime);
	
	/**
	 * How long to wait before the bid should be made.
	 * @param auction
	 * @return
	 */
	long wait(Auction auction);
	
	/**
	 * How much to bid according to the strategy.
	 * @param auction
	 * @return
	 */
	long bidAmount(Auction auction);
	
}
