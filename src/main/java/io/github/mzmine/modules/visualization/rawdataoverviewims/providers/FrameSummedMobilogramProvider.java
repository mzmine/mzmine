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

package io.github.mzmine.modules.visualization.rawdataoverviewims.providers;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;

public class FrameSummedMobilogramProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final CachedFrame cachedFrame;

  private final List<Double> domainValues;
  private final List<Double> rangeValues;

  private double finishedPercentage;

  public FrameSummedMobilogramProvider(CachedFrame frame) {
    cachedFrame = frame;

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    domainValues = new ArrayList<>();
    rangeValues = new ArrayList<>();
    finishedPercentage = 0d;
  }

  @Override
  public String getLabel(int index) {
    return mzFormat
        .format(cachedFrame.getSortedMobilityScans().get(index).getHighestDataPoint().getMZ());
  }

  @Override
  public String getToolTipText(int itemIndex) {
    MobilityScan scan = cachedFrame.getSortedMobilityScans().get(itemIndex);
    if (scan == null || scan.getHighestDataPoint() == null) {
      return null;
    }
    return "Scan #" + scan.getMobilityScamNumber()
        + "\nMobility: " + mobilityFormat.format(scan.getMobility())
        + "\nBase peak m/z " + mzFormat.format(scan.getHighestDataPoint().getMZ())
        + "\nBase peak intensity " + intensityFormat
        .format(scan.getHighestDataPoint().getIntensity());
  }

  @Override
  public List<Double> getDomainValues() {
    return domainValues;
  }

  @Override
  public List<Double> getRangeValues() {
    return rangeValues;
  }

  @Override
  public Color getAWTColor() {
    return cachedFrame.getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return cachedFrame.getDataFile().getColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return cachedFrame.getDataFile().getName() + " - Frame " + cachedFrame.getFrameId() + " "
        + rtFormat.format(cachedFrame.getRetentionTime()) + " min";
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    cachedFrame.getSortedMobilityScans()
        .forEach(scan -> rangeValues.add(ScanUtils.getTIC(scan.getDataPoints(), 0.d)));
    finishedPercentage = 0.5;
    cachedFrame.getSortedMobilityScans().forEach(scan -> domainValues.add(scan.getMobility()));
    finishedPercentage = 1.0;
  }
}