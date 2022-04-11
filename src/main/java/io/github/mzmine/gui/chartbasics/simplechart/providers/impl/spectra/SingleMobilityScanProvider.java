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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Used to plot a single {@link MobilityScan} in a {@link  io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart}.
 *
 * @author https://github.com/SteffenHeu
 */
public class SingleMobilityScanProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final MobilityScan scan;
  private double finishedPercentage;

  public SingleMobilityScanProvider(MobilityScan scan) {
    this.scan = scan;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @Override
  public Color getAWTColor() {
    return scan.getFrame().getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return scan.getFrame().getDataFile().getColor();
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format(scan.getMzValue(index));
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "Frame #" + scan.getFrame().getFrameId() + " Mobility scan #"
        + scan.getMobilityScanNumber();
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Frame #" + scan.getFrame().getFrameId() + "\nMobility scan #"
        + scan.getMobilityScanNumber() + "\nMobility: " + mobilityFormat.format(scan.getMobility())
        + " " + scan.getMobilityType().getUnit() + "\nm/z: "
        + mzFormat.format(scan.getMzValue(itemIndex)) + "\nIntensity: "
        + intensityFormat.format(scan.getIntensityValue(itemIndex));

  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    finishedPercentage = 1.d;
  }

  @Override
  public double getDomainValue(int index) {
    return scan.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return scan.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return scan.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }
}
