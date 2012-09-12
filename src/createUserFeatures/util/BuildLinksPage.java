package createUserFeatures.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import simulator.database.DBConnection;



public class BuildLinksPage {
	public static void main(String[] args) {
		new BuildLinksPage().buildFeedbackPageLinks();
	}
	
	private void buildFeedbackPageLinks() {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT bidderId FROM bids WHERE NOT EXISTS (SELECT userId FROM users WHERE bidderId=userId);");
			ResultSet rs = pstmt.executeQuery();
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:/links.html"));
			while (rs.next()) {
				writeMemberLink(bw, rs.getInt("bidderId"));
			}
			bw.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeMemberLink(BufferedWriter bw, int memberId) {
		try {
			bw.write("<a href=\"http://www.trademe.co.nz/Members/Feedback.aspx?member=");
			bw.write(memberId + "\"> link </a>");
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
