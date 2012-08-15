package simulator.buffers;

import simulator.objects.Auction;

public class Message {
	MessageType type;
	Auction auction;
	
	public Message(MessageType type, Auction auction) {
		this.type = type;
		this.auction = auction;
	}
	
	public MessageType getType() {
		return this.type;
	}
	
	public Auction getAuction() {
		return this.auction;
	}
	
	@Override
	public String toString() {
		return type + ":" + auction;
	}
}