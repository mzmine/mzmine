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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.text.NumberFormat;
import java.util.Arrays;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to plot a {@link SummedIntensityMobilitySeries} in an XY chart.
 *
 * @author https://github.com/SteffenHeu
 */
public class SummedMobilogramXYProvider implements PlotXYDataProvider {

  private static final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final String seriesKey;
  private final ObjectProperty<Color> color;
  private final boolean swapAxes;
  private final boolean normalize;

  private final double normalizationFactor;
  private final SummedIntensityMobilitySeries data;

  public SummedMobilogramXYProvider(final Feature f) {
    this(f, false);
  }

  public SummedMobilogramXYProvider(final Feature f, boolean swapAxes) {
    this(f, swapAxes, false, null);
  }

  public SummedMobilogramXYProvider(final Feature f, boolean swapAxes, boolean normalize,
      @Nullable Double normalizationFactor) {
    IonTimeSeries<? extends Scan> series = f.getFeatureData();
    if (!(series instanceof IonMobilogramTimeSeries)) {
      throw new IllegalArgumentException(
          "Feature does not possess an IonMobilogramTimeSeries, cannot create mobilogram chart");
    }
    var data = ((IonMobilogramTimeSeries) series).getSummedMobilogram();
    var color = new SimpleObjectProperty<>(f.getRawDataFile().getColor());
    var seriesKey = "m/z " + mzFormat.format(f.getMZ());
    this(data, color, seriesKey, swapAxes, normalize, normalizationFactor);
  }

  public SummedMobilogramXYProvider(SummedIntensityMobilitySeries summedMobilogram,
      ObjectProperty<Color> color, String seriesKey) {
    this(summedMobilogram, color, seriesKey, false, false, null);
  }

  public SummedMobilogramXYProvider(SummedIntensityMobilitySeries summedMobilogram,
      ObjectProperty<Color> color, String seriesKey, boolean swapAxes, boolean normalize,
      @Nullable Double normalizationFactor) {
    this.seriesKey = seriesKey;
    this.color = color;
    this.data = summedMobilogram;
    this.swapAxes = swapAxes;
    this.normalize = normalize;
    if (normalize) {
      // use normalization factor or max intensity of data
      this.normalizationFactor = requireNonNullElse(normalizationFactor,
          Arrays.stream(data.getIntensityValues(new double[data.getNumberOfValues()])).max()
              .orElse(1d));
    } else {
      this.normalizationFactor = 1d;
    }
  }

  @NotNull
  @Override
  public java.awt.Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color.get());
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color.get();
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
  public void computeValues(Property<TaskStatus> status) {
  }

  @Override
  public double getDomainValue(int index) {
    if (swapAxes) {
      return getIntensity(index);
    }
    return data.getMobility(index);
  }

  private double getIntensity(int index) {
    return data.getIntensity(index) / normalizationFactor;
  }

  @Override
  public double getRangeValue(int index) {
    if (swapAxes) {
      return data.getMobility(index);
    }
    return getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return data.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 0;
  }

  /**
   * @return true if computed. Providers that are precomputed may use true always
   */
  public boolean isComputed() {
    return true;
  }
}
