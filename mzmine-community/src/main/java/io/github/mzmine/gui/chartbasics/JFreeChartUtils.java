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

package io.github.mzmine.gui.chartbasics;

import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.main.ConfigService;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

public class JFreeChartUtils {

  private enum TriangleDirection {
    UP, DOWN, LEFT, RIGHT
  }

  public static final Shape[] DEFAULT_SERIES_SHAPES = createStandardSeriesShapes();

  public static Shape defaultShape() {
    return ColoredXYShapeRenderer.defaultShape;
  }

  public static DefaultDrawingSupplier createDefaultDrawingSupplier() {
    final Color[] colors = ConfigService.getDefaultColorPalette().getColorsAWT()
        .toArray(Color[]::new);
    return new DefaultDrawingSupplier(colors, DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, DEFAULT_SERIES_SHAPES);
  }

  /**
   * @return an array of shapes for charts
   */
  public static Shape[] createStandardSeriesShapes() {
    double size = 7.0;
    double delta = size / 2.0;

    return List.of(
        // circle
        createCircle(size),
        // square
        createSquare(size),
        // up-pointing triangle
        createTriangle(size, TriangleDirection.UP),
        // diamond
        createDiamond(size),
        // X shape
        createX(size),
        // plus shape
        createPlus(size),
        // horizontal rectangle
        new Rectangle2D.Double(-delta, -delta / 2, size, size / 2),
        // vertical ellipse
        new Ellipse2D.Double(-delta / 2, -delta, size / 2, size),
        // down-pointing triangle
        createTriangle(size, TriangleDirection.DOWN),
        // vertical rectangle
        new Rectangle2D.Double(-delta / 2, -delta, size / 2, size),
        // horizontal ellipse
        new Ellipse2D.Double(-delta, -delta / 2, size, size / 2),
        // right-pointing triangle
        createTriangle(size, TriangleDirection.RIGHT),
        // left-pointing triangle
        createTriangle(size, TriangleDirection.LEFT),
        // cross shape
        createHollowPlus(size),
        // pentagon
        createPentagon(size),
        // hexagon
        createHexagon(size),
        // star shape
        createDefaultStar(size, 0, 0) //
    ).toArray(Shape[]::new);
  }

  private static Ellipse2D.@NotNull Double createCircle(double size) {
    size *= 0.92; // needs a bit smaller size
    final double delta = size / 2;
    return new Ellipse2D.Double(-delta, -delta, size, size);
  }

  private static Rectangle2D.@NotNull Double createSquare(double size) {
    size *= 0.9; // needs a bit smaller size
    final double delta = size / 2;
    return new Rectangle2D.Double(-delta, -delta, size, size);
  }

  private static @NotNull Path2D createHexagon(double size) {
    final double delta = size / 2;
    Path2D hexagon = new Path2D.Double();
    hexagon.moveTo(-delta / 2, -delta);
    hexagon.lineTo(-delta, 0);
    hexagon.lineTo(-delta / 2, delta);
    hexagon.lineTo(delta / 2, delta);
    hexagon.lineTo(delta, 0);
    hexagon.lineTo(delta / 2, -delta);
    hexagon.closePath();
    return hexagon;
  }

  private static @NotNull Path2D createPentagon(double size) {
    final double delta = size / 2;
    Path2D pentagon = new Path2D.Double();
    pentagon.moveTo(0, -delta);
    pentagon.lineTo(-delta, -delta / 3);
    pentagon.lineTo(-delta / 2, delta);
    pentagon.lineTo(delta / 2, delta);
    pentagon.lineTo(delta, -delta / 3);
    pentagon.closePath();
    return pentagon;
  }

  private static @NotNull Path2D createHollowPlus(double size) {
    // usually needs bigger
    size *= 1.1;
    final double delta = size / 2;
    Path2D cross = new Path2D.Double();
    cross.moveTo(-delta, -delta / 3);
    cross.lineTo(-delta / 3, -delta / 3);
    cross.lineTo(-delta / 3, -delta);
    cross.lineTo(delta / 3, -delta);
    cross.lineTo(delta / 3, -delta / 3);
    cross.lineTo(delta, -delta / 3);
    cross.lineTo(delta, delta / 3);
    cross.lineTo(delta / 3, delta / 3);
    cross.lineTo(delta / 3, delta);
    cross.lineTo(-delta / 3, delta);
    cross.lineTo(-delta / 3, delta / 3);
    cross.lineTo(-delta, delta / 3);
    cross.closePath();
    return cross;
  }

