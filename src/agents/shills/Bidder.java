package agents.shills;

import simulator.objects.Auction;

public interface Bidder {
	public boolean shouldBid(Auction auction);
}
