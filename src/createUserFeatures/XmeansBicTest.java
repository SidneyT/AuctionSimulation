package createUserFeatures;

import java.lang.reflect.Field;

import org.apache.commons.math3.util.Pair;

import util.IncrementalSD;
import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

public class XmeansBicTest {
	public static void main(String[] args) throws Exception {
		Instances ds = DataSource.read("F:/workstuff2011/AuctionSimulation/TradeMeUserFeaturesSmall-1ln-2ln-3ln-4ln-6ln-13-9-5-10-11_noId.csv");

		for (int numClusters = 2; numClusters < 11; numClusters++) {
			IncrementalSD bicMean = new IncrementalSD();
			IncrementalSD distortionMean = new IncrementalSD();
			for (int seed = 5000; seed < 5030; seed++) {
				Pair<Double, Double> results = xMeansBidTest(seed, numClusters, ds);
				double distortion = results.getKey();
				double bic = results.getValue();
				
				distortionMean.add(distortion);
				bicMean.add(bic);
				System.out.println(numClusters + "," + seed + "," + bic + "," + distortion);
			}
//			System.out.println(numClusters + "," + bicMean.average() + "," + bicMean.getSD() + "," + distortionMean.average() + "," + distortionMean.getSD());
		}
	}
	
	private static Pair<Double, Double> xMeansBidTest(int seed, int clusters, Instances ds) throws Exception {
		XMeans xmeans = new XMeans();
		xmeans.setMaxIterations(20);
		xmeans.setMinNumClusters(clusters);
		xmeans.setMaxNumClusters(clusters);
		xmeans.setSeed(seed);
		
		xmeans.buildClusterer(ds);
		
		Field distortionField = XMeans.class.getDeclaredField("m_Mle");
		distortionField.setAccessible(true);
		double distortion = Utils.sum((double[]) distortionField.get(xmeans));
		
		Field bicField = XMeans.class.getDeclaredField("m_Bic");
		bicField.setAccessible(true); //required if field is not normally accessible
		double bic = (double) bicField.get(xmeans);
		
		return new Pair<Double, Double>(distortion, bic);
	}
}
