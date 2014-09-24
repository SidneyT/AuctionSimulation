package util;

public class IncrementalSD extends IncrementalMean {
	protected double stdDev = 0;
	
	@Override
	public void add(double newValue) {
		double oldAverage = average;
		super.add(newValue); // update average
		stdDev = stdDev + (newValue - oldAverage) * (newValue - average);
	}
	
	public double getSD() {
		if (numElements <=  1)
			return Double.NaN;
		return Math.sqrt(stdDev / (numElements - 1));
	}
	
	public String toStringLong() {
		return "(sd: " + this.stdDev + ", avg: " + this.average + ", numEle: " + this.numElements + ")";
	}
	
	@Override
	public String toString() {
		return this.average + "," + this.stdDev;
	}

	public String toStringShort() {
		return this.average + "," + this.stdDev + "," + this.numElements;
	}

	
	public static void main(String[] args) {
		IncrementalSD sd = new IncrementalSD();
		sd.add(1.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.add(3.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.add(5.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.add(7.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.add(9.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.add(11.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
//		System.out.println(sd.average);
	}
	
}
