/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.KendrickMassPlotToolTipGenerator;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.NameItemLabelGenerator;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.XYBlockPixelSizePaintScales;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.XYBlockPixelSizeRenderer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Task to create a Kendrick mass plot of selected features of a selected feature list
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private XYDataset dataset2D;
  private XYZDataset dataset3D;
  private JFreeChart chart;
  private PeakList peakList;
  private String title;
  private String xAxisLabel;
  private String yAxisLabel;
  private String zAxisLabel;
  private String zAxisScaleType;
  private Range<Double> zScaleRange;
  private String paintScaleStyle;
  private PeakListRow rows[];
  private ParameterSet parameterSet;
  private int totalSteps = 3, appliedSteps = 0;

  public KendrickMassPlotTask(ParameterSet parameters) {
    peakList = parameters.getParameter(KendrickMassPlotParameters.peakList).getValue()
        .getMatchingPeakLists()[0];

    title = "Kendrick mass plot [" + peakList + "]";
    xAxisLabel = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    yAxisLabel = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue();
    zAxisLabel = parameters.getParameter(KendrickMassPlotParameters.zAxisValues).getValue();
    zAxisScaleType = parameters.getParameter(KendrickMassPlotParameters.zScaleType).getValue();
    zScaleRange = parameters.getParameter(KendrickMassPlotParameters.zScaleRange).getValue();
    paintScaleStyle = parameters.getParameter(KendrickMassPlotParameters.paintScale).getValue();
    rows = parameters.getParameter(IntensityPlotParameters.selectedRows).getMatchingRows(peakList);
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

    /**
     * create dataset 2D, if no third dimension was selected
     */
    if (zAxisLabel.equals("none")) {
      logger.info("Creating new 2D chart instance");
      appliedSteps++;

      // load dataset
      dataset2D = new KendrickMassPlotXYDataset(parameterSet);

      // create chart
      chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset2D,
          PlotOrientation.VERTICAL, true, true, false);

      XYPlot plot = (XYPlot) chart.getPlot();
      plot.setBackgroundPaint(Color.WHITE);
      appliedSteps++;

      // set renderer
      // XYBlockRenderer renderer = new XYBlockRenderer();
      XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();
      // calc block sizes
      double maxX = plot.getDomainAxis().getRange().getUpperBound();
      double maxY = plot.getRangeAxis().getRange().getUpperBound();

      if (xAxisLabel.contains("KMD")) {
        renderer.setBlockWidth(0.001);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));
      } else {
        renderer.setBlockWidth(1);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));
      }
      // set tooltip generator
      KendrickMassPlotToolTipGenerator tooltipGenerator = new KendrickMassPlotToolTipGenerator(

          xAxisLabel, yAxisLabel, zAxisLabel, rows);
      renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
      plot.setRenderer(renderer);
      // set item label generator
      NameItemLabelGenerator generator = new NameItemLabelGenerator(rows);
      renderer.setDefaultItemLabelGenerator(generator);
      renderer.setDefaultItemLabelsVisible(false);
      renderer.setDefaultItemLabelFont(legendFont);
    }
    // 3D, if a third dimension was selected
    else {
      logger.info("Creating new 3D chart instance");
      appliedSteps++;
      // load dataseta
      dataset3D = new KendrickMassPlotXYZDataset(parameterSet);

      // copy and sort z-Values for min and max of the paint scale
      double[] copyZValues = new double[dataset3D.getItemCount(0)];
      for (int i = 0; i < dataset3D.getItemCount(0); i++) {
        copyZValues[i] = dataset3D.getZValue(0, i);
      }
      Arrays.sort(copyZValues);
      // get index in accordance to percentile windows
      int minScaleIndex = 0;
      int maxScaleIndex = copyZValues.length - 1;
      double min = 0;
      double max = 0;

      if (zAxisScaleType.equals("percentile")) {
        minScaleIndex = (int) Math.round(copyZValues.length * (zScaleRange.lowerEndpoint() / 100));
        maxScaleIndex = copyZValues.length
            - (int) (Math.ceil(copyZValues.length * ((100 - zScaleRange.upperEndpoint()) / 100)));
        if (zScaleRange.upperEndpoint() == 100) {
          maxScaleIndex = copyZValues.length - 1;
        }
        if (zScaleRange.lowerEndpoint() == 0) {
          minScaleIndex = 0;
        }
        min = copyZValues[minScaleIndex];
        max = copyZValues[maxScaleIndex];
      }
      if (zAxisScaleType.equals("custom")) {
        min = zScaleRange.lowerEndpoint();
        max = zScaleRange.upperEndpoint();
      }

      Paint[] contourColors =
          XYBlockPixelSizePaintScales.getPaintColors(zAxisScaleType, zScaleRange, paintScaleStyle);
      LookupPaintScale scale = null;
      scale = new LookupPaintScale(min, max, new Color(244, 66, 223));
      double[] scaleValues = new double[contourColors.length];
      double delta = (max - min) / (contourColors.length - 1);
      double value = min;
      for (int i = 0; i < contourColors.length; i++) {
        scale.add(value, contourColors[i]);
        scaleValues[i] = value;
        value = value + delta;
      }

      // create chart
      chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset3D,
          PlotOrientation.VERTICAL, true, true, false);
      XYPlot plot = chart.getXYPlot();
      // set renderer
      XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();
      appliedSteps++;
      // calc block sizes
      double maxX = plot.getDomainAxis().getRange().getUpperBound();
      double maxY = plot.getRangeAxis().getRange().getUpperBound();

      if (xAxisLabel.contains("KMD")) {
        renderer.setBlockWidth(0.001);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));
      } else {
        renderer.setBlockWidth(1);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));
      }
      renderer.setPaintScale(scale);

      KendrickMassPlotToolTipGenerator tooltipGenerator =
          new KendrickMassPlotToolTipGenerator(xAxisLabel, yAxisLabel, zAxisLabel, rows);
      renderer.setSeriesToolTipGenerator(0, tooltipGenerator);

      // set item label generator
      NameItemLabelGenerator generator = new NameItemLabelGenerator(rows);
      renderer.setDefaultItemLabelGenerator(generator);
      renderer.setDefaultItemLabelsVisible(false);
      renderer.setDefaultItemLabelFont(legendFont);

      plot.setRenderer(renderer);
      plot.setBackgroundPaint(Color.white);
      plot.setRangeGridlinePaint(Color.white);
      plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
      plot.setOutlinePaint(Color.black);
      plot.setBackgroundPaint(Color.white);

      // Legend
      NumberAxis scaleAxis = new NumberAxis(zAxisLabel);
      scaleAxis.setRange(min, max);
      scaleAxis.setAxisLinePaint(Color.white);
      scaleAxis.setTickMarkPaint(Color.white);
      PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

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

    // Create Kendrick mass plot Window
    KendrickMassPlotWindow frame = new KendrickMassPlotWindow(chart);
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
    frame.setVisible(true);
    frame.pack();

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating Kendrick mass plot of " + peakList);
  }

}
