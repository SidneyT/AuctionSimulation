package shillScore.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class RecalculateShillScores {
	public static void main(String[] args) throws IOException {
		new RecalculateShillScores(Arrays.asList(3,4,5,6,7,8), 
				Arrays.asList(0.0820,0.0049,-0.0319,0.5041,0.2407,0.2003)).recalculate();
	}

	private final List<Integer> ratingColumns;
	private final List<Double> weights;
	
	/**
	 * column numbers start at 0.
	 * @param ratingColumns
	 */
	public RecalculateShillScores(List<Integer> ratingColumns, List<Double> weights) {
		this.ratingColumns = ratingColumns;
		this.weights = weights;
	}
	
	public void recalculate() throws IOException {
		File synDataFolder = new File("F:/workstuff2011/AuctionSimulation/shillingResults/backup");
		
		BufferedWriter bw_normal = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "normal_ss.csv"), Charset.defaultCharset());
		BufferedWriter bw_fraud = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "fraud_ss.csv"), Charset.defaultCharset());
		int lineCount = 0;
		for (File file : synDataFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		})) {

			BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

			if (br.ready()) // skip the first line with the column headings
				br.readLine();
			while(br.ready()) {
				if (lineCount != 0 && lineCount % 1048576 == 0) {
					bw_normal.flush();
					bw_normal = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "all_" + lineCount / 1048576 + ".csv"), Charset.defaultCharset());
				}
				lineCount++;
				
				String[] parts = br.readLine().split(",");
				if (Integer.parseInt(parts[0]) < 5000)
					writeLine(bw_normal, parts);
				else
					writeLine(bw_fraud, parts);
			}
		}
		bw_normal.flush();
		System.out.println("Done.");
	}
	
	public void writeLine(BufferedWriter bw, String[] parts) throws IOException {
		double newScore = 0;
		for (int i = 0; i < ratingColumns.size(); i++) {
			newScore += weights.get(i) * Double.parseDouble(parts[ratingColumns.get(i)]);
		}
		bw.append(newScore + "").append("\r\n");
	}
}
