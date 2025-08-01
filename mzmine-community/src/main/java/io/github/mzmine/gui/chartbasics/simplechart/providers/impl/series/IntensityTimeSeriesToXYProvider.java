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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntensityTimeSeriesToXYProvider implements PlotXYDataProvider {

  private final javafx.scene.paint.Color colorFx;
  @NotNull
  private final Color colorAwt;
  private final IntensityTimeSeries series;
  private final NumberFormats formats;
  private final @NotNull String seriesKey;

  public IntensityTimeSeriesToXYProvider(OtherTimeSeries series) {
    this(series, series.getOtherDataFile().getCorrespondingRawDataFile().getColorAWT());
  }

  public IntensityTimeSeriesToXYProvider(IntensityTimeSeries series, @NotNull Color colorAwt) {
    this(series, colorAwt, null);
  }

  public IntensityTimeSeriesToXYProvider(@NotNull MrmTransition transition,
      @NotNull Color colorAwt) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    this(transition.chromatogram(), colorAwt,
        "%s → %s".formatted(formats.mz(transition.q1mass()), formats.mz(transition.q3mass())));
  }

  public IntensityTimeSeriesToXYProvider(IntensityTimeSeries series, @NotNull Color colorAwt,
      @Nullable String seriesKey) {
    colorFx = FxColorUtil.awtColorToFX(colorAwt);
    this.colorAwt = colorAwt;
    this.series = series;
    formats = ConfigService.getGuiFormats();

    if (seriesKey == null) {
      if (series instanceof OtherTimeSeries other) {
        seriesKey = "%s %s".formatted(other.getChromatoogramType(), other.getName());
      } else {
        seriesKey = "%s-%s".formatted(formats.rt(series.getRetentionTime(0)),
            formats.rt(series.getRetentionTime(series.getNumberOfValues() - 1)));
      }
    }
    this.seriesKey = seriesKey;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return colorAwt;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return colorFx;
  }

  @Override
  public @Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public @Nullable String getToolTipText(int itemIndex) {
    return "Intensity: " + formats.intensity(getDomainValue(itemIndex)) + "\nRT: " + formats.rt(
        getRangeValue(itemIndex));
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    return;
  }

  @Override
  public double getDomainValue(int index) {
    return series.getRetentionTime(index);
  }

  @Override
  public double getRangeValue(int index) {
    return series.getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return series.getNumberOfValues();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  public IntensityTimeSeries getTimeSeries() {
    return series;
  }
}
