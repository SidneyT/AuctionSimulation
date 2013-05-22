package createUserFeatures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



import util.IncrementalSD;

import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

//printUserId = splits.contains("0");
//printNumAuctionsBidIn = splits.contains("1");
//printNumAuctionsBidInLn = splits.contains("1ln");
//printRep = splits.contains("2");
//printRepLn = splits.contains("2ln");
//printAvgBid = splits.contains("3");
//printAvgBidLn = splits.contains("3ln");
//printAvgBidPropMax = splits.contains("10");
//printBidInc = splits.contains("4");
//printPropWin = splits.contains("5");
//printAvgBidsPerAuc = splits.contains("6");
//printAvgBidProp = splits.contains("11");
//printBidPeriod = splits.contains("7");
//printBidPeriodAlt = splits.contains("9");
//printAucPerCat = splits.contains("8");

public class ClusterAnalysis {
	public static void main(String[] args) {
//		clusterSimData("");
		clusterTmData(1356, "");
		System.out.println("Finished.");
	}
	
	public static void clusterSimData(String suffix) {
////		String featuresForClustering = "-1ln-2ln-3ln-10-6ln-11-12";
////		String featuresToPrint = "-1ln-2ln-3ln-10-6ln-5-11-12-15";
//		String featuresForClustering = "-3ln-10-5-6ln-11";
//		String featuresToPrint = "-3ln-10-5-6ln-11";
////		String featuresForClustering = "-1ln-2ln-3ln-10-4ln-5-11-9";
////		String featuresToPrint = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
		
		int numberOfClusters = 4;
		List<Features> featureList = Arrays.asList(
				Features.AvgBidPropMax10,
				Features.PropWin5,
				Features.BidsPerAuc6Ln,
				Features.AvgBidProp11);

		clusterToFile(new BuildSimFeatures(true), 2468, featureList, featureList, numberOfClusters, suffix);
	}
	
	public static void clusterTmData(int seed, String suffix) {
////		String featuresForClustering = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15";
//		String featuresToPrint = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15";
////		String featuresForClustering = "-1ln-2ln-3ln-10-6ln-11-12";
////		String featuresToPrint = "-1ln-2ln-3ln-10-6ln-5-11-12-15";
//		String featuresForClustering = "-10-5-6ln-11";
////		String featuresToPrint = "-2ln-3ln-10-5-6ln-11";
////		String featuresForClustering = "-1ln-2ln-3ln-10-4ln-5-11-9";
////		String featuresToPrint = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
		
		List<Features> featureList = Arrays.asList(
				Features.AvgBidPropMax10,
				Features.PropWin5,
				Features.BidsPerAuc6Ln,
				Features.AvgBidProp11);

		int numberOfClusters = 4;
		clusterToFile(new BuildTMFeatures(), seed, featureList, featureList, numberOfClusters, suffix);
	}
	
	public static String generateFilename(Class<? extends BuildUserFeatures> clazz, boolean trim, int seed, List<Features> featuresToPrintString, int numberOfClusters, String suffix) {
		String delimiter = "_";
		if (trim)
			return clazz.getSimpleName() + delimiter + Features.labels(featuresToPrintString) + delimiter + "t" + delimiter + SimpleKMeans.class.getSimpleName() + delimiter + seed + delimiter + numberOfClusters + "c" + suffix;
		else
			return clazz.getSimpleName() + delimiter + Features.labels(featuresToPrintString) + delimiter + SimpleKMeans.class.getSimpleName() + delimiter + seed + delimiter + numberOfClusters + "c" + suffix;
	}
	
	// returns the Filename of the file written to
	public static String clusterToFile(BuildUserFeatures buf, int seed, List<Features> featuresToCluster, List<Features> featuresToPrint, int numberOfClusters, String suffix) {
		return clusterToFile(buf, seed, featuresToCluster, featuresToPrint, numberOfClusters, false, false, -1, suffix);
	}
	
