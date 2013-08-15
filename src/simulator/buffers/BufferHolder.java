package simulator.buffers;

import simulator.objects.Auction;
import simulator.objects.Feedback;

/**
 * Object with fields that point to buffers.
 *
 */
public class BufferHolder {
	
	private final TimeMessage timeMessage;
	private final MessagesToUsers messagesToUsers; // information to users about auctions 
	private final MessagesToAh<Auction> auctionsToAh; // holds new auctions submitted by sellers to AH
	private final MessagesToAh<Feedback> feedbackToAh; // hold new feedbacks submitted by users
	private final BidsToAh bidsToAh; // holds new bids made by bidders
	
	public BufferHolder(TimeMessage timeMessage, MessagesToUsers messagesToUsers, 
			MessagesToAh<Auction> auctionMessageToAh, MessagesToAh<Feedback> feedbackToAh, 
			BidsToAh bidMessageToAh) {
		this.timeMessage = timeMessage;
		this.messagesToUsers = messagesToUsers;
		this.auctionsToAh = auctionMessageToAh;
		this.feedbackToAh = feedbackToAh;
		this.bidsToAh = bidMessageToAh;
	}
	
	public TimeMessage getTimeMessage() {
		return this.timeMessage;
	}
	
	public int getTime() {
		return this.timeMessage.getTime();
	}
	
	public MessagesToUsers getMessagesToUsers() {
		return this.messagesToUsers;
	}
	
	public MessagesToAh<Auction> getAuctionMessagesToAh() {
		return this.auctionsToAh;
	}
	
	public MessagesToAh<Feedback> getFeedbackToAh() {
		return this.feedbackToAh;
	}
	
	public BidsToAh getBidMessageToAh() {
		return this.bidsToAh;
	}
	
	public void startAhTurn() {
		messagesToUsers.startAhTurn();
		auctionsToAh.startAhTurn();
		bidsToAh.startAhTurn();
		feedbackToAh.startAhTurn();
		timeMessage.startAhTurn();
	}
	
	public void startUserTurn() {
		messagesToUsers.startUsersTurn();
		auctionsToAh.startUsersTurn();
		bidsToAh.endAhTurn();
		feedbackToAh.startUsersTurn();
		timeMessage.endAhTurn();
	}
	
}
