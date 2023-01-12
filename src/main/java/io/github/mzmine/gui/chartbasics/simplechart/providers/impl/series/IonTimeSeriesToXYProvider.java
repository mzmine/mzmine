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


package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorPropertyProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Can be used to plot {@link IonTimeSeries<Scan>} and extending classes as a chromatogram.
 *
 * @author https://github.com/SteffenHeu
 */
public class IonTimeSeriesToXYProvider implements PlotXYDataProvider, ColorPropertyProvider {

  private final IonTimeSeries<? extends Scan> series;
  private final String seriesKey;
  private final ObjectProperty<javafx.scene.paint.Color> color;

  public IonTimeSeriesToXYProvider(@NotNull IonTimeSeries<? extends Scan> series,
      @NotNull String seriesKey, @NotNull ObjectProperty<javafx.scene.paint.Color> color) {
    this.series = series;
    this.seriesKey = seriesKey;
    this.color = color;
  }

  public IonTimeSeriesToXYProvider(Feature f) {
    series = f.getFeatureData();
    seriesKey = FeatureUtils.featureToString(f);
    color = new SimpleObjectProperty<>(f.getRawDataFile().getColor());
  }

  @NotNull
  @Override
  public Color getAWTColor() {
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
    // no computation needed, all data is taken from the double buffers in the feature data.
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

  @Override
  public ObjectProperty<javafx.scene.paint.Color> fxColorProperty() {
    return color;
  }

  @Nullable
  public Scan getScan(int index) {
    if (index >= series.getNumberOfValues()) {
      return null;
    }
    return series.getSpectra().get(index);
  }
}
