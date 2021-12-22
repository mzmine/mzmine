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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Used in {@link SpectraPlot} to unify multiple MS2 (MS3, ...) scans into one spectrum.
 */
public class UnifyScansDataSet extends AbstractXYDataset implements IntervalXYDataset {

  private static final long serialVersionUID = 1L;

  // For comparing small differences.
  private static final double EPSILON = 0.0000001;

  private final String label;
  private final List<Scan> scans;
  private final Map<Integer, String> annotation = new Hashtable<>();
  private final Map<Double, String> mzAnnotationMap = new Hashtable<>();
  private final boolean allSameMsLevel;

  /*
   * Save a local copy of m/z and intensity values, because accessing the scan every time may cause
   * reloading the data from HDD
   */
  // private DataPoint dataPoints[];

  public UnifyScansDataSet(List<Scan> scans) {
    this.scans = scans;

    allSameMsLevel = scans.stream().allMatch(s -> s.getMSLevel() == scans.get(0).getMSLevel());

    if (allSameMsLevel) {
      this.label = String.format("%d Scans (MS%d)", scans.size(), scans.get(0).getMSLevel());
    } else {
      this.label = String.format("%d Scans", scans.size());
    }

  }

  @Override
  public int getSeriesCount() {
    return scans.size();
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return label;
  }

  @Override
  public int getItemCount(int series) {
    return scans.get(series).getNumberOfDataPoints();
  }

  @Override
  public Number getX(int series, int item) {
    return scans.get(series).getMzValue(item);
  }

  @Override
  public Number getY(int series, int item) {
    return scans.get(series).getIntensityValue(item);
  }

  @Override
  public Number getEndX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getEndXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getEndYValue(int series, int item) {
    return getYValue(series, item);
  }

  @Override
  public Number getStartX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getStartXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getStartYValue(int series, int item) {
    return getYValue(series, item);
  }

  /**
   * This function finds highest data point intensity in given m/z range. It is important for
   * normalizing isotope patterns.
   */
  public double getHighestIntensity(int series, Range<Double> mzRange) {
    return series >= 0 && series < scans.size() ? ScanUtils.findBasePeak(scans.get(series), mzRange)
        .getIntensity() : 0d;
  }

  public List<Scan> getScans() {
    return scans;
  }

}
