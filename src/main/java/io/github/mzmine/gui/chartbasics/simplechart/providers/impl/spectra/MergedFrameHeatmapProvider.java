/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.awt.Color;
import java.util.Collection;
import javafx.beans.property.Property;
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
  public void computeValues(Property<TaskStatus> status) {
    merged = SpectraMerging.getMergedFrame(null, tolerance, frames, mobilityScanBin,
        IntensityMergingType.MAXIMUM, 100d, null, Math.min(frames.size() - 1, 5), progress);

    final int maxDp = merged.getMaxMobilityScanRawDataPoints();
    final double[] mzs = new double[maxDp];
    final double[] intensities = new double[maxDp];

    boxHeight = IonMobilityUtils.getSmallestMobilityDelta(merged);
    boxWidth = Double.MAX_VALUE;

    for (MobilityScan scan : merged.getMobilityScans()) {
      if (status.getValue() == TaskStatus.CANCELED) {
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
