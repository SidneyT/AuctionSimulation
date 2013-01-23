package shillScore.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Reads a file that contains the 6 shill score ratings, and calculates a weighted average of those
 * scores using sets of weights given.
 * Scores of fraud and normal users are written in different files.
 */
public class RecalculateShillScores_old {
	public static void main(String[] args) throws IOException {
//		List<Integer> wantedColumns = ImmutableList.of(4,5,6,7,8,9);
		List<Integer> wantedColumns = ImmutableList.of(3,4,5,6,7,8);
		Collection<List<Double>> weightSets = ImmutableList.<List<Double>>of(
//				ImmutableList.of(9.0,2.0,5.0,2.0,2.0,2.0), // default from SS
//				ImmutableList.of(1.0,1.0,1.0,1.0,1.0,1.0), // equivalent weights
				ImmutableList.of(0.0820,0.0049,-0.0319,0.5041,0.2407,0.2003), // given by NN
				ImmutableList.of(0.000252653,0.10053923,0.005029133,-0.034119916,0.453745575,0.246415436,0.228137889) // average from NN
//				ImmutableList.of(0.039578424,18.68319145,1.117054671,-7.275003797,114.901611,54.85097605,45.64194598)
				);

		File synDataFolder = new File("F:/workstuff2011/AuctionSimulation/shillingResults/Trevathan");
		new RecalculateShillScores_old(wantedColumns, weightSets, synDataFolder).recalculate();
	}

	private final List<Integer> ratingColumns;
	private final Collection<List<Double>> weightSets;
	private final File inputDirectory;
	
	/**
	 * column numbers start at 0.
	 * @param ratingColumns
	 */
	public RecalculateShillScores_old(List<Integer> ratingColumns, Collection<List<Double>> weights, File inputDirectory) {
		this.ratingColumns = ratingColumns;
		this.weightSets = weights;
		this.inputDirectory = inputDirectory;
	}
	
	public void recalculate() throws IOException {
		
		BufferedWriter bw_normal = Files.newBufferedWriter(Paths.get(inputDirectory.getPath(), "nn_weights_normal.csv"), Charset.defaultCharset());
		BufferedWriter bw_fraud = Files.newBufferedWriter(Paths.get(inputDirectory.getPath(), "nn_weights_fraud.csv"), Charset.defaultCharset());
		bw_normal.append(heading());
		bw_fraud.append(heading());
		
		int lineCount = 0;
		for (File file : inputDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("ShillScores_") && name.endsWith(".csv");
			}
		})) {

			BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

			if (br.ready()) // skip the first line with the column headings
				br.readLine();
			while(br.ready()) {
				String[] parts = br.readLine().split(",");
				
				if (Integer.parseInt(parts[0]) < 5000) { // is normal user
//				if (!parts[0].toLowerCase().contains("puppet")) { // is normal user
					if (lineCount != 0 && lineCount % 1048575 == 0) {
						bw_normal.flush();
						bw_normal.close();
						bw_normal = Files.newBufferedWriter(Paths.get(inputDirectory.getPath(), "normal_ss_" + lineCount / 1048576 + ".csv"), Charset.defaultCharset());
						bw_normal.append(heading());
					}
					lineCount++;
					
					bw_normal.append(line(parts)).append(",normal");
					bw_normal.newLine();
				} else { // is fraud user
					bw_fraud.append(line(parts)).append(",fraud");
					bw_fraud.newLine();
				}
			}
		}
		bw_normal.flush();
		bw_fraud.flush();
		System.out.println("Done.");
	}
	
	private String heading() {
		StringBuilder sb = new StringBuilder();
		for (List<Double> weightSet : weightSets) {
			sb.append(weightSet.toString().replace(", ", "|"));
			sb.append(",");
		}
		sb.append("class");
		sb.append("\r\n");
		return sb.toString();
	}
	
	private String line(String[] parts) throws IOException {
		double newScore = 0;
		StringBuilder sb = new StringBuilder();
		for (List<Double> weightSet : weightSets) {
			for (int i = 0; i < ratingColumns.size(); i++) {
				Double rating = Double.parseDouble(parts[ratingColumns.get(i)]);
				newScore += weightSet.get(i) * rating;
			}
//			newScore += -221.793742153912;
			sb.append(newScore + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
