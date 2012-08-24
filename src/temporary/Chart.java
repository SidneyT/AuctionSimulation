package temporary;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Chart<T extends Number & Comparable<?>>  {

	XYSeriesCollection dataset = new XYSeriesCollection();
	
	public void addSeries(List<T> values, String label) {
		XYSeries series = new XYSeries(label);
		for (int i = 0; i < values.size(); i++) {
			series.add((double) i/values.size(), values.get(i));
		}
		dataset.addSeries(series);
	}
	
	public void build() {
		JFreeChart chart = ChartFactory.createScatterPlot("title", "xaxis", "yAxisLabel", dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getXYPlot().setRangeAxis(new LogarithmicAxis("ln(cents)"));
//		chart.getXYPlot().setDomainAxis(new LogarithmicAxis("percentile"));
		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		
        ChartPanel chartPanel = new ChartPanel(chart);
        // default size
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        // add it to our application
        JFrame frame = new JFrame();
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
	}

	public void saveChart(JFreeChart chart) {
		String fileName = "myTimeSeriesChart.jpg";
		try {
			/**
			 * This utility saves the JFreeChart as a JPEG First Parameter: FileName Second Parameter: Chart To Save
			 * Third Parameter: Height Of Picture Fourth Parameter: Width Of Picture
			 */
			ChartUtilities.saveChartAsJPEG(new File(fileName), chart, 800, 600);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problem occurred creating chart.");
		}
	}
	
}