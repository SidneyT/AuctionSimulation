package agents.shills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import agents.SimpleUserI;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.puppets.PuppetI;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Simulation;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.database.SaveToDatabase;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import util.Sample;

/**
 * Similar to HybridT. Similar to the Trevathan collusive shill.
 * But allows multiple collusive sellers. When an auction is to be submitted one of the sellers are selected at random.
 */
public class Hybrid extends CollusiveShillController {
	
	private static Logger logger = Logger.getLogger(Hybrid.class);

	public Hybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, PuppetFactoryI factory, int numSeller, int biddersPerSeller, int shillsPerAuction, int auctionCount) {
		super(bh, ps, is, ah, ur, types, strategy, factory, numSeller, biddersPerSeller, auctionCount);
		
		this.shillsPerAuction = shillsPerAuction;
//		cb = new CombinationGenerator(cbs.size(), shillsPerAuction);
		r = new Random();
	}

	protected Map<Auction, List<PuppetI>> shillsAssigned = new HashMap<>(); // Map<Auction, Shills assigned to that auction>
	private final int shillsPerAuction; // number of bidders to use in an auction
//	private final CombinationGenerator cb;
	protected final Random r;
	/**
	 * Return the puppet bidder to bid in the auction
	 */
	@Override
	protected PuppetI pickBidder(Auction auction) {
		List<PuppetI> selected;
		if (!shillsAssigned.containsKey(auction)) {
		// pick the set of users to use for this auction
			selected = selectSet();
			shillsAssigned.put(auction, selected);
//			System.out.println("choosing from: " + selected + " for " + auction);
		} else {
			selected = shillsAssigned.get(auction);
		}
		
		// pick the bidder to bid for this auction
		return simplePickBidder(auction, selected);
	}
	
	/**
	 * Select the set of shills to use in the shill auction
	 */
	protected List<PuppetI> selectSet() {
//		List<PuppetBidder> selected = new ArrayList<>(shillsPerAuction);
//		// goes through each combination of users deterministically.
//		int[] combination = cb.getNext();
//		for (int i = 0; i < combination.length; i++) {
//			selected.add(cbs.get(combination[i]));
//		}
		
		List<PuppetI> selected = Sample.randomSample(cbs, shillsPerAuction, r);
		Collections.shuffle(selected);
		return selected;
	}
	
	/*
	 * Copied from Alternating Bid class
	 */
	Map<Auction, Integer> alternatingBidderAssigned = new HashMap<>(); // Map<auction, index of next bidder who should bid in that auction>
	protected PuppetI simplePickBidder(Auction auction, List<PuppetI> bidders) {
		if (!alternatingBidderAssigned.containsKey(auction)) {
			PuppetI chosen = bidders.get(0);
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
					Hybrid sc = new Hybrid(bh, ps, is, ah, ur, types, strategy, Puppet.getFactory(), 1, bidderPerAgent, 2, 40);
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
		Simulation.run(SaveToDatabase.instance(), getAgentAdder(numberOfAgents, strategy, 4));
	}

	@Override
	protected PuppetI pickSeller() {
		return css.get(r.nextInt(css.size())); // pick the next seller randomly
	}

}
