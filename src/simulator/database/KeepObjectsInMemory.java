package simulator.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import simulator.categories.CategoryNode;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import agents.SimpleUser;

public class KeepObjectsInMemory implements SaveObjects {
	public KeepObjectsInMemory() {
	}

	private List<SimpleUser> userStore = new ArrayList<SimpleUser>();
	public void saveUser(SimpleUser user) {
		userStore.add(user);
	}

	private Collection<CategoryNode> categories;
	public void saveCategories(Collection<CategoryNode> categories) {
		this.categories = categories;
	}

	private Collection<ItemType> types;
	public void saveItemTypes(Collection<ItemType> types) {
		this.types = types;
	}

	private List<Auction> expiredStore = new ArrayList<Auction>();
	public void saveExpiredAuction(Auction auction, boolean sold) {
		expiredStore.add(auction);
	}

	private  List<Object[]> bidStore = new ArrayList<Object[]>();
	public void saveBid(Auction auction, Bid bid) {
		bidStore.add(new Object[] { auction, bid });
	}

	private List<Feedback> feedbackStore = new ArrayList<>();
	public void saveFeedback(Feedback feedback) {
		feedbackStore.add(feedback);
	}

	public void cleanup() {
	}

	public List<SimpleUser> getUserStore() {
		return userStore;
	}

	public Collection<CategoryNode> getCategories() {
		return categories;
	}

	public Collection<ItemType> getTypes() {
		return types;
	}

	public List<Auction> getExpiredStore() {
		return expiredStore;
	}

	public List<Object[]> getBidStore() {
		return bidStore;
	}

	public List<Feedback> getFeedbackStore() {
		return feedbackStore;
	}
}
