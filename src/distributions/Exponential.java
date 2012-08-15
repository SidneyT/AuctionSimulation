package distributions;

import java.util.Random;

public class Exponential implements Distribution {

	private final Random r;
	private final double mean;
	
	/**
	 * lambda = 1.
	 */
	public Exponential(double mean) {
		if (mean <= 0)
			throw new IllegalArgumentException("Mean must be greater than 0.");
		r = new Random();
		this.mean = mean;
	}
	
	@Override
	public double nextDouble() {
		double result = -mean * Math.log(1 - r.nextDouble());
		return result;
	}

	/**
	 * 
	 * @param random >= 0, < 1.
	 * @param mean
	 * @return
	 */
	public static double nextDouble(double random, double mean) {
		if (mean <= 0)
			throw new IllegalArgumentException("Mean must be greater than 0.");
		if (random == 1)
			throw new IllegalArgumentException("Random number must be less than 1.");
		return -mean * Math.log(1 - random);
	}
	
}
