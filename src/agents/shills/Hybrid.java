package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Main;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import util.CombinationGenerator;

public class Hybrid extends CollusiveShillController {
	
	private static Logger logger = Logger.getLogger(Hybrid.class);

	public Hybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int bidderPerAgent) {
		super(bh, ps, is, ah, ur, types, strategy, 1, bidderPerAgent);
	}
	
	protected Hybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int numSeller, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, numSeller, numBidder);
	}

	private CombinationGenerator cb;
	private Map<Auction, List<PuppetBidder>> auctionsAssigned = new HashMap<>();
	private final int numberPerAuction = 2; // number of bidders to use in an auction
	@Override
	protected PuppetBidder pickBidder(Auction auction, List<PuppetBidder> bidders) {
		// pick the set of users to use for this auction
		List<PuppetBidder> selected;
		if (!auctionsAssigned.containsKey(auction)) {
			if (cb == null || !cb.hasMore())
				cb = new CombinationGenerator(bidders.size(), numberPerAuction);
			
			int[] combination = cb.getNext();
			selected = new ArrayList<>(numberPerAuction);
			for (int i = 0; i < combination.length; i++) {
				selected.add(bidders.get(combination[i]));
			}
			
			auctionsAssigned.put(auction, selected);
		} else {
			selected = auctionsAssigned.get(auction);
		}
		
		// pick the bidder to bid for this auction
		return simplePickBidder(auction, selected);
	}
	
	/*
	 * Copied from Alternating Bid class
	 */
	Map<Auction, Integer> alternatingBidderAssigned = new HashMap<>(); // Map<auction, index of next bidder who should bid in that auction>
	public PuppetBidder simplePickBidder(Auction auction, List<PuppetBidder> bidders) {
		if (!alternatingBidderAssigned.containsKey(auction)) {
			PuppetBidder chosen = bidders.get(0);
			alternatingBidderAssigned.put(auction, 1 % bidders.size());
//			System.out.println("chosen " + chosen);
			return chosen;
		} else {
			int index = alternatingBidderAssigned.put(auction, (alternatingBidderAssigned.get(auction) + 1) % bidders.size());
			return bidders.get(index);
		}
	}

	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy, final int bidderPerAgent) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					Hybrid sc = new Hybrid(bh, ps, is, ah, ur, types, strategy, bidderPerAgent);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "Hybrid." + numberOfAgents + "." + strategy;
			}
		};
	}

	public static void main(String[] args) {
		final int numberOfAgents = 1;
		Strategy strategy = new TrevathanStrategy(0.85, 0.85, 0.85);
		logger.info("Running hybrid with " + strategy.getClass().getSimpleName() + ".");
		Main.run(getAgentAdder(numberOfAgents, strategy, 4));
	}
	
}
