package simulator.categories.priceDistributions;

import java.util.List;

import distributions.Normal;

public class MultiNormalPrice implements PriceDistribution {

	private Normal g;
	private List<NormalParameters> params;
	
	public MultiNormalPrice(List<NormalParameters> dists) {
		g = new Normal();
		params = dists;
	}
	
	@Override
	public long getPrice() {
		// for making sure that weights add up to 1 - can be removed
		{
		double sumWeights = 0;
		for(NormalParameters param : params) {
			sumWeights += param.weight; // count total weights to make sure they add up to 1
		}
		assert(Math.abs(sumWeights - 1) < 0.0001);
		}
		
		NormalParameters dist = chooseDistribution(params, Math.random()); // this is the distribution picked by the random number 
		
		double price = Normal.nextDouble(g.nextDouble(), dist.mean, dist.stdDev);
		if (price < 100) {
			price = -price + 200;
		}
		
		return Math.round(price);
	}
	
	public static NormalParameters chooseDistribution(List<NormalParameters> params, double randomNumber) {
		int i;
		for (i = 0; randomNumber >= 0; i++) {
			randomNumber -= params.get(i).weight;
		}
		return params.get(i - 1); // this is the distribution picked by the random number 
	}
	
	/**
	 * Parameters for specifying weight and parameters for a Normal Distribution. 
	 */
	public static class NormalParameters {
		
		private double weight, mean, stdDev;
		
		public NormalParameters(double weight, double mean, double stdDev) {
			this.weight = weight;
			this.mean = mean;
			this.stdDev = stdDev;
		}
		
	}
	
	
}
