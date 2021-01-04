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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.beans.property.SimpleObjectProperty;

public class FrameHeatmapProvider implements PlotXYZDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;

  private final List<Double> domainValues;
  private final List<Double> rangeValues;
  private final List<Double> zValues;
  private final List<MobilityScan> mobilityScanAtValueIndex;

  private double finishedPercentage;

  public FrameHeatmapProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    domainValues = new ArrayList<>();
    rangeValues = new ArrayList<>();
    zValues = new ArrayList<>();
    mobilityScanAtValueIndex = new ArrayList<>();
    finishedPercentage = 0d;
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
    return frame.getDataFile().getName() + " - Frame " + frame.getFrameId() + " "
        + rtFormat.format(frame.getRetentionTime()) + " min";
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    double numScans = frame.getNumberOfMobilityScans();
    int finishedScans = 0;
    for (MobilityScan mobilityScan : frame.getSortedMobilityScans()) {
      for (DataPoint dp : mobilityScan) {
        rangeValues.add(mobilityScan.getMobility());
        domainValues.add(dp.getMZ());
        zValues.add(dp.getIntensity());
        mobilityScanAtValueIndex.add(mobilityScan);
      }
      finishedScans++;
      finishedPercentage = finishedScans / numScans;
    }

  }

  public MobilityScan getMobilityScanAtValueIndex(int index) {
    return mobilityScanAtValueIndex.get(index);
  }

  @Override
  public double getDomainValue(int index) {
    return domainValues.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return rangeValues.get(index);
  }

  @Override
  public int getValueCount() {
    return domainValues.size();
  }

  @Override
  public double getZValue(int index) {
    return zValues.get(index);
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
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
