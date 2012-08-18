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

import weka.clusterers.SimpleKMeans;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.ClusterAnalysis;


/**
 * Doesn't do what you need.
 * 
 * Evaluates accuracy of TM and Syn datasets using a user specified seed for each dataset.
 * The problem is that the seed is used to pick a random instances in each dataset. Using the
 * same seed in different datasets is unlikely to give you similar centroids, so you can't
 * do Accuracy VS centroid choice, since you don't actually control centroids chosen.
 * 
 */
public class MultipleEvaluation_Bad implements Runnable {
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
		ExecutorService es = Executors.newFixedThreadPool(3);
		for (int simSeed = 0; simSeed < 50; simSeed++) {
			System.out.println("Using seed: " + simSeed + ".");
			List<Callable<Object>> tasks = new ArrayList<>();
			for (File simFile : getClusteredFiles()) {
				tasks.add(Executors.callable(new MultipleEvaluation_Bad(simSeed, simFile)));
			}
			es.invokeAll(tasks);
		}
		es.shutdown();
	}
	
	private static final String folder = "synData";
	private static BufferedWriter bw_full;
	private static BufferedWriter bw_short;
	private final static int tmSeed = 1357;
	private final static int numberOfClusters = 4;
	
	private final int simSeed;
	private final File simFile;
	
	public MultipleEvaluation_Bad(int simSeed, File simFile) {
		this.simSeed = simSeed;
		this.simFile = simFile;
	}
	
	/**
	 * Evaluates 1 file with 1 seed.
	 */
	public void run() {
		try {
			System.out.println("Processing " + simFile + " using seed " + simSeed + ".");
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
			SimpleKMeans clusterer = new SimpleKMeans();
			clusterer.setSeed(simSeed);
			clusterer.setNumClusters(numberOfClusters);
			ClusterAnalysis.clusterUserFeatures(simFile.toPath(), clusteredSimFile, clusterInfoFile, clusterer);
			
			AccuracyEvaluation.evaluate(tmFilename, clusteredSimFile.toString(), numberOfClusters, bw_full, bw_short, features, tmSeed, simSeed, runNumber);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Evaluates all files in the folder with all seeds.
	 */
	private static void runAll() {
		try {
			int numberOfClusters = 4;
			int tmSeed = 1357;
	//		int simSeed = 2468;
			String folder = "synData";
			BufferedWriter bw_full = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_full.txt"), Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			BufferedWriter bw_short = Files.newBufferedWriter(Paths.get(folder, "MultipleEvaluation_short.txt"), Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			for (int simSeed = 0; simSeed < 50; simSeed++) {
				System.out.println("Using seed: " + simSeed + ".");
				for (File simFile : getClusteredFiles()) {
	//				System.out.println("Processing " + simFile + ".");
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
					SimpleKMeans clusterer = new SimpleKMeans();
					clusterer.setSeed(simSeed);
					clusterer.setNumClusters(numberOfClusters);
					ClusterAnalysis.clusterUserFeatures(simFile.toPath(), clusteredSimFile, clusterInfoFile, clusterer);
					
					AccuracyEvaluation.evaluate(tmFilename, clusteredSimFile.toString(), numberOfClusters, bw_full, bw_short, features, tmSeed, simSeed, runNumber);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Finds files with names like: "-3ln-10-6ln-11_t_0.csv"
	 * @return
	 */
	private static File[] getClusteredFiles() {
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
