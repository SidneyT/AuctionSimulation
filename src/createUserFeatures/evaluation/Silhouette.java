package createUserFeatures.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import util.IncrementalMean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class Silhouette {
	public static void main(String[] args) throws IOException {
		run();
	}
	
	private static void run() {
		try {
			for (File file : getFiles()) {
				Path inputFilePath = file.toPath();
				HashMap<Integer, List<Double>> centroids = centroids(inputFilePath);
				ImmutableMultimap<Integer, List<Double>> data = data(inputFilePath);
				
				IncrementalMean globalS = new IncrementalMean();
				
				Set<Integer> clusterKeys = data.keySet();
				for (Entry<Integer, List<Double>> pointEntry : data.entries()) {
					int cluster = pointEntry.getKey();
					List<Double> point = pointEntry.getValue();
					
					double a = a(point, data.get(cluster));
	//				System.out.println("a for point: " + point + " is " + a + ".");
					double b = b(cluster, point, clusterKeys, data);
					
					double s = (b - a)/FastMath.max(a, b);
	//				System.out.println("s: " + s);
					globalS.addNext(s);
					
				}
				System.out.println(globalS);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets all files from the silhouette directory with filenames
	 * which start with "BuildTMFeatures..." and end with "_4c.csv". 
	 * @return
	 */
	public static File[] getFiles() {
		File directory = new File("silhouette"); 
		File[] files = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File directory, String filename) {
				return filename.startsWith("BuildTMFeatures_AvgBidPropMax10,") && filename.endsWith("_4c.csv");
			}
		});
		return files;
	}
	
	private static double a(List<Double> point, Collection<List<Double>> others) {
		IncrementalMean mean = new IncrementalMean();
		for (List<Double> other : others) {
			mean.addNext(euclideanDistance(point, other));
		}
		mean.removeAverage(1, 0); // 'others' includes 'point'. This undoes the adding of the extra distance of 0.
		
		return mean.average();
	}
	
	private static double b(int pointCluster, List<Double> point, Set<Integer> clusterKeys, Multimap<Integer, List<Double>> data) {
		List<Double> distances = new ArrayList<Double>();
		for (int cluster : clusterKeys) {
			if (cluster == pointCluster)
				continue;
			
			Collection<List<Double>> others = data.get(cluster);
			double distance = bPart(point, others);
			distances.add(distance);
		}
		Collections.sort(distances);
//		System.out.println("distances for " + point + " are " + distances);
		return distances.get(0);
	}
	
	private static double bPart(List<Double> point, Collection<List<Double>> others) {
		IncrementalMean mean = new IncrementalMean();
		for (List<Double> other : others) {
			mean.addNext(euclideanDistance(point, other));
		}
		
		return mean.average();
	}
	
	public static double euclideanDistance(List<Double> point1, List<Double> point2) {
		if (point1.size() != point2.size())
			throw new RuntimeException("2 points do not have the same number of dimensions.");
		
		double sumSqDiff = 0;
		for (int i = 0; i < point1.size(); i++) {
			double diff = point1.get(i) - point2.get(i);
			sumSqDiff += diff * diff;
		}
		
		return FastMath.sqrt(sumSqDiff);
	}
	
	public static HashMap<Integer, List<Double>> centroids(Path inputFile) throws IOException {
		BufferedReader fileReader = Files.newBufferedReader(inputFile, Charset.defaultCharset());
		
		HashMap<Integer, List<Double>> points = new HashMap<>();
		int centroidIndex = 0;
		while (fileReader.ready()) {
			if (fileReader.readLine().startsWith("@centroids")) {
				while (fileReader.ready()) {
					String line = fileReader.readLine();
					if (line.startsWith("@"))
						break;
					
					List<Double> point = new ArrayList<Double>();
					for (String valueStr : line.split(",")) {
						point.add(Double.parseDouble(valueStr));
					}
					points.put(centroidIndex++, point);
				}
			}
				
		}
		return points;
	}
	
	public static ImmutableMultimap<Integer, List<Double>> data(Path inputFile) throws IOException {
		BufferedReader fileReader = Files.newBufferedReader(inputFile, Charset.defaultCharset());
		
		ArrayListMultimap<Integer, List<Double>> points = ArrayListMultimap.create();
		while (fileReader.ready()) {
			if (fileReader.readLine().startsWith("@data")) {
				while (fileReader.ready()) {
					String line = fileReader.readLine();
					if (line.startsWith("@"))
						break;
					
					List<Double> point = new ArrayList<Double>();
					String[] splitLine = line.split(",");
					for (int i = 0; i < splitLine.length - 1; i++) {
						point.add(Double.parseDouble(splitLine[i]));
					}
					int cluster = Integer.parseInt(splitLine[splitLine.length - 1].replace("c", "")); 
					points.put(cluster, ImmutableList.copyOf(point));
				}
			}
		}
		for (int key : points.keySet()) {
			List<List<Double>> pointList = points.get(key);
			Collections.sort(pointList, new Comparator<List<Double>>() {
				@Override
				public int compare(List<Double> o1, List<Double> o2) {
					for (int i = 0; i < o1.size(); i++) {
						int result = Double.compare(o1.get(i), o2.get(i));
						if (result != 0)
							return result;
					}
					return 0;
				}
			});
		}
		return ImmutableMultimap.copyOf(points);
	}
}
