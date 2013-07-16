package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;

import agents.shills.strategies.Strategy;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import util.Sample;
import util.Util;

/**
 * Extends Hybrid, but uses a random number of shill bidders to work together in each auction
 */
public class RandomHybrid extends Hybrid {
	
	private static Logger logger = Logger.getLogger(RandomHybrid.class);

	
	public RandomHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, numBidder);
	}

	private final Random r = new Random();
	@Override
	protected List<PuppetBidder> selectSet() {
		int num = r.nextInt(cbs.size() + 1);
		// pick users from available
		List<PuppetBidder> selected = Sample.getSample(cbs.iterator(), num);
		return selected;
	}

	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					Hybrid sc = new RandomHybrid(bh, ps, is, ah, ur, types, strategy, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "AgentAdderRandomHybrid." + numberOfAgents + "." + strategy;
			}
		};
	}


}
