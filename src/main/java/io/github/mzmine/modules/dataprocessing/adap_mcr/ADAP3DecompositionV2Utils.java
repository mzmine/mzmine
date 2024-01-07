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
