package createUserFeatures;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import simulator.categories.ItemType;

import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public interface SimAuctionIterator {

	/**
	 * 
	 * @return List of BidObjects ordered in price ascending order 
	 */
	public abstract Iterator<Pair<SimAuction, List<BidObject>>> getIterator();
	public abstract Map<Integer, UserObject> users();
	public abstract Map<Integer, ItemType> itemTypes();

}