package simulator.buffers;

public class TimeMessage {
	
//	private boolean ahTurn;
	private int time;
	
	public TimeMessage() {
//		this.ahTurn = true;
	}
	
	/**
	 * NOT TO BE CALLED BY AuctionHouse
	 */
	public synchronized int getTime() {
//		while(this.ahTurn) {
//			try {
//				wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		return time;
	}
	
	/**
	 * ONLY TO BE CALLED BY AuctionHouse
	 */
	public synchronized void setTime(int time) {
//		while(!this.ahTurn) {
//			try {
//				wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		
		this.time = time;
	}
	
	public synchronized void endAhTurn() {
//		this.ahTurn = false;
//		notifyAll();
	}
	
	public synchronized void startAhTurn() {
//		this.ahTurn = true;
//		notifyAll();
	}
	
}
