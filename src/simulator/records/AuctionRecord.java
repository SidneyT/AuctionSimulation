package simulator.records;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import simulator.objects.Auction;
import util.Util;

/**
 * NOT THREAD SAFE.  All methods that change data structures should only
 * be accessed by 1 thread - the AuctionHouse.
 */
public class AuctionRecord {

	// Map<User ID, Set<Auction>> - keeps track of auctions submitted by users
//	private final Map<Integer, Set<Auction>> submissions;
	private final AtomicInteger auctionCount; // number of auctions submitted.  used to give auctions an ID.
	// Map<Auction endTime, Auction object>
	private final Map<Long, Set<Auction>> currentAuctions;
//	private final Map<Long, Set<Auction>> expiredAuctions;
	
	// the set of auctions that are running
	private final Set<Auction> currentSet;
	
	public AuctionRecord() {
		auctionCount = new AtomicInteger();
		
//		this.submissions = new HashMap<Integer, Set<Auction>>();
		this.currentAuctions = new HashMap<Long, Set<Auction>>();
//		this.expiredAuctions = new HashMap<Long, Set<Auction>>();
		this.currentSet = new HashSet<Auction>();
	}
	
	public void addAuction(Auction auction, long time) {
		// set the id and startTime of the auction
		auction.setId(auctionCount.getAndIncrement());
		auction.setStartTime(time);
		
		// recording who submitted this auction
//		int sellerId = auction.getSeller().getId();
//		Util.mapSetAdd(this.submissions, sellerId, auction);

		// recording this as part of current auctions
		this.currentSet.add(auction);
		
		// recording auction according to expiry date
		long expiryTime = auction.getDuration() + time;
		Util.mapSetAdd(this.currentAuctions, expiryTime, auction);

//		SaveToDatabase.saveAuction(auction);
		
		assert(auction.idAndStartTimeSet()); // auction id & startTime must be set
	}
	
	public void addAuctions(Collection<Auction> auctions, long time) {
		for (Auction auction : auctions) {
			addAuction(auction, time);
		}
	}
	
	public boolean isCurrent(Auction auction) {
		return this.currentSet.contains(auction);
	}
	
	public void extendAuction(Auction auction, long time) {
		assert time > 0;
		// remove the auction from the currentAuctions map using the original endTime
		boolean removed = currentAuctions.get(auction.getEndTime()).remove(auction);
		assert(removed);
		if (currentAuctions.get(auction.getEndTime()).isEmpty())
			currentAuctions.remove(auction.getEndTime());
		// change the endTime of the auction
		long newEndTime = auction.extendAuction(time);
		// add the auction into the currentAuctions map using the new endTime
		boolean added = Util.mapSetAdd(currentAuctions, newEndTime, auction);
		assert(added);
	}
	
	public Map<Long, Set<Auction>> getCurrentAuctions() {
		return this.currentAuctions;
	}
	
	/**
	 * Removes expired auctions from the list of currentAuctions and adds them to the 
	 * list of expiredAuctions.
	 * 
	 * @return the list of expired auctions or an emptyList if there are no expired auctions
	 */
	public Set<Auction> removeExpiredAuctions(long time) {
		Set<Auction> expireds = this.currentAuctions.get(time);
		if (expireds == null) {
			return Collections.emptySet();
		}
		this.currentSet.removeAll(expireds);
		return this.currentAuctions.remove(time);
	}

//	public boolean expiresThisTurn(long time, Auction auction) {
//		Set<Auction> bin = this.currentAuctions.get(time);
//		if (bin == null)
//			return false;
//		else
//			return bin.contains(auction);
//	}
	
}
