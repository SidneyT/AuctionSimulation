package simulator.records;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import simulator.objects.Auction;
import agent.EventListener;

public class AuctionInterestRecord {
	
	// Map<Auction, List of Users>
	private final ConcurrentHashMap<Auction, Set<EventListener>> interestMap;

	public AuctionInterestRecord() {
		this.interestMap = new ConcurrentHashMap<Auction, Set<EventListener>>();
	}
	
	/**
	 * Thread safe.
	 * @param auction
	 * @param user
	 */
	public void register(Auction auction, EventListener user) {
		Set<EventListener> users = this.interestMap.get(auction);
		if (users == null) {
			Set<EventListener> newUsers = Collections.synchronizedSet(new HashSet<EventListener>());
			users = interestMap.putIfAbsent(auction, newUsers);
			if (users == null) // putIfAbsent success
				newUsers.add(user);
			else // putIfAbsent fails
				users.add(user);
		} else {
			users.add(user);
		}
	}
	
	public boolean unregister(Auction auction, EventListener user) {
		return this.interestMap.get(auction).remove(user);
	}
	
	
	/**
	 * Returns a synchronised set.
	 */
	public Set<EventListener> getInterested(Auction auction) {
		Set<EventListener> interested = this.interestMap.get(auction);
		if (interested == null)
			return Collections.emptySet();
		else
			return interested;
	}
	
	/**
	 * 
	 * @param auction
	 * @return set of interested Users, or an empty set.
	 */
	public Set<EventListener> removeAuction(Auction auction) {
		Set<EventListener> interested = this.interestMap.remove(auction);
		if (interested == null)
			return Collections.emptySet();
		else
			return interested;
	}
}
