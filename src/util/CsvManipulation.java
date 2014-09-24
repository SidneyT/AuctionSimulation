package util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.CsvManipulation.CsvThing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;

import au.com.bytecode.opencsv.CSVReader;

public class CsvManipulation {
	
	public static interface LineFilter {boolean acceptLine(String[] line);}
	
	/**
	 * Returns a filter that checks for the string in each cell.
	 * Filter returns true/false if it finds the string depending on the action param.
	 * @param string
	 * @return
	 */
	public static LineFilter lineContainsFilter(final String string, final boolean keepMatch) {
		return new LineFilter() {
			@Override
			public boolean acceptLine(String[] line) {
				for (String part : line) {
					if (part.equals(string))
						return keepMatch;
				}
				return !keepMatch;
			}
		};
	}
	public static LineFilter lineContainsAtFilter(final String string, final int columnIndex, final boolean keepMatch) {
		return new LineFilter() {
			@Override
			public boolean acceptLine(String[] line) {
				boolean match = line[columnIndex].equals(string);
				if (match) {
//					System.out.println("keeping:" + Arrays.toString(line));
					return keepMatch;
				} else {
//					System.out.println("discarding:" + Arrays.toString(line));
					return !keepMatch;
				}
//				return match != !keepMatch; // short version
			}
		};
	}
	public static LineFilter lineContainsAtFilter(final int columnIndex, final boolean keepMatch, final int... strings) {
		return new LineFilter() {
			@Override
			public boolean acceptLine(String[] line) {
				boolean match = false;
				for (int string : strings) {
					if (line[columnIndex].equals(string + "")) { // match
						match = true;
						break;
					}
				}
				if (match) {
//					System.out.println("keeping:" + Arrays.toString(line));
					return keepMatch;
				} else {
//					System.out.println("discarding:" + Arrays.toString(line));
					return !keepMatch;
				}
//				return match != !keepMatch; // short version
			}
		};
	}
	
	public static List<String[]> removeColumns(List<String[]> input, List<Integer> unwantedColumns) {
		List<String[]> results = new ArrayList<>();
		for (String[] line : input) {
			results.add(removeColumns(line, unwantedColumns));
		}
		return results;
	}
 
	
	public static String[] removeColumns(String[] input, List<Integer> unwantedColumns) {
		ArrayList<String> outputList = new ArrayList<String>(Arrays.asList(input));
		for (int unwanted : unwantedColumns) {
			outputList.remove(unwanted);
		}
		
		return outputList.toArray(new String[0]);
	}
	
	public static List<String[]> filter(Iterable<String[]> input, LineFilter filter) {
		List<String[]> output = new ArrayList<>();
		for (String[] line : input) {
			if (filter.acceptLine(line))
				output.add(line);
		}
		return output;
	}
	/**
	 * Given input, returns a new list with lines that are accepted by all filters.
	 * @param input
	 * @param filters
	 * @return
	 */
	public static List<String[]> filtersAND(Iterable<String[]> input, LineFilter... filters) {
		List<String[]> output = new ArrayList<>();
		for (String[] line : input) {
			boolean accept = true;
			if (line.length == 0) {
				continue;
			}
			for (LineFilter filter : filters) { // apply each filter. passes if all filters say accept.
				if (!filter.acceptLine(line)) {
					accept = false;
					break;
				}
			}
			if (accept) {
				output.add(line);
			}
		}
		return output;
	}
	
