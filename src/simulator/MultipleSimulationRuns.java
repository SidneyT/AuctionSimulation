package simulator;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import createUserFeatures.BuildSimFeatures;
import createUserFeatures.BuildUserFeatures;
import createUserFeatures.Feature;
import createUserFeatures.Features;
import createUserFeatures.UserFeatures;

/**
 * Runs the simulator multiple times with each run giving multiple files with different feature sets.
 * Files from each run are numbered.
 */
public class MultipleSimulationRuns {
	public static void main(String[] args) {
		System.out.println("Started.");
		for (int i = 20; i < 30; i++) {
			run(i + "");
			System.out.println("Run " + i + " done.");
		}
		System.out.println("Finished.");
	}

	/**
	 * @param label number for for labelling the output files
	 */
	public static void run(String label) {
		try {
			// run the simulator
			Main.run();

			// make multiple sets of user features from the synthetic data
			List<List<Feature>> featureSets = Arrays.asList(
					Arrays.<Feature>asList(Features.values()),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.PropWin5, Features.BidsPerAuc6Ln, Features.AvgBidProp11),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.BidsPerAuc6Ln, Features.AvgBidProp11),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.PropWin5, Features.BidsPerAuc6Ln, Features.AvgBidProp11, Features.BidTimesMinsBeforeEnd12),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.PropWin5, Features.BidsPerAuc6Ln, Features.BidTimesMinsBeforeEnd12),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.BidsPerAuc6Ln, Features.AvgBidProp11, Features.BidTimesMinsBeforeEnd12),
					Arrays.<Feature>asList(Features.AvgBid3Ln, Features.AvgBidPropMax10, Features.BidsPerAuc6Ln, Features.BidTimesMinsBeforeEnd12)
					);
			boolean trim = true;
			BuildSimFeatures buildFeatures = new BuildSimFeatures(trim);
			
			
			Map<Integer, UserFeatures> userFeatureMap = buildFeatures.build();
			
			String folder = "synData";
			for (List<Feature> featureSet : featureSets) {
				String filename;
				if (trim)
					filename = featureSet + "_t_" + label + ".csv";
				else
					filename = featureSet + "_" + label + ".csv";
				BuildUserFeatures.writeToFile(userFeatureMap.values(), featureSet, Paths.get(folder, filename));

				// cluster the data
//				SimpleKMeans clusterer = new SimpleKMeans();
//				int seed = 2468;
//				int numberOfClusters = 4;
//				clusterer.setNumClusters(numberOfClusters);
//				clusterer.setSeed(seed);
//				
//				ClusterAnalysis.clusterUserFeatures(Paths.get(folder, filename), 
//						Paths.get(folder, features + "_" + trim + "_" + seed + "_" + numberOfClusters + "c" + "_" + runCount + ".csv"),
//						Paths.get(folder, features + "_" + trim + "_" + seed + "_" + numberOfClusters + "c" + "_" + runCount + "_info.txt"),
//						clusterer,
//						1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
