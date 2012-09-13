package simulator.modelEvaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.ClusterAnalysis;
import createUserFeatures.Feature;
import createUserFeatures.Features;

import util.HungarianAlgorithm;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemovePercentage;

/**
 * For finding the stability of a given clustering solution of the TradeMe dataset.
 * I.e., find the best number of clusters to use to cluster the TradeMe data 
 */
public class TMStability implements Runnable{
	
	private static final Logger logger = Logger.getLogger(TMStability.class); 

	private static BufferedWriter bw_full, bw_short;

	public static void main(String[] args) throws Exception {
		
		List<Feature> featuresToPrint = Arrays.<Feature>asList(Features.AvgBidPropMax10, 
				Features.PropWin5, 
				Features.BidsPerAuc6Ln, 
				Features.AvgBidProp11);
		
		System.out.println(featuresToPrint);
		System.out.println(Features.labels(featuresToPrint));

		ExecutorService es = Executors.newFixedThreadPool(3);
		for (int sampleCount = 0; sampleCount < 10; sampleCount++) {
			List<Callable<Object>> tasks = new ArrayList<>(); 
			for (int numberOfClusters = 2; numberOfClusters <= 12; numberOfClusters++) {
				tasks.add(Executors.callable(new TMStability(numberOfClusters)));
			}
			es.invokeAll(tasks);
		}

		es.shutdown();
		
		flushBws();
		System.out.println("Finished");
	}
	
	private static void makeBws() throws IOException {
		bw_full = Files.newBufferedWriter(new File("TMStability_full_results").toPath(), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		bw_short = Files.newBufferedWriter(new File("TMStability_short_results").toPath(), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	private static void flushBws() throws IOException {
		bw_full.flush();
		bw_short.flush();
	}

	public synchronized static void writeResults(BufferedWriter bw_full, BufferedWriter bw_short, int numberOfClusters, List<Feature> featuresForClustering, double[][] correctMatrix, int[][] assignment, int correct, int total) throws IOException {
		long currentTime = System.currentTimeMillis();
		write(bw_full, "Clusters:" + numberOfClusters);
		write(bw_full, "Timestamp:" + new Date(currentTime).toString());
		write(bw_full, "Clustering Features:" + Features.labels(featuresForClustering));
		write(bw_full, AccuracyEvaluation.multiArrayString(correctMatrix).toString());
		write(bw_full, AccuracyEvaluation.multiArrayString(assignment).toString());
		write(bw_full, correct + ":" + total);
		write(bw_short, numberOfClusters + "," + featuresForClustering + "," + currentTime + "," + correct);
	}

	private static void write(BufferedWriter bw, String s) throws IOException {
		bw.append(s);
		bw.newLine();
		bw.flush();
	}
	
	private final int numberOfClusters;
	
	public TMStability(int numberOfClusters) {
		this.numberOfClusters = numberOfClusters;
	}
	
	public void run() {
		try {
//			String featuresForClustering = "-1ln-2ln-3ln-10-11-12";
//			String featuresToPrint = "-1ln-2ln-3ln-10-11-12";
			List<Feature> featuresToPrint = Arrays.<Feature>asList(Features.AvgBidPropMax10, 
					Features.PropWin5, 
					Features.BidsPerAuc6Ln, 
					Features.AvgBidProp11);
			List<Feature> featuresForClustering = featuresToPrint;
			
			String tmClusteredFile = "BuildTMFeatures_SimpleKMeans" + featuresForClustering + "_" + numberOfClusters + "clusters.csv";
			Random random = new Random();
			tmClusteredFile = ClusterAnalysis.clusterToFile(new BuildTMFeatures(), random.nextInt(), featuresToPrint, featuresForClustering, numberOfClusters, "");
//			logger.warn("TM clustered instances in file: " + tmClusteredFile + ".");
			
			// split the TM data into training and testing sets
			Instances[] isArray = splitData(tmClusteredFile);
			Instances training = isArray[0];
			training.setClassIndex(training.numAttributes() - 1);
			Instances testing = isArray[1];
			testing.setClassIndex(testing.numAttributes() - 1);
	
			// build the classifier using the training set
			Classifier classifier = AccuracyEvaluation.buildIBk(training, 3);
			
			// build the correct matrix with the classifier and the testing set
			double[][] correctMatrix = AccuracyEvaluation.calculateCorrectMatrix(numberOfClusters, classifier, testing);
			
			// run the hungarian algorithm
			int[][] assignment = HungarianAlgorithm.hgAlgorithm(correctMatrix, "max");
//			for (int i = 0; i < assignment.length; i++) {
//				int classification = assignment[i][0];
//				int cluster = assignment[i][1];
////			logger.warn("class " + classification + " should be paired with cluster " + cluster + " for highest accuracy.");
//			}
//			System.out.println("assignment:" + Evaluation.multiArrayString(assignment).toString());
			
			// calculate number correct
			int correct = 0;
			for (int i = 0; i < assignment.length; i++) {
				correct += correctMatrix[assignment[i][0]][assignment[i][1]];
			}
//			System.out.println("evaluation:" + Evaluation.multiArrayString(correctMatrix).toString());
			
	//		logger.warn(Evaluate.multiArrayString(correctMatrix));
			logger.warn("Number of instances that had matching cluster and class for " + numberOfClusters + " clusters with features " + featuresForClustering + ": " + correct + " out of " + training.numInstances() + ".");

			writeResults(bw_full, bw_short, numberOfClusters, featuresForClustering, correctMatrix, assignment, correct, training.numInstances());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Splits instances in the input file into 2 complementary sets of equal size.
	 * @param inputFilename
	 * @return
	 * @throws Exception 
	 */
	public static Instances[] splitData(String inputFilename) throws Exception {
		DataSource ds = new DataSource(inputFilename);
		
		Instances is = ds.getDataSet();
		is.setClassIndex(is.numAttributes() - 1);
		
//		is.randomize(new Random(59543875318231387L));
		is.randomize(new Random());
		
		RemovePercentage rp = new RemovePercentage();
		rp.setPercentage(50);
		
		rp.setInputFormat(is);
		Instances newIs1 = Filter.useFilter(is, rp);
//		System.out.println(newIs1);
		
		rp.setInvertSelection(true);
		rp.setInputFormat(is);
		Instances newIs2 = Filter.useFilter(is, rp);
//		System.out.println(newIs2);
		
		Instances[] isArray = {newIs1, newIs2};
		
		return isArray;
	}
}
