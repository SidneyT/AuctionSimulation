package createUserFeatures.evaluation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableMultimap;

public class Dunn {
	
	public static void main(String[] args) throws IOException {
		run();
	}
	private static void run() throws IOException {
		File[] inputFiles = Silhouette.getFiles();
		
		for (File inputFile : inputFiles) {
			Path inputFilePath = inputFile.toPath();
			HashMap<Integer, List<Double>> centroids = Silhouette.centroids(inputFilePath);
			ImmutableMultimap<Integer, List<Double>> data = Silhouette.data(inputFilePath);
			
			System.out.println(inputFile.getName() + ", " + dunnIndex(data, centroids));
		}
		
	}
	
	private static double dunnIndex(ImmutableMultimap<Integer, List<Double>> clusters, HashMap<Integer, List<Double>> centroids) {
		
		// find the biggest cluster
		double largestClusterSize = 0;
//		for (Object cluster : clusters.values()) {
		for (int centroidIndex : centroids.keySet()) {
			double clusterSize = clusterSize(clusters.get(centroidIndex), centroids.get(centroidIndex));
			largestClusterSize = Math.max(clusterSize, largestClusterSize);
		}
		
		// find the minimum distance
		double minimumClusterDistance = 0;
		for (int i = 0; i < centroids.size() - 1; i++) {
			for (int j = i + 1; j < centroids.size(); j++) {
				double clusterDistance = clusterDistance(clusters.get(i), centroids.get(i), clusters.get(j), centroids.get(j));
				minimumClusterDistance = Math.max(clusterDistance , minimumClusterDistance);
			}
		}
		
		return minimumClusterDistance/largestClusterSize;
		
	}
	
	private static double clusterDistance(Collection<List<Double>> cluster1, List<Double> centroid1, Collection<List<Double>> cluster2, List<Double> centroid2) {
		double sum = 0;
		
		// distance of cluster 1 points to centroid 2
		for (List<Double> point : cluster1) {
			double distance = Silhouette.euclideanDistance(point, centroid2);
			sum += distance;
		}
		// distance of cluster 2 points to centroid 1
		for (List<Double> point : cluster2) {
			double distance = Silhouette.euclideanDistance(point, centroid1);
			sum += distance;
		}
		
		sum /= cluster1.size() + cluster2.size();
		
		return sum;
	}
	
	private static double clusterSize(Collection<List<Double>> cluster, List<Double> centroid) {
		double sum = 0;
		for (List<Double> point : cluster) {
			double distance = Silhouette.euclideanDistance(point, centroid);
			sum += distance;
		}
		
		sum *= 2;
		sum /= cluster.size();
		
		return sum;
	}
}
