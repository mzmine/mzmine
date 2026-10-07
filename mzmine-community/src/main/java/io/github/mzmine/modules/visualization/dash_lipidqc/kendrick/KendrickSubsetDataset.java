/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JFreeChart XYZ bubble dataset holding a predicate-filtered subset of rows from a full
 * {@link io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset},
 * with an optional Z-value override for custom colouring.
 */
public class KendrickSubsetDataset extends org.jfree.data.xy.AbstractXYZDataset
    implements XYZBubbleDataset, XYItemObjectProvider<FeatureListRow>, ToolTipTextProvider {

  private final @NotNull FeatureListRow[] rows;
  private final double @NotNull [] x;
  private final double @NotNull [] y;
  private final double @NotNull [] z;
  private final double @NotNull [] bubble;

  public KendrickSubsetDataset(final @NotNull KendrickMassPlotXYZDataset source,
      final @NotNull Predicate<FeatureListRow> predicate) {
    this(source, predicate, null);
  }

  public KendrickSubsetDataset(final @NotNull KendrickMassPlotXYZDataset source,
      final @NotNull Predicate<FeatureListRow> predicate,
      final @Nullable ToDoubleFunction<FeatureListRow> zValueOverride) {
    final List<FeatureListRow> rowList = new ArrayList<>();
    final List<Double> xList = new ArrayList<>();
    final List<Double> yList = new ArrayList<>();
    final List<Double> zList = new ArrayList<>();
    final List<Double> bubbleList = new ArrayList<>();
    for (int i = 0; i < source.getItemCount(0); i++) {
      final FeatureListRow row = source.getItemObject(i);
      if (row == null || !predicate.test(row)) {
        continue;
      }
      rowList.add(row);
      xList.add(source.getXValue(0, i));
      yList.add(source.getYValue(0, i));
      zList.add(resolveZValue(source, row, i, zValueOverride));
      bubbleList.add(source.getBubbleSizeValue(0, i));
    }
    rows = rowList.toArray(new FeatureListRow[0]);
    x = xList.stream().mapToDouble(Double::doubleValue).toArray();
    y = yList.stream().mapToDouble(Double::doubleValue).toArray();
    z = zList.stream().mapToDouble(Double::doubleValue).toArray();
    bubble = bubbleList.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private static double resolveZValue(final @NotNull KendrickMassPlotXYZDataset source,
      final @NotNull FeatureListRow row, final int itemIndex,
      final @Nullable ToDoubleFunction<FeatureListRow> zValueOverride) {
    if (zValueOverride != null) {
      final double overriddenValue = zValueOverride.applyAsDouble(row);
      if (Double.isFinite(overriddenValue)) {
        return overriddenValue;
      }
    }
    return source.getZValue(0, itemIndex);
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey(final int series) {
    return "Kendrick subset";
  }

  @Override
  public int getItemCount(final int series) {
    return rows.length;
  }

  @Override
  public @NotNull Number getX(final int series, final int item) {
    return x[item];
  }

  @Override
  public @NotNull Number getY(final int series, final int item) {
    return y[item];
  }

  @Override
  public @NotNull Number getZ(final int series, final int item) {
    return z[item];
  }

  @Override
  public double getBubbleSizeValue(final int series, final int item) {
    return bubble[item];
  }

  @Override
  public @NotNull double[] getBubbleSizeValues() {
    return bubble;
  }

  @Override
  public @Nullable FeatureListRow getItemObject(final int item) {
    return item >= 0 && item < rows.length ? rows[item] : null;
  }

  @Override
  public @Nullable String getToolTipText(final int itemIndex) {
    final FeatureListRow row = getItemObject(itemIndex);
    if (row == null) {
      return null;
    }
    final String annotation = row.getPreferredAnnotationName();
    return annotation == null ? "Feature list ID: " + row.getID()
        : "Feature list ID: " + row.getID() + "\n" + annotation;
  }
}
