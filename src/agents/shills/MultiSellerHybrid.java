package agents.shills;

import java.util.ArrayList;
import java.util.List;

import agents.shills.strategies.Strategy;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.records.UserRecord;

public class MultiSellerHybrid extends Hybrid {

	public MultiSellerHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy, int numSeller, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, numSeller, numBidder);
	}
	
	private int sellerIndex = -1; 
	@Override
	protected PuppetSeller pickSeller() {
		sellerIndex = (sellerIndex + 1) % css.size();
		return css.get(sellerIndex);
	}
	
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final int numSeller, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					MultiSellerHybrid sc = new MultiSellerHybrid(bh, ps, is, ah, ur, types, strategy, numSeller, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "MultiSellerHybrid." + numberOfAgents + "." + numSeller + "." + numBidder + "." + strategy;
			}
		};
	}

}
