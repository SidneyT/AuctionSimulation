package createUserFeatures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import agents.SimpleUser;

import simulator.categories.ItemType;
import simulator.database.SavedObjects;
import simulator.objects.Auction;
import simulator.objects.Bid;

import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public class SimMemoryAuctionIterator implements SimAuctionIterator {

	private final SavedObjects savedObjects;
	private final boolean trim;
	
	public SimMemoryAuctionIterator(SavedObjects savedObjects, boolean trim) {
		this.savedObjects = savedObjects;
		this.trim = trim;
	}
	
	@Override
	public Iterator<Pair<SimAuction, List<BidObject>>> getIterator() {
		return new AuctionIterator(savedObjects, trim);
	}
	
	private class AuctionIterator implements Iterator<Pair<SimAuction, List<BidObject>>> {
		private final Iterator<Auction> it;
		private boolean hasNext;

		public AuctionIterator(SavedObjects savedObjects, boolean trim) {
			it = savedObjects.getBidStore().keySet().iterator();
			hasNext = !savedObjects.getBidStore().isEmpty();
		}

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
	}

	@Override
	public Map<Integer, UserObject> users() {
		Builder<Integer, UserObject> userObjects = ImmutableMap.builder();
		for (SimpleUser user : savedObjects.getUserStore()) {
			userObjects.put(user.getId(), new UserObject(user));
		}
		return userObjects.build();
	}

	@Override
	public Map<Integer, ItemType> itemTypes() {
		Builder<Integer, ItemType> b = ImmutableMap.builder();
		for (ItemType itemType : savedObjects.getTypes()) {
			b.put(itemType.getId(), itemType);
		}
		return b.build();
	}

}
