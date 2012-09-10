package simulator.buffers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import simulator.objects.Auction;
import simulator.objects.Bid;

public class BidsToAh {

	private final ConcurrentHashMap<Auction, List<Bid>> map;
	
	public BidsToAh() {
		this.map = new ConcurrentHashMap<Auction, List<Bid>>();
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
	public Map<Auction, List<Bid>> get() {
		return this.map;
	}
	
	/**
	 * NOT TO BE CALLED BY AuctionHouse
	 */
	public void put(Auction auction, Bid bid) {
		if (!this.map.containsKey(auction)) {
			List<Bid> newAuctionBids = Collections.synchronizedList(new ArrayList<Bid>());
			map.putIfAbsent(auction, newAuctionBids);
		}
		this.map.get(auction).add(bid);
	}
	
}
