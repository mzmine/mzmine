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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims.providers;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.beans.property.SimpleObjectProperty;

public class FrameSummedMobilogramProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;

  private double[] mobilites;
  private double[] intensities;

  private double finishedPercentage;

  public FrameSummedMobilogramProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    finishedPercentage = 0d;
  }

  @Override
  public double getDomainValue(int index) {
    return intensities[index];
  }

  @Override
  public double getRangeValue(int index) {
    return mobilites[index];
  }

  @Override
  public int getValueCount() {
    return mobilites.length;
  }

  @Override
  public String getLabel(int index) {
    Double mz = frame.getSortedMobilityScans().get(index).getBasePeakMz();
    if(mz != null) {
      return mzFormat.format(mz);
    }
    return null;
  }

  @Override
  public String getToolTipText(int itemIndex) {
    MobilityScan scan = frame.getSortedMobilityScans().get(itemIndex);
    if (scan == null || scan.getBasePeakIndex() == null) {
      return null;
    }
    return "Scan #" + scan.getMobilityScamNumber() + "\nMobility: "
        + mobilityFormat.format(scan.getMobility()) + "\nBase peak m/z "
        + mzFormat.format(scan.getBasePeakMz()) + "\nBase peak intensity "
        + intensityFormat.format(scan.getBasePeakIntensity());
  }

  @Override
  public Color getAWTColor() {
    return frame.getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return frame.getDataFile().getColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "Total ion mobilogram for " + frame.getDataFile().getName() + " - Frame "
        + frame.getFrameId() + " " + rtFormat.format(frame.getRetentionTime()) + " min";
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    List<MobilityScan> scans = frame.getSortedMobilityScans();

    int numScans = scans.size();
    mobilites = new double[numScans];
    intensities = new double[numScans];
    for (int i = 0; i < numScans; i++) {
      mobilites[i] = scans.get(i).getMobility();
      intensities[i] = scans.get(i).getTIC();
      finishedPercentage = (double) i / numScans;
    }
    finishedPercentage = 1.0;
  }
}
