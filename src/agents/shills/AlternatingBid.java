package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class AlternatingBid extends CollusiveShillController {

	public AlternatingBid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, 1, numBidder, 40);
	}

	Map<Auction, Integer> alternatingBidderAssigned = new HashMap<>(); // Map<auction, index of next bidder who should bid in that auction>
	private int bidderIndex = 0;
	/**
	 * Assign bids to shills according to the alternatig bid strategy.
	 * All bidders take turns to bid in a particular auction.
	 * 
	 * Bidder index keeps track of who should make the first bid in this auction.
	 */
	@Override
	protected PuppetBidder pickBidder(Auction auction) { 
		if (!alternatingBidderAssigned.containsKey(auction)) {
			PuppetBidder chosen = cbs.get(bidderIndex % cbs.size());
			bidderIndex = (bidderIndex + 1) % cbs.size();
			alternatingBidderAssigned.put(auction, bidderIndex);
//			System.out.println("chosen " + chosen);
			return chosen;
		} else {
			int index = alternatingBidderAssigned.put(auction, (alternatingBidderAssigned.get(auction) + 1)  % cbs.size());
			return cbs.get(index);
		}
	}
	
	/**
	 * Chooses the next bidder in the bidList.
	 * @param auction
	 * @param bidders
	 * @return
	 */
	public PuppetBidder simplePickBidder(Auction auction, List<PuppetBidder> bidders) {
		if (!alternatingBidderAssigned.containsKey(auction)) {
			PuppetBidder chosen = bidders.get(0);
			alternatingBidderAssigned.put(auction, 1);
//			System.out.println("chosen " + chosen);
			return chosen;
		} else {
			int index = alternatingBidderAssigned.put(auction, (alternatingBidderAssigned.get(auction) + 1)  % bidders.size());
			return bidders.get(index);
		}
	}
	
	public static AgentAdder getAgentAdder(final int numberOfGroups, final Strategy strategy, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					AlternatingBid sc = new AlternatingBid(bh, ps, is, ah, ur, types, strategy, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "AgentAdderAlternatingBid:" + numberOfGroups + ":" + strategy;
			}
		};
	}
	
	public static void main(String[] args) {
		final int numberOfGroups = 1;
		Main.run(SaveToDatabase.instance(), getAgentAdder(numberOfGroups, new TrevathanStrategy(0.85, 0.85, 0.85), 4));
	}

	@Override
	protected PuppetSeller pickSeller() {
		return css.get(0);
	}
	
}
