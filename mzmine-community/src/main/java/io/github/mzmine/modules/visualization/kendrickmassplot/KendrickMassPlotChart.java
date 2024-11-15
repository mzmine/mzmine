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
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.FeatureShapeChart;
import io.github.mzmine.datamodel.features.types.graphicalnodes.FeatureShapeMobilogramChart;
import io.github.mzmine.datamodel.features.types.graphicalnodes.ImageChart;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.AllowsRegionSelection;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.main.ConfigService;
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
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;

public class KendrickMassPlotChart extends EChartViewer implements AllowsRegionSelection {

  private final String colorScaleLabel;
  private final Color legendBg = new Color(0, 0, 0, 0);

  private final BooleanProperty isDrawingRegion = new SimpleBooleanProperty(false);
  private RegionSelectionListener currentRegionListener = null;
  private XYShapeAnnotation currentRegionAnnotation;


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

    PaintScaleLegend legend = generateLegend(paintScale);
    getChart().addSubtitle(legend);
    this.getChart().getXYPlot().setRenderer(renderer);

//    getMouseAdapter().addGestureHandler(
//        new ChartGestureHandler(new ChartGesture(Entity.XY_ITEM, Event.ENTERED), e -> {
//          if (e.getEntity() instanceof XYItemEntity xy) {
//            if (xy.getDataset() instanceof ColoredXYDataset data) {
//              if (data.getValueProvider() instanceof XYItemFeatureProvider provider) {
//                var row = provider.getItemObject(xy.getItem());
//                // always set even if null - will automatically show and hide
//                infoPane.setRow(row);
//                if (row != null) {
//                  createAndAddAnnotation(itemEntity, itemIndex, seriesIndex, xValue, yValue, plot,
//                      screenX, screenY);
//                }
//              }
//            }
//          }
//        }));

//    addChartMouseListener(new ChartMouseListenerFX() {
//      @Override
//      public void chartMouseClicked(ChartMouseEventFX event) {
//      }
//
//      @Override
//      public void chartMouseMoved(ChartMouseEventFX event) {
//        handleMouseMovement(event);
//      }
//
//      private void handleMouseMovement(ChartMouseEventFX event) {
//        int displayTolerance = 30;
//        int removeTolerance = 50;
//        if (true) { // TODO remove
//          return; // blank out for now
//        }
//        if (event.getEntity() instanceof XYItemEntity itemEntity) {
//          int seriesIndex = itemEntity.getSeriesIndex();
//          int itemIndex = itemEntity.getItem();
//          XYPlot plot = getChart().getXYPlot();
//
//          double xValue = itemEntity.getDataset().getXValue(seriesIndex, itemIndex);
//          double yValue = itemEntity.getDataset().getYValue(seriesIndex, itemIndex);
//
//          Rectangle2D dataArea = getCanvas().getRenderingInfo().getPlotInfo().getDataArea();
//
//          double screenX = plot.getDomainAxis()
//              .valueToJava2D(xValue, dataArea, plot.getDomainAxisEdge());
//          double screenY = plot.getRangeAxis()
//              .valueToJava2D(yValue, dataArea, plot.getRangeAxisEdge());
//
//          Point2D canvasScreenPosition = getCanvas().localToScreen(0, 0);
//
//          double localMouseX = event.getTrigger().getScreenX() - canvasScreenPosition.getX();
//          double localMouseY = event.getTrigger().getScreenY() - canvasScreenPosition.getY();
//
//          double distance = Math.sqrt(
//              Math.pow(screenX - localMouseX, 2) + Math.pow(screenY - localMouseY, 2));
//
//          // If mouse is within the display tolerance, show or update the annotation
//          if (distance <= displayTolerance || (imageBounds != null && imageBounds.contains(
//              localMouseX, localMouseY))) {
//            if (currentImageAnnotation == null) {
//              createAndAddAnnotation(itemEntity, itemIndex, seriesIndex, xValue, yValue, plot,
//                  screenX, screenY);
//            }
//          }
//          // If mouse is outside the remove tolerance and not within image bounds, remove the annotation
//          else if (distance > removeTolerance && currentImageAnnotation != null && (
//              imageBounds == null || !imageBounds.contains(localMouseX, localMouseY))) {
//            plot.removeAnnotation(currentImageAnnotation);
//            currentImageAnnotation = null;
//            imageBounds = null;
//          }
//        } else {
//          // Clear annotation if not hovering over a data point
//          if (currentImageAnnotation != null) {
//            getChart().getXYPlot().removeAnnotation(currentImageAnnotation);
//            currentImageAnnotation = null;
//            imageBounds = null;
//          }
//        }
//      }
//
//      private void createAndAddAnnotation(XYItemEntity itemEntity, int itemIndex, int seriesIndex,
//          double xValue, double yValue, XYPlot plot, double screenX, double screenY) {
//        // Create and add the annotation if it's not already displayed
//        KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) itemEntity.getDataset();
//        FeatureListRow row = dataset.getItemObject(itemIndex);
//
//        if (row != null) {
//          KendrickToolTipGenerator generator = new KendrickToolTipGenerator("m/z", "Retention Time",
//              "Intensity", "Bubble Size");
//          String tooltipText = generator.generateToolTip(itemEntity.getDataset(), seriesIndex,
//              itemIndex);
//
//          XYAnnotation currentImageAnnotation = new XYImageAnnotation(xValue, yValue, image);
//          plot.addAnnotation(currentImageAnnotation);
//          getChart().addSubtitle(new CompositeTitle(new BlockContainer()));
//          plot.addAnnotation(new TextAnnotation(xValue, yValue, tooltipText));
//
//          imageBounds = new Rectangle2D.Double(screenX - image.getWidth() / 2,
//              screenY - image.getHeight(), image.getWidth(), image.getHeight());
//        }
//      }
//
//    });
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

