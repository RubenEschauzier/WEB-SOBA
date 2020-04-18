package com.apporiented.algorithm.clustering;

import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ElbowPlotter extends ApplicationFrame{
	/**
	 * A demonstration application showing an XY series containing a null value.
	 *
	 * @param title  the frame title.
	 */

	public ElbowPlotter(final String title , Map<Integer, Double> elbow_data) {
	
	    super(title);
	    final XYSeries series = new XYSeries("Sum of within-cluster sum of square (WSS)");
	    for (Map.Entry<Integer, Double> entry : elbow_data.entrySet()) {
		    series.add(entry.getKey(), entry.getValue());
		    }
	    final XYSeriesCollection data = new XYSeriesCollection(series);
	    final JFreeChart chart = ChartFactory.createXYLineChart(
	        "Elbow method",
	        "Recursion depth", 
	        "Sum of WSS", 
	        data,
	        PlotOrientation.VERTICAL,
	        true,
	        true,
	        false
	    );
	
	    final ChartPanel chartPanel = new ChartPanel(chart);
	    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
	    setContentPane(chartPanel);
	
	}
	
	
}	


