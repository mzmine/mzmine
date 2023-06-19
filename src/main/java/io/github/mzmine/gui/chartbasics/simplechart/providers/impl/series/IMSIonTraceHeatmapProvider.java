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
import javafx.beans.property.Property;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Constructs an ion trace with a given mz and rt range from a raw data file. This trace is freshly
 * calculated from the raw data. If a dataset has already been calculated in a
 * {@link io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries}, use the
 * {@link IonTimeSeriesToXYProvider} instead.
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
   * freshly calculated from the raw data. If a dataset has already been calculated in a
   * {@link io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries}, use the
   * {@link IonMobilogramTimeSeriesToRtMobilityHeatmapProvider} instead.
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
  public void computeValues(Property<TaskStatus> status) {

    Collection<? extends Frame> eligibleFrames = rawDataFile.getFrames(1, rtRange);
    int num = 0;
    int numFrames = eligibleFrames.size();
    for (Frame frame : eligibleFrames) {
      if (status.getValue() == TaskStatus.CANCELED) {
        return;
      }
      // todo mobilityscandataaccess
      for (MobilityScan scan : frame.getMobilityScans()) {
        DataPoint[] dps = ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(scan),
            mzRange);
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
