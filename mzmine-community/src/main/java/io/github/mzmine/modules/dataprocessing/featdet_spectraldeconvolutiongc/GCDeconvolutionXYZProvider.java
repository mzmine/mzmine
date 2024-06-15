/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class GCDeconvolutionXYZProvider implements PlotXYZDataProvider {

  private final List<ModularFeature> features;
  private final List<Double> xValuesSet;
  private final List<Double> yValuesSet;
  private final Double zValue;
  private final AtomicDouble progress;
  private final Random random = new Random();

  public GCDeconvolutionXYZProvider(List<ModularFeature> features, AtomicDouble progress,
      Double zValue) {
    this.features = features;
    this.progress = progress;
    this.zValue = zValue;
    xValuesSet = new ArrayList<>();
    yValuesSet = new ArrayList<>();
  }

  @Override
  public @NotNull Color getAWTColor() {
    return Color.white;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.WHITE;
  }

  @Override
  public String getLabel(int index) {
    return "m/z " + features.get(index).getMZ() + " rt " + features.get(index).getRT();
  }

  /**
   * @return The series key to label the dataset in the chart's legend.
   */
  @Override
  public Comparable<?> getSeriesKey() {
    return "m/z " + features.getFirst().getMZ() + " rt " + features.getFirst().getRT();
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "m/z " + features.get(itemIndex).getMZ() + " rt " + features.get(itemIndex).getRT();
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {

    try {
      int size = features.size();
      int fi = 0;
      for (Feature f : features) {
        xValuesSet.add((double) f.getRT());
        yValuesSet.add(f.getMZ());
        if (progress != null) {
          progress.addAndGet(1.0 / size);
        }
      }

      if (progress != null) {
        progress.set((double) fi / size);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public double getDomainValue(int index) {
    return xValuesSet.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return yValuesSet.get(index);
  }

  @Override
  public int getValueCount() {
    return xValuesSet.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return progress.doubleValue();
  }

  @Override
  public @Nullable PaintScale getPaintScale() {
    return null;
  }

  @Override
  public double getZValue(int index) {
    // Generate either -1 or 1
    int randomSign = random.nextBoolean() ? 1 : -1;
    // Return zValue plus or minus 1
    return zValue + randomSign;
  }

  @Override
  public @Nullable Double getBoxHeight() {
    return 1.0;
  }

  @Override
  public @Nullable Double getBoxWidth() {
    return 1.0;
  }
}
