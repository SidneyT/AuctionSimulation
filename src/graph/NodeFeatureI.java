package graph;

import java.util.Map;

import com.google.common.collect.Multiset;

interface NodeFeatureI {
	double value(Map<Integer, Multiset<Integer>> adjacencyList, int user);
}