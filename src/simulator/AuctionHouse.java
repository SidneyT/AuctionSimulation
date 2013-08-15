package simulator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.google.common.collect.ListMultimap;

import agents.EventListener;
import agents.EventListenerI;
import agents.SimpleUser;
import agents.SimpleUserI;
import simulator.buffers.BufferHolder;
import simulator.buffers.Message;
import simulator.buffers.MessageType;
import simulator.buffers.MessagesToUsers;
import simulator.database.SaveObjects;
import simulator.objects.Auction;
import simulator.objects.Bid;
import simulator.objects.Feedback;
import simulator.records.AuctionInterestRecord;
import simulator.records.AuctionRecord;
import simulator.records.BidRecord;
import simulator.records.UserRecord;

/**
 * Announces time to other threads. Processes messages sent to it from other threads. Sends messages to other threads.
 * Identifies auctions that have expired and removes them.
 * 
 * Keeps track of time.
 * 
 * Single threaded.
 */
public class AuctionHouse implements Runnable {

	public static final int UNIT_LENGTH = 5; // length of each time unit
	private static final Logger logger = Logger.getLogger(AuctionHouse.class);

	int time;

	private final UserRecord userRecord; // event messages are sent to those in the userRecords
	private final Set<EventListener> eventListeners; // those who want event messages and who are not users
	private final AuctionRecord auctionRecord;
	private final BidRecord bidRecord;

	// used to keep track of who to announce auction events to
	private final AuctionInterestRecord interestRecord;

	// private final CategoryRecord categoryRecord;

	private final BufferHolder buffers;

	private final SaveObjects saveObjects;
	
	public AuctionHouse(UserRecord userRecord, BufferHolder buffers,
	// , CategoryRecord categoryRecord
			SaveObjects saveObjects
	) {
		time = -1;
		this.userRecord = userRecord;
		this.buffers = buffers;
		// this.categoryRecord = categoryRecord;
		this.auctionRecord = new AuctionRecord();
		this.bidRecord = new BidRecord(); //TODO
		this.interestRecord = new AuctionInterestRecord();

//		snipingRecord = Collections.newSetFromMap(new ConcurrentHashMap<EventListenerI, Boolean>());
		snipingRecord = new HashSet<>();

		eventListeners = new HashSet<>();
		
		this.saveObjects = saveObjects;
	}

	public boolean addEventListener(EventListener eventListener) {
		return eventListeners.add(eventListener);
	}

	public Set<EventListener> getEventListeners() {
		return eventListeners;
	}

	// public CategoryRecord getCategoryRecord() {
	// return this.categoryRecord;
	// }

	@Override
	public void run() {
		processFeedbackMessages();
		processAuctionMessages();
		processBidMessages();
		processExpiredAuctions();

		giveSnipeWarning();

		incrementTime();
		announceTimeTick();
	}

	private void processFeedbackMessages() {
		Collection<Feedback> feedbacks = buffers.getFeedbackToAh().get();

		for (Feedback feedback : feedbacks) {
			SimpleUserI user;
			if (feedback.forSeller())
				user = feedback.getAuction().getSeller();
			else
				user = feedback.getAuction().getWinner();

			feedback.setTime(this.time);
			user.addFeedback(feedback);
			saveObjects.saveFeedback(feedback);
		}

	}

	private void announceTimeTick() {
		logger.debug("Announcing time: " + this.time);
		this.buffers.getTimeMessage().setTime(this.time);
	}

	private void incrementTime() {
		time++;
	}

	// int debugAuctionCounter = 0;
	private void processAuctionMessages() {
		List<Auction> newAuctions = this.buffers.getAuctionMessagesToAh().get();
		// debugAuctionCounter += newAuctions.size();
		MessagesToUsers buffer = this.buffers.getMessagesToUsers();
		Collection<SimpleUserI> userMap = this.userRecord.getUsers();

		for (Auction newAuction : newAuctions) {
			this.auctionRecord.addAuction(newAuction, this.time);
			sendNewAuctionMessages(userMap, newAuction, buffer);
			sendNewAuctionMessages(eventListeners, newAuction, buffer);
			logger.debug("Processed new auction: " + newAuction + " at time " + time + ".");
		}
	}

	private void sendNewAuctionMessages(Collection<? extends EventListenerI> users, Auction newAuction, MessagesToUsers buffer) {
		for (EventListenerI user : users) {
			buffer.putMessages(user.getId(), new Message(MessageType.NEW, newAuction));
		}
	}

