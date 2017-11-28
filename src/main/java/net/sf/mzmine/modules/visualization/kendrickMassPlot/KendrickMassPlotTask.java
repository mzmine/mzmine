package net.sf.mzmine.modules.visualization.kendrickMassPlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class KendrickMassPlotTask  extends AbstractTask{
	/**
	 * 
	 */
	static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
	static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private XYDataset dataset2D;
	private XYZDataset dataset3D;
	private JFreeChart chart;
	private PeakList peakList;
	private String title;
	private String xAxisLabel;
	private String yAxisLabel;
	private String zAxisLabel;
	private ParameterSet parameterSet;
	private int totalSteps = 3, appliedSteps = 0;


	public KendrickMassPlotTask(ParameterSet parameters) {
		peakList = parameters
				.getParameter(KendrickMassPlotParameters.peakList).getValue()
				.getMatchingPeakLists()[0];

		title = "Kendrick mass plot [" + peakList + "]";
		xAxisLabel = parameters
				.getParameter(KendrickMassPlotParameters.xAxisValues)
				.getValue();
		yAxisLabel = parameters
				.getParameter(KendrickMassPlotParameters.yAxisValues)
				.getValue();
		zAxisLabel = parameters
				.getParameter(KendrickMassPlotParameters.zAxisValues)
				.getValue();

		parameterSet = parameters;
	}

	@Override
	public String getTaskDescription() {
		return "Create Kendrick mass plot for " + peakList;
	}

	@Override
	public double getFinishedPercentage() {
		return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
	}

	@Override
	public void run() {
		setStatus(TaskStatus.PROCESSING);
		logger.info("Create Kendrick mass plot of " + peakList);
		// Task canceled?
		if (isCanceled())
			return;

		// create dataset
		//2D, if no third dimension was selected
		if(zAxisLabel.equals("none")) {
			logger.info("Creating new 2D chart instance");
			appliedSteps++;
			
			dataset2D = new KendrickMassPlotXYDataset(parameterSet);
			
			chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel,
					dataset2D, PlotOrientation.VERTICAL, true, true, false);
//			chart = createFastScatterPlot(dataset2D);
//			debug.printTimeAndSetCurrent("Plot creation");
			
			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setBackgroundPaint(Color.WHITE);
			appliedSteps++;
			// set renderer
			XYBlockRenderer renderer = new XYBlockRenderer();
			// calc block sizes
			double maxX = plot.getDomainAxis().getRange().getUpperBound();
			double minX = plot.getDomainAxis().getRange().getLowerBound();
			double maxY = plot.getRangeAxis().getRange().getUpperBound();
			if(xAxisLabel.contains("KMD")) {
				renderer.setBlockWidth(0.002);
				renderer.setBlockHeight(0.002);
			}
			else {
				renderer.setBlockWidth(0.9);
				renderer.setBlockHeight(maxY/(maxX-minX));
			}
			
			KendrickMassPlotXYZToolTipGenerator tooltipGenerator = new KendrickMassPlotXYZToolTipGenerator(xAxisLabel, yAxisLabel, zAxisLabel);
			renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
			plot.setRenderer(renderer);
		}
		//3D, if a third dimension was selected
		else{
			logger.info("Creating new 3D chart instance");
			appliedSteps++;
			dataset3D = new KendrickMassPlotXYZDataset(parameterSet);
			
			double[] copyZValues = new double[dataset3D.getItemCount(0)];
			for (int i = 0; i < dataset3D.getItemCount(0); i++) {
				copyZValues[i] = dataset3D.getZValue(0, i);
			}
			Arrays.sort(copyZValues);
			double min = copyZValues[0];
			double max = copyZValues[copyZValues.length-1];
			chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel,
					dataset3D, PlotOrientation.VERTICAL, true, true, false);
			XYBlockRenderer renderer = new XYBlockRenderer();
			Paint[] contourColors = null;
			LookupPaintScale scale = null;

			contourColors = KendrickMassPlotPaintScales.getFullRainBowScale();
			scale = new LookupPaintScale(min, max, Color.white);

			double [] scaleValues = new double[contourColors.length];
			double delta = (max - min)/(contourColors.length -1);
			double value = min;
			for(int i=0; i<contourColors.length; i++){
				scale.add(value, contourColors[i]);
				scaleValues[i] = value;
				value = value + delta;
			}
			XYPlot plot = chart.getXYPlot();
			appliedSteps++;
			renderer.setPaintScale(scale);
			double maxX = plot.getDomainAxis().getRange().getUpperBound();
			double minX = plot.getDomainAxis().getRange().getLowerBound();
			double maxY = plot.getRangeAxis().getRange().getUpperBound();
			if(xAxisLabel.contains("KMD")) {
				renderer.setBlockWidth(0.002);
				renderer.setBlockHeight(0.002);
			}
			else {
				renderer.setBlockWidth(0.9);
				renderer.setBlockHeight(maxY/(maxX-minX));
			}
			KendrickMassPlotXYZToolTipGenerator tooltipGenerator = new KendrickMassPlotXYZToolTipGenerator(xAxisLabel, yAxisLabel, zAxisLabel);
			renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
			plot.setRenderer(renderer);
			plot.setBackgroundPaint(Color.white);
			plot.setDomainGridlinesVisible(false);
			plot.setRangeGridlinePaint(Color.white);
			plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
			plot.setOutlinePaint(Color.black);
			plot.setBackgroundPaint(Color.white);
			//Legend
			NumberAxis scaleAxis = new NumberAxis(zAxisLabel);
			scaleAxis.setRange(min, max);
			scaleAxis.setAxisLinePaint(Color.white);
			scaleAxis.setTickMarkPaint(Color.white);
			PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);//new PaintScaleLegend(new GrayPaintScale(), scaleAxis);
			legend.setStripOutlineVisible(false);
			legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
			legend.setAxisOffset(5.0);
			legend.setMargin(new RectangleInsets(5, 5, 5, 5));
			legend.setFrame(new BlockBorder(Color.white));
			legend.setPadding(new RectangleInsets(10, 10, 10, 10));
			legend.setStripWidth(10);
			legend.setPosition(RectangleEdge.LEFT);
			legend.getAxis().setLabelFont(legendFont);
			legend.getAxis().setTickLabelFont(legendFont);
			chart.addSubtitle(legend);
			appliedSteps++;
			
		}
		
		chart.setBackgroundPaint(Color.white);

		//Create Frame
		JFrame frame = new JFrame();
		// create chart JPanel
		ChartPanel chartPanel = new ChartPanel(chart);
		frame.add(chartPanel, BorderLayout.CENTER);

		// set title properties
		TextTitle chartTitle = chart.getTitle();
		chartTitle.setMargin(5, 0, 0, 0);
		chartTitle.setFont(titleFont);
		LegendTitle legend = chart.getLegend();
		legend.setItemFont(legendFont);
		legend.setBorder(0, 0, 0, 0);
		legend.setVisible(false);


		frame.setTitle(title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBackground(Color.white);
		// Add the Windows menu
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new WindowsMenu());
		frame.setJMenuBar(menuBar);
		
		frame.setVisible(true);
		frame.pack();
		setStatus(TaskStatus.FINISHED);
		logger.info("Finished creating Kendrick mass plot of " + peakList);

	}


	private JFreeChart createFastScatterPlot(XYDataset dataset) {
		final NumberAxis domainAxis = new NumberAxis("X");
        domainAxis.setAutoRangeIncludesZero(false);
        final NumberAxis rangeAxis = new NumberAxis("Y");
        rangeAxis.setAutoRangeIncludesZero(false);
        
        float[][] data = new float[2][dataset.getItemCount(0)];
        for (int i = 0; i < dataset.getItemCount(0); i++) {
			data[0][i] = (float) dataset.getXValue(0, i);
			data[1][i] = (float) dataset.getYValue(0, i);
		}
        final FastScatterPlot plot = new FastScatterPlot(data, domainAxis, rangeAxis);
        final JFreeChart chart = new JFreeChart("Fast Scatter Plot", plot);
//        chart.setLegend(null);

        // force aliasing of the rendered content..
        chart.getRenderingHints().put
            (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return chart;
	}
}
