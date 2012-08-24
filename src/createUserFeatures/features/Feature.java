package createUserFeatures.features;

import createUserFeatures.UserFeatures;

public interface Feature {
	public String label();
	public double value(UserFeatures uf);
}
