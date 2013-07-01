package temporary;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
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
	
	public void addSeries2(List<int[]> values, String label) {
		XYSeries series = new XYSeries(label);
		for (int i = 0; i < values.size(); i++) {
			series.add(values.get(i)[0], values.get(i)[1]);
		}
		dataset.addSeries(series);
	}
	
	XYSeries series;
	public void start(String label) {
		series = new XYSeries(label);
	}
	public void addPoint(double x, double y) {
		series.add(x, y);
	}
	public void done() {
		dataset.addSeries(series);
	}
	
	
	public void build() {
		build("", "");
	}
	public void build(String xAxisLabel, String yAxisLabel) {
		JFreeChart chart = ChartFactory.createScatterPlot("title", "xaxis", "yAxisLabel", dataset, PlotOrientation.VERTICAL, true, true, false);
		XYPlot xyPlot = chart.getXYPlot();
		xyPlot.setDomainAxis(new LogarithmicAxis(xAxisLabel));
		xyPlot.setRangeAxis(new LogarithmicAxis(yAxisLabel));
		xyPlot.setBackgroundPaint(Color.WHITE);
		
		// change size of plotted points
		XYItemRenderer renderer = xyPlot.getRenderer();
	    renderer.setSeriesPaint(0, Color.blue);
	    double size = 2.0;
	    double delta = size / 1.0;
	    Shape shape1 = new Rectangle2D.Double(-delta, -delta, size, size);
	    renderer.setSeriesShape(0, shape1);
	    Shape shape2 = new Ellipse2D.Double(-delta, -delta, size, size);
	    renderer.setSeriesShape(1, shape2);
		
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