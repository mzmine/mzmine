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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class FeatureImageProvider implements PlotXYZDataProvider {

  private final ModularFeature feature;
  private final IonTimeSeries<ImagingScan> series;
  private double width;
  private double height;

  public FeatureImageProvider(ModularFeature feature) {
    this.feature = feature;
    series = (IonTimeSeries<ImagingScan>) feature.getFeatureData();
  }


  @NotNull
  @Override
  public Color getAWTColor() {
    return feature.getRawDataFile().getColorAWT();
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return feature.getRawDataFile().getColor();
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return null;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return FeatureUtils.featureToString(feature);
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    ImagingParameters imagingParam = ((ImagingRawDataFile) feature.getRawDataFile())
        .getImagingParam();
    height = imagingParam.getLateralHeight() / imagingParam.getMaxNumberOfPixelY();
    width = imagingParam.getLateralWidth() / imagingParam.getMaxNumberOfPixelX();
  }

  @Override
  public double getDomainValue(int index) {
    return series.getSpectra().get(index).getCoordinates().getX() * width;
  }

  @Override
  public double getRangeValue(int index) {
    return series.getSpectra().get(index).getCoordinates().getY() * height;
  }

  @Override
  public int getValueCount() {
    return series.getSpectra().size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public double getZValue(int index) {
    return series.getIntensity(index);
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return height;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return width;
  }
}
