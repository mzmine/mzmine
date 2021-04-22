package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Copied from {@link org.jfree.chart.renderer.xy.XYBlockRenderer}. Modified to use the data set
 * color and draw pie charts.
 */
public class ColoredXYZPieRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

  private static final Logger logger = Logger.getLogger(ColoredXYZPieRenderer.class.getName());

  private ColoredXYZPieDataset<?> currentDataset = null;

  /**
   * Creates a new {@code ColoredXYZPieRenderer} instance with default attributes.
   */
  public ColoredXYZPieRenderer() {
    // default
  }


  /**
   * Returns the lower and upper bounds (range) of the x-values in the specified dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   * @see #findRangeBounds(XYDataset)
   */
  @Override
  public Range findDomainBounds(XYDataset dataset) {
    if (dataset == null) {
      return null;
    }
    Range r = DatasetUtils.findDomainBounds(dataset, false);
    if (r == null) {
      return null;
    }
    return new Range(r.getLowerBound(),
        r.getUpperBound());
  }

  /**
   * Returns the range of values the renderer requires to display all the items from the specified
   * dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   * @see #findDomainBounds(XYDataset)
   */
  @Override
  public Range findRangeBounds(XYDataset dataset) {
    if (dataset != null) {
      Range r = DatasetUtils.findRangeBounds(dataset, false);
      if (r == null) {
        return null;
      } else {
        return new Range(r.getLowerBound(),
            r.getUpperBound());
      }
    } else {
      return null;
    }
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
  public void drawItem(Graphics2D g2, XYItemRendererState state,
      Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
      ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
      int series, int item, CrosshairState crosshairState, int pass) {

    if (!(dataset instanceof ColoredXYZPieDataset<?> pieDataset) || series > 0) {
      // we draw all series at once, because we need all values at once to draw the pie correctly.
      return;
    }

    currentDataset = pieDataset;

    final double x = dataset.getXValue(series, item);
    final double y = dataset.getYValue(series, item);
    final double z = pieDataset.getZValue(series, item);
    final double pieWidth = pieDataset.getPieWidth(item);

    double cx = domainAxis.valueToJava2D(x, dataArea,
        plot.getDomainAxisEdge());
    double cy = rangeAxis.valueToJava2D(y, dataArea,
        plot.getRangeAxisEdge());

    int startAngle = 0;
    for (int seriesIndex = 0; seriesIndex < pieDataset.getSeriesCount(); seriesIndex++) {
      try {
        g2.setPaint(pieDataset.getSliceColor(seriesIndex));
        int endAngle = (int) (startAngle + pieDataset.getZValue(seriesIndex, item) * 360 / z);
        g2.fillArc((int) (cx - pieWidth / 2), (int) (cy - pieWidth / 2), (int) pieWidth,
            (int) pieDataset.getPieWidth(item), startAngle, endAngle);
        g2.setPaint(Color.BLACK);
        g2.drawArc((int) (cx - pieWidth / 2), (int) (cy - pieWidth / 2), (int) pieWidth,
            (int) pieDataset.getPieWidth(item), startAngle, endAngle);
        startAngle = endAngle;
      } catch (ClassCastException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e,
            () -> "The identifier delivered by the dataset could not be cast to the required generic type.");
        return;
      }
    }

    final PlotOrientation orientation = plot.getOrientation();

    if (isItemLabelVisible(series, item)) {
      drawItemLabel(g2, orientation, dataset, series, item,
          cx, y + pieWidth, y < 0.0);
    }

    int datasetIndex = plot.indexOf(dataset);
    double transX = domainAxis.valueToJava2D(x, dataArea,
        plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea,
        plot.getRangeAxisEdge());
    updateCrosshairValues(crosshairState, x, y, datasetIndex,
        transX, transY, orientation);

    EntityCollection entities = state.getEntityCollection();
    if (entities != null) {
      addEntity(entities, new Ellipse2D.Double(cx, cy, pieWidth, pieWidth), dataset, series, item,
          cx, cy);
    }

  }

  /**
   * Tests this {@code XYBlockRenderer} for equality with an arbitrary object.  This method returns
   * {@code true} if and only if:
   * <ul>
   * <li>{@code obj} is an instance of {@code XYBlockRenderer} (not
   *     {@code null});</li>
   * <li>{@code obj} has the same field values as this
   *     {@code XYBlockRenderer};</li>
   * </ul>
   *
   * @param obj the object ({@code null} permitted).
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ColoredXYZPieRenderer)) {
      return false;
    }
    return super.equals(obj);
  }

  /**
   * Returns a clone of this renderer.
   *
   * @return A clone of this renderer.
   * @throws CloneNotSupportedException if there is a problem creating the clone.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    ColoredXYZPieRenderer clone = new ColoredXYZPieRenderer();
    return clone;
  }

  /*@Override
  public LegendItemCollection getLegendItems() {
    final XYPlot plot = getPlot();
    if (plot == null) {
      return new LegendItemCollection();
    }
    final int index = plot.getIndexOf(this);
    final XYDataset dataset = plot.getDataset(index);
    if (!(dataset instanceof ColoredXYZPieDataset)) {
      return super.getLegendItems();
    }

    final LegendItemCollection result = new LegendItemCollection();
    final ColoredXYZPieDataset<?> ds = (ColoredXYZPieDataset<?>) dataset;
    for (int series = 0; series < ds.getSeriesCount(); series++) {
      final LegendItem item = getLegendItemForSlice(index, series);
      if (item != null) {
        result.add(item);
      }
    }
    return result;
  }*/

  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    XYDataset dataset = getPlot().getDataset(datasetIndex);
    if((dataset instanceof ColoredXYZPieDataset<?> ds)) {
      currentDataset = ds;
    }
    return super.getLegendItem(datasetIndex, series);

    /*final String label = ds.getSeriesKey(series).toString();
    String toolTipText = null;
    if (getLegendItemToolTipGenerator() != null) {
      toolTipText = getLegendItemToolTipGenerator().generateLabel(
          ds, series);
    }
    String urlText = null;
    if (getLegendItemURLGenerator() != null) {
      urlText = getLegendItemURLGenerator().generateLabel(ds,
          series);
    }

    Shape shape = lookupLegendShape(series);
    Paint paint = lookupSeriesPaint(series);
    LegendItem item = new LegendItem(label, paint);
    item.setToolTipText(toolTipText);
    item.setURLText(urlText);
    item.setLabelFont(lookupLegendTextFont(series));
    Paint labelPaint = lookupLegendTextPaint(series);
    if (labelPaint != null) {
      item.setLabelPaint(labelPaint);
    }
    item.setSeriesKey(ds.getSeriesKey(series));
    item.setSeriesIndex(series);
    item.setDataset(ds);
    item.setDatasetIndex(datasetIndex);

    if (getTreatLegendShapeAsLine()) {
      item.setLineVisible(true);
      item.setLine(shape);
      item.setLinePaint(paint);
      item.setShapeVisible(false);
    }
    else {
      Paint outlinePaint = lookupSeriesOutlinePaint(series);
      Stroke outlineStroke = lookupSeriesOutlineStroke(series);
      item.setOutlinePaint(outlinePaint);
      item.setOutlineStroke(outlineStroke);
    }
    return item;*/
  }

  @Override
  public Paint lookupSeriesPaint(int series) {
    if(currentDataset != null) {
      return currentDataset.getSliceColor(series);
    }
    return super.lookupSeriesPaint(series);
  }
}

