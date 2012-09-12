package simulator.database;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;

import simulator.categories.CategoryNode;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import agents.SimpleUser;

public interface SavedObjects {
	public abstract List<SimpleUser> getUserStore();
	public abstract Collection<CategoryNode> getCategories();
	public abstract Collection<ItemType> getTypes();
	public abstract Set<Auction> getExpiredStore();
	public abstract ArrayListMultimap<Auction, Bid> getBidStore();
	public abstract List<Feedback> getFeedbackStore();
}