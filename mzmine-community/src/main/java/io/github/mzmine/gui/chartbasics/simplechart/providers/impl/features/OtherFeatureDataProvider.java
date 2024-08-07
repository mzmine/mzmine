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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
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

public class OtherFeatureDataProvider implements PlotXYDataProvider {

  private final OtherFeature feature;
  private final String seriesKey;
  private final Color awt;

  public OtherFeatureDataProvider(OtherFeature feature, Color awt) {
    this.feature = feature;
    final OtherTimeSeries timeSeries = feature.getFeatureData();
    final String name = timeSeries.getName();
    final NumberFormats formats = ConfigService.getGuiFormats();
    seriesKey = "%s %s-%s".formatted(name, formats.rt(timeSeries.getRetentionTime(0)),
        formats.rt(timeSeries.getRetentionTime(timeSeries.getNumberOfValues() - 1)));
    this.awt = awt;
  }

  public OtherFeatureDataProvider(OtherFeature feature, String seriesKey, Color awt) {
    this.feature = feature;
    this.seriesKey = seriesKey;
    this.awt = awt;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return awt;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(awt);
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
    final OtherTimeSeries timeSeries = feature.getFeatureData();
    final NumberFormats formats = ConfigService.getGuiFormats();
    return "%s: %s %s\n%s: %s %s".formatted(
        timeSeries.getTimeSeriesData().getTimeSeriesRangeLabel(),
        formats.intensity(timeSeries.getIntensity(itemIndex)),
        timeSeries.getTimeSeriesData().getTimeSeriesRangeUnit(),
        timeSeries.getTimeSeriesData().getTimeSeriesDomainLabel(),
        formats.rt(timeSeries.getRetentionTime(itemIndex)),
        timeSeries.getTimeSeriesData().getTimeSeriesDomainUnit());
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    // nothing to do
  }

  @Override
  public double getDomainValue(int index) {
    return feature.getFeatureData().getRetentionTime(index);
  }

  @Override
  public double getRangeValue(int index) {
    return feature.getFeatureData().getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return feature.getFeatureData().getNumberOfValues();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1;
  }

  public OtherFeature getFeature() {
    return feature;
  }
}
