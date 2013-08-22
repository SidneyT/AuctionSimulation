package createUserFeatures;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.RandomizableClusterer;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.PrincipalComponents;

public class Weka {
	
	public static void main(String[] args) {
		String pca_input = "UserFeatures-1-2-3-5-6-7-8.csv";
		String pca_output = "UserFeatures-1-2-3-5-6-7-8_pca.arff";
		pca(pca_input, pca_output);
		
//		String filename = "UserFeatures_pca.arff";
//		RandomizableClusterer clusterer = new SimpleKMeans();
//		cluster(clusterer, filename, clusterer.getClass().getSimpleName() + "_" + filename + ".centers");
		
		System.out.println("Finished.");
	}
	
	public static void pca(String inputFilename, String outputFilename) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFilename), Charset.defaultCharset())) {
			DataSource ds = new DataSource(inputFilename);
			Instances data = ds.getDataSet();
			data.setClassIndex(-1); // no class
			
			PrincipalComponents pc = new PrincipalComponents();
			pc.setInputFormat(data);
			pc.setMaximumAttributeNames(-1);
			pc.setCenterData(false);
			
			for (int i = 0; i < data.numInstances(); i++) {
				pc.input(data.instance(i));
			}
			pc.batchFinished();
			
			bw.append(pc.getOutputFormat().toString());
			
			int outputCount = pc.numPendingOutput();
			for (int i = 0; i < outputCount; i++) {
				bw.append(pc.output().toString());
				bw.newLine();
			}
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper method. Calls cluster(RandomizableClusterer clusterer, String dataFilename, BufferedWriter bw).
	 */
	public static List<Integer> cluster(RandomizableClusterer clusterer, String dataFilename, String clusteringInfoOutputFile) {
		BufferedWriter bw = null;
		List<Integer> results = null;
		try {
			bw = new BufferedWriter(new FileWriter(clusteringInfoOutputFile));
			results = cluster(clusterer, dataFilename, bw);
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * Returns clustering assignments done using the clusterer from data in 
	 * dataFilename, and writes clusterer options used and dataset information
	 * to bw. The order of clustering assignments is the same as the order of
	 * instances in dataFilename
	 * @param clusterer
	 * @param inputDataFilename
	 * @return
	 */
	public static List<Integer> cluster(RandomizableClusterer clusterer, String inputDataFilename, BufferedWriter infoWriter) {
		List<Integer> clusterAssignments = new ArrayList<Integer>();
		try {
			DataSource ds = new DataSource(inputDataFilename);
			Instances data = ds.getDataSet();
//			System.out.println(data.toSummaryString());
			
			// write clusterer and data information
			infoWriter.append(Arrays.toString(clusterer.getOptions()));
			infoWriter.newLine();
			for (int i = 0; i < data.numAttributes(); i++) {
				infoWriter.append(data.attribute(i).toString());
				infoWriter.newLine();
			}
			clusterer.buildClusterer(data);
			ClusterEvaluation ce = new ClusterEvaluation();
			ce.setClusterer(clusterer);
			ce.evaluateClusterer(data);
			double[] clusAssign = ce.getClusterAssignments();
			for (int i = 0; i < clusAssign.length; i++) {
				clusterAssignments.add((int) clusAssign[i]);
			}
			infoWriter.write(ce.clusterResultsToString());
			
			infoWriter.newLine();
			infoWriter.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clusterAssignments;
	}
	
}
