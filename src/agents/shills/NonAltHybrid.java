package agents.shills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import agents.shills.strategies.Strategy;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;

public class NonAltHybrid extends Hybrid {

	private final Random r;
	
	public NonAltHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, numBidder);
		
		r = new Random();
	}
	
	
	
	List<Double> weights = Arrays.asList(0.65, 0.35);
	@Override
	public PuppetBidder simplePickBidder(Auction auction, List<PuppetBidder> bidders) {
		double num = r.nextDouble();
		for (int i = 0; i < bidders.size(); i++) {
			num = num - weights.get(i);
			if (num < 0)
				return bidders.get(i);
		}
		return null;
	}
	
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					NonAltHybrid sc = new NonAltHybrid(bh, ps, is, ah, ur, types, strategy, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "NonAltHybrid." + numberOfAgents + "." + strategy;
			}
		};
	}

}
