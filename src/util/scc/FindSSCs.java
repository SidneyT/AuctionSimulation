package util.scc;

import graph.EdgeType;
import graph.EdgeTypeI;
import graph.GraphOperations;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import simulator.database.DBConnection;

import com.google.common.collect.Multiset;

import createUserFeatures.BuildTMFeatures;
import createUserFeatures.BuildTMFeatures.TMAuctionIterator;
import createUserFeatures.BuildUserFeatures.AuctionObject;

public class FindSSCs {

	public static void main(String[] args) {
		run();
	}
	
	private static void run() {
		SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> g = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		
		Map<Integer, Multiset<Integer>> adjacencyList = getGraph();
		for (Integer from : adjacencyList.keySet()) {
			g.addVertex(from);
			Multiset<Integer> tos = adjacencyList.get(from);
			for (Integer to : tos.elementSet()) {
				g.addVertex(to);
				
				int weight = tos.count(to);
				
				DefaultWeightedEdge edge = g.addEdge(from, to);
				
				assert edge != null;
				
				if (weight > 1)
					g.setEdgeWeight(edge, weight);
			}
		}
		
		StrongConnectivityInspector<Integer, DefaultWeightedEdge> sci = new StrongConnectivityInspector<>(g);
		List<Set<Integer>> sscs = sci.stronglyConnectedSets();
		for (Set<Integer> ssc : sscs) {
			System.out.print(ssc.size() + ",");
		}
	}
	
	public static <T extends AuctionObject> Map<Integer, Multiset<Integer>> getGraph() {
		Connection conn = DBConnection.getConnection("trademe");
		TMAuctionIterator it = new TMAuctionIterator(conn, BuildTMFeatures.DEFAULT_QUERY);
		EdgeTypeI edgeType = EdgeType.WIN;
		Map<Integer, Multiset<Integer>> graph = GraphOperations.duplicateAdjacencyList(it.iterator(), edgeType);
		return graph;
	}

}
