package createUserFeatures;
public enum BidPeriod {
	BEGINNING(0),
	MIDDLE(1),
	END(2);
	private final int i;
	private BidPeriod(int i) {
		this.i = i;
	}
	public int getI() {
		return this.i;
	}
};
