package distributions;

import org.apache.commons.math3.distribution.ExponentialDistribution;

public class Test {
	public static void main(String[] args) {
		ExponentialDistribution zd = new ExponentialDistribution(0.7708);
//		ZipfDistributionImpl zd = new ZipfDistributionImpl(3500, 0.1);
		for (int i = 0; i < 10000; i++) {
			System.out.print(zd.sample() + ",");
		}
		System.out.println("finished");
	}
	
	// output = offset - input
	private static int manipulate(int input, int offset) {
		return offset-input;
	}
}
