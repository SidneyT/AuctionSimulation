package graph.outliers.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import util.CsvManipulation;
import util.IncrementalMean;
import util.IncrementalSD;

/**
 * Uses output from NeuralNetwork class.
 * Needs csv files in 2 columns; first column is the ideal value (actual class), the second the output from the neural network.
 * 
 * Has methods to find the ROC or AUC.
 */
public class ROC_builder {
	public static void main(String[] args) throws IOException {
//		for (String name : new String[]{"repFraud", "hybridBothVGS", "hybridNormalEE"}) {
//			run("F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\multi\\propMatrixTuning_noNormalise\\finalScores_" + name + "_0.32-0.4_2.2.csv", name);
//			run_NetProbe("F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\multi\\propMatrixTuning_Netprobe\\finalScores_" + name + "_0.04-0.12_NetProbe.csv", name);
//		}
		
//		selectedTableValues();
//		multipleEval();
//		multipleEvalEdgesRemoved();
		allTPRs2();
//		allTPRsEdgesRemoved();
//		allFPRsEdgesRemoved();
	}

	/**
	 * For printing out specific TPR/FPR pairs, and AUC
	 */
	private static void selectedTableValues() {
		String directory = "F:/workstuff2011/AuctionSimulation/graphFeatures_processed/no_jitter/";
		for (String name : new String[]{
				"repFraud", 
				"hybridBothVGS", 
				"hybridNormalEE",
				}) {
			
			IncrementalSD sd001 = new IncrementalSD();
			IncrementalSD sd01 = new IncrementalSD();
			IncrementalSD sd05 = new IncrementalSD();
			IncrementalSD auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				
				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_" + i + ".csv", 3);
//				System.out.println(TPR(fpr_tpr, 0.005));
				sd001.add(TPR(fpr_tpr, 0.005));
				sd01.add(TPR(fpr_tpr, 0.01));
				sd05.add(TPR(fpr_tpr, 0.05));
				auc.add(AUC(fpr_tpr));
			}
			System.out.println(name + "," + sd001.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());

			sd001 = new IncrementalSD();
			sd01 = new IncrementalSD();
			sd05 = new IncrementalSD();
			auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores/finalScores_" + name + "_NetProbe_" + i + ".csv", directory + "ROC/ROC_" + name + "_NetProbe_" + i + ".csv");
				sd001.add(TPR(fpr_tpr, 0.005));
				sd01.add(TPR(fpr_tpr, 0.01));
				sd05.add(TPR(fpr_tpr, 0.05));
				auc.add(AUC(fpr_tpr));
			}
			System.out.println(name + "_NetProbe," + sd001.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());

			sd001 = new IncrementalSD();
			sd01 = new IncrementalSD();
			sd05 = new IncrementalSD();
			auc = new IncrementalSD();
			for (int i = 0; i < 20; i++) {
				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_UP_" + i + ".csv", 2);
				sd001.add(TPR(fpr_tpr, 0.005));
				sd01.add(TPR(fpr_tpr, 0.01));
				sd05.add(TPR(fpr_tpr, 0.05));
				auc.add(AUC(fpr_tpr));
			}
			System.out.println(name + "_UP," + sd001.toStringShort() + "," + sd01.toStringShort() + "," + sd05.toStringShort() + "," + auc.toStringShort());
			
		}
	}
	
	private static void multipleEval() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
