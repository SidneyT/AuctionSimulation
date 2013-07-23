package agents.repInflation;

import java.util.ArrayList;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.records.UserRecord;
import agents.SimpleUser;

public class SimpleRepInflation extends SimpleUser implements AgentAdder {

	public SimpleRepInflation(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah) {
		super(bh, ps, is, ah);
		// TODO Auto-generated constructor stub
	}

	
	
	
	
	
	@Override
	public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
		
	}
	
}
