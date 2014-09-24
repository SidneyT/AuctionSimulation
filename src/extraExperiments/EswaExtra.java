package extraExperiments;

import java.sql.Connection;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.TreeMultiset;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.TMAuction;
import simulator.database.DBConnection;
import util.Util;

public class EswaExtra {
	public static void main(String[] args) {
		excessBidIncCount();
	}

	private static void excessBidIncCount() {
		Connection conn = DBConnection.getConnection("trademe_small");
		
		TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		
		TreeMultiset<Integer> overMinInc = TreeMultiset.create();
		for (Pair<TMAuction, List<BidObject>> e : it) {
			List<BidObject> bids = e.getValue();
			for (int i = 1; i < bids.size(); i++) {
				int inc = bids.get(i).amount - bids.get(i - 1).amount;
				int min = Util.minIncrement(bids.get(i - 1).amount);
				int diff = inc - min;
				if (diff < 0) {
					System.out.println(diff);
					diff = 0;
				}
				overMinInc.add(diff);
			}
		}
		
		System.out.println(overMinInc);
		System.out.println(overMinInc.size());
		
	}
}