//				"repFraud", 
//				"hybridBothVGS", 
				"hybridNormalEE",
				}) {
			StringBuffer sb1 = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();
			StringBuffer sb3 = new StringBuffer();
			sb1.append(name + ",up");
			sb2.append(name + ",ours");
			sb3.append(name + ",np");
			for (int i = 0; i < 1; i++) {
				ArrayList<Pair<Double, Double>> fpr_tpr_up = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_UP_" + i + ".csv", 2);
				sb1.append("," + AUC(fpr_tpr_up));
				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_" + i + ".csv", 3);
				sb2.append("," + AUC(fpr_tpr));
				ArrayList<Pair<Double, Double>> fpr_tpr_np = run_NetProbe(directory + "finalScores/finalScores_" + name + "_NetProbe_" + i + ".csv", directory + "ROC/ROC_" + name + "_NetProbe_" + i + ".csv");
				sb3.append("," + AUC(fpr_tpr_np));

				ArrayList<Triplet> npVours = TPRvsTPR(fpr_tpr_np, fpr_tpr);
				writeROC2(directory + "TPRvTPR/npVours_" + name + "_" + i + ".csv", npVours);
				ArrayList<Triplet> upVours = TPRvsTPR(fpr_tpr_up, fpr_tpr);
				writeROC2(directory + "TPRvTPR/upVours_" + name + "_" + i + ".csv", upVours);
			}
			System.out.println(sb1.toString());
			System.out.println(sb2.toString());
			System.out.println(sb3.toString());
		}
	}

	private static void multipleEvalEdgesRemoved() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
				"repFraud", 
				"hybridBothVGS", 
				"hybridNormalEE",
				}) {
				for (int j = 0; j < 51; j ++) {
					double k = j * 0.02;
					System.out.print(name + "," + k);
					for (int i = 0; i < 19; i++) {
//						for (int l = 0; l < 10; l++) {
//						ArrayList<Pair<Double, Double>> FPR_TPR = run(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
						ArrayList<Pair<Double, Double>> FPR_TPR = run(directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_ffEdgesRemoved/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
						System.out.print("," + AUC(FPR_TPR));
//							System.out.print("," + TPR(FPR_TPR, 0.005));
//						}
					}
					System.out.println();
				}
				for (int j = 0; j < 51; j ++) {
					double k = j * 0.02;
					System.out.print(name + "_np," + k);
					for (int i = 0; i < 19; i++) {
//					for (int l = 0; l < 10; l++) {
//						ArrayList<Pair<Double, Double>> FPR_TPR_np = run_NetProbe(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
						ArrayList<Pair<Double, Double>> FPR_TPR_np = run_NetProbe(directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_ffEdgesRemoved/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
						System.out.print("," + AUC(FPR_TPR_np));
//						System.out.print("," + TPR(FPR_TPR_np, 0.005));
//					}
					}
					System.out.println();
				}
		}
	}
	
	private static void allTPRs() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
			"repFraud", 
			"hybridBothVGS", 
			"hybridNormalEE"
			}) {
			for (int i = 0; i < 20; i++) {
				ArrayList<Pair<Double, Double>> fpr_tpr_up = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_UP_" + i + ".csv", 2);
				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_" + i + ".csv", 3);
				ArrayList<Pair<Double, Double>> fpr_tpr_np = run_NetProbe(directory + "finalScores/finalScores_" + name + "_NetProbe_" + i + ".csv", directory + "ROC/ROC_" + name + "_NetProbe_" + i + ".csv");
				
				double FPR = 0.002;
				System.out.println(name + "_" + i + "," + TPR(fpr_tpr_up, FPR) + "," + TPR(fpr_tpr, FPR) + "," + TPR(fpr_tpr_np, FPR));
			}
		}
	}
	private static void allTPRs2() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
