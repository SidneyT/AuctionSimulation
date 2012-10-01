package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads the rows from all CSV files in a folder and writes them all into the same file.
 */
public class CombineFiles {
	public static void main(String[] args) throws IOException {
		File synDataFolder = new File("F:/workstuff2011/AuctionSimulation/shillingResults/waitStartTrevathan");
		
		String filename = "syn_WaitStartTrevathan_normal";
		String suffix = ".csv";
		Path outputFolder = Paths.get(synDataFolder.getPath(), "combined");
		BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFolder.toString(), filename + suffix), Charset.defaultCharset());
		int lineCount = 0;
		for (File file : synDataFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
//				return name.startsWith("syn_") && name.endsWith(".csv");
				return name.startsWith("ShillScores_") && name.endsWith(".csv");
			}
		})) {

			BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

			if (br.ready()) // skip the first line with the column headings
				br.readLine();
			while(br.ready()) {
				String line = br.readLine();
				if (!line.startsWith("Puppet")) {
					lineCount++;
					if (lineCount != 0 && lineCount % 1048576 == 0) {
						bw.flush();
						bw.close();
						bw = Files.newBufferedWriter(Paths.get(outputFolder.toString(), filename + lineCount / 1048576 + suffix), Charset.defaultCharset());
					}
					bw.append(line).append(",fraud").append("\r\n");
				}
			}
		}
		bw.flush();
		System.out.println("Done.");
	}
}
