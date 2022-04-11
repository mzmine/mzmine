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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Constructs an ion trace with a given mz and rt range from a raw data file. This trace is freshly
 * calculated from the raw data. If a dataset has already been calculated in a {@link
 * io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries}, use the {@link
 * IonTimeSeriesToXYProvider} instead.
 *
 * @author https://github.com/SteffenHeu
 */
public class IMSIonTraceHeatmapProvider implements PlotXYZDataProvider,
    MassSpectrumProvider<MobilityScan> {

  private final List<Double> rtValues;
  private final List<Double> mobilityValues;
  private final List<Double> intensityValues;
  private final List<MobilityScan> mobilityScansAtIndex;
  private final IMSRawDataFile rawDataFile;
  private final Range<Double> mzRange;
  private final Range<Float> rtRange;
  private final double noiseLevel;
  private final NumberFormat mzFormat;

  private double finishedPerecentage;

  /**
   * Constructs an ion trace with a given mz and rt range from a raw data file. This trace is
   * freshly calculated from the raw data. If a dataset has already been calculated in a {@link
   * io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries}, use the {@link
   * IonMobilogramTimeSeriesToRtMobilityHeatmapProvider} instead.
   *
   * @author https://github.com/SteffenHeu
   */
  public IMSIonTraceHeatmapProvider(IMSRawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, double noiseLevel) {
    this.rtValues = new ArrayList<>();
    this.mobilityValues = new ArrayList<>();
    this.intensityValues = new ArrayList<>();
    this.mobilityScansAtIndex = new ArrayList<>();
    this.rawDataFile = rawDataFile;
    this.mzRange = mzRange;
    this.rtRange = rtRange;
    this.noiseLevel = noiseLevel;
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    finishedPerecentage = 0;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

    Collection<? extends Frame> eligibleFrames = rawDataFile.getFrames(1, rtRange);
    int num = 0;
    int numFrames = eligibleFrames.size();
    for (Frame frame : eligibleFrames) {
      if (status.get() == TaskStatus.CANCELED) {
        return;
      }
      // todo mobilityscandataaccess
      for (MobilityScan scan : frame.getMobilityScans()) {
        DataPoint[] dps =
            ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(scan), mzRange);
        if (dps.length == 0) {
          continue;
        }
        double intensity = ScanUtils.getTIC(dps, 0d);
        if (intensity < noiseLevel) {
          continue;
        }
        rtValues.add((double) scan.getRetentionTime());
        intensityValues.add(intensity);
        mobilityValues.add(scan.getMobility());
        mobilityScansAtIndex.add(scan);
      }
      num++;
      finishedPerecentage = (double) num / numFrames;
    }
  }

  @Override
  public double getDomainValue(int index) {
    return rtValues.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return mobilityValues.get(index);
  }

  public MobilityScan getSpectrum(int index) {
    return mobilityScansAtIndex.get(index);
  }

  @Override
  public int getValueCount() {
    return rtValues.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPerecentage;
  }

  @Override
  public double getZValue(int index) {
    return intensityValues.get(index);
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

  @Override
  public Color getAWTColor() {
    return Color.black;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
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
    return rawDataFile.getName() + ": m/z " + mzFormat.format(mzRange.lowerEndpoint()) + " - "
        + mzFormat.format(mzRange.upperEndpoint());
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }
}
