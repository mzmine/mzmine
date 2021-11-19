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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Provides a {@link io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset} with a
 * {@link IonMobilogramTimeSeries}. The underlying data is used, no calculations are needed. Domain
 * axis = retention time, range axis = mobility, z axis = time.
 *
 * @author https://github.com/SteffenHeu
 */
public class IonMobilogramTimeSeriesToRtMobilityHeatmapProvider implements PlotXYZDataProvider,
    PaintScaleProvider, MassSpectrumProvider<MobilityScan> {

  private final IonMobilogramTimeSeries data;
  private final String seriesKey;
  private final javafx.scene.paint.Color color;
  private final boolean isUseSingleColorPaintScale;
  int numValues = 0;
  private double progress;
  private PaintScale paintScale = null;

  public IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(final ModularFeature f) {
    if (!(f.getFeatureData() instanceof IonMobilogramTimeSeries)) {
      throw new IllegalArgumentException("Cannot create IMS heatmap for non-IMS feature");
    }
    data = (IonMobilogramTimeSeries) f.getFeatureData();
    seriesKey = FeatureUtils.featureToString(f);
    color = f.getRawDataFile().getColor();
    isUseSingleColorPaintScale = false;
    progress = 1d;
  }

  /**
   * @param data                     The data to plot.
   * @param seriesKey                The series key.
   * @param color                    A color which will be used if useSingleColorPaintScale is
   *                                 true.
   * @param useSingleColorPaintScale If true, a paint scale will be generated from the passed
   *                                 color.
   */
  public IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(final IonMobilogramTimeSeries data,
      final String seriesKey, final javafx.scene.paint.Color color,
      final boolean useSingleColorPaintScale) {
    this.data = data;
    this.seriesKey = seriesKey;
    this.color = color;
    this.isUseSingleColorPaintScale = useSingleColorPaintScale;
    progress = 1d;
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
    return paintScale;
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
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < data.getMobilograms().size(); i++) {
      numValues += data.getMobilogram(i).getNumberOfValues();
      for (int j = 0; j < data.getMobilogram(i).getNumberOfValues(); j++) {
        max = Math.max(data.getMobilogram(i).getIntensity(j), max);
      }
    }
    if (isUseSingleColorPaintScale) {
      javafx.scene.paint.Color base = javafx.scene.paint.Color.BLACK;
//          MZmineCore.getConfiguration().isDarkMode() ? javafx.scene.paint.Color.BLACK
//              : javafx.scene.paint.Color.WHITE;
      paintScale = new SimpleColorPalette(new javafx.scene.paint.Color[]{base, color}).toPaintScale(
          PaintScaleTransform.LINEAR, Range.closed(0d, max));
    }
  }

  @Override
  public double getDomainValue(int index) {
    for (IonMobilitySeries mobilitySeries : data.getMobilograms()) {
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
    for (IonMobilitySeries mobilitySeries : data.getMobilograms()) {
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
    for (IonMobilitySeries mobilitySeries : data.getMobilograms()) {
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

  @Nullable
  @Override
  public MobilityScan getSpectrum(int index) {
    for (IonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getSpectrum(index);
      }
    }
    return null;
  }


}
