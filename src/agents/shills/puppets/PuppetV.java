package agents.shills.puppets;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import agents.shills.Controller;

/**
 * Puppet that only relays events to the controller.
 */
final class PuppetV extends Puppet {

	public PuppetV(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes, Controller controller, int id) {
		super(bh, ps, is, ah, controller, itemTypes, id);
	}
	
	public PuppetV(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, List<ItemType> itemTypes, Controller controller) {
		super(bh, ps, is, ah, controller, itemTypes);
	}

	@Override
	public void run() {
		super.run();
	}

	@Override
	public void winAction(Auction auction, long time) {
		controller.winAction(this, auction);
	}

	@Override
	public void soldAction(Auction auction, long time) {
		controller.soldAction(this, auction);
	}

	@Override
	public void gotPaidAction(Collection<Payment> paymentSet) {
		controller.gotPaidAction(this, paymentSet);
	}

	@Override
	public void itemReceivedAction(Set<ItemSold> itemSet) {
		controller.itemReceivedAction(this, itemSet);
	}
	
	public static PuppetFactoryI getFactory() {
		return new PuppetFactoryI() {
			@Override
			public PuppetI instance(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, Controller controller, List<ItemType> itemTypes) {
				return new PuppetV(bh, ps, is, ah, itemTypes, controller);
			}
		};
	}

}