	/**
	 * Writes user features from buf to a temporary file, then clusters that file.
	 * @param buf
	 * @param seed
	 * @param featuresToPrint
	 * @param numberOfClusters
	 * @param usePca
	 * @param recluster
	 * @param clusterToRecluster
	 * @param suffix
	 * @return
	 */
	private static String clusterToFile(BuildUserFeatures buf, int seed, List<Features> featuresToClusterOn, List<Features> featuresToPrint, int numberOfClusters, boolean usePca, boolean recluster, int clusterToRecluster, String suffix) {
		File tempUserFeaturesFile = null;
		String tempUserFeaturesFilePath = null;
		try {
			tempUserFeaturesFile = File.createTempFile("userFeatures", ".csv");
//			tempUserFeaturesFile.deleteOnExit();
			tempUserFeaturesFilePath = tempUserFeaturesFile.getCanonicalPath();
			System.out.println("Temporary file path: " + tempUserFeaturesFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Map<Integer, UserFeatures> userFeaturesMap = buf.build();
		String filename = "";
//		if (recluster) {
//			int clusterId = clusterToRecluster;
//			userFeaturesMap = buf.reclustering_contructUserFeatures(clusterId);
//			filename = "_recluster" + clusterId + buf.getFeaturesToPrintString();
//		}
		BuildUserFeatures.writeToFile(userFeaturesMap.values(), featuresToClusterOn, tempUserFeaturesFile.toPath());
		BuildUserFeatures.removeIncompleteUserFeatures(userFeaturesMap);
//		
//		if (usePca) {
//			try {
//				filename += "_pca";
//				File tempFile2 = File.createTempFile("userFeatures", ".arff");
//				tempFile2.deleteOnExit();
//				tempUserFeaturesFilePath = tempFile2.getCanonicalPath();
//				Weka.Pca(tempUserFeaturesFile.getCanonicalPath(), tempFile2.getCanonicalPath());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		
		for (int seedInc = 0; seedInc < 1000; seedInc++) {
		seed++;
		filename = generateFilename(buf.getClass(), buf.trim(), seed, featuresToPrint, numberOfClusters, suffix);
		
//		filename += buf.getClass().getSimpleName() + buf.getFeaturesToPrintString();
//		if (buf.trim())
//			filename += "_t";

		// clusterer to use
//		XMeans clusterer = new XMeans();
		SimpleKMeans kMeans = new SimpleKMeans();
		kMeans.setSeed(seed);
		try {
			kMeans.setNumClusters(numberOfClusters);
//			filename += "_" + numberOfClusters + "c";
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
//		filename += "_" + clusterer.getClass().getSimpleName();
		
		// name of the output infoFile, and clustered file
		String infoFilename = filename + "_info.txt";
		String clusteredDataFilename = filename + suffix +  ".csv";
		
		// write UserFeatures object and the cluster it belongs to, to a file
		try {
			BufferedWriter infoFileWriter = Files.newBufferedWriter(Paths.get(infoFilename), Charset.defaultCharset());
			List<Integer> clusteringAssignments = Weka.cluster(kMeans, tempUserFeaturesFilePath, infoFileWriter);
			Map<Integer, List<UserFeatures>> clusterMap = sortIntoClusters(userFeaturesMap, clusteringAssignments); // Map<Cluster index, list of user features>
			Map<Integer, ClusterInfo> clusterStats = new HashMap<Integer, ClusterInfo>();// Map<Cluster index, cluster info>
			
			infoFileWriter.write("@cluster_averages");
			infoFileWriter.newLine();
			infoFileWriter.write(ClusterInfo.headings());
			infoFileWriter.newLine();
			for (int i = 0; i < clusterMap.size(); i++) {
				ClusterInfo ci = calculateAggregates(clusterMap.get(i));
				ci.clusterLabel = "c" + i;
				clusterStats.put(i, ci);
				infoFileWriter.write(ci.toString());
				infoFileWriter.newLine();
			}
			infoFileWriter.newLine();
			infoFileWriter.flush();

			Path outputFilePath = Paths.get(clusteredDataFilename);
			BufferedWriter outputBW = Files.newBufferedWriter(outputFilePath, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			
			// print out the centroids found by simpleKMeans
			outputBW.append("@centroids");
			outputBW.newLine();
			Instances centroids = kMeans.getClusterCentroids();
			for (int i = 0; i < centroids.numInstances(); i++) {
				double[] centroid = centroids.instance(i).toDoubleArray();
				for (int j = 0; j < centroid.length; j++) {
					if (j == 0)
						outputBW.append("" + centroid[j]);
					else
						outputBW.append("," + centroid[j]);
				}
				outputBW.newLine();
			}
			
			outputBW.append("@data");
			outputBW.newLine();
			
			// print out the headings
//			outputBW.append(Features.labels(featuresToPrint)).append(",Cluster");
//			outputBW.newLine();
			writeUserFeaturesWithClustersToFile(featuresToPrint, userFeaturesMap, clusteringAssignments, outputBW);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		}
		
		return null;
	}
	
	/**
	 * Cluster a file with a columns of user features, and write the file out together with the cluster they belong to.
	 * @param inputFileLocation Dataset with user features
	 * @param clusteredFileLocation Output file that will contain the user features and the cluster assignments
	 * @param clusteredInfoLocation Output file that will contain information about the clustering
	 * @param clusterer The clusterer to use
	 */
	public static void clusterUserFeatures(Path inputFileLocation, Path clusteredFileLocation, Path clusteredInfoLocation, RandomizableClusterer clusterer) {
		try {
			BufferedWriter infoWriter = Files.newBufferedWriter(clusteredInfoLocation, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			
			List<Integer> clusteringAssignments = Weka.cluster(clusterer, inputFileLocation.toString(), infoWriter);
			
			// reader and writer for user features & clustering assignments
			BufferedReader userFeaturesReader = Files.newBufferedReader(inputFileLocation, Charset.defaultCharset());
			BufferedWriter clusteredWriter = Files.newBufferedWriter(clusteredFileLocation, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

			// first row is headings, so just copy that across into the file with clustering assignments, and add the cluster column heading
			clusteredWriter.append(userFeaturesReader.readLine() + ",Cluster");
			clusteredWriter.newLine();
			
			// copy each row from the user features file, add the clustering assignment, then write it into the new file
			int lineCounter = 0;
			while (userFeaturesReader.ready()) {
				String line = userFeaturesReader.readLine();
				int cluster = clusteringAssignments.get(lineCounter);
				
				String lineWithCluster = line + ",c" + cluster;
				clusteredWriter.append(lineWithCluster);
				clusteredWriter.newLine();
				
				lineCounter++;
			}
			clusteredWriter.flush();
			assert lineCounter == clusteringAssignments.size() : lineCounter + " is not equal to " + clusteringAssignments.size();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Given 2 ordered collections of user features and clusters, put them into a map, with the user cluster as the key, and a list
	 * of user features that have that cluster assignment as the value.
	 * @param userFeatures
	 * @param userClusters
	 * @return a Map containing entries (cluster assignment, list(user features))
	 */
	private static Map<Integer, List<UserFeatures>> sortIntoClusters(Map<Integer, UserFeatures> userFeatures, List<Integer> userClusters) {
		// Map<clusterId, UserFeature List>
		Map<Integer, List<UserFeatures>> clusterMap = new HashMap<Integer, List<UserFeatures>>();

		// record the cluster each instance belongs to
		Iterator<UserFeatures> it1 = userFeatures.values().iterator();
		Iterator<Integer> it2 = userClusters.iterator();
		assert(userFeatures.size() == userClusters.size());
		while(it1.hasNext()) {
			UserFeatures userFeature = it1.next();
			int cluster = it2.next();
			if (!userFeature.isComplete())
				continue;

			if (!clusterMap.containsKey(cluster)) {
				clusterMap.put(cluster, new ArrayList<UserFeatures>());
			}
			clusterMap.get(cluster).add(userFeature);
		}

		return clusterMap;
	}

	/**
	 * UserFeatures objects in userFeaturesCol are written to the file filename.
	 * @param clusteringAssignments 
	 */
	private static void writeUserFeaturesWithClustersToFile(List<Features> featuresToPrint, Map<Integer, UserFeatures> userFeaturesMap, List<Integer> clusteringAssignments, BufferedWriter outputBW) {
		try {
			int clusterAssignmentsIndex = 0; // for going through clusterAssignments list while iterating through userFeatures collection
			for (UserFeatures uf : userFeaturesMap.values()) {
//				if (uf.pos != -1) { // && uf.neg != -1
					outputBW.append(Features.values(featuresToPrint, uf) + ",c" + clusteringAssignments.get(clusterAssignmentsIndex++));
					outputBW.newLine();
//				}
			}
			outputBW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ClusterInfo calculateAggregates(Collection<UserFeatures> userFeaturesCol) {
		int count = 0;

		ClusterInfo ci = new ClusterInfo();
		List<Features> features = Features.ALL_FEATURES_MINUS_TYPE;
		for (Features feature : features) {
			ci.clusterMap.put(feature, new IncrementalSD());
		}

		for (UserFeatures uf : userFeaturesCol) {
			
			for (Features feature : features) {
				ci.clusterMap.get(feature).addNext(feature.value(uf));
			}
			count++;
		}
		ci.size = count;

		return ci;
	}

	private static class ClusterInfo {
		private static final String delimiter = ",";

		int size;
		String clusterLabel;
		EnumMap<Features, IncrementalSD> clusterMap = new EnumMap<>(Features.class);
		

		public static String headings() {
			return "clusterLabel, size " + Features.labels(Arrays.asList(Features.values()));
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(clusterLabel + delimiter + size + delimiter);
			for (IncrementalSD sd : clusterMap.values()) {
				sb.append(sd).append(",");
			}
//			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}

}
