/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
      @NotNull String seriesKey,
      @NotNull ObjectProperty<javafx.scene.paint.Color> color) {
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
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
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
