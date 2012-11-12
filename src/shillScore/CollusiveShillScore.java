package shillScore;

import util.IncrementalMean;
import util.Util;

public class CollusiveShillScore {

	private final int id; // bidderId
	
	private double eta;
	final IncrementalMean bindingFactorB;
	private double theta;
	final IncrementalMean bindingFactorA;
	
	public CollusiveShillScore(int id) {
		this.id = id;
		bindingFactorB = new IncrementalMean();
		bindingFactorA = new IncrementalMean();
	}
	
	public int getId() {
		return this.id;
	}
	public void setEta(double eta) {
		this.eta = eta;
	}
	
	public double getEta() {
		return eta;
	}
	
	public void setTheta(double theta) {
		this.theta = theta;
	}
	
	public double getTheta() {
		return theta;
	}
	
	public double getBindingFactorB() {
		return bindingFactorB.average();
	}
	
	public double getBindingFactorA() {
		return bindingFactorA.average();
	}
	
	@Override
	public String toString() {
		return "(" + eta + ", " + bindingFactorB + ")";
	}
	
	public double getScore(ScoreType type, ShillScore ss) {
		switch (type) {
			case AltAuc: 	
				return alternatingAuctionScore(ss);
			case AltBid:
				return alternatingBidScore(ss);
			case Hybrid:
				return hybridScore(ss);
		}
		return -1;
	}

	public double alternatingBidScore(ShillScore ss) {
		double score = ss.getGamma() + ss.getDelta() + ss.getEpsilon() + this.getEta() + this.getBindingFactorB();
		return score /= 5;
	}
	public double alternatingAuctionScore(ShillScore ss) {
		double score = ss.getDelta() + ss.getEpsilon() + ss.getZeta() + this.getTheta() + this.getBindingFactorA();
		return score /= 5;
	}
	public double hybridScore(ShillScore ss) {
		double score = ss.getDelta() + ss.getEpsilon() + ss.getZeta() + this.getBindingFactorB() + this.getBindingFactorA();
		return score /= 5;
	}
	
	public double bayseanScore(ScoreType type, ShillScore ss, double groupSize, double groupMean) {
		double score = getScore(type, ss);
		double bayseanScore = Util.bayseanAverage(groupSize, groupMean, ss.getLossCount(), score);
		return bayseanScore;
	}

	public enum ScoreType {
		AltBid("AltBid"),
		AltAuc("AltAuc"),
		Hybrid("Hybrid");
		
		public final String name;
		ScoreType(String name) {
			this.name = name;
		}
	}
}