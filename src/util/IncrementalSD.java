package util;

public class IncrementalSD extends IncrementalMean {
	protected double stdDev = 0;
	
	@Override
	public void addNext(double newValue) {
		double oldAverage = average;
		super.addNext(newValue); // update average
		stdDev = stdDev + (newValue - oldAverage) * (newValue - average);
	}
	
	public double getSD() {
		if (numElements <=  1)
			return Double.NaN;
		return Math.sqrt(stdDev / (numElements - 1));
	}
	
	@Override
	public String toString() {
		return "(sd: " + this.stdDev + ", avg: " + this.average + ", numEle: " + this.numElements + ")";
	}

	public String toStringShort() {
		return this.average + "," + this.stdDev + "," + this.numElements;
	}

	
	public static void main(String[] args) {
		IncrementalSD sd = new IncrementalSD();
		sd.addNext(1.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.addNext(3.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.addNext(5.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.addNext(7.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.addNext(9.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
		sd.addNext(11.0);
		System.out.println(Math.sqrt(sd.stdDev / (sd.numElements - 1)));
		System.out.println(sd.getSD());
//		System.out.println(sd.average);
	}
	
}
