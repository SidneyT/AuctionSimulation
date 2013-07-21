package agents.shills;

import java.util.ArrayList;
import java.util.List;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.records.UserRecord;
import util.Sample;
import agents.shills.strategies.Strategy;

/**
 * Extends HybridT. 
 * Instead of having a set number of users colluding in each auction, the number is varied. 
 */
public class HybridTVaryCollusion extends HybridT {

	public HybridTVaryCollusion(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy) {
		super(bh, ps, is, ah, ur, types, strategy);
	}

	@Override
	protected List<PuppetBidder> selectSet() {
		int selectCount = r.nextInt(cbs.size()) + 1;
		List<PuppetBidder> selected = Sample.randomSample(cbs.iterator(), selectCount, r);
		return selected;
	}
	
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					HybridTVaryCollusion sc = new HybridTVaryCollusion(bh, ps, is, ah, ur, types, strategy);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "HybridTVaryCollusion." + numberOfAgents + "." + strategy;
			}
		};
	}

}
