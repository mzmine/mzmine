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

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.awt.Color;
import java.util.Collection;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class MergedFrameHeatmapProvider implements PlotXYZDataProvider {


  @NotNull
  private final Collection<Frame> frames;
  @NotNull
  private final MZTolerance tolerance;
  private final int mobilityScanBin;
  private final AtomicDouble progress;
  private Frame merged;
  private double boxHeight;
  private double boxWidth;
  private int numValues = 0;

  public MergedFrameHeatmapProvider(@NotNull final Collection<Frame> frames,
      @NotNull final MZTolerance tolerance, final int mobilityScanBin) {
    this.frames = frames;
    this.tolerance = tolerance;
    this.mobilityScanBin = mobilityScanBin;
    progress = new AtomicDouble(0d);
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
    return merged != null ? merged.getScanDefinition() : "Merged frame";
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    merged = SpectraMerging.getMergedFrame(frames, tolerance, null, mobilityScanBin, progress);

    final int maxDp = merged.getMaxMobilityScanRawDataPoints();
    final double mzs[] = new double[maxDp];
    final double intensities[] = new double[maxDp];

    boxHeight = IonMobilityUtils.getSmallestMobilityDelta(merged);
    boxWidth = Double.MAX_VALUE;

    for (MobilityScan scan : merged.getMobilityScans()) {
      if (status.get() == TaskStatus.CANCELED) {
        return;
      }

      scan.getMzValues(mzs);
      scan.getIntensityValues(intensities);
      numValues += scan.getNumberOfDataPoints();

      final double mzDelta = ArrayUtils.smallestDelta(mzs, scan.getNumberOfDataPoints());
      boxWidth = Math.min(mzDelta, boxWidth);
    }
  }

  @Override
  public double getDomainValue(int index) {
    for (MobilityScan scan : merged.getMobilityScans()) {
      if (index >= scan.getNumberOfDataPoints()) {
        index -= scan.getNumberOfDataPoints();
      } else {
        return scan.getMzValue(index);
      }
    }
    return 0;
  }

  @Override
  public double getRangeValue(int index) {
    for (MobilityScan scan : merged.getMobilityScans()) {
      if (index >= scan.getNumberOfDataPoints()) {
        index -= scan.getNumberOfDataPoints();
      } else {
        return scan.getMobility();
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
    return progress.get() * 0.8;
  }

  @Override
  public double getZValue(int index) {
    for (MobilityScan scan : merged.getMobilityScans()) {
      if (index >= scan.getNumberOfDataPoints()) {
        index -= scan.getNumberOfDataPoints();
      } else {
        return scan.getIntensityValue(index);
      }
    }
    return 0;
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return boxHeight;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return boxWidth;
  }
}
