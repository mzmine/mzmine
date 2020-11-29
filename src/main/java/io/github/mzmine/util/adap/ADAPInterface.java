/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package io.github.mzmine.util.adap;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAPInterface {

  public static Component getComponent(final FeatureListRow row) {
    if (row.getNumberOfFeatures() == 0)
      throw new IllegalArgumentException("No peaks found");

    NavigableMap<Double, Double> spectrum = new TreeMap<>();

    // Read Spectrum information
    IsotopePattern ip = row.getBestIsotopePattern();
    if (ip != null) {
      for (DataPoint dataPoint : ip.getDataPoints())
        spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
    }

    // Read Chromatogram
    final Feature peak = row.getBestFeature();
    final RawDataFile dataFile = peak.getRawDataFile();

    NavigableMap<Double, Double> chromatogram = new TreeMap<>();

    for (final int scan : peak.getScanNumbers()) {
      final DataPoint dataPoint = peak.getDataPoint(scan);
      if (dataPoint != null)
        chromatogram.put((double) dataFile.getScan(scan).getRetentionTime(), dataPoint.getIntensity());
    }

    return new Component(null,
        new Peak(chromatogram, new PeakInfo().mzValue(peak.getMZ()).peakID(row.getID())), spectrum,
        null);
  }

  @Nonnull
  public static ModularFeature peakToFeature(@Nonnull RawDataFile file, @Nonnull BetterPeak peak) {

    Chromatogram chromatogram = peak.chromatogram;

    // Retrieve scan numbers
    int representativeScan = 0;
    int[] scanNumbers = new int[chromatogram.length];
    int count = 0;
    for (int num : file.getScanNumbers()) {
      double retTime = file.getScan(num).getRetentionTime();
      Double intensity = chromatogram.getIntensity(retTime, false);
      if (intensity != null)
        scanNumbers[count++] = num;
      if (retTime == peak.getRetTime())
        representativeScan = num;
    }

    // Calculate peak area
    double area = 0.0;
    for (int i = 1; i < chromatogram.length; ++i) {
      double base = (chromatogram.xs[i] - chromatogram.xs[i - 1]) * 60d;
      double height = 0.5 * (chromatogram.ys[i] + chromatogram.ys[i - 1]);
      area += base * height;
    }

    // Create array of DataPoints
    DataPoint[] dataPoints = new DataPoint[chromatogram.length];
    count = 0;
    for (double intensity : chromatogram.ys)
      dataPoints[count++] = new SimpleDataPoint(peak.getMZ(), intensity);

    return new ModularFeature(file, peak.getMZ(), (float) peak.getRetTime(), (float) peak.getIntensity(),
        (float) area, scanNumbers, dataPoints, FeatureStatus.ESTIMATED, representativeScan, representativeScan,
        new int[] {}, Range.closed((float) peak.getFirstRetTime(), (float) peak.getLastRetTime()),
        Range.closed(peak.getMZ() - 0.01, peak.getMZ() + 0.01),
        Range.closed(0.f, (float) peak.getIntensity()));
  }

  @Nonnull
  public static Feature peakToFeature(@Nonnull RawDataFile file, @Nonnull Peak peak) {

    NavigableMap<Double, Double> chromatogram = peak.getChromatogram();

    double[] retTimes = new double[chromatogram.size()];
    double[] intensities = new double[chromatogram.size()];
    int index = 0;
    for (Entry<Double, Double> e : chromatogram.entrySet()) {
      retTimes[index] = e.getKey();
      intensities[index] = e.getValue();
      ++index;
    }

    BetterPeak betterPeak = new BetterPeak(peak.getInfo().peakID,
        new Chromatogram(retTimes, intensities), peak.getInfo());

    return peakToFeature(file, betterPeak);
  }
}
