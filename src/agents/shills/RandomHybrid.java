package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.records.UserRecord;
import util.Util;

public class RandomHybrid extends Hybrid {
	
	private static Logger logger = Logger.getLogger(RandomHybrid.class);

	private final Random r;
	
	public static void main(String[] args) {
		System.out.println(5%1);
	}
	
	public RandomHybrid(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, Strategy strategy, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, numBidder);
		
		r = new Random();
	}

	private Map<Auction, List<PuppetBidder>> auctionsAssigned = new HashMap<>();
	@Override
	protected PuppetBidder pickBidder(Auction auction, List<PuppetBidder> bidders) {
		// pick the set of users to use for this auction
		List<PuppetBidder> selected;
		if (!auctionsAssigned.containsKey(auction)) {
			// randomly choose number of bidders to use
			int num = Util.randomInt(r.nextDouble(), 1, bidders.size());
			// randomly choose users
			selected = new ArrayList<>(randomSample4(bidders, num));
			auctionsAssigned.put(auction, selected);
		} else {
			selected = auctionsAssigned.get(auction);
		}
		
		// pick the bidder to bid for this auction
		return simplePickBidder(auction, selected);
	}

	/**
	 * from <code>http://eyalsch.wordpress.com/2010/04/01/random-sample/,
	 * Floyd's Algorithm</code>.
	 * 
	 * Randomly chooses m items from the given list.
	 * 
	 * @param items items to be chosen from
	 * @param m number of items to choose
	 * @return
	 */
	public <T> Set<T> randomSample4(List<T> items, int m){   
	    HashSet<T> res = new HashSet<T>(m); 
	    int n = items.size();
	    for(int i=n-m;i<n;i++){
	        int pos = r.nextInt(i+1);
	        T item = items.get(pos);
	        if (res.contains(item))
	            res.add(items.get(i));
	        else
	            res.add(item);      
	    }
	    return res;
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
