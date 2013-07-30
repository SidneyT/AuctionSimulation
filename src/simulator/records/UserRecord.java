package simulator.records;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import agents.EventListenerI;
import agents.SimpleUser;
import agents.SimpleUserI;

/**
 * Keeps track of bidders/sellers in the Auction House.
 * Event messages are sent to those in the record.
 * 
 * Not thread safe.
 * @author SidTDesktop
 */
public class UserRecord {
	
	// Set<User> - keeps track of users
	private final List<SimpleUserI> users;

	public UserRecord() {
		this.users  = Collections.synchronizedList(new ArrayList<SimpleUserI>());
	}
	
	public void addUser(SimpleUserI user) {
		boolean isNew = this.users.add(user);
		assert isNew ;
	}
	
	public List<SimpleUserI> getUsers() {
		return this.users;
	}
	
	public void addUsers(Collection<SimpleUserI> users) {
		for (SimpleUserI user : users) {
			addUser(user);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (SimpleUserI user : users) {
			sb.append(user);
			sb.append(",");
		}
		if (!users.isEmpty())
			sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}
}
