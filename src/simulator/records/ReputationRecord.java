package simulator.records;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import simulator.objects.Feedback;

/**
 * Records the total positive and negative feedback scores of a user from other
 * users. Unique fields means that only the first score is counted from each unique user.
 *
 */
public class ReputationRecord {
//	private long reputation;
	private int pos, posUnique, neu, neg, negUnique;
//	private List<Feedback> repHistory;
	private final Set<Integer> receivedPosFeedback; // for keeping track of unique positive or negative reputations
	private final Set<Integer> receivedNegFeedback; // for keeping track of unique positive or negative reputations
	
	public ReputationRecord() {
		this.receivedPosFeedback = new HashSet<Integer>();
		this.receivedNegFeedback = new HashSet<Integer>();
		this.pos = 0;
		this.posUnique = 0;
		this.neu = 0;
		this.neg = 0;
		this.negUnique = 0;
	}
	
	public int getPos() {
		return pos;
	}

	public int getPosUnique() {
		return posUnique;
	}

	public int getNeu() {
		return neu;
	}

	public int getNeg() {
		return neg;
	}

	public int getNegUnique() {
		return negUnique;
	}
	
	/**
	 * return getPosUnique() - getNegUnique()
	 */
	public int getNetRep() {
		return getPosUnique() - getNegUnique();
	}

	public void addFeedback(int userId, Feedback feedback) {
		assert(feedback.timeIsSet());
		
		// find out whether this reputation record is for this user as a seller or winner
		int winnerId = feedback.getAuction().getWinner().getId();
		int sellerId = feedback.getAuction().getSeller().getId();
		int feedbackGiverId;
		if (userId != winnerId) {
			feedbackGiverId = winnerId;
		} else {
			feedbackGiverId = sellerId;
		}
		
		switch (feedback.getVal()) {
			case POS:
				this.pos++;
				if (receivedPosFeedback.add(feedbackGiverId))
					this.posUnique++;
				break;
			case NEU:
				this.neu++;
				break;
			case NEG:
				this.neg++;
				if (receivedNegFeedback.add(feedbackGiverId))
					this.negUnique++;
				break;
		}
		
	}
	
	public static void generateRep(ReputationRecord rr, Random r) {
		double nRandom = r.nextGaussian() * 1.5432 + 4.4746; // mean = 4, std dev = 1.4213
		int netRep;
		if (nRandom < 0) {
			netRep = (int) -(Math.exp(-nRandom) + 0.5);
			rr.posUnique = (int) (r.nextGaussian() / 2.77);
			if (rr.posUnique < 0)
				rr.posUnique = -rr.posUnique;
			rr.negUnique = -netRep + rr.posUnique;
		} else {
			netRep = (int) (Math.exp(nRandom) + 0.5);
			rr.negUnique = (int) (r.nextGaussian() / 2.77 + (0.005435 * netRep));
			if (rr.negUnique < 0)
				rr.negUnique = -rr.negUnique;
			rr.posUnique = netRep + rr.negUnique;
		}
		if (!(rr.posUnique >= 0 && rr.negUnique >= 0))
			throw new AssertionError();
	}

	@Override
	public String toString() {
		return posUnique - negUnique + "";
	}
	
}
