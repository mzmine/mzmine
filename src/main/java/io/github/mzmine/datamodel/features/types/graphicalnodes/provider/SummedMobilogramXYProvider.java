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

package io.github.mzmine.datamodel.features.types.graphicalnodes.provider;

import io.github.mzmine.datamodel.IonMobilityTimeSeries;
import io.github.mzmine.datamodel.MsTimeSeries;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SummedIonMobilitySeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SummedMobilogramXYProvider implements PlotXYDataProvider {

  private static NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final ModularFeature f;
  private final SummedIonMobilitySeries data;

  public SummedMobilogramXYProvider(final ModularFeature f) {
    this.f = f;
    MsTimeSeries<? extends Scan> series = f.getFeatureData();
    if (!(series instanceof IonMobilityTimeSeries)) {
      throw new IllegalArgumentException(
          "Feature does not possess an IonMobilityTime series, cannot create mobilogram chart");
    }
    data = ((IonMobilityTimeSeries) series).getSummedMobilogram();
  }

  @Nonnull
  @Override
  public Color getAWTColor() {
    return f.getRawDataFile().getColorAWT();
  }

  @Nonnull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return f.getRawDataFile().getColor();
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nonnull
  @Override
  public Comparable<?> getSeriesKey() {
    return "m/z " + mzFormat.format(f.getMZ());
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
    return data.getMobility(index);
  }

  @Override
  public double getRangeValue(int index) {
    return data.getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return data.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 0;
  }
}
