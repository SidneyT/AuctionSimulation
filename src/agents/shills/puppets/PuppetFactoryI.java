package agents.shills.puppets;

import java.util.List;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import agents.shills.Controller;
import agents.shills.PuppetI;

public interface PuppetFactoryI {
	
	PuppetI instance(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes);
	
}
