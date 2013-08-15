package agents.shills;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.records.UserRecord;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.puppets.PuppetI;
import agents.shills.strategies.Strategy;

/**
 * Hybrid collusive shill as defined by Trevathan.
 * Multiple shills working with ONE seller.
 * 
 * Many of the parameters are set, giving the optimum choice of (shillCount, shillsPerAuction, and auctionCount)
 */
public class HybridT extends Hybrid {
	
	private static Logger logger = Logger.getLogger(HybridT.class);

	public HybridT(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, PuppetFactoryI factory) {
		super(bh, ps, is, ah, ur, types, strategy, factory, 1, 6, 3, 20); // shillCount = 6, shillsPerAuction = 3. This gives 20 combinations of 3 users. So 20 the shill auctionCount is 20.
	}

	@Override
	protected PuppetI pickSeller() {
		return css.get(0);
	}

	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final PuppetFactoryI factory) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					HybridT sc = new HybridT(bh, ps, is, ah, ur, types, strategy, factory);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "HybridT." + numberOfAgents + "." + strategy;
			}
		};
	}

}
