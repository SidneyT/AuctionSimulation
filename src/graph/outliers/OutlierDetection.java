package graph.outliers;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import util.CsvManipulation;
import util.IncrementalMean;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.CoverTree;
import weka.core.neighboursearch.KDTree;
import weka.core.neighboursearch.LinearNNSearch;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.LOF;
import weka.filters.unsupervised.attribute.Remove;

public class OutlierDetection {
	
	public static void main(String[] args) throws Exception {
//		filterVersion();
		
//		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_sellerGraphFeatures.csv";
//		int[] columnIndicies = new int[]{13,3};

//		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_bidderGraphFeatures_few.csv";
//		int[] columnIndicies = new int[]{19,2};
//
//		int minBound = 20;
//		int maxBound = 100;
//		int boundInc = 5;
//		multiBounds(filePath, minBound, maxBound, boundInc, columnIndicies);
		
//		multiIndices();
//		specificIndiciesBidder();
//		specificIndiciesSeller();

//		allFeaturesAtOnceBidder();
//		allFeaturesAtOnceSeller();
		
//		tmBidder();
//		tmSeller();
//		tmSellerDeduplicated();
		tmBidderDeduplicated();
	}
	
	public static void allFeaturesAtOnceBidder() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		
		for (String name : new String[]{
				"repFraud", 
				"hybridNormalEE",
				"hybridBothVGS"
				}) {
			for (int i = 0; i < 20; i++) {
				String filePath = directory + "syn_" + name + "_20k_" + i + "_bidderGraphFeatures.csv";
				int[] columnsWanted = new int[]{6,26,3,24,11,29};
				String outputFilepath = directory + "allLof/allLof_" + name + "_bidder_" + i + ".csv";
				lof(filePath, Collections.singletonList(new Triplet(8 * 50, columnsWanted)), outputFilepath);
			}
		}
	}
	public static void allFeaturesAtOnceSeller() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		
		for (String name : new String[]{
				"repFraud", "hybridNormalEE", 
				"hybridBothVGS"}) {
			for (int i = 0; i < 20; i++) {
				String filePath = directory + "syn_" + name + "_20k_" + i + "_sellerGraphFeatures.csv";
				int[] columnsWanted = new int[]{3,28,2,29,14,15,20};
				String outputFilepath = directory + "allLof/allLof_" + name + "_seller_" + i + ".csv";
				lof(filePath, Collections.singletonList(new Triplet(8 * 50, columnsWanted)), outputFilepath);
			}
		}
	}
	
	private static void lof(String inputFilePath, List<Triplet> featurePairs, String outputFilePath) {
		StringBuffer headingRow = new StringBuffer();
		headingRow.append("id,userType");
		for (Triplet pair : featurePairs) {
			headingRow.append("," + pair);
		}
			
		List<Map<Integer, String>> thing = new ArrayList<>();
		List<HashMap<Integer, String>> maps = new ArrayList<>();
		HashMap<Integer, String> userTypes = new HashMap<>();
		for (Triplet pair : featurePairs) {
			try {
				ResultThing resultThing = pickAttributesThenLOF(inputFilePath, pair.radius, pair.radius, pair.columns);
				
				userTypes.putAll(resultThing.userTypes);
				HashMap<Integer, String> outlierScores = new HashMap<>();
				for (Integer key : resultThing.outlierScores.keySet()) {
					outlierScores.put(key, resultThing.outlierScores.get(key) + "");
				}
				maps.add(outlierScores);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		thing.add(userTypes);
		thing.addAll(maps);
		
		CsvManipulation.writeMaps(outputFilePath, thing, headingRow.toString());
	}
	
	public static final List<Triplet> tmBidderFeatureTriplets = Arrays.asList(new Triplet[]{new Triplet(3, 6, 8 * 50), new Triplet(2, 5, 8 * 50), new Triplet(4, 5, 6 * 50), new Triplet(4, 7, 8 * 50)}); 
	public static final List<Triplet> tmSellerFeatureTriplets = Arrays.asList(new Triplet[]{new Triplet(3, 7, 8 * 50), new Triplet(4, 5, 8 * 50), new Triplet(6, 5, 4 * 50), new Triplet(6, 8, 4 * 50)}); 
	
	public static void tmBidder() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		String filePath = directory + "trademe_jit_bidderGraphFeatures.csv";
		String outputFilepath = directory + "chosenLof/chosenLof_trademe_jit_bidder.csv";
		lof(filePath, tmBidderFeatureTriplets, outputFilepath);
	}

	public static void tmBidderDeduplicated() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (Triplet triplet : tmBidderFeatureTriplets) {
			int c1 = triplet.columns[0]; 
			int c2 = triplet.columns[1]; 
			String filePath = directory + "trademe_bidderGraphFeatures_" + c1 + "," + c2 + "_jit_dedupe.csv";
			String outputFilepath = directory + "chosenLof/chosenLof_trademe_bidder_jit_dedupe_" + c1 + "," + c2 + ".csv";
			
			lof(filePath, Collections.singletonList(new Triplet(1, 2, triplet.radius)), outputFilepath);
		}
	}

	public static void tmSeller() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		String filePath = directory + "trademe_jit_sellerGraphFeatures.csv";
		String outputFilepath = directory + "chosenLof/chosenLof_trademe_jit_seller.csv";
		lof(filePath, tmSellerFeatureTriplets, outputFilepath);
	}

	public static void tmSellerDeduplicated() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (Triplet triplet : tmSellerFeatureTriplets) {
			int c1 = triplet.columns[0]; 
			int c2 = triplet.columns[1]; 
			String filePath = directory + "trademe_sellerGraphFeatures_" + c1 + "," + c2 + "_jit_dedupe.csv";
			String outputFilepath = directory + "chosenLof/chosenLof_trademe_seller_jit_dedupe_" + c1 + "," + c2 + ".csv";
			
			lof(filePath, Collections.singletonList(new Triplet(1, 2, triplet.radius)), outputFilepath);
		}
	}

	public static void specificIndiciesBidder() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
				"repFraud",
				"hybridNormalEE", 
				"hybridBothVGS"
				}) {
			for (int i = 0; i < 20; i++) {
				String filePath = directory + "syn_" + name + "_20k_" + i + "_bidderGraphFeatures.csv";
				final List<Triplet> featurePairs = Arrays.asList(new Triplet[]{new Triplet(6, 26, 8 * 50), new Triplet(3, 24, 8 * 50), new Triplet(11, 24, 6 * 50), new Triplet(11, 29, 8 * 50)});
				String outputFilepath = directory + "chosenLof/chosenLof_" + name + "_bidder_" + i + ".csv";
				lof(filePath, featurePairs, outputFilepath);
			}
		}
	}
	public static void specificIndiciesSeller() throws Exception {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
				"repFraud", "hybridNormalEE", 
				"hybridBothVGS"}) {
			for (int i = 0; i < 20; i++) {
				String filePath = directory + "syn_" + name + "_20k_" + i + "_sellerGraphFeatures.csv";
				final List<Triplet> featurePairs = Arrays.asList(new Triplet[]{new Triplet(3, 28, 8 * 50), new Triplet(2, 29, 8 * 50), new Triplet(14, 15, 4 * 50), new Triplet(20, 29, 4 * 50)});
				String outputFilepath = directory + "chosenLof/chosenLof_" + name + "_seller_" + i + ".csv";
				lof(filePath, featurePairs, outputFilepath);
			}
		}
	}

	public static class Triplet {
		public final int[] columns;
		public final int radius;
		Triplet(int column1, int column2, int radius) {
			this.columns = new int[]{column1, column2};
			this.radius = radius;
		}
		
		public Triplet(int radius, int... columns) {
			this.radius = radius;
			this.columns = columns;
		}
		
		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < columns.length; i++) {
				buffer.append(columns[i] + "-");
			}
			buffer.append(radius + "");
			return buffer.toString();
		}
	}
		
	/**
	 * Calculate LOF for multiple pairs of graph features and write them to file.
	 */
	public static void multiIndices() throws Exception {
//		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_bidderGraphFeatures_few.csv";
//		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_sellerGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_hybridNormalEE_20k_0_bidderGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_hybridNormalEE_20k_0_sellerGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_repFraud_20k_0_bidderGraphFeatures.csv";
		String filePath = "graphFeatures_processed/no_jitter/syn_repFraud_20k_0_sellerGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_hybridBothVGS_20k_0_bidderGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_hybridBothVGS_20k_0_sellerGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_simplews_20k_0_bidderGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/no_jitter/syn_simplews_20k_0_sellerGraphFeatures.csv";
//		String filePath = "graphFeatures_processed/syn_hybridNormalEE_20k_0_sellerGraphFeatures.csv";
//		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_small_0_sellerGraphFeatures.csv";
		
		System.out.println("doing: " + filePath);
		
		int minBound = 100;
		int maxBound = 401;
		int boundInc = 50;
		
		int maxAttrs = 30; // bidder
		for (int c1 = 8; c1 < maxAttrs; c1++) {
			for (int c2 = c1 + 1; c2 < maxAttrs; c2++) {
//				if (c1 <= 9 && c2 <= 28 || c2 != 28) // skip the first 2 columns
//					continue;
				try {
					System.out.println("trying: " + c1 + ", " + c2);
					multiBounds(filePath, minBound, maxBound, boundInc, new int[]{c1, c2});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Finds the LOF for two columns (columnIndicies) in the given file (filePath).
	 * The parameter for LOF is given by minBound, maxBound and boundInc. 
	 * @param filePath
	 * @param minBound
	 * @param maxBound
	 * @param boundInc
	 * @param columnIndicies
	 * @throws Exception
	 */
	public static void multiBounds(String filePath, int minBound, int maxBound, int boundInc, int[] columnIndicies) throws Exception {
		File file = new File(filePath);

		String filename = file.getParent() + "/multi/lof_" + columnIndicies[0] + "," + columnIndicies[1] + "_" + file.getName();
		
		if (new File(filename).exists())
			return;
		
		HashMap<Integer, String> userTypes = null;
		ArrayListMultimap<Integer, Double> allOutlierScores = ArrayListMultimap.create();
		String headings = "id,userType";
		for (int bounds = minBound; bounds < maxBound; bounds += boundInc) {
			headings += ",b" + bounds;
			ResultThing results = pickAttributesThenLOF(file.getAbsolutePath(), bounds, bounds, columnIndicies);
			userTypes = results.userTypes;
			for (Integer user : results.outlierScores.keySet()) {
				allOutlierScores.put(user, results.outlierScores.get(user));
			}
		}

		BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename), Charset.forName("UTF-8"));
		bw.write(headings);
		bw.newLine();
		for (Integer id : allOutlierScores.keySet()) {
			bw.write(id.toString());
			bw.append(",").write(userTypes.get(id));
			List<Double> outlierScores = allOutlierScores.get(id);
			for (int i = 0; i < outlierScores.size(); i++) {
				double score = outlierScores.get(i);
				bw.write(",");
				bw.write(score + "");
			}
			bw.newLine();
		}
		bw.flush();
			
//		else
//			pickAttributesThenLOF(file.getAbsolutePath(), file.getParent() + "/lof_7,8_" + file.getName(), new int[]{7,8});
	}
	
	static class ResultThing {
		HashMap<Integer, String> userTypes = new HashMap<>();
		HashMap<Integer, Double> outlierScores = new HashMap<>();
	} 
	
	/**
	 * Runs LOF over the wantedColumns in the file. lowerBound is MinPtsLB and uppperBound is MinPtsUP.
	 * @param dataSourcePath
	 * @param lowerBound
	 * @param upperBound
	 * @param wantedColumns
	 * @return
	 * @throws Exception
	 */
	private static ResultThing pickAttributesThenLOF(String dataSourcePath, int lowerBound, int upperBound, int[] wantedColumns) throws Exception {
		DataSource dataSource = new DataSource(dataSourcePath);
		Instances instances = dataSource.getDataSet();
		
		Remove removeAllOthers = new Remove();
		removeAllOthers.setAttributeIndicesArray(wantedColumns);
		removeAllOthers.setInvertSelection(true);
		removeAllOthers.setInputFormat(instances);
		Instances onlyWanted = Filter.useFilter(instances, removeAllOthers);

//		KDTree nnSearch = new KDTree();
//		kdTree.setDistanceFunction(DistanceFunction);
		LinearNNSearch nnSearch = new LinearNNSearch();

		LOF los = new LOF();
		los.setNNSearch(nnSearch);
		los.setMinPointsLowerBound(lowerBound + "");
		los.setMinPointsUpperBound(upperBound + "");
		los.setInputFormat(onlyWanted);
		los.setNumExecutionSlots("1");
//		los.set
		
		long t1 = System.nanoTime();
		Instances filteredInstances = Filter.useFilter(onlyWanted, los);

		Attribute userTypeAttr = instances.attribute(1);

		ResultThing result = new ResultThing();
		
		for (int i = 0; i < filteredInstances.size(); i++) {
			double id = instances.get(i).value(0);
			String userType = userTypeAttr.value((int) instances.get(i).value(1));
			result.userTypes.put((int) id, userType);
			
			double outlierScore = filteredInstances.get(i).value(filteredInstances.numAttributes() - 1);
			result.outlierScores.put((int) id, outlierScore);
		}
		long t2 = System.nanoTime() - t1;
		System.out.println(dataSourcePath + ", " + Arrays.toString(wantedColumns) + ": " + t2 / 1000000);
		
		return result;
	}
	
	public static void multi() throws Exception {
		int[] bidderAttrA2B = new int[]{19,2};
		String path = "F:/workstuff2011/AuctionSimulation/lof_features_fixed";
		File[] files = new File(path).listFiles();
		for (File file : files) {
			if (!file.getName().startsWith("syn_repFraud_") || file.getName().endsWith("Copy.csv"))
				continue;
			if (file.getName().contains("bidder")) {
				pickAttributesThenLOF(file.getAbsolutePath(), file.getParent() + "/lof_19,2_" + file.getName(), 20, 20, bidderAttrA2B);
			}
//			else
//				pickAttributesThenLOF(file.getAbsolutePath(), file.getParent() + "/lof_7,8_" + file.getName(), new int[]{7,8});
			System.out.println("done " + file);
			break;
		}
		
	}
	
	private static void pickAttributesThenLOF(String dataSourcePath, String outputPath, int lowerBound, int upperBound, int[] wantedColumns) throws Exception {
		DataSource dataSource = new DataSource(dataSourcePath);
		Instances instances = dataSource.getDataSet();
		
		Remove removeAllOthers = new Remove();
		removeAllOthers.setAttributeIndicesArray(wantedColumns);
		removeAllOthers.setInvertSelection(true);
		removeAllOthers.setInputFormat(instances);
		Instances only2Columns = Filter.useFilter(instances, removeAllOthers);

		
//		CoverTree nnSearch = new CoverTree();
		KDTree nnSearch = new KDTree();
		nnSearch.setInstances(only2Columns);
//		kdTree.setDistanceFunction(DistanceFunction);
		

		LOF los = new LOF();
		los.setNNSearch(nnSearch);
		los.setMinPointsLowerBound(lowerBound + "");
		los.setMinPointsUpperBound(upperBound + "");
		los.setInputFormat(only2Columns);
		los.setNumExecutionSlots("1");
//		los.set
		
		BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
		long t1 = System.nanoTime();
		Instances filteredInstances = Filter.useFilter(only2Columns, los);

		Attribute userTypeAttr = instances.attribute(1);

		
		
		for (int i = 0; i < filteredInstances.size(); i++) {
			double id = instances.get(i).value(0);
			String userType = userTypeAttr.value((int) instances.get(i).value(1));
//			System.out.println(id + "," + filteredInstances.get(i));
			bw.append(id + "," + userType + ",").append(filteredInstances.get(i).value(filteredInstances.numAttributes() - 1) + "");
			bw.newLine();
		}
		bw.flush();
		long t2 = System.nanoTime() - t1;
		System.out.println(t2 / 1000000);
	}

	static void filterVersion() throws Exception {
		DataSource dataSource = new DataSource("bidder_uniquevstotal_testSmallLog_jitter.arff");
		Instances instances = dataSource.getDataSet();
		
		Remove removeId = new Remove();
		removeId.setAttributeIndicesArray(new int[]{0});
		removeId.setInputFormat(instances);
		Instances noIdInstances = Filter.useFilter(instances, removeId);

		
//		CoverTree nnSearch = new CoverTree();
		KDTree nnSearch = new KDTree();
		nnSearch.setInstances(noIdInstances);
//		kdTree.setDistanceFunction(DistanceFunction);
		

		LOF los = new LOF();
		los.setNNSearch(nnSearch);
		los.setMinPointsLowerBound("20");
		los.setMinPointsUpperBound("20");
		los.setInputFormat(noIdInstances);
		los.setNumExecutionSlots("1");
//		los.set
		
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("lof_test2.csv"), Charset.defaultCharset());
		long t1 = System.nanoTime();
		Instances filteredInstances = Filter.useFilter(noIdInstances, los);
		for (int i = 0; i < filteredInstances.size(); i++) {
			double id = instances.get(i).value(0);
//			System.out.println(id + "," + filteredInstances.get(i));
			bw.append(id + ",").append(filteredInstances.get(i) + "");
			bw.newLine();
		}
		bw.flush();
		long t2 = System.nanoTime() - t1;
		System.out.println(t2 / 1000000);
	}
	
}
