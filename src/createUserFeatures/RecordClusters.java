package createUserFeatures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import simulator.database.TradeMeDbConn;


public class RecordClusters {
	public static void main(String[] args) throws IOException, SQLException {
		RecordClusters cr = new RecordClusters();
		cr.record(new File("UserFeatures-0-1-1ln-2-2ln-3-3ln-10-5-11-9.csv"), new File("SimpleKMeans-1ln-2ln-3ln-10-5-11-9_4clusters.csv"), "SimpleKMeans_4");
		cr.record(new File("reclusterIds_0.csv"), new File("SimpleKMeans_recluster0-1ln-2ln-3ln-10-5-11-9_2clusters.csv"), "SimpleKMeans_4");
		cr.record(new File("reclusterIds_1.csv"), new File("SimpleKMeans_recluster1-1ln-2ln-3ln-10-5-11-9_3clusters.csv"), "SimpleKMeans_4");
		cr.record(new File("reclusterIds_2.csv"), new File("SimpleKMeans_recluster2-1ln-2ln-3ln-10-5-11-9_3clusters.csv"), "SimpleKMeans_4");
		cr.record(new File("reclusterIds_3.csv"), new File("SimpleKMeans_recluster3-1ln-2ln-3ln-10-5-11-9_3clusters.csv"), "SimpleKMeans_4");
		System.out.println("Finished.");
	}

	public static List<Integer> getClusters(File file){
		List<Integer> clusters = new ArrayList<Integer>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader(file));
			// skip the first line, since it's just column headings
			br.readLine();
			while (br.ready()) {
				String line = br.readLine();
				String[] sArr = line.split(",");
				//int instanceId = Integer.parseInt(sArr[0]);
				int clusterId = Integer.parseInt(sArr[sArr.length - 1].replaceAll("[^0-9]+", ""));
				clusters.add(clusterId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clusters;
	}

	Connection conn;
	private void record(File idFile, File clusterFile, String algName) throws SQLException {
		conn = TradeMeDbConn.getConnection();
		List<Integer> userId = getUserIds(idFile);
		List<Integer> clusters = getClusters(clusterFile);
		
		conn.setAutoCommit(false);
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO clusterAssignments (userId, cluster, algorithm) VALUES (?, ?, ?);");
		if (algName == null || algName.isEmpty())
			algName = clusterFile.getName().substring(0, clusterFile.getName().lastIndexOf("."));
		for (int i = 0; i < userId.size(); i++) {
			addToBatch(pstmt, userId.get(i), clusters.get(i) + "", algName);
		}
		pstmt.executeBatch();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	private void recordReclustering(File idFile, File clusterFile, String algName, int bigCluster) throws SQLException {
		conn = TradeMeDbConn.getConnection();
		List<Integer> userId = getUserIds(idFile);
		List<Integer> clusters = getClusters(clusterFile);
		
		conn.setAutoCommit(false);
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO clusterAssignments (userId, cluster, algorithm) VALUES (?, ?, ?);");
		if (algName == null || algName.isEmpty())
			algName = clusterFile.getName().substring(0, clusterFile.getName().lastIndexOf("."));
		for (int i = 0; i < userId.size(); i++) {
			addToBatch(pstmt, userId.get(i), bigCluster + "-" + clusters.get(i), algName);
		}
		pstmt.executeBatch();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	private void addToBatch(PreparedStatement pstmt, int userId, String cluster, String algName) {
		try {
			System.out.println("query param:" + userId + "," + cluster);
			pstmt.setInt(1, userId);
			pstmt.setString(2, cluster);
			pstmt.setString(3, algName);
			pstmt.addBatch();
		} catch (SQLException e) {
//			// testing for expected exception for when userId does not exist in the "users" table
//			// this is because some users have their bids recorded, but their feedback pages were not crawled
//			if (e.getErrorCode() != 1452 || !e.getMessage().startsWith("Cannot add or update a child row: a foreign key constraint fails"))
				throw new RuntimeException(e);
		}
	}
	
	public static List<Integer> getUserIds(File file) {
		List<Integer> userIdList = new ArrayList<Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			br.readLine();
			while(br.ready()) {
				String line = br.readLine();
				userIdList.add(Integer.parseInt(line.split(",")[0]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userIdList;
	}
	
}
