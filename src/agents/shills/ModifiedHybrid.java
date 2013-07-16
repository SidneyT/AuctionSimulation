package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import agents.SimpleUser;
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
public class ModifiedHybrid extends Hybrid {
	
	private static Logger logger = Logger.getLogger(ModifiedHybrid.class);

	private final Strategy strategy2;
	private final Map<PuppetBidder, WinLoss> winLossMap;
	private final Random r;
	
	public ModifiedHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy1, Strategy strategy2, int numBidder) {
		this(bh, ps, is, ah, ur, types, strategy1, strategy2, 1, numBidder);
	}
	
	protected ModifiedHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy1, Strategy strategy2, int numSeller, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy1, numSeller, numBidder);
		
		r = new Random();
		
		// define strategy and register for sniping events
		this.strategy2 = strategy2;
		ah.registerForSniping(this);
		
		// make winLoss count objects for each bidder
		winLossMap = new HashMap<>();
		for (PuppetBidder puppet : cbs) {
			winLossMap.put(puppet, new WinLoss());
		}
	}

	private static class WinLoss {
		private int winCount = 0;
		private int shillWinCount = 0;
		private int lossCount = 0;
		private int shillLossCount = 0;
	}
	
	@Override
	public void run() {
		super.run();
		
		// now find auctions to snipe in...
	}
	
	
	
	@Override
	public void winAction(SimpleUser agent, Auction auction) {
		super.winAction(agent, auction);
		if (shillAuctions.containsKey(auction))
			winLossMap.get(agent).shillWinCount++;
		else
			winLossMap.get(agent).winCount++;
	}
	
	@Override
	public void lossAction(SimpleUser agent, Auction auction) {
		super.lossAction(agent, auction);
		if (shillAuctions.containsKey(auction))
			winLossMap.get(agent).shillLossCount++;
		else
			winLossMap.get(agent).lossCount++;
	}
	
	@Override
	protected void endSoonAction(Auction auction, long time) {
		super.endSoonAction(auction, time);
		
		if (!shouldSnipe(auction))
			return;
		
		if (strategy2.shouldBid(auction, bh.getTimeMessage().getTime())) {
			// try to find a bidder to bid with
			int size = cbs.size();
			int index = r.nextInt(size); // starting index
			PuppetBidder chosen = null;
			for (int i = index; i < index + size; i++ ) {
				WinLoss wl = winLossMap.get(cbs.get(i % size));
//				if (wl.winCount + wl.shillWinCount < wl.lossCount + wl.shillLossCount) {
				if (wl.winCount < wl.shillLossCount) {
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
		if (shillAuctions.containsKey(auction))
			return false;
		return true;
	}
		
	public static AgentAdder getAgentAdder(final int numberOfAgents, final Strategy strategy1, final Strategy strategy2,  final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfAgents; i++) {
					ModifiedHybrid sc = new ModifiedHybrid(bh, ps, is, ah, ur, types, strategy1, strategy2, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "ModifiedHybrid." + numberOfAgents + "." + strategy1 + "." + strategy2;
			}
		};
	}

	public static void main(String[] args) {
		final int numberOfAgents = 1;
		Strategy strategy = new TrevathanStrategy(0.85, 0.85, 0.85);
		logger.info("Running hybrid with " + strategy.getClass().getSimpleName() + ".");
		Main.run(SaveToDatabase.instance(), getAgentAdder(numberOfAgents, strategy, new LowPriceStrategy(), 4));
	}
	
}
