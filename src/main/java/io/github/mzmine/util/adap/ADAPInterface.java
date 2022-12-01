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
package io.github.mzmine.util.adap;

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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.*;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author aleksandrsmirnov
 */
public class ADAPInterface {

  public static Component getComponent(final FeatureListRow row) {
    if (row.getNumberOfFeatures() == 0) {
      throw new IllegalArgumentException("No peaks found");
    }

    NavigableMap<Double, Double> spectrum = new TreeMap<>();

    // Read Spectrum information
    IsotopePattern ip = row.getBestIsotopePattern();
    if (ip != null) {
      for (DataPoint dataPoint : ip) {
        spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
      }
    }

    // Read Chromatogram
    final Feature peak = row.getBestFeature();
    final RawDataFile dataFile = peak.getRawDataFile();

    NavigableMap<Double, Double> chromatogram = new TreeMap<>();

    List<DataPoint> dataPoints = peak.getDataPoints();
    for (int i = 0; i < dataPoints.size(); i++) {
      final DataPoint dataPoint = dataPoints.get(i);
      if (dataPoint != null) {
        float rt = peak.getRetentionTimeAtIndex(i);
        chromatogram.put(Double.valueOf(String.valueOf(rt)), dataPoint.getIntensity());
      }
    }

    return new Component(null,
            new Peak(chromatogram, new PeakInfo().mzValue(peak.getMZ()).peakID(row.getID())), spectrum,
            null);
  }

  @NotNull
  public static ModularFeature peakToFeature(@NotNull ModularFeatureList alignedFeatureList, FeatureList originalFeatureList,
                                             @NotNull RawDataFile file, @NotNull BetterPeak peak) {

    Chromatogram chromatogram = peak.chromatogram;

    List<Scan> scanNumbers = new ArrayList<>();
    int count = 0;
    double startRetTime = chromatogram.getFirstRetTime();
    double endRetTime = chromatogram.getLastRetTimes();

    List<? extends Scan> scans = originalFeatureList.getSeletedScans(file);

    for (Scan scan : scans) {
      double retTime = scan.getRetentionTime();
      if (Math.floor(retTime*1000) < Math.floor(startRetTime*1000)) continue;
      else if (Math.floor(retTime*1000) > Math.floor(endRetTime*1000)) break;

      scanNumbers.add(scan);

      if (scanNumbers.size() == chromatogram.length)
        break;
    }

    // Calculate peak area
    double area = 0.0;
    for (int i = 1; i < chromatogram.length; ++i) {
      double base = (chromatogram.xs[i] - chromatogram.xs[i - 1]) * 60d;
      double height = 0.5 * (chromatogram.ys[i] + chromatogram.ys[i - 1]);
      area += base * height;
    }

    //Get mzs values from peak and intensities from chromatogram
    final double[] newMzs = new double[scanNumbers.size()];
    final double[] newIntensities = new double[scanNumbers.size()];
    for (int i = 0; i < newMzs.length; i++) {
      newMzs[i] = peak.getMZ();
      newIntensities[i] = chromatogram.ys[i];
    }

    SimpleIonTimeSeries simpleIonTimeSeries = new SimpleIonTimeSeries(null, newMzs, newIntensities, scanNumbers);

    return new ModularFeature(alignedFeatureList, file, simpleIonTimeSeries, FeatureStatus.ESTIMATED);

  }

  @NotNull
  public static Feature peakToFeature(@NotNull ModularFeatureList featureList,FeatureList originalFeatureList,
                                      @NotNull RawDataFile file, @NotNull Peak peak) {

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

    return peakToFeature(featureList, originalFeatureList,file, betterPeak);
  }
}