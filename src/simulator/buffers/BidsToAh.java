package simulator.buffers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import simulator.objects.Auction;
import simulator.objects.Bid;

public class BidsToAh {

	private final ListMultimap<Auction, Bid> map;
	
	public BidsToAh() {
		this.map = Multimaps.synchronizedListMultimap(ArrayListMultimap.<Auction, Bid>create());
	}
	
	public void endAhTurn() {
		this.map.clear();
	}
	
	public void startAhTurn() {
	}
	
	/**
	 * ONLY TO BE CALLED BY AuctionHouse
	 * Don't need to synchronise when it's a read by 1 thread...? 
	 */
	public ListMultimap<Auction, Bid> get() {
		return this.map;
	}
	
	/**
	 * NOT TO BE CALLED BY AuctionHouse
	 */
	public void put(Auction auction, Bid bid) {
		map.put(auction, bid);
	}
	
}
