package simulator.objects;

import org.apache.log4j.Logger;

import agents.SimpleUser;

/**
 * Bid object. Comparator sorts list from lowest to highest.
 */
public class Bid implements Comparable<Bid>{
	
	Logger logger = Logger.getLogger(Bid.class);
	
	private int id;
	private long time;
	private final SimpleUser bidder;
	private final long price;
	
	public Bid(SimpleUser bidder, long price) {
		this.id = -1;
		this.bidder = bidder;
		this.price = price;
		this.time = -1;
		
		assert(bidder != null) : "Bidder is null";
		assert(price > 0) : "Price must be > 0, but is " + price + ".";
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		if (this.id == -1) {
			this.id = id;
		} else {
			logger.error("Bid id cannont be changed.");
			assert false;
		}
	}
	
	public SimpleUser getBidder() {
		return bidder;
	}

	public long getPrice() {
		return price;
	}

	public long getTime() {
		return this.time;
	}
	
	public void setTime(long time) {
		if (this.time == -1) {
			this.time = time;
		} else {
			logger.error("Bid time cannont be changed.");
			assert false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(id:");
		sb.append(this.getId());
		sb.append(", bidder:");
		sb.append(this.getBidder());
		sb.append(", time:");
		sb.append(this.getTime());
		sb.append(", price:");
		sb.append(this.getPrice());
		sb.append(")");
		return sb.toString();
	}

	// sorts bids from lowest to highest
	@Override
	public int compareTo(Bid o) {
		long diff = this.getPrice() - o.getPrice();
		if (diff > 1)
			return 1;
		else if (diff < 1)
			return -1;
		else return 0;
	}
}