//			"repFraud", 
//			"hybridBothVGS", 
			"hybridNormalEE",
			}) {
			
			System.out.println(name);
			
			Multimap<Double, Double> TPRs = ArrayListMultimap.create(); 
			for (int i = 0; i < 20; i++) {
//				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_allFeatures/finalScores_" + name + "_" + i + ".csv", directory + "ROC_allFeatures/ROC_" + name + "_" + i + ".csv", 2);
//				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores/finalScores_" + name + "_" + i + ".csv", directory + "ROC/ROC_" + name + "_UP_" + i + ".csv", 2);
//				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "mlExtraEval/finalScores_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROC_" + name + "_UP_" + i + ".csv", 2);
//				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "mlExtraEval/finalScores_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROC_" + name + "_" + i + ".csv", 3);
				ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "mlExtraEval/finalScoresW_" + name + "_" + i + ".csv", directory + "mlExtraEvalROC/ROCW_" + name + "_" + i + ".csv", 3);
//				ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores/finalScores_" + name + "_NetProbe_" + i + ".csv", directory + "ROC/ROC_" + name + "_NetProbe_" + i + ".csv");
				
				System.out.println(i + "," + AUC(fpr_tpr));
				
				// for hybridNormalEE
//				if (name.equals("hybridNormalEE") && Arrays.asList(new Integer[]{8,11,13,15,16,17}).contains(i))
//					continue;
				if (name.equals("hybridNormalEE") && !Arrays.asList(new Integer[]{1,2,3,4,5,6}).contains(i))
					continue;
				
				for (int j = 0; j < 400; j++) {
					double FPR = j * 0.0001;
					TPRs.put(FPR, TPR(fpr_tpr, FPR));
				}
				for (int j = 8; j <= 200; j++) {
					double FPR = j * 0.005;
					TPRs.put(FPR, TPR(fpr_tpr, FPR));
				}
			}
			
			for (Double fpr : TPRs.keySet()) {
				IncrementalMean mean = new IncrementalMean();
				mean.add(TPRs.get(fpr));
				System.out.println(fpr + "," + mean.average());
			}
		}
	}

	private static void allTPRsEdgesRemoved() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
//			"repFraud", 
			"hybridBothVGS", 
