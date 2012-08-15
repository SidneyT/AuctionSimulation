package distributions;

import java.util.Random;

public class Normal implements Distribution {

	private Random random;
	private final double mean;
	private final double stdDev;
	
	/**
	 * mean = 0, stdDev = 1;
	 */
	public Normal() {
		this.random = new Random();
		this.mean = 1;
		this.stdDev = 1;
	}

	public Normal(double mean, double stdDev) {
		this.mean = mean;
		random = new Random();
		this.stdDev = stdDev;
	}
	
	@Override
	public double nextDouble() {
		return random.nextGaussian() * stdDev + mean;
	}
	
	/**
	 * Transforms value from Nor(0,1) to Nor(mean, stdDev^2)  
	 */
	public static double nextDouble(double random, double mean, double stdDev) {
		return random * stdDev + mean;
	}

	public static double twoNormal(double random1, double random2, double mean1, double stdDev1, double weight1, double mean2, double stdDev2, double weight2) {
		assert weight1 + weight2 == 1;
		if (random1 < weight1)
			return nextDouble(random2, mean1, stdDev1);
		else
			return nextDouble(random2, mean2, stdDev2);
	}
	
}
