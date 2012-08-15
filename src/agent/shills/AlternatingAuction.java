package agent.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Main;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;

public class AlternatingAuction extends CollusiveShillController {

	public AlternatingAuction(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, 1, numBidder);
	}

	Map<Auction, PuppetBidder> AuctionsAssigned = new HashMap<>(); // Map<auction, bidder assigned to that auction> 
	private int bidderIndex = 0;
	/**
	 * Assign auctions to shills according to the alternating auction strategy
	 */
	@Override
	protected PuppetBidder pickBidder(Auction auction, List<PuppetBidder> bidders) {
		if (!AuctionsAssigned.containsKey(auction)) {
			PuppetBidder chosen = bidders.get(bidderIndex % bidders.size());
			AuctionsAssigned.put(auction, chosen);
//			System.out.println("new: picked " + chosen);
			bidderIndex++;
			return chosen;
		} else {
//			System.out.println("old: picked " + alternatingAuctionsAssigned.get(auction));
			return AuctionsAssigned.get(auction);
		}
	}
	
	public static AgentAdder getAgentAdder(final int numberOfGroups, final Strategy strategy, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					AlternatingAuction sc = new AlternatingAuction(bh, ps, is, ah, ur, types, strategy, numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "AgentAdderAlternatingAuction:" + numberOfGroups;
			}
		};
	}
	
	public static void main(String[] args) {
		final int numberOfGroups = 1;
		Main.run(getAgentAdder(numberOfGroups, new TrevathanStrategy(0.85, 0.85, 0.85), 4));
	}
	
}