	private void processBidMessages() {
		ListMultimap<Auction, Bid> allBids = this.buffers.getBidMessageToAh().get();
		MessagesToUsers buffer = this.buffers.getMessagesToUsers();

		for (Auction auction : allBids.keySet()) {
			// if (auctionRecord.expiresThisTurn(this.time, auction)) {
			// // do nothing. auction end method will take care of messages
			// } else {
			logger.debug("Recived " + allBids.get(auction).size() + " unprocessed bids " + allBids.get(auction)
					+ " for auction " + auction + " at " + this.time);

			// assert testAllBiddersRegisteredInterest(allBids.get(auction), interestRecord.getInterested(auction));
			// if (!testAllBiddersRegisteredInterest(allBids.get(auction), interestRecord.getInterested(auction))) {
			// testAllBiddersRegisteredInterest(allBids.get(auction), interestRecord.getInterested(auction));
			// }
			assert this.auctionRecord.isCurrent(auction) : "Bids " + allBids.get(auction) + " received at " + time
					+ " are invalid since " + auction + " is already expired at time " + time + ".";

			// find winner for this round for this auction
			// all runner up bids are discarded and ignored
			Bid winningBid = this.bidRecord.processAuctionBids(auction, allBids.get(auction), this.time);
			saveObjects.saveBid(auction, winningBid);
			
			// notify users of new bid
			Set<EventListener> interestedUsers = interestRecord.getInterested(auction);
			// assert interestedUsers != null : "Error. There were no interested users for auction: " + auction + " at "
			// + this.time + ".";
			sendNewBidMessages(interestedUsers, auction, buffer);

			logger.debug(auction.getWinner() + " winning auction " + auction);

			// extend the auction if it's near the end
			extendIfCloseToEnd(auction);
			// }
		}
	}

	private void extendIfCloseToEnd(Auction auction) {
		long timeLeft = auction.getEndTime() - this.time;
		if (timeLeft < 3) {
			auctionRecord.extendAuction(auction, 3 - timeLeft);
		}
	}

	// /**
	// * Tests whether all users that the bids belong to have registered intereset for the auction.
	// * @param bids
	// * @param interestedSet
	// * @return
	// */
	// private boolean testAllBiddersRegisteredInterest(List<Bid> bids, Set<SimpleUser> interestedSet) {
	// for (Bid bid : bids) {
	// if (!interestedSet.contains(bid.getBidder()))
	// return false;
	// }
	// return true;
	// }

	private void sendNewBidMessages(Set<EventListener> interestedUsers, Auction auction, MessagesToUsers buffer) {
		for (EventListener user : interestedUsers) {
			buffer.putMessages(user.getId(), new Message(MessageType.PRICE_CHANGE, auction));
		}
	}

	/**
	 * For telling agents who want to snipe, all auctions that are about to end.
	 */
	private void giveSnipeWarning() {
		int time = this.time + 3; // get auctions that will end at this time
		Set<Auction> endSoon = this.auctionRecord.getCurrentAuctions().get(time);
		if (endSoon == null)
			return;

		MessagesToUsers msgToUsers = this.buffers.getMessagesToUsers();
		for (Auction auction : endSoon) {
			for (EventListenerI agent : snipingRecord) {
				msgToUsers.putMessages(agent.getId(), new Message(MessageType.END_SOON, auction));
			}
		}
	}

	private void processExpiredAuctions() {
		// remove expired auctions
		Set<Auction> expireds = this.auctionRecord.removeExpiredAuctions(this.time);

		if (expireds.isEmpty())
			return;

		logger.debug("Removed expired auctions: " + expireds + " at " + this.time + ".");

		MessagesToUsers msgToUsers = this.buffers.getMessagesToUsers();

		for (Auction expired : expireds) {

			// remove interest list
			Set<EventListener> interestedSet = this.interestRecord.removeAuction(expired);

			// notify winner
			SimpleUserI winner = expired.getWinner();
			if (winner != null) {
				// boolean winnerWasInterested = interestedSet.remove(winner);
				// assert winnerWasInterested : winner + " was not interested for auction " + expired + ".";
				msgToUsers.putMessages(winner.getId(), new Message(MessageType.WIN, expired));
			}

			// notify losers
			for (EventListener interested : interestedSet) {
				if (interested != winner) // winner won, don't notify as loss.
					msgToUsers.putMessages(interested.getId(), new Message(MessageType.LOSS, expired));
			}

			// notify seller
			if (winner != null) {
				msgToUsers.putMessages(expired.getSeller().getId(), new Message(MessageType.SOLD, expired));
			} else {
				msgToUsers.putMessages(expired.getSeller().getId(), new Message(MessageType.EXPIRED, expired));
			}

			saveObjects.saveExpiredAuction(expired, winner != null);
		}
	}

	public void saveUsers() {
		for (SimpleUserI user : userRecord.getUsers()) {
			saveObjects.saveUser(user);
		}
	}

	// @Override
	public void registerForAuction(EventListener user, Auction auction) {
		logger.debug("Registering " + user + " from " + auction);
		interestRecord.register(auction, user);
	}

	private final Set<EventListenerI> snipingRecord;

	public void registerForSniping(EventListenerI user) {
		snipingRecord.add(user);
	}

	//
	// @Override
	// public void deregisterForAuction(EventListener user, Auction auction) {
	// logger.debug("Deregistering " + user + " from " + auction);
	// boolean exists = interestRecord.unregister(auction, user);
	// assert(exists);
	// }

}
