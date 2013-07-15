package temporary;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

public class Chart  {

	private final String title;
	public Chart(String chartTitle) {
		this.title = chartTitle;
	}
	
	public Chart() {
		this("");
	}

	XYSeriesCollection dataset = new XYSeriesCollection();
	
	public <T extends Number & Comparable<?>> void addSeries(List<T> values, String label) {
		XYSeries series = new XYSeries(label);
		for (int i = 0; i < values.size(); i++) {
			series.add((double) i/values.size(), values.get(i));
		}
		dataset.addSeries(series);
	}
	public <T extends Number & Comparable<?>> void addSeries2(Collection<T> values, String label) {
		XYSeries series = new XYSeries(label);
		int i = 0;
		for (T value : values) {
			series.add(i++, value);
		}
		dataset.addSeries(series);
	}
	
	public void addSeries(XYSeries series) {
		dataset.addSeries(series);
	}
	
	public int addXYLine() {
		dataset.addSeries(xEqualsY());
		return dataset.getSeriesCount() - 1;
	}
	
	private static XYSeries xEqualsY() {
		XYSeries series = new XYSeries("x=y");
//		for (int i = 1; i < 240; i++) {
//			series.add(i, i);
//		}
		series.add(1, 1);
		series.add(240, 240);
		return series;
	}
	

	
//	XYSeries series;
//	public void start(String label) {
//		series = new XYSeries(label);
//	}
//	public void addPoint(double x, double y) {
//		series.add(x, y);
//	}
//	public void done() {
//		dataset.addSeries(series);
//	}
	
	
	public void build() {
		build("", "");
	}
	
	public static boolean XYLINE = true;
	public static boolean LOGAXIS = true;
	
	public void build(String xAxisLabel, String yAxisLabel) {
		JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		XYPlot xyPlot = chart.getXYPlot();
		if (LOGAXIS) {
			xyPlot.setDomainAxis(new LogarithmicAxis(xAxisLabel));
			xyPlot.setRangeAxis(new LogarithmicAxis(yAxisLabel));
		}
		xyPlot.setBackgroundPaint(Color.WHITE);
		
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		xyPlot.setRenderer(renderer);
		renderer.setBaseLinesVisible(false);
	    if (XYLINE) {
			int seriesIndex = addXYLine();
			renderer.setSeriesLinesVisible(seriesIndex, true);
			renderer.setSeriesShapesVisible(seriesIndex, false);
	    }
		
		// change size of plotted points
//		XYItemRenderer renderer = xyPlot.getRenderer();
	    renderer.setSeriesPaint(0, Color.blue);
	    double size = 1.0;
	    double delta = size / 1.0;
	    Shape shape1 = new Rectangle2D.Double(-delta/2, -delta * 2, size/2, size * 2);
	    renderer.setSeriesShape(0, shape1);
	    Shape shape2 = new Ellipse2D.Double(-delta * 4, -delta * 4, size * 4, size * 4);
	    renderer.setSeriesShape(1, shape2);
	    Shape shape3 = ShapeUtilities.createDiagonalCross(2f, 0.02f);
	    renderer.setSeriesShape(2, shape3);
	    Shape shape4 = ShapeUtilities.createRegularCross(3f, 0.02f);
	    renderer.setSeriesShape(3, shape4);
	    Shape shape5 = ShapeUtilities.createDiamond(2);
	    renderer.setSeriesShape(4, shape5);
	    Shape shape6 = ShapeUtilities.createUpTriangle(2);
	    renderer.setSeriesShape(5, shape6);
	    Shape shape7 = ShapeUtilities.createDownTriangle(2);
	    renderer.setSeriesShape(6, shape7);
	    Shape shape8 = new Rectangle2D.Double(-delta * 2, -delta/2, size * 2, size/2);
	    renderer.setSeriesShape(7, shape8);


		//save chart using chart title as filename.
		saveChart(chart);
	    
        ChartPanel chartPanel = new ChartPanel(chart);
        // default size
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 600));
        // add it to our application
        JFrame frame = new JFrame();
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
	}

	public void saveChart(JFreeChart chart) {
		String fileName = chart.getTitle().getText() + ".jpg";
		try {
			/**
			 * This utility saves the JFreeChart as a JPEG First Parameter: FileName Second Parameter: Chart To Save
			 * Third Parameter: Height Of Picture Fourth Parameter: Width Of Picture
			 */
			ChartUtilities.saveChartAsJPEG(new File(fileName), chart, 1000, 600);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problem occurred creating chart.");
		}
	}
	
}