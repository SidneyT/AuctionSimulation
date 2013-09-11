package graph;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.util.FastMath;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public class MRF {

	final int nodeCount;
	final Map<Integer, Integer> originalToConsecutive;
	final int[] consecutiveToOriginal;
	
	Multiset<Integer>[] graph;
	double[] outlierScores; // should be normalised to between 0-1
	double[] fraudBeliefs;
	double[] normalBeliefs;
	
	double[][] partialMessages; // double[][fraud belief, normal Belief]
	double[][] messageFor; // stores messages for nodes. message for a node is in the corresponding index, e.g. messageFor[node][0] and messageFor[node][1]
	
	public MRF(Map<Integer, Multiset<Integer>> graph, Map<Integer, Double> outlierScores) {
		int nodeCount = outlierScores.size();
		this.nodeCount = nodeCount;
		
		/**
		 * Map the user ids to a set of consecutive integers. </br>
		 * Remember the mapping from original to the new consecutive values, and vice versa.
		 */
		{
			Map<Integer, Integer> originalToConsecutive = new HashMap<>();
			int[] consecutiveToOriginal = new int[nodeCount];
			int count = 0;
			for (Integer origId : graph.keySet()) {
				originalToConsecutive.put(origId, count);
				consecutiveToOriginal[count] = origId;
				count++;
			}
			
			this.originalToConsecutive = originalToConsecutive;
			this.consecutiveToOriginal = consecutiveToOriginal;
		}
		
		/**
		 * Replace original ids in the graph with the new id values
		 */
		{
			this.graph = (Multiset<Integer>[]) Array.newInstance(Multiset.class, nodeCount);
			for (int conseId = 0; conseId < consecutiveToOriginal.length; conseId++) {
				// map the consecutive key to the original
				int origId = consecutiveToOriginal[conseId];
				
				HashMultiset<Integer> conseNeighbourIds = HashMultiset.create();
				// map the neighbours to the new ids
				Multiset<Integer> neighbours = graph.get(origId);
				for (Integer neighbour : neighbours.elementSet()) {
					int conseNeighbourId = originalToConsecutive.get(neighbour);
					conseNeighbourIds.add(conseNeighbourId, neighbours.count(neighbour));
				}
				
				this.graph[conseId] = ImmutableMultiset.copyOf(conseNeighbourIds);
			}
		}
		
		/**
		 * Replace the ids in outlierScores with consecutive Ids
		 */
		{
			this.outlierScores = new double[nodeCount];
			for (Entry<Integer, Double> entry : outlierScores.entrySet()) {
				int conseId = originalToConsecutive.get(entry.getKey());
				this.outlierScores[conseId] = normaliseOutlierScore(entry.getValue());
			}
		}
		
//		this.fraudBeliefs = new double[nodeCount];
		// initialise fraudBeliefs with outlierScores
		this.fraudBeliefs = Arrays.copyOf(this.outlierScores, this.outlierScores.length);
		this.normalBeliefs = new double[nodeCount];
		for (int i = 0; i < fraudBeliefs.length; i++)
			this.normalBeliefs[i] = 1 - this.fraudBeliefs[i];
		partialMessages = new double[nodeCount][2];
		messageFor = new double[nodeCount][2];
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
	}
	
	
	// Propagation Matrix
	// 			Fraud	Normal
	// Fraud	0.2		0.8
	// Normal	0.5		0.5
	private final static double[][] propagationMatrix = new double[][]{
			new double[]{0.5, 0.5},
			new double[]{0.2, 0.8}
		}; 

	private void run() {
		int maxIt = 500;
		while (!converged()) {
			System.out.println("belief: " + Arrays.toString(fraudBeliefs) + ", " + Arrays.toString(normalBeliefs));

			for (int i = 0; i < messageFor.length; i++)
				Arrays.fill(messageFor[i], 1d);

			multiplyMessages();
			propagateMessages();
			
			System.out.print("messages: ");
			for (int i = 0; i < messageFor.length; i++)
				System.out.print(Arrays.toString(messageFor[i]) + ",");
			System.out.println();
			
			beliefUpdate();
			
			
			
			if (maxIt-- < 0)
				break;
		}
	}
	
	private Multiset<Integer> getNeighbours(int node) {
		return graph[node];
	}
	
	public void propagate() {
		
	}
	
	public double getFraudBelief(int id) {
		return fraudBeliefs[id];
	}
	public double getNormalBelief(int id) {
		return normalBeliefs[id];
	}

	public double getOutlierScore(int id) {
		return this.outlierScores[id];
	}
	
	/**
	 * Lines 7 & 8. Find product of beliefs from all neighbours of each node.
	 */
	public void multiplyMessages() {
		for (int node = 0; node < nodeCount; node++) {
			Multiset<Integer> neighbours = getNeighbours(node);
			
			double fraudBelief = 1;
			double normalBelief = 1;
			for (Integer neighbour : neighbours.elementSet()) {
				double neighbourFraudBelief = getFraudBelief(neighbour);
				fraudBelief *= neighbourFraudBelief;
	
				normalBelief *= (1 - neighbourFraudBelief);
			}
			
			partialMessages[node][0] = fraudBelief;
			partialMessages[node][1] = normalBelief;
		}
	}
	
	/**
	 * Lines 9 - 11.
	 */
	public void propagateMessages() {
		for (int node = 0; node < nodeCount; node++) {
			Multiset<Integer> neighbours = getNeighbours(node);
			
			double targetFraudBelief = fraudBeliefs[node];
			
			// gather messages from neighbours
			for (int neighbour : neighbours.elementSet()) {
				// line 10: divide message to exclude the belief of the node this message is for
				// fraudBelief and normalBelief together makes up the message.
				double fraudBelief = partialMessages[neighbour][0];
				fraudBelief /= targetFraudBelief;
				
				double normalBelief = partialMessages[neighbour][1];
				normalBelief /= normalBeliefs[node];
				
				// line 11: complete message by using observation matrix AND propagation matrix
				// observation
				double fraudPhi = hiddenCompatabilityFunctionF(outlierScores[neighbour], fraudBeliefs[neighbour]);
				fraudBelief *= fraudPhi;
				double normalPhi = hiddenCompatabilityFunctionN(1 - outlierScores[neighbour], normalBeliefs[neighbour]);
				normalBelief *= normalPhi;
				// propagation
				double fraudPsi = neighbourCompatibilityFunctionF(fraudBeliefs[neighbour], targetFraudBelief);
				fraudBelief *= fraudPsi;
				double normalPsi = neighbourCompatibilityFunctionN(1 - fraudBeliefs[neighbour], 1 - targetFraudBelief);
				normalBelief *= normalPsi;
				
				// factor in edge weight
//				int edgeWeight = neighbours.count(neighbour);
//				fraudBelief = FastMath.pow(fraudBelief, edgeWeight);
//				normalBelief = FastMath.pow(normalBelief, edgeWeight);
				
				if (fraudBelief < 0.001 || normalBelief < 0.001) {
//					System.out.println("pause");
				}
				
				messageFor[node][0] *= fraudBelief;
				messageFor[node][1] *= normalBelief;
			}
			
		}
	}
	
	public void beliefUpdate() {
		for (int node = 0; node < nodeCount; node++) {
			double fraudMsg = messageFor[node][0];
			double normalMsg = messageFor[node][1];
			
			// observation
			double fraudPhi = hiddenCompatabilityFunctionF(outlierScores[node], fraudBeliefs[node]);
			fraudMsg *= fraudPhi;
			double normalPhi = hiddenCompatabilityFunctionN(1 - outlierScores[node], normalBeliefs[node]);
			normalMsg *= normalPhi;
			
			// normalisation
			double normalisedFraud = normaliseBeliefs(fraudMsg, normalMsg);
			double normalisedNormal = 1 - normalisedFraud;
			
			fraudBeliefs[node] = normalisedFraud;
			normalBeliefs[node] = normalisedNormal;
		}
	}
	
	double[] previousFraudBelief = null;
	public boolean converged() {
		if (previousFraudBelief == null) {
			previousFraudBelief = Arrays.copyOf(fraudBeliefs, fraudBeliefs.length);
			return false;
		}
		
		boolean converged = true;
		for (int i = 0; i < previousFraudBelief.length; i++) {
			double diff = Math.abs(previousFraudBelief[i] - fraudBeliefs[i]);
			if (diff > 0.001) {
				converged = false;
				return converged;
			}
		}
		
		previousFraudBelief = Arrays.copyOf(fraudBeliefs, fraudBeliefs.length);
		
		return converged;
	}
	
	public double hiddenCompatabilityFunctionF(double hiddenFraudBelief, double observedFraudBelief) {
//		return 1 - FastMath.abs(hiddenFraudBelief - observedFraudBelief);
		return (hiddenFraudBelief + 0.5) / 2;
	}
	public double hiddenCompatabilityFunctionN(double hiddenNormalBelief, double observedNormalBelief) {
//		return 1 - FastMath.abs(hiddenNormalBelief - observedNormalBelief);
		return (hiddenNormalBelief + 0.5) / 2;
	}
	public double neighbourCompatibilityFunctionF(double selfHiddenFraudBelief, double neighbourHiddenFraudBelief) {
//		return 1 - FastMath.abs(selfHiddenFraudBelief - neighbourHiddenFraudBelief);
		return (selfHiddenFraudBelief + 0.5) / 2;
	}
	public double neighbourCompatibilityFunctionN(double selfHiddenNormalBelief, double neighbourHiddenNormalBelief) {
//		return 1 - FastMath.abs(selfHiddenNormalBelief - neighbourHiddenNormalBelief);
		return ( selfHiddenNormalBelief + 0.5) / 2;
	}
	
	
	
	/**
	 * Returns the normalised fBelief.
	 * E.g. if parameters were (0.3, 0.2), then 0.6 is returned.
	 * @param fBelief
	 * @param nBelief
	 * @return
	 */
	public double normaliseBeliefs(double fBelief, double nBelief) {
		assert fBelief >= 0 && fBelief <= 1;
		assert nBelief >= 0 && nBelief <= 1;
		
		double sum = fBelief + nBelief;
		return fBelief / sum;
	}
	
	public static double normaliseOutlierScore(double outlierScore) {
		double score = normaliseOutlierScoreInner(outlierScore);
		
		score = (score * 0.8) + 0.2; 
		return score;
		
	}
	/**
	 * Given a outlier score, returns the "probability" that it's a suspicious user.
	 * E.g. with a score of 3, returns (3 - 0.5) / 3 = 0.83.
	 * The probability of being "normal" is then the complement; e.g. 1 - 0.83 = 0.17.
	 * @param outlierScore
	 * @return
	 */
	public static double normaliseOutlierScoreInner(double outlierScore) {
//		return (outlierScore - 1) / (outlierScore - 0.2);
		if (outlierScore < 1)
			return 0;
		return (outlierScore - 1) / (outlierScore);
	}
	
	public static void main(String[] args) {
//		for (int i = 0; i < 40; i++) {
//			System.out.println(i * 0.1 + "," + normaliseOutlierScore(i * 0.1));
//		}
		test1();
		
	}
	
	public static void test1() {
		Map<Integer, Multiset<Integer>> mockGraph = new HashMap<>();
		mockGraph.put(0, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,3,3})));
		mockGraph.put(1, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,2,2,2})));
		mockGraph.put(2, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,1,1})));
		mockGraph.put(3, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,0})));
		
		Map<Integer, Double> mockOutlierScores = new HashMap<>();
		mockOutlierScores.put(0, 1d);
		mockOutlierScores.put(1, 20d);
		mockOutlierScores.put(2, 2d);
		mockOutlierScores.put(3, 1d);
		
		MRF mrf = new MRF(mockGraph, mockOutlierScores);
		mrf.run();
		System.out.println("Finished");
	}
	
}
