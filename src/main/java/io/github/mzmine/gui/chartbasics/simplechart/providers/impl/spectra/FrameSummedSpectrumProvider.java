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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Used to plot a 2d intensity vs m/z spectrum in a {@link io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart}.
 *
 * @author https://github.com/SteffenHeu
 */
public class FrameSummedSpectrumProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;

  private double finishedPercentage;

  public FrameSummedSpectrumProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format((frame.getMzValue(index)));
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
    return frame.getScanDefinition();
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Frame #" + frame.getFrameId() + "RT " + frame.getRetentionTime() + "\nm/z "
        + mzFormat.format(frame.getMzValue(itemIndex)) + "\nIntensity "
        + intensityFormat.format(frame.getIntensityValue(itemIndex));
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
//    dataPoints = ScanUtils.extractDataPoints(frame);
  }

  @Override
  public double getDomainValue(int index) {
    return frame.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return frame.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return frame.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1;
  }
}
