package agents.shills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import agents.SimpleUserI;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.strategies.LowPriceStrategy;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Main;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.database.SaveToDatabase;
import simulator.objects.Auction;
import simulator.records.UserRecord;

/**
 * Does what Hybrid does, except bidding agents also bid in non-shill
 * auctions using the given strategy. E.g. bidding in low priced
 * auctions to improve reputations
 */
public class HybridLowPrice extends HybridTVaryCollusion {
	
	private static Logger logger = Logger.getLogger(HybridLowPrice.class);

	private final Strategy strategy2;
	private final Map<PuppetI, int[]> winLossMap; // Map<agent, [win, shillAuctionWin, loss, shillAuctionLoss]>
	
	public HybridLowPrice(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy1, Strategy strategy2, PuppetFactoryI factory) {
		super(bh, ps, is, ah, ur, types, strategy1, factory);
		
		// define strategy and register for sniping events
		this.strategy2 = strategy2;
		ah.registerForSniping(this);
		
		// make winLoss count objects for each bidder
		winLossMap = new HashMap<>();
		for (PuppetI puppet : cbs) {
			winLossMap.put(puppet, new int[4]);
		}
	}

	@Override
	public void run() {
		super.run();
	}
	
	
	
	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
		super.winAction(agent, auction);
		if (shillAuctions.contains(auction)) // count shill/non-shill auctions seperately
			winLossMap.get(agent)[1]++;
		else if (expiredShillAuctions.contains(auction))
			winLossMap.get(agent)[1]++;
		else
			winLossMap.get(agent)[0]++;
	}
	
	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
		super.lossAction(agent, auction);
		if (shillAuctions.contains(auction)) // count shill/non-shill auctions seperately
			winLossMap.get(agent)[3]++;
		else if (expiredShillAuctions.contains(auction))
			winLossMap.get(agent)[1]++;
		else
			winLossMap.get(agent)[2]++;
	}
	
	@Override
	public void endSoonAction(Auction auction, long time) {
		super.endSoonAction(auction, time);
		
		if (!shouldSnipe(auction))
			return;
		
		if (strategy2.shouldBid(auction, bh.getTimeMessage().getTime())) {
			// try to find a bidder to bid with
			int size = cbs.size();
			int index = r.nextInt(size); // starting index
			PuppetI chosen = null;
			for (int i = index; i < index + size; i++ ) {
				int[] counts = winLossMap.get(cbs.get(i % size));
//				if (wl.winCount + wl.shillWinCount < wl.lossCount + wl.shillLossCount) {
				if (counts[0] < counts[3]) {
					chosen = cbs.get(i % size);
					break; // found a suitable bidder, so break.
				}
			}
				
			if (chosen != null) {
				chosen.makeBid(auction, strategy2.bidAmount(auction));
			}
		}
	}
	
	private boolean shouldSnipe(Auction auction) {
		// snipe if the auction is not a known shill auction
		return !shillAuctions.contains(auction);
	}
		
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy1, final Strategy strategy2) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					HybridLowPrice sc = new HybridLowPrice(bh, ps, is, ah, ur, types, strategy1, strategy2, PuppetBidder.getFactory());
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "HybridLowPrice." + numberOfAgents + "." + strategy1 + "." + strategy2;
			}
		};
	}

	public static void main(String[] args) {
//		final int numberOfAgents = 1;
//		Strategy strategy = new TrevathanStrategy(0.85, 0.85, 0.85);
//		logger.info("Running hybrid with " + strategy.getClass().getSimpleName() + ".");
//		Main.run(SaveToDatabase.instance(), getAgentAdder(numberOfAgents, strategy, new LowPriceStrategy(), 4));
	}
	
}
