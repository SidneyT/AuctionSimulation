package graph;

import java.util.List;

import createUserFeatures.BuildUserFeatures.AuctionObject;
import createUserFeatures.BuildUserFeatures.BidObject;

public interface EdgeTypeI {
	List<int[]> getTuples(AuctionObject auction, List<BidObject> bids);
}