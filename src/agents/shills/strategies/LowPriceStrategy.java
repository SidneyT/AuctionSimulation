package agents.shills.strategies;

import simulator.objects.Auction;

/**
 * Try to snipe in auctions that have a low price.
 */
public class LowPriceStrategy implements Strategy {

	@Override
	public boolean shouldBid(Auction auction, long currentTime) {
		// bid if current price is at the minimum of $1, and auction is within 3 units of ending.
		if (auction.getEndTime() - currentTime <= 3) {
			if (auction.minimumBid() == 100)
				return true;
		}
		return false;
	}
	
	@Override
	public long wait(Auction auction) {
		return 0;
	}
	
	@Override
	public long bidAmount(Auction auction) {
		return auction.minimumBid();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
