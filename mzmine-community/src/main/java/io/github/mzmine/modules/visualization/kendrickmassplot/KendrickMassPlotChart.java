/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.AllowsRegionSelection;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.annotations.XYImageAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;

public class KendrickMassPlotChart extends EChartViewer implements AllowsRegionSelection {

  private final String colorScaleLabel;
  private final Color legendBg = new Color(0, 0, 0, 0);

  private final BooleanProperty isDrawingRegion = new SimpleBooleanProperty(false);
  private RegionSelectionListener currentRegionListener = null;
  private XYShapeAnnotation currentRegionAnnotation;
  private XYImageAnnotation currentImageAnnotation;


  public KendrickMassPlotChart(String title, String xAxisLabel, String yAxisLabel,
      String colorScaleLabel, KendrickMassPlotXYZDataset dataset) {
    super(ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, true));
    setStickyZeroRangeAxis(false);
    this.colorScaleLabel = colorScaleLabel;

    EStandardChartTheme defaultChartTheme = MZmineCore.getConfiguration().getDefaultChartTheme();
    defaultChartTheme.apply(this);
    double[] colorScaleValues = dataset.getColorScaleValues();
    final double[] quantiles = MathUtils.calcQuantile(colorScaleValues, new double[]{0.00, 1.00});
    PaintScale paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(quantiles[0], quantiles[1]));
    if (dataset.getParameters().getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .isKendrickType()) {
      getChart().getXYPlot().getRangeAxis().setRange(-0.5, 0.5);
    }
    if (dataset.getParameters().getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .isKendrickType()) {
      getChart().getXYPlot().getDomainAxis().setRange(-0.5, 0.5);
    }
    ColoredBubbleDatasetRenderer renderer = new ColoredBubbleDatasetRenderer();
    renderer.setPaintScale(paintScale);
    renderer.setDefaultToolTipGenerator(
        new KendrickToolTipGenerator(xAxisLabel, yAxisLabel, colorScaleLabel,
            dataset.getBubbleKendrickDataType().getName()));
    
    PaintScaleLegend legend = generateLegend(paintScale);
    getChart().addSubtitle(legend);
    this.getChart().getXYPlot().setRenderer(renderer);

    addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        // Optional mouse click handler
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        if (event.getEntity() instanceof XYItemEntity) {
          XYItemEntity itemEntity = (XYItemEntity) event.getEntity();
          int seriesIndex = itemEntity.getSeriesIndex();
          int itemIndex = itemEntity.getItem();
          XYPlot plot = getChart().getXYPlot();

          // Remove any existing image annotation
          if (currentImageAnnotation != null) {
            plot.removeAnnotation(currentImageAnnotation);
          }

          // Generate the tooltip text for the annotation
          KendrickToolTipGenerator generator = new KendrickToolTipGenerator("m/z", "Retention Time",
              "Intensity", "Bubble Size");
          String tooltipText = generator.generateToolTip(itemEntity.getDataset(), seriesIndex,
              itemIndex);

          // Create the image with multi-line text and background
          BufferedImage image = createTextImage(tooltipText);

          // Position the image at the data point
          double x = itemEntity.getDataset().getXValue(seriesIndex, itemIndex);
          double y = itemEntity.getDataset().getYValue(seriesIndex, itemIndex);

          // Add image annotation to the plot
          currentImageAnnotation = new XYImageAnnotation(x, y, image);
          plot.addAnnotation(currentImageAnnotation);
        } else {
          // Clear image annotation if not hovering over a data point
          if (currentImageAnnotation != null) {
            getChart().getXYPlot().removeAnnotation(currentImageAnnotation);
            currentImageAnnotation = null;
          }
        }
      }
    });
  }

  private PaintScaleLegend generateLegend(@NotNull PaintScale scale) {
    Paint axisPaint = this.getChart().getXYPlot().getDomainAxis().getAxisLinePaint();
    Font axisLabelFont = this.getChart().getXYPlot().getDomainAxis().getLabelFont();
    Font axisTickLabelFont = this.getChart().getXYPlot().getDomainAxis().getTickLabelFont();

    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(scale.getLowerBound(),
        Math.max(scale.getUpperBound(), scale.getUpperBound()));
    scaleAxis.setAxisLinePaint(axisPaint);
    scaleAxis.setTickMarkPaint(axisPaint);
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.#"));
    scaleAxis.setLabelFont(axisLabelFont);
    scaleAxis.setLabelPaint(axisPaint);
    scaleAxis.setTickLabelFont(axisTickLabelFont);
    scaleAxis.setTickLabelPaint(axisPaint);
    if (colorScaleLabel != null) {
      scaleAxis.setLabel(colorScaleLabel);
    }
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(RectangleEdge.RIGHT);
    newLegend.setBackgroundPaint(legendBg);
    return newLegend;
  }

  /**
   * Initializes a {@link RegionSelectionListener} and adds it to the plot. Following clicks will be
   * added to a region. Region selection can be finished by
   * {@link SimpleXYZScatterPlot#finishPath()}.
   */
  @Override
  public void startRegion() {
    isDrawingRegion.set(true);

    if (currentRegionListener != null) {
      removeChartMouseListener(currentRegionListener);
    }
    currentRegionListener = new RegionSelectionListener(this);
    currentRegionListener.pathProperty().addListener(((observable, oldValue, newValue) -> {
      if (currentRegionAnnotation != null) {
        getChart().getXYPlot().removeAnnotation(currentRegionAnnotation, false);
      }
      Color regionColor = new Color(0.6f, 0.6f, 0.6f, 0.4f);
      currentRegionAnnotation = new XYShapeAnnotation(newValue, new BasicStroke(1f), regionColor,
          regionColor);
      getChart().getXYPlot().addAnnotation(currentRegionAnnotation, true);
    }));
    addChartMouseListener(currentRegionListener);
  }

  /**
   * The {@link RegionSelectionListener} of the current selection. The path/points can be retrieved
   * from the listener object.
   *
   * @return The finished listener
   */
  @Override
  public RegionSelectionListener finishPath() {
    if (!isDrawingRegion.get()) {
      return null;
    }
    if (currentRegionAnnotation != null) {
      getChart().getXYPlot().removeAnnotation(currentRegionAnnotation);
    }
    isDrawingRegion.set(false);
    removeChartMouseListener(currentRegionListener);
    RegionSelectionListener tempRegionListener = currentRegionListener;
    currentRegionListener = null;
    return tempRegionListener;
  }

  /**
   * Creates a BufferedImage containing multi-line text with a background.
   */
  private BufferedImage createTextImage(String text) {

    // Set font and padding
    Font font = new Font("SansSerif", Font.PLAIN, 12);
    int padding = 10;

    // Create temporary image to get font metrics
    BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D tempG2d = tempImage.createGraphics();
    tempG2d.setFont(font);
    FontMetrics metrics = tempG2d.getFontMetrics();
    int lineHeight = metrics.getHeight();
    tempG2d.dispose();

    // Split text into lines
    String[] lines = text.split("\n");
    int width = metrics.stringWidth(getLongestLine(lines)) + 2 * padding;
    int height = lineHeight * lines.length + 2 * padding;

    // Create final image with calculated dimensions
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    // Set rendering hints for better text quality
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Set font and draw background
    g2d.setFont(font);
    g2d.setColor(new Color(0.9f, 0.9f, 0.9f, 0.8f)); // Light grey with transparency
    g2d.fill(new Rectangle2D.Double(0, 0, width, height));

    // Draw text
    g2d.setColor(Color.BLACK);
    int y = padding + metrics.getAscent();
    for (String line : lines) {
      g2d.drawString(line, padding, y);
      y += lineHeight;
    }

    g2d.dispose();
    return image;
  }


  /**
   * Helper method to get the longest line for calculating image width.
   */
  private String getLongestLine(String[] lines) {
    String longest = "";
    for (String line : lines) {
      if (line.length() > longest.length()) {
        longest = line;
      }
    }
    return longest;
  }
}

