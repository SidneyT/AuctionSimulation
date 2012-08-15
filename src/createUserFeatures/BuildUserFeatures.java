package createUserFeatures;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import createUserFeatures.FeaturesToUseWrapper.FeaturesToUse;


import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class BuildUserFeatures {
	protected static final double BEG_MID_BOUNDARY = 0.5;
	protected static final double MID_END_BOUNDARY = 0.95;
	
	protected TreeMap<Integer, UserFeatures> userFeaturesMap;
	protected final FeaturesToUseWrapper featuresToPrint; // controls what features are printed
	public boolean trim; // trim auction bid list lengths to 20
	
	public BuildUserFeatures(String features) {
		this.userFeaturesMap = new TreeMap<Integer, UserFeatures>();
		this.featuresToPrint = new FeaturesToUseWrapper(features);
		trim = false;
	}
	
	public FeaturesToUse getFeaturesToPrint() {
		return featuresToPrint.getFeaturesToUse();
	}
	
	public boolean trim() {
		return trim;
	}
	
	public void setFeaturesToPrint(String featureString) {
		this.featuresToPrint.setFeaturesToPrint(featureString);
	}

	public String getFeaturesToPrintString() {
		return this.featuresToPrint.getFeaturesToUseString();
	}
	
	public static void writeToFile(BuildUserFeatures buf, String filename) {
		writeToFile(buf.build().values(), buf.featuresToPrint.getFeaturesToUse(), filename);
	}
	
	public static void writeToFile(Collection<UserFeatures> userFeaturesCol, FeaturesToUse featuresToPrint, String filename) {
		writeToFile(userFeaturesCol, featuresToPrint, new File(filename).toPath());
	}
	public static void writeToFile(Collection<UserFeatures> userFeaturesCol, FeaturesToUse featuresToPrint, Path path) {
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeToFile(userFeaturesCol, featuresToPrint, bw);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void writeToFile(Collection<UserFeatures> userFeaturesCol, FeaturesToUse featureHeadings, BufferedWriter w) throws IOException {
		w.write(UserFeatures.headings(featureHeadings));
		w.newLine();
		for (UserFeatures uf : userFeaturesCol) {
//			System.out.println("id: " + uf.id());
			if (uf.isComplete()) {
				w.write(uf.toString());
				w.newLine();
			}
		}
		w.close();
	}
	
	/**
	 * POJO
	 */
	protected class TMAuctionObject {
		int auctionId;
		String category;
		Date endTime;
		int winnerId;
		
		public TMAuctionObject(int auctionId, String category, Date endTime, int winnerId) {
			this.auctionId = auctionId;
			this.category = category;
			this.endTime = endTime;
			this.winnerId = winnerId;
		}
		@Override
		public String toString() {
			return "(" + auctionId + ", " + category + ", " + endTime + ")";
		}
	}
	/**
	 * POJO
	 */
	protected class SimAuctionObject {
		int auctionId;
		int itemTypeId;
		Date endTime;
		int winnerId;
		
		public SimAuctionObject(int auctionId, int itemTypeId, Date endTime, int winnerId) {
			this.auctionId = auctionId;
			this.itemTypeId = itemTypeId;
			this.endTime = endTime;
			this.winnerId = winnerId;
		}
		@Override
		public String toString() {
			return "(" + auctionId + ", " + itemTypeId + ", " + endTime + ")";
		}
	}
	
	/**
	 * POJO
	 */
	protected class BidObject implements Comparable<BidObject>{
		int bidderId;
		int amount;
		Date time;
		
		public BidObject(int bidderId, int amount, Date time) {
			this.bidderId = bidderId;
			this.amount = amount;
			this.time = time;
		}
		@Override
		public String toString() {
			return "(" + bidderId + ", " + amount + ", " + time + ")";
		}
		
		@Override
		public int compareTo(BidObject o) {
			return this.compareTo(o);
		}
		
	}
	
	public static void printResultSet(ResultSet rs) throws SQLException {
		// for printing out the rows
		rs.beforeFirst();
		while(rs.next()) {
			for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
				System.out.print(rs.getObject(i+1) + ", ");
			}
			System.out.println();
		}
		rs.beforeFirst();
	}
	
	/**
	 * @param fractionToEnd proportion of time elapsed from the first bid to the end of the auction
	 * @return the BidPeriod the number falls in
	 */
	protected BidPeriod findBidPeriod(double fractionToEnd) {
		if (fractionToEnd < BEG_MID_BOUNDARY) {
			return BidPeriod.BEGINNING;
		} else if (fractionToEnd < MID_END_BOUNDARY) {
			return BidPeriod.MIDDLE;
		} else {
			return BidPeriod.END;
		}
	}

	protected static void removeIncompleteUserFeatures(Map<Integer, UserFeatures> userFeatures) {
		Iterator<UserFeatures> it = userFeatures.values().iterator();
		while (it.hasNext()) {
			UserFeatures uf = it.next();
			if (!uf.isComplete())
				it.remove();
		}
	}

	public abstract TreeMap<Integer, UserFeatures> build();
	public TreeMap<Integer, UserFeatures> reclustering_contructUserFeatures(int clusterId) {
		throw new NotImplementedException();
	}
}
