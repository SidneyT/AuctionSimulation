package simulator.modelEvaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import weka.clusterers.SimpleKMeans_Modified;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.ClusterAnalysis;
import createUserFeatures.features.Feature;
import createUserFeatures.features.Features;

/**
 * Similar to MultipleEvaluation_SetCentroids except centroids are not randomly picked using
 * a seed value, and instead read from a file.
 */
public class AccuracyEvaluation_SetCentroids implements Runnable {

	public static void main(String[] args) throws Exception {
		makeWriters();

		multi();
		System.out.println("Finished.");
	}

	private static void makeWriters() throws IOException {
		bw_full = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_full.txt"), Charset.defaultCharset(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		bw_short = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_short.txt"), Charset.defaultCharset(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	/**
	 * 
	 * @param centroidFile
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void multi() throws IOException, InterruptedException {
		Path centroidsFile = Paths.get("-10-5-6ln-11_centroids.csv");
		new AccuracyEvaluation_SetCentroids(centroidsFile).run();
	}

	private static final String folder = "synData";
	private static BufferedWriter bw_full;
	private static BufferedWriter bw_short;
	private final static int tmSeed = 1356;
	private final static int numberOfClusters = 4;
	private final Instances centroids;

	public AccuracyEvaluation_SetCentroids(Path centroidFile) {
		this.centroids = getCentroids(centroidFile);
	}

	private Instances getCentroids(Path file) {
		try {
			DataSource instances = new DataSource(file.toString());
			Instances centroids = instances.getDataSet();
			assert centroids.numInstances() == numberOfClusters;
			return centroids;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static int simSeed = -1; // i.e. not used

	public void run() {

		System.out.println("Using centroids:");
		printCentroids(centroids);

		try {
			String folder = "synData";
			for (File simFile : getSynDataFiles()) {
				System.out.println("Processing " + simFile + ".");
				String[] filenameParts = simFile.getName().replace(".csv", "").split("_");
				
				List<Feature> features = new ArrayList<>();
				for (String f : Arrays.asList(filenameParts[0].split(","))) {
					features.add(Features.valueOf(f));
				}
				
				int runNumber = Integer.parseInt(filenameParts[2]);

				// check if TM clustered file exists. If not, cluster it.
				String tmFilename = ClusterAnalysis.generateFilename(BuildTMFeatures.class, false, tmSeed, features,
						numberOfClusters, "") + ".csv";
				if (!new File(tmFilename).exists()) {
					ClusterAnalysis.clusterToFile(new BuildTMFeatures(), tmSeed, features, features, numberOfClusters, "");
					System.out.println(tmFilename + " does not exist. Creating.");
				}

				Path clusterInfoFile = Paths.get(folder,
						simFile.getName().replace(".csv", "_" + simSeed + "_clusterInfo.txt"));
				// cluster the simFile
				String clusteredSimFilename = simFile.getName().replace(".csv", "_" + simSeed + ".csv");
				Path clusteredSimFile = Paths.get(folder, clusteredSimFilename);
				SimpleKMeans_Modified clusterer = new SimpleKMeans_Modified(centroids);
				clusterer.setSeed(simSeed);
				clusterer.setNumClusters(numberOfClusters);

				ClusterAnalysis.clusterUserFeatures(simFile.toPath(), clusteredSimFile, clusterInfoFile, clusterer);

				AccuracyEvaluation.evaluate(tmFilename, clusteredSimFile.toString(), numberOfClusters, bw_full, bw_short,
						features, tmSeed, simSeed, runNumber);

				Instances centroidsUsed = clusterer.getCentroidsUsed();

				// System.out.println("Closest centroids Used:");
				printCentroids(centroidsUsed);
				// break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printCentroids(Instances instances) {
		for (int i = 0; i < instances.numInstances(); i++) {
			System.out.println(instances.instance(i));
		}
	}

	public static File[] getSynDataFiles() {
		File directory = new File("synData");
		return directory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (file.isFile() && file.getName().split("_").length == 3 && file.getName().endsWith(".csv"))
					return true;
				else
					return false;
			}
		});
	}
}
