package simulator.database;

import java.util.Collection;

import simulator.categories.CategoryNode;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;

import agents.SimpleUserI;

public interface SaveObjects {
	void saveBid(Auction auction, Bid bid);
	void saveCategories(Collection<CategoryNode> categories);
//	void saveAuction(Auction auction);
	void saveExpiredAuction(Auction auction, boolean sold);
	void saveFeedback(Feedback feedback);
	void saveItemTypes(Collection<ItemType> itemTypes);
	void saveUser(SimpleUserI user);
	void cleanup();
}