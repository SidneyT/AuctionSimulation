package simulator.records;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import simulator.database.SaveObjects;

import agents.SimpleUser;

/**
 * Keeps track of bidders/sellers in the Auction House.
 * 
 * Not thread safe.
 * @author SidTDesktop
 */
public class UserRecord {
	
	private int userIdCount = 0; // for assigning unique ids
	
	// Set<User> - keeps track of users
	private final List<SimpleUser> users;
	
	public UserRecord() {
		this.users  = Collections.synchronizedList(new ArrayList<SimpleUser>());
	}
	
	public int nextId() {
		return userIdCount++;
	}
	
	public void addUser(SimpleUser user) {
		boolean isNew = this.users.add(user);
		assert isNew ;
	}
	
	public void saveAllUsers() {
		for (SimpleUser user : users) {
			SaveObjects.saveUser(user);
		}
	}
	
	public List<SimpleUser> getUsers() {
		return this.users;
	}
	
	public void addUsers(Collection<? extends SimpleUser> users) {
		for (SimpleUser user : users) {
			addUser(user);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (SimpleUser user : users) {
			sb.append(user);
			sb.append(",");
		}
		if (!users.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}
}
