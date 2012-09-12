package createUserFeatures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import agents.SimpleUser;

import simulator.database.SavedObjects;
import simulator.objects.Auction;
import simulator.objects.Bid;

import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public class SimAuctionMemoryIterator implements SimAuctionIterator {

	private final SavedObjects savedObjects;
	private final boolean trim;
	
	public SimAuctionMemoryIterator(SavedObjects savedObjects, boolean trim) {
		this.savedObjects = savedObjects;
		this.trim = trim;
		hasNext = !savedObjects.getBidStore().isEmpty();
	}
	

	private boolean hasNext;
	
	@Override
	public Iterator<Pair<SimAuction, List<BidObject>>> iterator() {
		final Iterator<Auction> it = savedObjects.getBidStore().keySet().iterator();
		return new Iterator<Pair<SimAuction, List<BidObject>>>() {
			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public Pair<SimAuction, List<BidObject>> next() {
				Auction auction = it.next();
				List<Bid> bids = savedObjects.getBidStore().get(auction);
				List<BidObject> bidObjects = new ArrayList<>();
				for (Bid bid : trim || bids.size() <= 20? bids : bids.subList(bids.size() - 20, bids.size())) {
					bidObjects.add(new BidObject(auction, bid));
				}
				
				// remember if there's another after this
				hasNext = it.hasNext();
				
				return (new Pair<>(new SimAuction(auction), bidObjects));
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public Set<UserObject> userRep() {
		Set<UserObject> userObjects = new HashSet<>();
		for (SimpleUser user : savedObjects.getUserStore()) {
			userObjects.add(new UserObject(user));
		}
		return userObjects;
	}

}
