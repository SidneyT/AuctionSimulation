package simulator.evaluation;

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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weka.clusterers.SimpleKMeans_Modified;
import weka.core.Instances;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.ClusterAnalysis;

/**
 * Measures Accuracy.
 * 
 * Using a given seed, it finds centroids in the first of 30 synthetic data files.
 * Then, using centroids from the first, the instances closest to each of the
 * centroids is found in the subsequent files.
 * 
 * That way, we can measure accuracy (i.e. evaluated against Trade Me data) 
 * according to a particular set of centroids.
 * The centroids chosen in the first file is randomly chosen according to the seed
 * value given.
 * 
 */
public class MultipleEvaluation_SetSeeds implements Runnable {
	
	public static void main(String[] args) throws Exception {
		makeWriters();
		multi();
//		new MultipleEvaluation(1, new File("synData", "-3ln-10-6ln-11_t_0.csv")).run();
		System.out.println("Finished.");
	}
	
	private static void makeWriters() throws IOException {
		bw_full = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_full.txt"), Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		bw_short = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_short.txt"), Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	private static void multi() throws IOException, InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(1);
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int simSeed = 0; simSeed < 50; simSeed++) {
//		int[] seeds = {33};
//		for (int simSeed : seeds) {
//		int[] seeds = {29, 37};
//		for (int simSeed : seeds) {
//			System.out.println("Using seed: " + simSeed + ".");
			tasks.add(Executors.callable(new MultipleEvaluation_SetSeeds(simSeed)));
		}
		es.invokeAll(tasks);
		es.shutdown();
	}
	
	private static final String folder = "synData";
	private static BufferedWriter bw_full;
	private static BufferedWriter bw_short;
	private final static int tmSeed = 1356;
	private final static int numberOfClusters = 4;
	
	private final int simSeed;
	
	public MultipleEvaluation_SetSeeds(int simSeed) {
		this.simSeed = simSeed;
	}
	
	public void run() {
		try {
			String folder = "synData";
//			for (int simSeed = 0; simSeed < 2; simSeed++) {
//				System.out.println("Using seed: " + simSeed + ".");
				Instances centroids = null;
				for (File simFile : getSynDataFiles()) {
					System.out.println("Processing " + simFile + ".");
					String[] filenameParts = simFile.getName().replace(".csv", "").split("_");
					String features = filenameParts[0];
					int runNumber = Integer.parseInt(filenameParts[2]);
					
					// check if TM clustered file exists. If not, cluster it.
					String tmFilename = ClusterAnalysis.generateFilename(BuildTMFeatures.class, false, tmSeed, features, numberOfClusters, "") + ".csv"; 
					if (!new File(tmFilename).exists()) {
						ClusterAnalysis.clusterToFile(new BuildTMFeatures(features), tmSeed, features, numberOfClusters, "");
						System.out.println(tmFilename + " does not exist. Creating.");
					}
					
					Path clusterInfoFile = Paths.get(folder, simFile.getName().replace(".csv", "_" + simSeed + "_clusterInfo.txt"));
					// cluster the simFile
					String clusteredSimFilename = simFile.getName().replace(".csv", "_" + simSeed + ".csv"); 
					Path clusteredSimFile = Paths.get(folder, clusteredSimFilename);
					SimpleKMeans_Modified clusterer = new SimpleKMeans_Modified(centroids);
					clusterer.setSeed(simSeed);
					
//					if (centroids != null) {
//						System.out.println("Centroids suggested:");
//						printCentroids(centroids);
//					}
					
					clusterer.setNumClusters(numberOfClusters);
					ClusterAnalysis.clusterUserFeatures(simFile.toPath(), clusteredSimFile, clusterInfoFile, clusterer);
					
					AccuracyEvaluation.evaluate(tmFilename, clusteredSimFile.toString(), numberOfClusters, bw_full, bw_short, features, tmSeed, simSeed, runNumber);

					
					Instances centroidsUsed = clusterer.getCentroidsUsed();
					if (centroids == null)
						centroids = centroidsUsed;
					
//					System.out.println("Closest centroids Used:");
					printCentroids(centroidsUsed);
					break;
				}
				System.out.println("Thread for seed " + simSeed + " done.");
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void printCentroids(Instances instances) {
		for (int i = 0; i < instances.numInstances(); i++) {
			System.out.println(instances.instance(i));
		}
	}
	
	/**
	 * Finds files with names like: "-3ln-10-6ln-11_t_0.csv"
	 * @return
	 */
	private static File[] getSynDataFiles() {
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
