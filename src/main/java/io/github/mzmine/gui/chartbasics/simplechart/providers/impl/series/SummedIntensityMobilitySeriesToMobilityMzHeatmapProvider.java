/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider implements
    PlotXYZDataProvider {

  private final SummedIntensityMobilitySeries data;
  private final String seriesKey;
  private final String tooltip;
  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;
  private final double mz;
  private final ModularFeature feature;
  private Double mzwidth;

  public SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider(@NotNull final ModularFeature f) {
    data = ((IonMobilogramTimeSeries) f.getFeatureData()).getSummedMobilogram();
    feature = f;
    mz = f.getMZ();
    seriesKey = FeatureUtils.featureToString(f);

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    StringBuilder sb = new StringBuilder();
    sb.append("m/z:");
    sb.append(mzFormat.format(f.getRawDataPointsMZRange().lowerEndpoint()));
    sb.append(" - ");
    sb.append(mzFormat.format(f.getRawDataPointsMZRange().upperEndpoint()));
    sb.append("\n");
    sb.append(unitFormat.format("Retention time", "min"));
    sb.append(": ");
    sb.append(rtFormat.format(f.getRawDataPointsRTRange().lowerEndpoint()));
    sb.append(" - ");
    sb.append(rtFormat.format(f.getRawDataPointsRTRange().upperEndpoint()));
    sb.append("\n");
    if (f.getRawDataFile() instanceof IMSRawDataFile) {
      sb.append(((IMSRawDataFile) f.getRawDataFile()).getMobilityType().getAxisLabel());
      sb.append(": ");
      Range<Float> mobrange = f.get(MobilityRangeType.class);
      sb.append(mobilityFormat.format(mobrange.lowerEndpoint()));
      sb.append(" - ");
      sb.append(mobilityFormat.format(mobrange.upperEndpoint()));
      sb.append("\n");
    }
    sb.append("Height: ");
    sb.append(intensityFormat.format(f.getHeight()));
    sb.append("MS data file: ");
    sb.append(f.getRawDataFile().getName());
    tooltip = sb.toString();

    mzwidth =
        f.getRawDataPointsMZRange().upperEndpoint() - f.getRawDataPointsMZRange().lowerEndpoint();
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return Color.BLACK;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
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
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return tooltip;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

  }

  @Override
  public double getDomainValue(int index) {
    return mz;
  }

  @Override
  public double getRangeValue(int index) {
    return data.getMobility(index);
  }

  @Override
  public int getValueCount() {
    return data.getNumberOfValues();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public double getZValue(int index) {
    return data.getIntensity(index);
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return mzwidth;
  }

  @NotNull
  public SummedIntensityMobilitySeries getSourceSeries() {
    return data;
  }

  @Nullable
  public ModularFeature getSourceFeature() {
    return feature;
  }
}
