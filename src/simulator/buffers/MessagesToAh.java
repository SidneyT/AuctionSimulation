package simulator.buffers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessagesToAh<T> {

	private final List<T> synchronizedList;
	
	public MessagesToAh() {
		this.synchronizedList = Collections.synchronizedList(new ArrayList<T>());
	}
	
	public void startUsersTurn() {
		// make a new list to store new messages to AuctionHouse from users	
		this.synchronizedList.clear();
		assert(synchronizedList.isEmpty());
	}
	
	public void startAhTurn() {
	}
	
	/**
	 * ONLY CALLED BY AuctionHouse
	 * Don't need to synchronize when it's a read by 1 thread...? 
	 */
	public List<T> get() {
		// get messages
		return this.synchronizedList;
	}
	
	/**
	 * NOT TO BE CALLED BY AuctionHouse
	 * @param message
	 */
	public void put(T message) {
		this.synchronizedList.add(message);
	}
	
	
}
