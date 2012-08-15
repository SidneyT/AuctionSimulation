package simulator.objects;

import agent.SimpleUser;

public class Feedback {
	private boolean forSeller;
	private long time;
	private Val val;
	private Auction auction;

	public enum Val {
		POS(1), NEU(0), NEG(-1);
		private int val;
		Val(int val) {
			this.val = val;
		}
		public int getInt() {
			return this.val;
		}
	}

	/**
	 * @param val
	 * @param submitter person submitting the feedback
	 * @param auction
	 */
	public Feedback(Val val, SimpleUser submitter, Auction auction) {
		this.time = -1;
		this.val = val;
		if (auction.getSeller() == submitter)
			forSeller = false;
		else // auction.getWinner() == user
			forSeller = true;
		this.auction = auction;
	}
	
	public void setTime(long time) {
		if (this.time == -1)
			this.time = time;
		else
			assert false : "Feedback time can not be changed.";
	}
	
	public long getTime() {
		return this.time;
	}
	
	public Val getVal() {
		return this.val;
	}
	
	public boolean forSeller() {
		return this.forSeller;
	}
	
	public Auction getAuction() {
		return this.auction;
	}
	
	@Override
	public String toString() {
		return "(" + this.time + ", " + this.val + ", " + this.forSeller + ", " + this.auction + ")";
	}
	
	public boolean timeIsSet() {
		return time != -1;
	}

}
