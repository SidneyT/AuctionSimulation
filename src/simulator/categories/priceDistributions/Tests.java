package simulator.categories.priceDistributions;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import simulator.categories.priceDistributions.MultiNormalPrice.NormalParameters;


public class Tests {
	
	@Test
	public void chooseDistributionTest1() {
		NormalParameters dist1 = new NormalParameters(1, 0, 1);
		List<NormalParameters> params = new ArrayList<NormalParameters>();
		params.add(dist1);
		assertTrue(MultiNormalPrice.chooseDistribution(params, 0.2) == dist1);
	}

	@Test
	public void chooseDistributionTest2() {
		NormalParameters dist1 = new NormalParameters(0.5, 0, 1);
		NormalParameters dist2 = new NormalParameters(0.5, 0, 1);
		List<NormalParameters> params = new ArrayList<NormalParameters>();
		params.add(dist1);
		params.add(dist2);
		assertTrue(MultiNormalPrice.chooseDistribution(params, 0.2) == dist1);
	}

	@Test
	public void chooseDistributionTest3() {
		NormalParameters dist1 = new NormalParameters(0.5, 0, 1);
		NormalParameters dist2 = new NormalParameters(0.5, 1, 1);
		List<NormalParameters> params = new ArrayList<NormalParameters>();
		params.add(dist1);
		params.add(dist2);
		assertTrue(MultiNormalPrice.chooseDistribution(params, 0.7) == dist2);
	}

	@Test
	public void chooseDistributionTest4() {
		NormalParameters dist1 = new NormalParameters(0.1, 0, 1);
		NormalParameters dist2 = new NormalParameters(0.5, 1, 1);
		NormalParameters dist3 = new NormalParameters(0.4, 1, 1);
		List<NormalParameters> params = new ArrayList<NormalParameters>();
		params.add(dist1);
		params.add(dist2);
		params.add(dist3);
		assertTrue(MultiNormalPrice.chooseDistribution(params, 0.7) == dist3);
	}

	@Test
	public void chooseDistributionTest5() {
		NormalParameters dist1 = new NormalParameters(0.1, 0, 1);
		NormalParameters dist2 = new NormalParameters(0.5, 1, 1);
		NormalParameters dist3 = new NormalParameters(0.4, 1, 1);
		List<NormalParameters> params = new ArrayList<NormalParameters>();
		params.add(dist1);
		params.add(dist2);
		params.add(dist3);
		assertTrue(MultiNormalPrice.chooseDistribution(params, 0.05) == dist1);
	}

}