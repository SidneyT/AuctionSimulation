package simulator;

import java.nio.file.Paths;
import java.util.TreeMap;

import createUserFeatures.BuildSimFeatures;
import createUserFeatures.BuildUserFeatures;
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
			String[] featuresArray = {"-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15",
					"-3ln-10-5-6ln-11",
					"-3ln-10-6ln-11",
					"-3ln-10-5-6ln-11-12",
					"-3ln-10-5-6ln-12",
					"-3ln-10-6ln-11-12",
					"-3ln-10-6ln-12",
					};
			boolean trim = true;
			BuildSimFeatures buildFeatures = new BuildSimFeatures("", trim);
			
			
			TreeMap<Integer, UserFeatures> userFeatureMap = buildFeatures.build();
			
			String folder = "synData";
			for (String features : featuresArray) {
				String filename;
				if (trim)
					filename = features + "_t_" + label + ".csv";
				else
					filename = features + "_" + label + ".csv";
				buildFeatures.setFeaturesToPrint(features);
				BuildUserFeatures.writeToFile(userFeatureMap.values(), buildFeatures.getFeaturesToPrint(), Paths.get(folder, filename));

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