  private static Shape createDefaultStar(double radius, double centerX, double centerY) {
    radius *= 0.72;
    return createStar(centerX, centerY, radius / 2.63, radius, 5, Math.toRadians(-18));
  }

  private static Shape createStar(double centerX, double centerY, double innerRadius,
      double outerRadius, int numRays, double startAngleRad) {
    Path2D path = new Path2D.Double();
    double deltaAngleRad = Math.PI / numRays;
    for (int i = 0; i < numRays * 2; i++) {
      double angleRad = startAngleRad + i * deltaAngleRad;
      double ca = Math.cos(angleRad);
      double sa = Math.sin(angleRad);
      double relX = ca;
      double relY = sa;
      if ((i & 1) == 0) {
        relX *= outerRadius;
        relY *= outerRadius;
      } else {
        relX *= innerRadius;
        relY *= innerRadius;
      }
      if (i == 0) {
        path.moveTo(centerX + relX, centerY + relY);
      } else {
        path.lineTo(centerX + relX, centerY + relY);
      }
    }
    path.closePath();
    return path;
  }


  private static Shape createTriangle(double size, TriangleDirection direction) {
    double delta = size / 2.0;
    Path2D.Double path = new Path2D.Double();

    switch (direction) {
      case UP -> {
        path.moveTo(0, -delta);
        path.lineTo(delta, delta);
        path.lineTo(-delta, delta);
      }
      case DOWN -> {
        path.moveTo(-delta, -delta);
        path.lineTo(delta, -delta);
        path.lineTo(0, delta);
      }
      case RIGHT -> {
        path.moveTo(-delta, -delta);
        path.lineTo(delta, 0);
        path.lineTo(-delta, delta);
      }
      case LEFT -> {
        path.moveTo(-delta, 0);
        path.lineTo(delta, -delta);
        path.lineTo(delta, delta);
      }
    }
    path.closePath();
    return path;
  }

  private static Shape createDiamond(double size) {
    size *= 1.1; // needs a bit bigger size
    double delta = size / 2.0;
    Path2D.Double path = new Path2D.Double();
    path.moveTo(0, -delta);
    path.lineTo(delta, 0);
    path.lineTo(0, delta);
    path.lineTo(-delta, 0);
    path.closePath();
    return path;
  }

  private static Shape createX(double size) {
    // too big needs smaller
    size *= 0.9;
    double delta = size / 2.0;
    Path2D.Double path = new Path2D.Double();
    path.moveTo(-delta, -delta);
    path.lineTo(delta, delta);
    path.moveTo(delta, -delta);
    path.lineTo(-delta, delta);
    return path;
  }

  private static Shape createPlus(double size) {
    double delta = size / 2.0;
    Path2D.Double path = new Path2D.Double();
    path.moveTo(0, -delta);
    path.lineTo(0, delta);
    path.moveTo(-delta, 0);
    path.lineTo(delta, 0);
    return path;
  }

  /**
   * Plot may contain null datasets
   *
   * @return num datasets including null
   */
  public static int getDatasetCountNullable(XYPlot plot) {
    return plot.getDatasetCount();
//    return plot.getDatasets().size();
  }

  /**
   * Plot may contain null datasets
   *
   * @return num datasets EXCLUDING null
   */
  public static int getDatasetCountNotNull(XYPlot plot) {
    return plot.getDatasetCount();
  }

  /**
   * Plot may contain null datasets - find first index where dataset is null or return the
   * totalDataset num to append
   */
  public static int getNextDatasetIndex(XYPlot plot) {
    int totalDatasets = getDatasetCountNullable(plot);
    for (int i = 0; i < totalDatasets; i++) {
      if (plot.getDataset(i) == null) {
        return i;
      }
    }
    return totalDatasets;
  }


  /**
   * Removes all feature data sets.
   *
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public static void removeAllDataSetsOf(JFreeChart chart, Class<? extends XYDataset> clazz,
      boolean notify) {
    if (!(chart.getPlot() instanceof XYPlot plot)) {
      return;
    }

    plot.setNotify(false);
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset ds = plot.getDataset(i);
      if (clazz.isInstance(ds)) {
        plot.setDataset(i, null);
        plot.setRenderer(i, null);
      }
    }
    plot.setNotify(true);
    if (notify) {
      chart.fireChartChanged();
    }
  }
}
