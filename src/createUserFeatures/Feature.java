package createUserFeatures;

public interface Feature {
	/**
	 * @return the name of the feature
	 */
	public String label();
	/**
	 * @param uf
	 * @return the value of the feature for the given UserFeatures object
	 */
	public double value(UserFeatures uf);
}
