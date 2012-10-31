package simulator.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;

import simulator.categories.CategoryNode;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import agents.SimpleUser;

public class KeepObjectsInMemory implements SaveObjects, SavedObjects {
	private KeepObjectsInMemory() {}
	
	public static KeepObjectsInMemory instance() {
		return new KeepObjectsInMemory();
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

	private Set<Auction> expiredStore = new HashSet<Auction>();
	public void saveExpiredAuction(Auction auction, boolean sold) {
		expiredStore.add(auction);
	}

	private ArrayListMultimap<Auction, Bid> bidStore = ArrayListMultimap.create();
	public void saveBid(Auction auction, Bid bid) {
		bidStore.put(auction, bid);
	}

	private List<Feedback> feedbackStore = new ArrayList<>();
	public void saveFeedback(Feedback feedback) {
		feedbackStore.add(feedback);
	}

	public void cleanup() {
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getUserStore()
	 */
	@Override
	public List<SimpleUser> getUserStore() {
		return userStore;
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getCategories()
	 */
	@Override
	public Collection<CategoryNode> getCategories() {
		return categories;
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getTypes()
	 */
	@Override
	public Collection<ItemType> getTypes() {
		return types;
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getExpiredStore()
	 */
	@Override
	public Set<Auction> getExpiredStore() {
		return expiredStore;
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getBidStore()
	 */
	@Override
	public ArrayListMultimap<Auction, Bid> getBidStore() {
		return bidStore;
	}

	/* (non-Javadoc)
	 * @see simulator.database.SavedObjects#getFeedbackStore()
	 */
	@Override
	public List<Feedback> getFeedbackStore() {
		return feedbackStore;
	}
}
