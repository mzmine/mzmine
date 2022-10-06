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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.function.IntFunction;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnyXYProvider implements PlotXYDataProvider {

  private final Color awtColor;
  private final String seriesKey;
  private final int numValues;
  private final IntFunction<Double> domainFunction;
  private final IntFunction<Double> rangeFunction;

  public AnyXYProvider(Color awtColor, String seriesKey, int numValues,
      IntFunction<Double> domainFunction,
      IntFunction<Double> rangeFunction) {
    this.awtColor = awtColor;
    this.seriesKey = seriesKey;
    this.numValues = numValues;
    this.domainFunction = domainFunction;
    this.rangeFunction = rangeFunction;
  }


  @NotNull
  @Override
  public Color getAWTColor() {
    return awtColor;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(awtColor);
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    // nothint to compute
  }

  @Override
  public double getDomainValue(int index) {
    return domainFunction.apply(index);
  }

  @Override
  public double getRangeValue(int index) {
    return rangeFunction.apply(index);
  }

  @Override
  public int getValueCount() {
    return numValues;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1;
  }
}
