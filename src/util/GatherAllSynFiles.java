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
		File synDataFolder = new File("F:/workstuff2011/AuctionSimulation/single_feature_shillvsnormal");
		
		BufferedWriter bw = Files.newBufferedWriter(Paths.get(synDataFolder.getPath(), "all.csv"), Charset.defaultCharset());
		for (File file : synDataFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		})) {
			BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

			if (br.ready()) // skip the first line with the column headings
				br.readLine();
			while(br.ready())
				bw.append(br.readLine()).append("\r\n");
		}
		bw.flush();
		System.out.println("Done.");
	}
}
