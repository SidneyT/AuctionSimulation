package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Reads the rows from all CSV files in a folder and writes them all into the same file.
 */
public class GatherAllSynFiles {
	public static void main(String[] args) throws IOException {
		File synDataFolder = new File("F:/workstuff2011/AuctionSimulation/shillingResults/temp");
		
		BufferedWriter bw = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "all.csv"), Charset.defaultCharset());
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
					bw.flush();
					bw = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "all_" + lineCount / 1048576 + ".csv"), Charset.defaultCharset());
				}
				lineCount++;
				bw.append(br.readLine()).append("\r\n");
			}
		}
		bw.flush();
		System.out.println("Done.");
	}
}
