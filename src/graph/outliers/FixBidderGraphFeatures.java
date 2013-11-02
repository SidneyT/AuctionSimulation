package graph.outliers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimDBAuctionIterator;

import simulator.database.DBConnection;
import util.CsvManipulation;

/**
 * Class for adding in the userType in the second column of the csv by matching up the ids from the corresponding DB.
 *
 */
public class FixBidderGraphFeatures {
	public static void main(String[] args) throws IOException {
		run();
	}

	private static void run() throws IOException {
		String path = "F:/workstuff2011/AuctionSimulation/lof_features_fixed2";
		File[] files = new File(path).listFiles();
		for (File file : files) {
			String[] filenameParts = file.getName().split("_");
			String runNum = filenameParts[filenameParts.length - 2];
			String dbName = "";
			for (int i = 0; i < filenameParts.length - 2; i++)
				dbName += filenameParts[i] + "_";
			dbName += runNum;
			System.out.println("processing: " + dbName);
			
			SimDBAuctionIterator it = new SimDBAuctionIterator(DBConnection.getConnection(dbName), true);
			Map<Integer, UserObject> users = it.users();
			
			List<String[]> lines = CsvManipulation.readWholeFile(file.toPath(), false);
			BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset());
			String[] heading = lines.get(0); 
			writer.write(join(heading[0], "userType", Arrays.asList(heading).subList(1, heading.length)));
			writer.newLine();
			for (int i = 1; i < lines.size(); i++) {
				String[] line = lines.get(i);
				
				String completeLine = join(line[0], users.get(Integer.parseInt(line[0])).userType, Arrays.asList(line).subList(1, line.length));
				writer.append(completeLine);
				writer.newLine();
			}
			writer.close();
		}
	}
	
	private static String join(String p1, String p2, List<String> parts) {
		StringBuffer sb = new StringBuffer();
		sb.append(p1).append(",").append(p2).append(",");
		for (String part : parts) {
			sb.append(part).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	private static String join(String... parts) {
		StringBuffer sb = new StringBuffer();
		for (String part : parts) {
			sb.append(part).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
