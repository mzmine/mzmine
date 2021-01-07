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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleIonMobilitySeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class IonMobilityTimeSeriesXYZProvider implements PlotXYZDataProvider {

  private final ModularFeature f;
  private static NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final List<Double> rts;
  private final List<Double> mobilities;
  private final List<Double> intensities;
  private final IonMobilityTimeSeries data;
  private double progress;
  int numValues = 0;

  public IonMobilityTimeSeriesXYZProvider(final ModularFeature f) {
    if (!(f.getFeatureData() instanceof IonMobilityTimeSeries)) {
      throw new IllegalArgumentException("Cannot create IMS heatmap for non-IMS feature");
    }
    this.f = f;
    data = (IonMobilityTimeSeries) f.getFeatureData();
    rts = new ArrayList<>();
    mobilities = new ArrayList<>();
    intensities = new ArrayList<>();
    progress = 0;
  }

  @Override
  public Color getAWTColor() {
    return f.getRawDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return f.getRawDataFile().getColor();
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
    return "m/z " + mzFormat.format(f.getMZ());
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    numValues = 0;
    for(int i = 0; i < data.getMobilograms().size(); i++) {
      for (int j = 0; j < data.getMobilogram(i).getNumberOfDataPoints(); j++) {
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
    for(SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if(index >= mobilitySeries.getNumberOfDataPoints()) {
        index -= mobilitySeries.getNumberOfDataPoints();
      } else {
        return mobilitySeries.getScans().get(index).getRetentionTime();
      }
    }
    return 0;
  }

  @Override
  public double getRangeValue(int index) {
    for(SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if(index >= mobilitySeries.getNumberOfDataPoints()) {
        index -= mobilitySeries.getNumberOfDataPoints();
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
    for(SimpleIonMobilitySeries mobilitySeries : data.getMobilograms()) {
      if(index >= mobilitySeries.getNumberOfDataPoints()) {
        index -= mobilitySeries.getNumberOfDataPoints();
      } else {
        return mobilitySeries.getIntensityValue(index);
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
