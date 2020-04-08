package io.github.mzmine.testing;

import java.awt.Font;
import java.util.ArrayList;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class DistributionPlot extends ApplicationFrame
{

	ArrayList<Double> errors;
	ArrayList<Double> lines;

	public DistributionPlot(String title, ArrayList<Double> errors, ArrayList<Double> lines)
	{
		super(title);
		this.errors = errors;
		this.lines = lines;
		JFreeChart chart = createChart(title);
		ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		setContentPane(panel);
	}

	protected JFreeChart createChart(String title)
	{
		XYDataset dataset = createDataset();
		// XYItemRenderer renderer = new StandardXYItemRenderer();
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
		NumberAxis xAxis = new NumberAxis("Match number");
		NumberAxis yAxis = new NumberAxis("PPM error");
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

		for(Double line: lines)
		{
			ValueMarker valueMarker = new ValueMarker(line);
			plot.addRangeMarker(valueMarker);
		}
		
		return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
	}

	protected XYDataset createDataset()
	{
		XYSeries errorsXY = new XYSeries("PPM errors");
		for(int i = 0; i < errors.size(); i++)
		{
			errorsXY.add(i+1, errors.get(i));
		}

		// return errorsXY;
		return new XYSeriesCollection(errorsXY);
	}

	public static void main(String title, ArrayList<Double> errors, ArrayList<Double> lines)
	{
		// DistributionPlot plot = new DistributionPlot("ppm errors distribution");
		DistributionPlot plot = new DistributionPlot(title, errors, lines);
		plot.pack();
		RefineryUtilities.centerFrameOnScreen(plot);
		plot.setVisible(true);
	}

}