  /**
   * Creates a BufferedImage containing multi-line text, the best annotation, a FeatureShapeChart,
   * and conditionally a Mobilogram or Imaging chart with a background.
   */
  private BufferedImage createTooltipImageWithChart(String text, ModularFeatureListRow row) {
    AtomicDouble progress = new AtomicDouble(0.0);

    // Retrieve the best annotation, if available
    String bestAnnotation = row.getPreferredAnnotationName();
    String annotationText = bestAnnotation != null ? bestAnnotation : "";

    BufferedImage chartImage;
    if (row.hasFeatureType(ImageType.class) && row.getBestFeature() != null) {
      ImageChart imageChart = new ImageChart(row.getBestFeature(), progress);
      imageChart.getChart().getChart().setBackgroundPaint(new Color(0, 0, 0, 0));
      chartImage = imageChart.getChart().getChart().createBufferedImage(300, 200);
    } else {
      // Create the FeatureShapeChart with the row
      FeatureShapeChart featureShapeChart = new FeatureShapeChart(row, progress);
      chartImage = featureShapeChart.getChart().getChart().createBufferedImage(300, 100);
    }

    // Check for Mobilogram or Imaging data
    BufferedImage extraChartImage = null;
    if (row.hasRowType(FeatureShapeMobilogramType.class)) {
      FeatureShapeMobilogramChart mobilogramChart = new FeatureShapeMobilogramChart(row, progress);
      extraChartImage = mobilogramChart.getChart().getChart().createBufferedImage(300, 100);
    }

    // Set font and padding
    Font font = ConfigService.getConfiguration().getDefaultChartTheme().getMasterFont();
    int padding = 10;

    // Create temporary image to get font metrics
    BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D tempG2d = tempImage.createGraphics();
    tempG2d.setFont(font);
    FontMetrics metrics = tempG2d.getFontMetrics();
    int lineHeight = metrics.getHeight();
    tempG2d.dispose();

    // Split main tooltip text into lines and calculate dimensions
    String[] lines = text.split("\n");
    int mainTextWidth = metrics.stringWidth(getLongestLine(lines)) + 2 * padding;
    int mainTextHeight = lineHeight * lines.length + 2 * padding;

    // Calculate dimensions for annotation text
    int annotationTextWidth = metrics.stringWidth(annotationText) + 2 * padding;
    int textHeight = Math.max(mainTextHeight, lineHeight + 2 * padding);

    // Final image width accommodates both main and annotation texts side by side
    int width = Math.max(mainTextWidth + annotationTextWidth, chartImage.getWidth()) + 2 * padding;
    int height =
        textHeight + chartImage.getHeight() + (extraChartImage != null ? extraChartImage.getHeight()
            : 0) + 4 * padding;

    // Create final image with calculated dimensions
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    // Set rendering hints for better text quality
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Draw background
    if (ConfigService.getConfiguration().isDarkMode()) {
      // in dark mode chart background is transparent
      g2d.setColor(new Color(46, 46, 46));
    } else {
      g2d.setColor((Color) ConfigService.getConfiguration().getDefaultChartTheme()
          .getChartBackgroundPaint());
    }
    g2d.fill(new Rectangle2D.Double(0, 0, width, height));

    // Draw main text on the left
    g2d.setFont(font);
    g2d.setColor((Color) ConfigService.getConfiguration().getDefaultChartTheme().getTitlePaint());
    int y = padding + metrics.getAscent();
    for (String line : lines) {
      g2d.drawString(line, padding, y);
      y += lineHeight;
    }

    g2d.setColor((Color) ConfigService.getConfiguration().getDefaultChartTheme().getTitlePaint());
    int borderThickness = 2;
    int arcWidth = 20;
    int arcHeight = 20;

    g2d.setStroke(new BasicStroke(borderThickness));
    g2d.drawRoundRect(borderThickness / 2, borderThickness / 2, width - borderThickness,
        height - borderThickness, arcWidth, arcHeight);

    // Draw annotation text on the right, aligned with the main text
    g2d.drawString(annotationText, mainTextWidth + padding, padding + metrics.getAscent());

    // Draw the FeatureShapeChart below the text
    g2d.drawImage(chartImage, padding, textHeight + padding, null);

    // Draw the Mobilogram if present
    if (extraChartImage != null) {
      g2d.drawImage(extraChartImage, padding, textHeight + chartImage.getHeight() + 2 * padding,
          null);
    }

    g2d.dispose();
    return image;
  }


}

