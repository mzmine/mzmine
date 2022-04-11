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

package io.github.mzmine.modules.dataprocessing.adap_mcr;

import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.datamodel.PeakInfo;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class ADAP3DecompositionV2Utils {
  private final Logger log;

  public ADAP3DecompositionV2Utils() {
    this.log = Logger.getLogger(ADAP3DecompositionV2Task.class.getName());
  }

  /**
   * Convert MZmine PeakList to a list of ADAP Peaks
   *
   * @param peakList MZmine PeakList object
   * @return list of ADAP Peaks
   */
  @NotNull
  public List<BetterPeak> getPeaks(@NotNull final FeatureList peakList) {
    RawDataFile dataFile = peakList.getRawDataFile(0);

    List<BetterPeak> peaks = new ArrayList<>();

    for (FeatureListRow row : peakList.getRows()) {
      Feature peak = row.getBestFeature();
      List<Scan> scanNumbers = peak.getScanNumbers();

      // Build chromatogram
      double[] retTimes = new double[scanNumbers.size()];
      double[] intensities = new double[scanNumbers.size()];
      for (int i = 0; i < scanNumbers.size(); ++i) {
        Scan scan = scanNumbers.get(i);
        retTimes[i] = peak.getRetentionTimeAtIndex(i);
        DataPoint dataPoint = peak.getDataPointAtIndex(i);
        if (dataPoint != null)
          intensities[i] = dataPoint.getIntensity();
      }
      Chromatogram chromatogram = new Chromatogram(retTimes, intensities);

      if (chromatogram.length <= 1)
        continue;

      // Fill out PeakInfo
      PeakInfo info = new PeakInfo();

      try {
        // Note: info.peakID is the index of PeakListRow in
        // PeakList.peakListRows (starts from 0)
        // row.getID is row.myID (starts from 1)
        info.peakID = row.getID() - 1;

        double height = -Double.MIN_VALUE;

        for (int i = 0; i < scanNumbers.size(); i++) {
          DataPoint dataPoint = peak.getDataPointAtIndex(i);
          if (dataPoint == null)
            continue;

          double intensity = dataPoint.getIntensity();

          if (intensity > height) {
            height = intensity;
            info.peakIndex = scanNumbers.get(i).getScanNumber();
          }
        }

        info.leftApexIndex = scanNumbers.get(0).getScanNumber();
        info.rightApexIndex = scanNumbers.get(scanNumbers.size() - 1).getScanNumber();
        info.retTime = peak.getRT();
        info.mzValue = peak.getMZ();
        info.intensity = peak.getHeight();
        info.leftPeakIndex = info.leftApexIndex;
        info.rightPeakIndex = info.rightApexIndex;

      } catch (Exception e) {
        log.info("Skipping " + row + ": " + e.getMessage());
        continue;
      }

      BetterPeak betterPeak = new BetterPeak(row.getID(), chromatogram, info);
      betterPeak.setParentId(peak.getParentChromatogramRowID());
      peaks.add(betterPeak);
    }

    return peaks;
  }

}
