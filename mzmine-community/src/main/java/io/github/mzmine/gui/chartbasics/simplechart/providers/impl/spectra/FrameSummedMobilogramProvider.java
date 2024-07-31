/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import javafx.beans.property.Property;

/**
 * Calculates a summed mobilogram of a given frame. This wrapper is intended for alignment with a
 * frame, therefore the axis are inverted. Intensity is plotted on the domain and mobility on the
 * range axis. Useage of a {@link CachedFrame} is encouraged to increase GUI responsiveness.
 *
 * @author https://github.com/SteffenHeu
 */
public class FrameSummedMobilogramProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;
  private final int binWidth;

  private double[] mobilites;
  private double[] intensities;

  private double finishedPercentage;

  /**
   * @param binWidth Number of mobility scans to be accumulated for mobilogram generation.
   */
  public FrameSummedMobilogramProvider(Frame frame, int binWidth) {
    this.frame = frame;
    this.binWidth = Math.max(1, binWidth);
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    finishedPercentage = 0d;
  }

  public FrameSummedMobilogramProvider(Frame frame) {
    this(frame, 1);
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
    if (mz != null) {
      return mzFormat.format(mz);
    }
    return null;
  }

  @Override
  public String getToolTipText(int itemIndex) {
    MobilityScan scan = frame.getSortedMobilityScans().get(itemIndex / binWidth);
    if (scan == null || scan.getBasePeakMz() == null || scan.getBasePeakIntensity() == null) {
      return null;
    }
    return "Scan #" + scan.getMobilityScanNumber() + "\nMobility: " + mobilityFormat.format(
        scan.getMobility()) + "\nBase peak m/z " + mzFormat.format(scan.getBasePeakMz())
        + "\nBase peak intensity " + intensityFormat.format(scan.getBasePeakIntensity());
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
  public void computeValues(Property<TaskStatus> status) {
    List<MobilityScan> scans = frame.getSortedMobilityScans();

    final int numScans = (scans.size() / binWidth) * binWidth;
    mobilites = new double[numScans / binWidth];
    intensities = new double[numScans / binWidth];
    for (int i = 0; i < numScans; i++) {
      final int index = i / binWidth;
      mobilites[index] += scans.get(i).getMobility() / binWidth;
      intensities[index] += scans.get(i).getTIC();
      finishedPercentage = (double) i / numScans;
    }
    finishedPercentage = 1.0;
  }
}
