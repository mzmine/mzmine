package io.github.mzmine.modules.dataprocessing.masscalibration.errormodelling;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.ChartUtils;

public class DistributionPlot extends ApplicationFrame
{

	ArrayList<Double> errors;
	HashMap<String, Double> lines;
	JFreeChart chart;

	public DistributionPlot(String title, ArrayList<Double> errors, HashMap<String, Double> lines)
	{
		super(title);
		this.errors = errors;
		this.lines = lines;
		chart = createChart(title);
		ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		setContentPane(panel);
	}

	protected JFreeChart createChart(String title)
	{
		XYDataset dataset = createDataset();
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
		NumberAxis xAxis = new NumberAxis("Match number");
		NumberAxis yAxis = new NumberAxis("PPM error");
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

		for(String label: lines.keySet())
		{
			Double line = lines.get(label);
			ValueMarker valueMarker = new ValueMarker(line);
			valueMarker.setPaint(Color.black);
			valueMarker.setLabel(label);
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

		return new XYSeriesCollection(errorsXY);
	}

	public static void main(String title, ArrayList<Double> errors, HashMap<String, Double> lines) throws Exception
	{
		DistributionPlot plot = new DistributionPlot(title, errors, lines);
		plot.pack();
		RefineryUtilities.centerFrameOnScreen(plot);
		plot.setVisible(true);
		ChartUtils.saveChartAsPNG(new File(title.replace(' ', '-') + ".png"), plot.chart, 1600, 1200);
	}

}
