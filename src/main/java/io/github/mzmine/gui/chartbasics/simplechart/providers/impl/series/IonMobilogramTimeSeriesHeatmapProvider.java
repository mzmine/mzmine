/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Provides a {@link io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset} with a
 * {@link IonMobilogramTimeSeries}. The underlying data is used, no calculations are needed.
 *
 * @author https://github.com/SteffenHeu
 */
public class IonMobilogramTimeSeriesHeatmapProvider implements PlotXYZDataProvider {

  private final IonMobilogramTimeSeries data;
  private final String seriesKey;
  private final javafx.scene.paint.Color color;
  int numValues = 0;
  private double progress;

  public IonMobilogramTimeSeriesHeatmapProvider(final ModularFeature f) {
    if (!(f.getFeatureData() instanceof IonMobilogramTimeSeries)) {
      throw new IllegalArgumentException("Cannot create IMS heatmap for non-IMS feature");
    }
    data = (IonMobilogramTimeSeries) f.getFeatureData();
    seriesKey = FeatureUtils.featureToString(f);
    color = f.getRawDataFile().getColor();
    progress = 1d;
  }

  public IonMobilogramTimeSeriesHeatmapProvider(final IonMobilogramTimeSeries data,
      final String seriesKey, final javafx.scene.paint.Color color) {
    this.data = data;
    this.seriesKey = seriesKey;
    this.color = color;
  }

  @Override
  public Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color);
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color;
  }

  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return null;
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    numValues = 0;
    for (int i = 0; i < data.getMobilograms().size(); i++) {
      for (int j = 0; j < data.getMobilogram(i).getNumberOfValues(); j++) {
        numValues++;
      }
    }

/*

    for(int i = 0; i < data.getMobilograms().size(); i++) {
      SimpleIonMobilitySeries mobilogram = data.getMobilogram(i);
      for(int j = 0; j < mobilogram.getNumberOfDataPoints(); j++) {
        rts.add((double) data.getRetentionTime(i));
        mobilities.add(mobilogram.getMobility(j));
        intensities.add(mobilogram.getIntensityValue(j));
      }
      progress = i / (double) data.getMobilograms().size();
    }
*/
  }

  @Override
  public double getDomainValue(int index) {
    for (SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getSpectra().get(index).getRetentionTime();
      }
    }
    return 0;
  }

  @Override
  public double getRangeValue(int index) {
    for (SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getMobility(index);
      }
    }
    return 0;
  }

  @Override
  public int getValueCount() {
    return numValues;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return progress;
  }

  @Override
  public double getZValue(int index) {
    for (SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getIntensity(index);
      }
    }
    return 0;
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return null;
  }
}
