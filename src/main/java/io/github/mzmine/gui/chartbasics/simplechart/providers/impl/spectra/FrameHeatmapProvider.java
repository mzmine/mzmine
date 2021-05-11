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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Used to plot a Frame in a {@link io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot}.
 * Domain axis = m/z, range axis = mobility and z = intensity. Usage of a thresholded  {@link
 * CachedFrame} is encouraged to increase responsiveness of the GUI.
 *
 * @author https://github.com/SteffenHeu
 */
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
  private final PaintScaleTransform transform;
  private double finishedPercentage;
  private PaintScale paintScale;
  private double boxHeight;

  public FrameHeatmapProvider(@Nonnull final Frame frame) {
    this(frame, null);
  }

  public FrameHeatmapProvider(@Nonnull final Frame frame,
      @Nullable final PaintScaleTransform transform) {
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

    this.transform = transform != null ? transform : PaintScaleTransform.LINEAR;
    boxHeight = 1;
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
    return paintScale;
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
    int numScans = frame.getNumberOfMobilityScans();
    double finishedScans = 0;

    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (MobilityScan mobilityScan : frame.getSortedMobilityScans()) {
      for (int i = 0; i < mobilityScan.getNumberOfDataPoints(); i++) {
        rangeValues.add(mobilityScan.getMobility());
        domainValues.add(mobilityScan.getMzValue(i));

        double z = mobilityScan.getIntensityValue(i);
        zValues.add(z);
        minZ = Math.min(z, minZ);
        maxZ = Math.max(z, maxZ);

        mobilityScanAtValueIndex.add(mobilityScan);
      }
      finishedScans++;
      finishedPercentage = finishedScans / numScans;
    }

    boxHeight = Math.abs(
        frame.getMobilityScan(numScans / 2).getMobility() - frame.getMobilityScan(numScans / 2 - 1)
            .getMobility());

    this.paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(transform,
            Range.closed(minZ, maxZ));
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
    return boxHeight;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return null;
  }
}
