package simulator.categories.priceDistributions;

import distributions.Normal;

public class NormalPrice implements PriceDistribution {

	private Normal n;
	private double mean, stdDev; // parameters for distribution
	
	public NormalPrice(double mean, double stdDev) {
		n = new Normal();
		this.mean = mean;
		this.stdDev = stdDev;
	}
	
	@Override
	public long getPrice() {
		double price = Normal.nextDouble(n.nextDouble(), this.mean, this.stdDev);
		if (price < 100) {
			price = -price + 200;
		}
		return Math.round(price);
	}
	
}
