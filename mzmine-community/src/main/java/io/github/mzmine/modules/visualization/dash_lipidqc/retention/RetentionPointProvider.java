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

package io.github.mzmine.modules.visualization.dash_lipidqc.retention;

import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared provider used for all plotted retention datasets in the phase-1 refactor. Behavior is
 * varied by the supplied points, renderer, and dataset spec instead of provider subclasses.
 */
final class RetentionPointProvider implements PlotXYDataProvider,
    XYItemObjectProvider<RetentionPointRef> {

  private final @NotNull String seriesKey;
  private final @NotNull Color awtColor;
  private final @NotNull javafx.scene.paint.Color fxColor;
  private final @NotNull List<RetentionPointRef> points;

  RetentionPointProvider(final @NotNull String seriesKey, final @NotNull Color awtColor,
      final @NotNull List<RetentionPointRef> points) {
    this.seriesKey = seriesKey;
    this.awtColor = awtColor;
    fxColor = FxColorUtil.awtColorToFX(awtColor);
    this.points = List.copyOf(points);
  }

  @Override
  public void computeValues(final @NotNull Property<TaskStatus> status) {
    // decision: these providers are fully precomputed before the chart dataset is created.
  }

  @Override
  public double getDomainValue(final int index) {
    return points.get(index).x();
  }

  @Override
  public double getRangeValue(final int index) {
    return points.get(index).y();
  }

  @Override
  public int getValueCount() {
    return points.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public boolean isComputed() {
    return true;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public @Nullable String getLabel(final int index) {
    return points.get(index).label();
  }

  @Override
  public @Nullable String getToolTipText(final int itemIndex) {
    return points.get(itemIndex).tooltip();
  }

  @Override
  public @NotNull Color getAWTColor() {
    return awtColor;
  }

  @Override
  public @NotNull javafx.scene.paint.Color getFXColor() {
    return fxColor;
  }

  @Override
  public @Nullable RetentionPointRef getItemObject(final int item) {
    return item >= 0 && item < points.size() ? points.get(item) : null;
  }
}
