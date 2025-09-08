/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ItemShapeProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ZLegendCategoryProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.color.ColorUtils;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Renderer that either draws outlines or filled shapes. If this renderer is used for an XYZ
 * dataset, it will try to use the paint scale of the xyz dataset, unless the
 * {@link ColoredXYShapeRenderer#ignoreZPaintScale} is set to true using the
 * {@link ColoredXYShapeRenderer#ColoredXYShapeRenderer(boolean, Shape, boolean)} constructor.
 */
public class ColoredXYShapeRenderer extends XYShapeRenderer {

  private static final int defaultSize = 7;
  public static final Shape defaultShape = new Ellipse2D.Double((double) -defaultSize / 2,
      (double) -defaultSize / 2, defaultSize, defaultSize);

  private final Shape dataPointsShape;
  private final boolean drawOutlinesOnly;
  private final BasicStroke outlineStroke = EStandardChartTheme.DEFAULT_ITEM_OUTLINE_STROKE;
  private final boolean ignoreZPaintScale;

  /**
   * @param ignoreZPaintScale If true, the paint scale of a {@link ColoredXYZDataset} is ignored and
   *                          {@link ColoredXYDataset#getAWTColor()} is used instead.
   */
  public ColoredXYShapeRenderer(boolean drawOutlinesOnly, @NotNull Shape shape,
      boolean ignoreZPaintScale) {
    super();
    this.drawOutlinesOnly = drawOutlinesOnly;
    setDrawOutlines(drawOutlinesOnly);
    setUseOutlinePaint(false); // uses the "normal" paint (from the dataset)

    this.ignoreZPaintScale = ignoreZPaintScale;
    dataPointsShape = shape;

    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
    SimpleToolTipGenerator toolTipGenerator = new SimpleToolTipGenerator();
    setDefaultToolTipGenerator(toolTipGenerator);
    setDefaultItemLabelsVisible(false);
    setSeriesItemLabelsVisible(0, false);
    setSeriesShape(0, dataPointsShape);
  }

  public ColoredXYShapeRenderer(boolean drawOutlinesOnly, @NotNull Shape shape) {
    this(drawOutlinesOnly, shape, false);
  }

  public ColoredXYShapeRenderer(boolean drawOutlinesOnly) {
    this(drawOutlinesOnly, defaultShape);
  }

  public ColoredXYShapeRenderer() {
    this(false);
  }

  @Override
  protected Paint getPaint(XYDataset dataset, int series, int item) {
    if (dataset instanceof ColoredXYZDataset zds && !ignoreZPaintScale) {
      final PaintScale ps = zds.getPaintScale();
      if (ps != null) {
        return ps.getPaint(zds.getZValue(series, item));
      }
    }
    if (dataset instanceof ColoredXYDataset ds) {
      return ds.getAWTColor();
    }
    return super.getPaint(dataset, series, item);
  }

  // need to override because the legend item does not have the correct paint otherwise.
  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    XYPlot xyplot = getPlot();
    if (xyplot == null) {
      return null;
    }
    XYDataset dataset = xyplot.getDataset(datasetIndex);
    if (dataset == null) {
      return null;
    }
    String label = getLegendItemLabelGenerator().generateLabel(dataset, series);
    String description = label;
    String toolTipText = null;
    if (getLegendItemToolTipGenerator() != null) {
      toolTipText = getLegendItemToolTipGenerator().generateLabel(dataset, series);
    }
    String urlText = null;
    if (getLegendItemURLGenerator() != null) {
      urlText = getLegendItemURLGenerator().generateLabel(dataset, series);
    }
    // shapes may be defined by
    final Shape shape;
    if (dataset instanceof ColoredXYDataset coloredDataset
        && coloredDataset.getValueProvider() instanceof ZLegendCategoryProvider shapeProvider) {
      shape = shapeProvider.getLegendCategoryShape(series);
    } else {
      shape = this.dataPointsShape;
    }

    Paint paint = (dataset instanceof ColoredXYDataset) ? getPaint(dataset, series, 0)
        : lookupSeriesPaint(series);
    LegendItem item = new LegendItem(label, paint);
    item.setToolTipText(toolTipText);
    item.setURLText(urlText);
    item.setLabelFont(lookupLegendTextFont(series));
    Paint labelPaint = lookupLegendTextPaint(series);
    if (labelPaint != null) {
      item.setLabelPaint(labelPaint);
    }
    item.setSeriesKey(dataset.getSeriesKey(series));
    item.setSeriesIndex(series);
    item.setDataset(dataset);
    item.setDatasetIndex(datasetIndex);

    if (getTreatLegendShapeAsLine() || drawOutlinesOnly) {
      item.setLineVisible(true);
      item.setLine(shape);
      item.setLinePaint(paint);
      item.setShapeVisible(false);
      item.setOutlineStroke(outlineStroke);
    } else {
      Paint outlinePaint = lookupSeriesOutlinePaint(series);
      item.setOutlinePaint(outlinePaint);
      item.setOutlineStroke(outlineStroke);
    }
    return item;
  }

  /**
   * Draws the block representing the specified item.
   *
   * @param g2             the graphics device.
   * @param state          the state.
   * @param dataArea       the data area.
   * @param info           the plot rendering info.
   * @param plot           the plot.
   * @param domainAxis     the x-axis.
   * @param rangeAxis      the y-axis.
   * @param dataset        the dataset.
   * @param series         the series index.
   * @param item           the item index.
   * @param crosshairState the crosshair state.
   * @param pass           the pass index.
   */
  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    Shape hotspot;
    EntityCollection entities = null;
    if (info != null) {
      entities = info.getOwner().getEntityCollection();
    }

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    if (Double.isNaN(x) || Double.isNaN(y)) {
      // can't draw anything
      return;
    }

    double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());

    PlotOrientation orientation = plot.getOrientation();

    // draw optional guide lines
    if ((pass == 0) && isGuideLinesVisible()) {
      g2.setStroke(getGuideLineStroke());
      g2.setPaint(this.getGuideLinePaint());
      if (orientation == PlotOrientation.HORIZONTAL) {
        g2.draw(new Line2D.Double(transY, dataArea.getMinY(), transY, dataArea.getMaxY()));
        g2.draw(new Line2D.Double(dataArea.getMinX(), transX, dataArea.getMaxX(), transX));
      } else {
        g2.draw(new Line2D.Double(transX, dataArea.getMinY(), transX, dataArea.getMaxY()));
        g2.draw(new Line2D.Double(dataArea.getMinX(), transY, dataArea.getMaxX(), transY));
      }
    } else if (pass == 1) {
      Shape shape;
      // may provide a different shape for each item
      if (dataset instanceof ColoredXYDataset coloredDataset
          && coloredDataset.getValueProvider() instanceof ItemShapeProvider shapeProvider) {
        shape = shapeProvider.getItemShape(item);
      } else {
        shape = getItemShape(series, item);
      }

      if (orientation == PlotOrientation.HORIZONTAL) {
        shape = ShapeUtils.createTranslatedShape(shape, transY, transX);
      } else if (orientation == PlotOrientation.VERTICAL) {
        shape = ShapeUtils.createTranslatedShape(shape, transX, transY);
      }
      hotspot = shape;
      if (shape.intersects(dataArea)) {
        //if (getItemShapeFilled(series, item)) {
        if (!drawOutlinesOnly) {
          g2.setPaint(getPaint(dataset, series, item));
          g2.fill(shape);
        }
        //}
        if (this.getDrawOutlines()) {
          if (getUseOutlinePaint()) {
            g2.setPaint(getPaint(dataset, series, item));
          } else {
            g2.setPaint(getPaint(dataset, series, item));
          }
          g2.setStroke(getItemOutlineStroke(series, item));
          g2.draw(shape);
        }
      }

      int datasetIndex = plot.indexOf(dataset);
      updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);

      // add an entity for the item...
      if (entities != null) {
        addEntity(entities, hotspot, dataset, series, item, 0.0, 0.0);
      }
    }
  }

  @Override
  public LegendItemCollection getLegendItems() {
    final int index = this.getPlot().getIndexOf(this);
    if (!(getPlot().getDataset(index) instanceof ColoredXYZDataset zds
        && zds.getValueProvider() instanceof ZLegendCategoryProvider zcat)) {
      return super.getLegendItems();
    }

    final EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    LegendItemCollection result = new LegendItemCollection();
    final int numCategories = zcat.getNumberOfLegendCategories();
    for (int i = 0; i < numCategories; i++) {
      final String labelText = zcat.getLegendCategoryLabel(i);
      final Paint paint = zcat.getLegendCategoryItemColor(i);
      final Shape shape = zcat.getLegendCategoryShape(i);

      final LegendItem item = new LegendItem(labelText, null, null, null, shape,
          !drawOutlinesOnly ? paint : ColorUtils.TRANSPARENT_AWT, outlineStroke,
          drawOutlinesOnly ? paint : ColorUtils.TRANSPARENT_AWT);
      theme.applyToLegendItem(item);
      result.add(item);
    }
    return result;
  }


}
