package simulator;

import java.util.ArrayList;

import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.records.UserRecord;

/**
 * Used by the simulator to add agents during initialisation.
 * Mainly for adding fraud controllers.
 */
public interface AgentAdder {
	void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types);
}
