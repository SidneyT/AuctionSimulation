package distributions;

import java.util.Random;

public class Uniform implements Distribution {

	private Random r;
	private double range, offset;
	
	/**
	 * min inclusive, max exclusive
	 */
	public Uniform(int min, int max) {
		r = new Random();
		this.range = max - min;
		this.offset = min;
	}
	
	@Override
	public double nextDouble() {
		return r.nextDouble() * range + offset;
	}
	
	/**
	 * min inclusive, max exclusive
	 */
	public static double nextDouble(double random, double min, double max) {
		return random * (max - min) + min;
	}

	/**
	 * min inclusive, max exclusive
	 */
	public static int nextInt(double random, int min, int max) {
		return (int) (random * (max - min) + min);
	}
	
}