package distributions;

public interface Distribution {
	/**
	 * Any value may be returned, including negative numbers.
	 * @return
	 */
	public double nextDouble();
}