//			"hybridNormalEE",
			}) {
			
			System.out.println(name);
			
//			Path outputFile = Paths.get(directory + name + "_ffEdgeSwap.csv");
//			Path outputFile = Paths.get(directory + name + "_ffEdgeSwap_netProbe.csv");
//			Path outputFile = Paths.get(directory + name + "_fnEdgeSwap.csv");
//			Path outputFile = Paths.get(directory + name + "_fnEdgeSwap_netProbe.csv");
			
//			Path outputFile = Paths.get(directory + name + "_fScoreSwap.csv");
//			Path outputFile = Paths.get(directory + name + "_fScoreSwap_netProbe.csv");
//			Path outputFile = Paths.get(directory + name + "_nScoreSwap.csv");
			Path outputFile = Paths.get(directory + name + "_nScoreSwap_netProbe.csv");
			
//			for (int l = 0; l < 1; l++) {
			for (int l = 0; l < 51; l++) {
				double k = l * 0.02;

				Multimap<Double, Double> TPRs = ArrayListMultimap.create(); // FPR, TPRs
				for (int i = 0; i < 20; i++) { // for each of 20 datasets
	
					// for edge removal files
//					ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_ffEdgesRemoved/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
//					ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores_ffEdgesRemoved/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_ffEdgesRemoved/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
//					ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
//					ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
					
					// for score swap files
//					ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_fScoresSwapped/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_fScoresSwapped/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
//					ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores_fScoresSwapped/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_fScoresSwapped/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
//					ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_nScoresSwapped/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_nScoresSwapped/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
					ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores_nScoresSwapped/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_nScoresSwapped/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
					
//					System.out.println(i + "," + AUC(fpr_tpr));
					
					// for hybridNormalEE
					if (name.equals("hybridNormalEE") && Arrays.asList(new Integer[]{8,11,13,15,16,17}).contains(i))
						continue;
					
					for (int j = 0; j < 400; j++) {
						double FPR = j * 0.0001;
						TPRs.put(FPR, TPR(fpr_tpr, FPR));
					}
					for (int j = 8; j <= 200; j++) {
						double FPR = j * 0.005;
						TPRs.put(FPR, TPR(fpr_tpr, FPR));
					}
				}
				
				HashMap<Double, Double> tprMeans = new HashMap<>();
				for (Double fpr : TPRs.keySet()) {
					IncrementalMean mean = new IncrementalMean();
					mean.add(TPRs.get(fpr));
//					System.out.println(fpr + "," + mean.average());
					tprMeans.put(fpr, mean.average());
				}
				
				if (l == 0) {
					ArrayList<Double> fprSet = new ArrayList<>(tprMeans.keySet());
					Collections.sort(fprSet);
					CsvManipulation.addColumn(outputFile, outputFile, "FPR", fprSet);
				}
				
				ArrayList<Double> tprMeansList = new ArrayList<Double>(tprMeans.values());
				Collections.sort(tprMeansList);
				CsvManipulation.addColumn(outputFile, outputFile, k + "", tprMeansList);
			}
		}
	}
	private static void allFPRsEdgesRemoved() {
		String directory = "F:\\workstuff2011\\AuctionSimulation\\graphFeatures_processed\\no_jitter\\";
		for (String name : new String[]{
			"repFraud", 
			"hybridBothVGS", 
			"hybridNormalEE",
			}) {
			
			System.out.println(name);
			
//			Path outputFile = Paths.get(directory + name + "_fnEdgeSwap_FPR.csv");
			Path outputFile = Paths.get(directory + name + "_fnEdgeSwap_FPR_netProbe.csv");
			
//			for (int l = 0; l < 1; l++) {
			for (int l = 0; l < 51; l++) {
				double k = l * 0.02;

				Multimap<Double, Double> FPRs = ArrayListMultimap.create(); // FPR, TPRs
				for (int i = 0; i < 20; i++) { // for each of 20 datasets
	
					// for edge removal files
//					ArrayList<Pair<Double, Double>> fpr_tpr = run(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_" + k + "_" + i + ".csv", 3);
					ArrayList<Pair<Double, Double>> fpr_tpr = run_NetProbe(directory + "finalScores_fnEdgesRemoved/finalScores_" + name + "_NetProbe_" + k + "_" + i + ".csv", directory + "ROC_fnEdgesRemoved/ROC_" + name + "_NetProbe_" + k + "_" + i + ".csv");
					
//					System.out.println(i + "," + AUC(fpr_tpr));
					
					// for hybridNormalEE
					if (name.equals("hybridNormalEE") && Arrays.asList(new Integer[]{8,11,13,15,16,17}).contains(i))
						continue;
					
					for (int j = 0; j < 14; j++) {
						double TPR = j * 0.05;
						double FPR = FPR(fpr_tpr, TPR);
						FPRs.put(TPR, FPR);
					}
					for (int j = 0; j <= 30; j++) {
						double TPR = j * 0.01 + 0.7;
						double FPR = FPR(fpr_tpr, TPR);
						FPRs.put(TPR, FPR);
					}
				}
				
				HashMap<Double, Double> fprMeans = new HashMap<>();
				for (Double fpr : FPRs.keySet()) {
					IncrementalMean mean = new IncrementalMean();
					mean.add(FPRs.get(fpr));
//					System.out.println(fpr + "," + mean.average());
					fprMeans.put(fpr, mean.average());
				}
				
				if (l == 0) {
					ArrayList<Double> fprSet = new ArrayList<>(fprMeans.keySet());
					Collections.sort(fprSet);
					CsvManipulation.addColumn(outputFile, outputFile, "TPR", fprSet);
				}
				
				ArrayList<Double> tprMeansList = new ArrayList<Double>(fprMeans.values());
				Collections.sort(tprMeansList);
				CsvManipulation.addColumn(outputFile, outputFile, k + "", tprMeansList);
			}
		}
	}

	public static double TPR(ArrayList<Pair<Double, Double>> FPR_TPR, double FPR) {
		for (Pair<Double, Double> pair : FPR_TPR) {
//			System.out.println(pair.getKey() + "," + pair.getValue());
			if (pair.getKey() > FPR)
				return pair.getValue();
		}
		return 1;
	}
	private static double FPR(ArrayList<Pair<Double, Double>> FPR_TPR, double TPR) {
		for (Pair<Double, Double> pair : FPR_TPR) {
//			System.out.println(pair.getKey() + "," + pair.getValue());
			if (pair.getValue() > TPR)
				return pair.getKey();
		}
		return 1;
	}

	private static ArrayList<Triplet> TPRvsTPR(ArrayList<Pair<Double, Double>> FPR_TPR1, ArrayList<Pair<Double, Double>> FPR_TPR2) {
		ArrayList<Triplet> TPRvTPR = new ArrayList<>();
		
		double FPR = FastMath.min(FPR_TPR1.get(1).getKey(), FPR_TPR2.get(1).getKey());
		for (int i = 1, j = 1; i < FPR_TPR1.size() && j < FPR_TPR2.size();) {
//			System.out.println(FPR_TPR1.get(i).getKey() + "," + FPR_TPR1.get(i).getValue() + "|" + FPR_TPR2.get(j).getKey() + "," + FPR_TPR2.get(j).getValue());
			
			double vi = FPR_TPR1.get(i).getValue();
			double vj = FPR_TPR2.get(j).getValue();
			
//			System.out.println(vi + "," + vj);
			
			if (FPR >= FPR_TPR1.get(i).getKey() && FPR >= FPR_TPR2.get(j).getKey()) {
				TPRvTPR.add(new Triplet(FPR, vi, vj));
//				System.out.println("adding pair: " + vi + "," + vj);
				i++;
				j++;
				continue;
			}
			
			if (FPR >= FPR_TPR1.get(i).getKey()) {
				TPRvTPR.add(new Triplet(FPR, vi, vj));
//				System.out.println("adding pair: " + vi + "," + vj);
				i++;
				continue;
			}
				
			if (FPR >= FPR_TPR2.get(j).getKey()) {
				TPRvTPR.add(new Triplet(FPR, vi, vj));
//				System.out.println("adding pair: " + vi + "," + vj);
				j++;
				continue;
			}
			
			FPR = FastMath.max(FPR_TPR1.get(i).getKey(), FPR_TPR2.get(j).getKey());
//			System.out.println("new FPR: " + FPR);
		}
		
		return TPRvTPR;
	}
	
	public static ArrayList<Pair<Double, Double>> run_NetProbe(String filePath, String outputPath) {
		ArrayList<Pair<Double, String>> orderedPairs = getOrderedPairs_NetProbe(filePath);
		ArrayList<Pair<Double, Double>> FPR_TPR = ROC(orderedPairs); // Pair<FraudBelief, UserType>
		writeROC(outputPath, FPR_TPR);
//		System.out.println(filePath + " AUC: " + AUC(FPR_TPR));
		
		return FPR_TPR;
	}

	public static ArrayList<Pair<Double, Double>> run(String filePath, String outputPath, int column) {
		ArrayList<Pair<Double, String>> orderedPairs = getOrderedPairs(filePath, column);
		ArrayList<Pair<Double, Double>> FPR_TPR = ROC(orderedPairs); // Pair<FraudBelief, UserType>
		writeROC(outputPath, FPR_TPR);
//		System.out.println(filePath + "|" + column + " AUC: " + auc);
		
		return FPR_TPR;
	}

	/**
	 * Use trapezium method to find area under ROC.
	 * @param FPR_TPR
	 */
	public static double AUC(List<Pair<Double, Double>> FPR_TPR) {
		
		double totalArea = 0;
		
		for (int i = 0; i < FPR_TPR.size() - 1; i++) {
			Pair<Double, Double> pair = FPR_TPR.get(i);
			Pair<Double, Double> nextPair = FPR_TPR.get(i + 1);

			double base = nextPair.getKey() - pair.getKey();
			double height = (nextPair.getValue() + pair.getValue())/2;
			double area = base * height;
			
			totalArea += area;
		}
		
		return totalArea;
	}

	static ArrayList<Pair<Double, String>> getOrderedPairs_NetProbe(String filePath) {
		ArrayList<Pair<Double, String>> pairs = new ArrayList<>();
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(filePath), true);
		for (String[] row : rows) {
			String userType = row[1];
			double normalBelief = Double.parseDouble(row[5]);
			
			Pair<Double, String> givenActualPairs = new Pair<>(normalBelief, userType);
			pairs.add(givenActualPairs);
		}
		
		Collections.sort(pairs, new Comparator<Pair<Double, String>>() {
			@Override
			public int compare(Pair<Double, String> pair1, Pair<Double, String> pair2) {
				int compare1 = Double.compare(pair1.getKey(), pair2.getKey()); // ascending order
				return compare1;
			}
		});
		
		return pairs;
	}
	
	static ArrayList<Pair<Double, String>> getOrderedPairs(String filePath, int column) {
		ArrayList<Pair<Double, String>> pairs = new ArrayList<>();
		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(filePath), true);
		for (String[] row : rows) {
			String userType = row[1];
			double fraudBelief = Double.parseDouble(row[column]);
			
			Pair<Double, String> givenActualPairs = new Pair<>(fraudBelief, userType);
			pairs.add(givenActualPairs);
		}
		
		Collections.shuffle(pairs);
		
		Collections.sort(pairs, new Comparator<Pair<Double, String>>() {
			@Override
			public int compare(Pair<Double, String> pair1, Pair<Double, String> pair2) {
				int compare1 = Double.compare(pair2.getKey(), pair1.getKey()); // descending order
				return compare1;
			}
		});
		
		return pairs;
	}
	
	private static ArrayList<Pair<Double, Double>> ROC(ArrayList<Pair<Double, String>> pairs) {
		int TP = 0;
		int FN = 0;
		int FP = 0;
		int TN = 0;
		
		for (Pair<Double, String> pair : pairs) { // go through once to find the totals for FP/TN
			// since threshold is max, everything is labelled as negative...
			if (!isFraud(pair.getValue())) // correctly classed as normal
				TN++;
			else //if (pair.b == 1) // incorrectly classed as fraudulent
				FN++;
		}
		
		double TPR = Double.NaN;
		double FPR = Double.NaN;
		
		ArrayList<Pair<Double, Double>> FPR_TPR = new ArrayList<>();
		
		for (Pair<Double, String> pair : pairs) { // now go through, moving the threshold, and calculating TPR, FPR
			TPR = (TP+FN) == 0 ? 0 : (double) TP/(TP+FN);
			FPR = (FP+TN) == 0 ? 0 : (double) FP/(FP+TN);
			FPR_TPR.add(new Pair<Double, Double>(FPR, TPR));
//			System.out.println(line++ + ", " + TP + "," + FN + "," + FP + "," + TN + "," + FPR + "," + TPR);
			
			// threshold is just after this pair, so everything before and including this pair should be classified as positive.
			if (isFraud(pair.getValue())) {  // correctly classified as fruad
				TP++;
				FN--;
			} else { // if (pair.b == 0) // incorrectly classified 
				FP++;
				TN--;
			}
		}
//		System.out.println(line++ + "," + TP + "," + FN + "," + FP + "," + TN + "," + FPR + "," + TPR);
		FPR_TPR.add(new Pair<Double, Double>(FPR, TPR));

		return FPR_TPR;
	}
	
	private static void writeROC(String outputPath, ArrayList<Pair<Double, Double>> fPR_TPR) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			for (Pair<Double, Double> pair : fPR_TPR) {
				bw.write(pair.getKey() + "," + pair.getValue());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeROC2(String outputPath, ArrayList<Triplet> triplets) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			bw.write("FPR,TPR1,TPR2");
			bw.newLine();
			for (Triplet pair : triplets) {
				bw.write(pair.toString());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class Triplet {
		double v1, v2, v3;
		public Triplet(double v1, double v2, double v3) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
		}
		@Override
		public String toString() {
			return v1 + "," + v2 + "," + v3;
		}
	}
	
	public static boolean isFraud(String userType) {
		return !(userType.startsWith("Cluster") || userType.equals("TMSeller"));
			
	}
	
}
