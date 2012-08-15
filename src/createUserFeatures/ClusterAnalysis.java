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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import createUserFeatures.FeaturesToUseWrapper.FeaturesToUse;

import util.Util;

import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;

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
		clusterTmData("");
		System.out.println("Finished.");
	}
	
	public static void clusterSimData(String suffix) {
//		String featuresForClustering = "-1ln-2ln-3ln-10-6ln-11-12";
//		String featuresToPrint = "-1ln-2ln-3ln-10-6ln-5-11-12-15";
		String featuresForClustering = "-3ln-10-5-6ln-11";
		String featuresToPrint = "-3ln-10-5-6ln-11";
//		String featuresForClustering = "-1ln-2ln-3ln-10-4ln-5-11-9";
//		String featuresToPrint = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
		int numberOfClusters = 4;
		clusterToFile(new BuildSimFeatures(featuresForClustering, true), 2468, featuresToPrint, numberOfClusters, suffix);
	}
	
	public static void clusterTmData(String suffix) {
//		String featuresForClustering = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15";
		String featuresToPrint = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15";
//		String featuresForClustering = "-1ln-2ln-3ln-10-6ln-11-12";
//		String featuresToPrint = "-1ln-2ln-3ln-10-6ln-5-11-12-15";
		String featuresForClustering = "-10-5-6ln-11";
//		String featuresToPrint = "-2ln-3ln-10-5-6ln-11";
//		String featuresForClustering = "-1ln-2ln-3ln-10-4ln-5-11-9";
//		String featuresToPrint = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
		int numberOfClusters = 4;
		clusterToFile(new BuildTMFeatures(featuresForClustering), 1356, featuresToPrint, numberOfClusters, suffix);
	}
	
	public static String generateFilename(Class clazz, boolean trim, int seed, String featuresToPrintString, int numberOfClusters, String suffix) {
		String delimiter = "_";
		if (trim)
			return clazz.getSimpleName() + delimiter + featuresToPrintString + delimiter + "t" + delimiter + SimpleKMeans.class.getSimpleName() + delimiter + seed + delimiter + numberOfClusters + "c" + suffix;
		else
			return clazz.getSimpleName() + delimiter + featuresToPrintString + delimiter + SimpleKMeans.class.getSimpleName() + delimiter + seed + delimiter + numberOfClusters + "c" + suffix;
	}
	
	// returns the Filename of the file written to
	public static String clusterToFile(BuildUserFeatures buf, int seed, String featuresToPrint, int numberOfClusters, String suffix) {
		return clusterToFile(buf, seed, featuresToPrint, numberOfClusters, false, false, -1, suffix);
	}
	
	/**
	 * Writes user features from buf to a temporary file, then clusters that file.
	 * @param buf
	 * @param seed
	 * @param featuresToPrintString
	 * @param numberOfClusters
	 * @param usePca
	 * @param recluster
	 * @param clusterToRecluster
	 * @param suffix
	 * @return
	 */
	private static String clusterToFile(BuildUserFeatures buf, int seed, String featuresToPrintString, int numberOfClusters, boolean usePca, boolean recluster, int clusterToRecluster, String suffix) {
		File tempUserFeaturesFile = null;
		String tempUserFeaturesFilePath = null;
		try {
			tempUserFeaturesFile = File.createTempFile("userFeatures", ".csv");
//			tempUserFeaturesFile.deleteOnExit();
			tempUserFeaturesFilePath = tempUserFeaturesFile.getCanonicalPath();
//			System.out.println("Temporary file path: " + tempUserFeaturesFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TreeMap<Integer, UserFeatures> userFeaturesMap = buf.build();
		String filename = "";
//		if (recluster) {
//			int clusterId = clusterToRecluster;
//			userFeaturesMap = buf.reclustering_contructUserFeatures(clusterId);
//			filename = "_recluster" + clusterId + buf.getFeaturesToPrintString();
//		}
		BuildUserFeatures.writeToFile(userFeaturesMap.values(), buf.featuresToPrint.getFeaturesToUse(), tempUserFeaturesFile.toPath());
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
		
		filename = generateFilename(buf.getClass(), buf.trim(), seed, buf.getFeaturesToPrintString(), numberOfClusters, suffix);
		
//		filename += buf.getClass().getSimpleName() + buf.getFeaturesToPrintString();
//		if (buf.trim())
//			filename += "_t";

		// clusterer to use
//		XMeans clusterer = new XMeans();
		SimpleKMeans clusterer = new SimpleKMeans();
		clusterer.setSeed(seed);
		try {
			clusterer.setNumClusters(numberOfClusters);
//			filename += "_" + numberOfClusters + "c";
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
//		filename += "_" + clusterer.getClass().getSimpleName();
		
		// name of the output file
		String infoFilename = filename + "_info.txt";
		
		// write UserFeatures object and the cluster it belongs to, to a file
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(infoFilename), Charset.defaultCharset());
			List<Integer> clusteringAssignments = Weka.cluster(clusterer, tempUserFeaturesFilePath, bw);
			Map<Integer, List<UserFeatures>> clusterMap = sortIntoClusters(userFeaturesMap.values(), clusteringAssignments);
			Map<Integer, ClusterInfo> clusterStats = new HashMap<Integer, ClusterInfo>();
			
			bw.write("@cluster_averages");
			bw.newLine();
			bw.write(ClusterInfo.headings());
			bw.newLine();
			for (int i = 0; i < clusterMap.size(); i++) {
				ClusterInfo ci = calculateAggregates(clusterMap.get(i));
				ci.clusterLabel = "c" + i;
				clusterStats.put(i, ci);
				bw.write(ci.toString());
				bw.newLine();
			}
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String clusteredDataFilename = filename + suffix +  ".csv";
		
		buf.setFeaturesToPrint(featuresToPrintString);
		writeUserFeaturesWithClustersToFile(new FeaturesToUse(featuresToPrintString), userFeaturesMap.values(), clusteredDataFilename);
		
		return clusteredDataFilename;
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
	 * @param userFeaturesCol
	 * @param userClusters
	 * @return a Map containing entries (cluster assignment, list(user features))
	 */
	private static Map<Integer, List<UserFeatures>> sortIntoClusters(Collection<UserFeatures> userFeaturesCol, List<Integer> userClusters) {
		// Map<clusterId, UserFeature List>
		Map<Integer, List<UserFeatures>> clusterMap = new HashMap<Integer, List<UserFeatures>>();

		// record the cluster each instance belongs to
		Iterator<UserFeatures> it1 = userFeaturesCol.iterator();
		Iterator<Integer> it2 = userClusters.iterator();
		assert(userFeaturesCol.size() == userClusters.size());
		while(it1.hasNext()) {
			UserFeatures userFeature = it1.next();
			int cluster = it2.next();
			if (!userFeature.isComplete())
				continue;

			userFeature.cluster = cluster + "";

			if (!clusterMap.containsKey(cluster)) {
				clusterMap.put(cluster, new ArrayList<UserFeatures>());
			}
			clusterMap.get(cluster).add(userFeature);
		}

		return clusterMap;
	}

	/**
	 * Needs ufs to know what headings to print.
	 * UserFeatures objects in userFeaturesCol are written to the file filename.
	 */
	private static void writeUserFeaturesWithClustersToFile(FeaturesToUse ufs, Collection<UserFeatures> userFeaturesCol, String filename) {
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(filename));
			w.write(UserFeatures.headingsWithCluster(ufs));
			w.newLine();
			for (UserFeatures uf : userFeaturesCol) {
//				if (uf.pos != -1) { // && uf.neg != -1
					w.write(uf.toStringWithCluster());
					w.newLine();
//				}
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ClusterInfo calculateAggregates(Collection<UserFeatures> userFeaturesCol) {
		int count = 0;

		ClusterInfo ci = new ClusterInfo();
		for (UserFeatures userFeatures : userFeaturesCol) {
			ci.auctionCount(userFeatures.numAuctionsBidIn(), count);
			ci.auctionCountLn(userFeatures.numAuctionsBidInLn(), count);
			ci.repAvg(userFeatures.rep(), count);
			ci.repAvgLn(userFeatures.repLn(), count);
			ci.bidAvgAvg(userFeatures.avgBid(), count);
			ci.avgBidPropMaxAvg(userFeatures.avgBidAmountComparedToFinal, count);
			ci.avgBidIncAvg(userFeatures.avgBidInc(), count);
			ci.propWinAvg(userFeatures.propWin(), count);
			ci.avgBidPerAucAvg(userFeatures.bidsPerAuc(), count);
			ci.avgBidPropAvg(userFeatures.avgBidProp, count);
			ci.bidPeriodBegAvg(userFeatures.bidPropBeg(), count);
			ci.bidPeriodMidAvg(userFeatures.bidPropMid(), count);
			ci.bidPeriodEndAvg(userFeatures.bidPropEnd(), count);
			ci.bidTimeUntilEndAvg(userFeatures.bidTimeBeforeEndAvg(), count);
			ci.auctionsPerCategoryAvg(userFeatures.auctionsPerCat(), count);

			count++;
		}
		ci.size = count;

		return ci;
	}

	private static class ClusterInfo {
		private static final String delimiter = ",";

		int size;

		String clusterLabel;
		double auctionCountAvg = 0;
		double auctionCountAvgLn = 0;
		double repAvg = 0;
		double repAvgLn = 0;
		double bidAvgAvg = 0;
		double bidAvgIncAvg = 0;
		double avgBidPropMaxAvg = 0;
		double propWinAvg = 0;
		double avgBidPerAucAvg = 0;
		double avgBidPropAvg = 0;
		double bidPeriodBegAvg = 0;
		double bidPeriodMidAvg = 0;
		double bidPeriodEndAvg = 0;
		double bidTimeUntilEndAvg = 0;
		double auctionsPerCategoryAvg = 0;


		public void auctionCount(double newValue, int numElements) {
			auctionCountAvg = Util.incrementalAvg(auctionCountAvg, numElements, newValue);
		}

		public void auctionCountLn(double newValue, int numElements) {
			auctionCountAvgLn = Util.incrementalAvg(auctionCountAvgLn, numElements, newValue);
		}
		
		public void repAvg(int newValue, int numElements) {
			repAvg = Util.incrementalAvg(repAvg, numElements, newValue);
		}

		public void repAvgLn(double newValue, int numElements) {
			repAvgLn = Util.incrementalAvg(repAvgLn, numElements, newValue);
		}
		
		public void bidAvgAvg(double newValue, int numElements) {
			bidAvgAvg = Util.incrementalAvg(bidAvgAvg, numElements, newValue);
		}

		public void avgBidIncAvg(double newValue, int numElements) {
			bidAvgIncAvg = Util.incrementalAvg(bidAvgIncAvg, numElements, newValue);
		}

		public void avgBidPropMaxAvg(double newValue, int numElements) {
			avgBidPropMaxAvg = Util.incrementalAvg(avgBidPropMaxAvg, numElements, newValue);
		}

		public void propWinAvg(double newValue, int numElements) {
			propWinAvg = Util.incrementalAvg(propWinAvg, numElements, newValue);
		}

		public void avgBidPerAucAvg(double newValue, int numElements) {
			avgBidPerAucAvg = Util.incrementalAvg(avgBidPerAucAvg, numElements, newValue);
		}

		public void avgBidPropAvg(double newValue, int numElements) {
			avgBidPropAvg = Util.incrementalAvg(avgBidPropAvg, numElements, newValue);
		}

		public void bidPeriodBegAvg(double newValue, int numElements) {
			bidPeriodBegAvg = Util.incrementalAvg(bidPeriodBegAvg, numElements, newValue);
		}

		public void bidPeriodMidAvg(double newValue, int numElements) {
			bidPeriodMidAvg = Util.incrementalAvg(bidPeriodMidAvg, numElements, newValue);
		}

		public void bidPeriodEndAvg(double newValue, int numElements) {
			bidPeriodEndAvg = Util.incrementalAvg(bidPeriodEndAvg, numElements, newValue);
		}
		
		public void bidTimeUntilEndAvg(double newValue, int numElements) {
			bidTimeUntilEndAvg = Util.incrementalAvg(bidTimeUntilEndAvg, numElements, newValue);
		}

		public void auctionsPerCategoryAvg(double newValue, int numElements) {
			auctionsPerCategoryAvg = Util.incrementalAvg(auctionsPerCategoryAvg, numElements, newValue);
		}

		public static String headings() {
			return "clusterLabel, size, auctionCountAvg, auctionCountAvgLn, repAvg, repAvgLn, bidAvgAvg, bidAvgIncAvg, " +
					"avgBidPropMaxAvg, propWinAvg, bidsPerAucAvg, avgBidPropAvg, bidPeriodBegAvg, bidPeriodMidAvg, " +
					"bidPeriodEndAvg, bidTimeUntilEndAvg, auctionsPerCategoryAvg";
		}

		public String toString() {
			return clusterLabel + delimiter + size + delimiter + auctionCountAvg + delimiter + auctionCountAvgLn + 
					delimiter + repAvg + delimiter + repAvgLn + delimiter + bidAvgAvg + delimiter + bidAvgIncAvg + 
					delimiter + avgBidPropMaxAvg + delimiter + propWinAvg + delimiter + avgBidPerAucAvg + 
					delimiter + avgBidPropAvg + delimiter + bidPeriodBegAvg + delimiter + bidPeriodMidAvg + 
					delimiter + bidPeriodEndAvg + delimiter + bidTimeUntilEndAvg + delimiter + auctionsPerCategoryAvg + delimiter;
		}
	}

}
