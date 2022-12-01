/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.Color;
import java.awt.Font;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.NameItemLabelGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.ScatterPlotToolTipGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.dialogs.FeatureOverviewWindow;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;

/**
 * Task to create a Van Krevelen Diagram of selected features of a selected feature list
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class VanKrevelenDiagramTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private JFreeChart chart;
  private FeatureList featureList;
  private String zAxisLabel;
  private PaintScale paintScaleParameter;
  private FeatureListRow rows[];
  private FeatureListRow filteredRows[];
  private String title;
  private int displayedFeatures;
  private int featuresWithoutFormula;
  private int featuresWithFormulasWithoutCHO;
  private int totalSteps = 3, appliedSteps = 0;
  private ParameterSet parameters;

  public VanKrevelenDiagramTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    featureList = parameters.getParameter(VanKrevelenDiagramParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];
    zAxisLabel =
        parameters.getParameter(VanKrevelenDiagramParameters.zAxisValues).getValue().toString();
    paintScaleParameter =
        parameters.getParameter(VanKrevelenDiagramParameters.paintScale).getValue();
    rows = parameters.getParameter(VanKrevelenDiagramParameters.selectedRows)
        .getMatchingRows(featureList);
    filteredRows = filterSelectedRows(rows);
    title = "Van Krevelen Diagram [" + featureList + "]";
  }

  @Override
  public String getTaskDescription() {
    return "Van Krevelen Diagram for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Create Van Krevelen diagram of " + featureList);
      // Task canceled?
      if (isCanceled()) {
        return;
      }
      chart = create3DVanKrevelenDiagram();
      chart.setBackgroundPaint(Color.white);

      // create chart JPanel
      EChartViewer chartViewer = new EChartViewer(chart, true, true, true, true, false);

      // get plot
      XYPlot plot = (XYPlot) chart.getPlot();

      // mouse listener
      chartViewer.addChartMouseListener(new ChartMouseListenerFX() {

        @Override
        public void chartMouseMoved(ChartMouseEventFX event) {}

        @Override
        public void chartMouseClicked(ChartMouseEventFX event) {
          double xValue = plot.getDomainCrosshairValue();
          double yValue = plot.getRangeCrosshairValue();
          if (plot.getDataset() instanceof VanKrevelenDiagramXYZDataset) {
            VanKrevelenDiagramXYZDataset dataset = (VanKrevelenDiagramXYZDataset) plot.getDataset();
            double[] xValues = new double[dataset.getItemCount(0)];
            for (int i = 0; i < xValues.length; i++) {
              if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
                  && (event.getTrigger().getClickCount() == 2)) {
                if (dataset.getX(0, i).doubleValue() == xValue
                    && dataset.getY(0, i).doubleValue() == yValue) {
                  new FeatureOverviewWindow(rows[i]);
                }
              }
            }
          }
          if (plot.getDataset() instanceof VanKrevelenDiagramXYDataset) {
            VanKrevelenDiagramXYDataset dataset = (VanKrevelenDiagramXYDataset) plot.getDataset();
            double[] xValues = new double[dataset.getItemCount(0)];
            for (int i = 0; i < xValues.length; i++) {
              if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
                  && (event.getTrigger().getClickCount() == 2)) {
                if (dataset.getX(0, i).doubleValue() == xValue
                    && dataset.getY(0, i).doubleValue() == yValue) {
                  new FeatureOverviewWindow(rows[i]);
                }
              }
            }
          }
        }
      });

      // Create Van Krevelen Diagram tab
      if(!MZmineCore.isHeadLessMode()) {
        Platform.runLater(() -> {
          VanKrevelenDiagramTab newTab = new VanKrevelenDiagramTab(parameters, chartViewer);
          MZmineCore.getDesktop().addTab(newTab);

          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle("Results summary");
          alert.setHeaderText(null);
          alert.setContentText(displayedFeatures
              + " feature list rows are displayed in the Van Krevelen diagram.\n"
              + featuresWithFormulasWithoutCHO
              + " feature list rows are not displayed, because the annotated molecular formula does not contain the elements C, H, and O.\n"
              + featuresWithoutFormula
              + " feature list rows are not displayed, because no molecular formula was assigned.");
          alert.show();
        });
      }
      logger.info("Finished creating van Krevelen diagram of " + featureList);
      setStatus(TaskStatus.FINISHED);
    } catch (

    Throwable t) {
      setErrorMessage(
          "Nothing to plot here or some peaks have other identities than molecular formulas.\n"
              + "Have you annotated your features with molecular formulas?\n"
              + "You can use the feature list method \"Formula prediction\" to handle the task.");
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * create 3D Van Krevelen Diagram chart
   */
  private JFreeChart create3DVanKrevelenDiagram() {
    logger.info("Creating new 3D chart instance");
    appliedSteps++;
    // load data set
    VanKrevelenDiagramXYZDataset dataset3D =
        new VanKrevelenDiagramXYZDataset(zAxisLabel, filteredRows);

    // copy and sort z-Values for min and max of the paint scale
    Double[] copyZValues = new Double[dataset3D.getItemCount(0)];
    for (int i = 0; i < dataset3D.getItemCount(0); i++) {
      copyZValues[i] = dataset3D.getZValue(0, i);
    }
    Arrays.sort(copyZValues);
    double min = copyZValues[0];
    double max = copyZValues[copyZValues.length - 1];
    PaintScale paintScale = createPaintScale(copyZValues);

    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(paintScale);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);

    // create chart
    chart = ChartFactory.createScatterPlot(title, "O/C", "H/C", dataset3D, PlotOrientation.VERTICAL,
        true, true, false);
    // set renderer
    XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();

    XYPlot plot = chart.getXYPlot();
    appliedSteps++;
    renderer.setPaintScale(paintScale);
    double maxX = plot.getDomainAxis().getRange().getUpperBound();
    double maxY = plot.getRangeAxis().getRange().getUpperBound();

    renderer.setBlockWidth(0.001);
    renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));

    // set tooltip generator
    ScatterPlotToolTipGenerator tooltipGenerator =
        new ScatterPlotToolTipGenerator("O/C", "H/C", zAxisLabel, filteredRows);
    renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
    renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER,
        TextAnchor.TOP_RIGHT, TextAnchor.TOP_RIGHT, -45), true);

    // set item label generator
    NameItemLabelGenerator generator = new NameItemLabelGenerator(filteredRows);
    renderer.setDefaultItemLabelGenerator(generator);
    renderer.setDefaultItemLabelsVisible(false);
    renderer.setDefaultItemLabelFont(legendFont);

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    plot.setBackgroundPaint(Color.white);
    plot.setDomainCrosshairPaint(Color.GRAY);
    plot.setRangeCrosshairPaint(Color.GRAY);
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    // Legend
    NumberAxis scaleAxis = new NumberAxis(zAxisLabel);
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend legend = new PaintScaleLegend(paintScale, scaleAxis);

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

    return chart;
  }

  /*
   * This method removes all feature list rows, that cannot be plot in a Van Krevelen diagram
   */
  private FeatureListRow[] filterSelectedRows(FeatureListRow[] selectedRows) {
    ArrayList<FeatureListRow> rows = new ArrayList<FeatureListRow>();
    for (FeatureListRow featureListRow : selectedRows) {
      boolean hasIdentity = false;
      boolean isFormula = false;
      boolean hasSuitableElements = false;

      // check for identity
      if (featureListRow.getPreferredFeatureIdentity() != null)
        hasIdentity = true;

      // check for formula
      if (hasIdentity && featureListRow.getPreferredFeatureIdentity()
          .getPropertyValue(FeatureIdentity.PROPERTY_FORMULA) != null) {
        isFormula = true;
      } else {
        featuresWithoutFormula++;
      }

      // check if formula is suitable for Van Krevelen diagram (needs
      // elements C, H, and O)
      if (isFormula) {
        String s = featureListRow.getPreferredFeatureIdentity()
            .getPropertyValue(FeatureIdentity.PROPERTY_FORMULA);
        if (s.contains("C") && s.contains("H") && s.contains("O")) {
          hasSuitableElements = true;
          displayedFeatures++;
        } else {
          logger.log(Level.WARNING,
              "Warning, " + s + " cannot be plottet in a Van_Krevelen diagram");
          featuresWithFormulasWithoutCHO++;
        }
      }

      // add featureListRow
      if (hasIdentity && isFormula && hasSuitableElements)
        rows.add(featureListRow);
    }
    if (rows.size() > 0) {
      return rows.toArray(FeatureListRow[]::new);
    } else
      return null;
  }

  private PaintScale createPaintScale(Double[] zValues) {
    Range<Double> zValueRange = Range.closed(zValues[0], zValues[zValues.length - 1]);
    return new PaintScale(paintScaleParameter.getPaintScaleColorStyle(),
        paintScaleParameter.getPaintScaleBoundStyle(), zValueRange);
  }

}
