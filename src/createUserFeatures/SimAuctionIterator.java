package createUserFeatures;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;

public interface SimAuctionIterator {

	public abstract Iterator<Pair<SimAuction, List<BidObject>>> iterator();
	public abstract Set<UserObject> userRep();

}