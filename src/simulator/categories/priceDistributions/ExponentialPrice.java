package simulator.categories.priceDistributions;

import distributions.Exponential;

public class ExponentialPrice implements PriceDistribution {

	private Exponential e;
	private double lambda;
	
	public ExponentialPrice(double lambda) {
		e = new Exponential(lambda);
		this.lambda = lambda;
	}
	
	@Override
	public long getPrice() {
		return Math.round(e.nextDouble() / lambda); 
	}
	
}