	/**
	 * Reads entire file, then closes it.
	 * @param inputCsvFile
	 * @param skipFirstRow
	 * @return a List of strings split using "," as the delimiter.
	 */
	public static List<String[]> readWholeFile(Path inputCsvFile, boolean skipFirstRow) {
		try {
			CSVReader reader = new CSVReader(new FileReader(inputCsvFile.toFile()));
			String[] nextLine;
			List<String[]> lines = new ArrayList<>();
			while ((nextLine = reader.readNext()) != null) {
				if (skipFirstRow) {
					skipFirstRow = false;
				} else {
					lines.add(nextLine);
				}
			}
			reader.close();
			return lines;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static CsvRows readFile(Path inputCsvFile, boolean skipFirstRow) {
		try {
			return new CsvRows(inputCsvFile, skipFirstRow);
		} catch (IOException e) { throw new RuntimeException(e); }
	}
	
	/**
	 * 
	 * @param outputCsvFile
	 * @param headings accepts null. I.e. no row for headings
	 * @param rows
	 * @param appendToFile
	 * @param extraColumns
	 * @throws IOException
	 */
	public static void writeFile(Path outputCsvFile, String headings, List<String[]> rows, boolean appendToFile, String... extraColumns) throws IOException{
		if (appendToFile)
			writeFile(Files.newBufferedWriter(outputCsvFile, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND), headings, rows, extraColumns);
		else
			writeFile(Files.newBufferedWriter(outputCsvFile, Charset.defaultCharset()), headings, rows, extraColumns);
	}
	public static void writeFile(BufferedWriter writer, String headings, List<String[]> rows, String[] extraColumns) throws IOException{
		if (headings != null && headings.length() > 0) {
			writer.append(headings);
			writer.newLine();
		}
		
		for (String[] row : rows) {
			for (int i = 0; i < row.length; i++) {
				if (i > 0)
					writer.append(",");
				writer.append(row[i]);
			}
			for (String extraColumn : extraColumns) {
				writer.append("," + extraColumn);
			}
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * Iterator to go throuh all rows in a csv file.
	 * Reads the file row by row with next().
	 * So can't write to the same file while reading.
	 */
	public static class CsvRows implements Iterable<String[]> {
		public final String headingRow;
		private final BufferedReader reader;
		private final boolean skipFirst;
		public CsvRows(Path path, boolean skipFirstRow) throws IOException {
			 this.reader = Files.newBufferedReader(path, Charset.defaultCharset());
			 this.skipFirst = skipFirstRow;
			 
			if (skipFirst && reader.ready()) {
				headingRow = reader.readLine();
			} else {
				headingRow = null; 
			}
		}
		
		public void reset() {
			try {
				this.reader.reset();
				if (skipFirst && reader.ready()) {
					reader.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public Iterator<String[]> iterator() {
			return new Iterator<String[]>() {
				@Override
				public void remove() {throw new UnsupportedOperationException();}
				
				@Override
				public String[] next() {
					try {
						String line = reader.readLine();
						String[] splitLine = line.split(",");
						return splitLine;
					} catch (IOException e) { throw new RuntimeException(e); }
				}
				
				@Override
				public boolean hasNext() {
					try {
						return reader.ready();
					} catch (IOException e) { throw new RuntimeException(e); }
				}
			};
		}
	}

	/**
	 * Adds a new column, along with a column heading.
	 * If <code>matchIds</code> contains the value in the first (0th) column is in matchIdsthen add <code>matchValue</code>
	 * at the end, otherwise add <code>unmatchValue</code>.
	 * 
	 * @param inputCsvFile
	 * @param matchIds
	 * @param matchValue
	 * @param unmatchValue
	 */
	public static void addColumn(Path inputCsvFile, Path outputCsvFile, String columnHeading, List<Integer> matchIds, double matchValue, double unmatchValue) {
		try {
			
			// read the file, save the lines
			CSVReader reader = new CSVReader(new FileReader(inputCsvFile.toFile()));
			String[] nextLine;
			List<String[]> lines = new ArrayList<>();
			while ((nextLine = reader.readNext()) != null) {
				lines.add(nextLine);
			}
			reader.close();
			
			// write the lines, adding the extra element at the end
			BufferedWriter writer = Files.newBufferedWriter(outputCsvFile, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			if (lines.size() <= 1) {
				return;
			}
			
			String[] headingRow = lines.get(0);
			for (String value : headingRow) {
				writer.append(value + ",");
			}
			writer.append(columnHeading);
			writer.newLine();
			
			Set<Integer> matchIdSet = ImmutableSet.copyOf(matchIds);
			for (int i = 1; i < lines.size(); i++) {
				String[] line = lines.get(i);
				for (String value : line) {
					writer.append(value + ",");
				}
				int id = Integer.parseInt(line[0]);
				if (matchIdSet.contains(id)) {
					writer.append(matchValue + "");
				} else {
					writer.append(unmatchValue + "");
				}
				
				writer.newLine();
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void addColumn(Path inputCsvFile, Path outputCsvFile, String columnHeading, List<Double> columnValues) {
		try {
			String[] headingRow;
			List<String[]> lines = new ArrayList<>();
			
			// read the file, save the lines'
			try {
				CSVReader reader = new CSVReader(new FileReader(inputCsvFile.toFile()));
				String[] nextLine;
				while ((nextLine = reader.readNext()) != null) {
					lines.add(nextLine);
				}
				reader.close();
			} catch (IOException e) {
				headingRow = new String[]{};
			}
			
			if (lines.isEmpty())
				headingRow = new String[]{};
			else 
				headingRow = lines.get(0);
			
			// write the lines, adding the extra element at the end
			BufferedWriter writer = Files.newBufferedWriter(outputCsvFile, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

			for (String value : headingRow) {
				writer.append(value + ",");
			}
			writer.append(columnHeading);
			writer.newLine();
			
			for (int i = 0; i < columnValues.size(); i++) {
				if (i + 1 < lines.size()) {
					String[] line = lines.get(i + 1);
					for (String value : line) {
						writer.append(value + ",");
					}
				}
				writer.append(columnValues.get(i) + "");
				writer.newLine();
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static void writeMaps(String outputPath, List<Map<Integer, String>> thing, String headingRow) {
		try {
			HashSet<Integer> allIds = new HashSet<>();
			for (Map<Integer, ? extends Object> lofValues : thing) {
				allIds.addAll(lofValues.keySet());
			}
			
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), Charset.defaultCharset());
			writer.append(headingRow.toString());
			writer.newLine();
			
			for (Integer id : allIds) {
				writer.append(id + "");
				for (Map<Integer, ? extends Object> lofValues : thing) {
					if (lofValues.containsKey(id))
						writer.append("," + lofValues.get(id));
					else 
						writer.append(",");
				}
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * featureValues can hold nulls if there is no value for that feature for that user.
	 */
	public static class CsvThing {
		public String[] headingRow;
		public ArrayListMultimap<Integer, Double> featureValues;
	}

	/**
	 * Reads csv file into a ArrayListMultimap.
	 * First column is used as a key, and must be an Integer. The other columns make up the array list.
	 * If a value cannot be parsed as a double, the value is left as null.
	 */
	public static CsvManipulation.CsvThing readCsvFile(String filepath) {
		List<String[]> rows = readWholeFile(Paths.get(filepath), false);
		
		CsvManipulation.CsvThing csvThing = new CsvManipulation.CsvThing();
		csvThing.headingRow = rows.get(0);
		
		ArrayListMultimap<Integer, Double> featureValues = ArrayListMultimap.create(); 
		for (String[] row : rows.subList(1, rows.size())) {
			int id = (int) Double.parseDouble(row[0]);
			for (int i = 1; i < row.length; i++) {
				String valueString = row[i];
	
				Double value;
				if (valueString.isEmpty() || valueString.equals("null"))
					value = null;
				else if (valueString.equals("-Infinity"))
					value = Double.NEGATIVE_INFINITY;
				else if (valueString.equals("Infinity")) {
					value = Double.POSITIVE_INFINITY;
				} else {
					try {
						value = Double.parseDouble(row[i]);
					} catch (NumberFormatException e) {
						value = null;
					}
				}
				
				featureValues.put(id, value);
			}
		}
		
		csvThing.featureValues = featureValues;
		return csvThing;
	}
	
	
}
