package agents.shills.puppets;

import java.util.List;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import agents.sellers.TMSeller;

public class TMSellerPuppet extends TMSeller {

	private final int repInflationTarget;
	
	public TMSellerPuppet(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes, int repInflationTarget) {
		super(bh, ps, is, ah, itemTypes);
		this.repInflationTarget = repInflationTarget;
	}

	/**
	 * Override the method, so that no puppet submits too many auctions.
	 * This is so that seller puppets do not greatly affect the total number of auctions in the simulation. 
	 */
	@Override
	protected int numberOfAuctions(double random) {
		int numberOfAuctions = super.numberOfAuctions(random);
		while (numberOfAuctions > 25)
			numberOfAuctions = super.numberOfAuctions(Math.random());
		return numberOfAuctions;
//		return 0;
	}
	
	public int repInflationTarget() {
		return repInflationTarget;
	}
	
	@Override
	public void run2() {
		super.run2();
	}
}
