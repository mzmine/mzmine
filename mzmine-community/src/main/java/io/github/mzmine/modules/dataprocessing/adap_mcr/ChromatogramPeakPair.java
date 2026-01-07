/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Might not have chromatograms
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public record ChromatogramPeakPair(@NotNull FeatureList peaks, @Nullable FeatureList chromatograms) {

  @Override
  public String toString() {
    return chromatograms==null? peaks.getName() : chromatograms.getName() + " / " + peaks.getName();
  }

  public static Map<RawDataFile, ChromatogramPeakPair> fromParameterSet(
      @NotNull ParameterSet parameterSet) {
    Map<RawDataFile, ChromatogramPeakPair> pairs = new HashMap<>();

    FeatureList[] chromatograms = parameterSet.getParameter(
        ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS).getValue().getMatchingFeatureLists();
    FeatureList[] peaks = parameterSet.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();
    if (chromatograms == null || chromatograms.length == 0 || peaks == null || peaks.length == 0) {
      assert peaks != null;
      for (final FeatureList flist : peaks) {
        pairs.put(flist.getRawDataFile(0), new ChromatogramPeakPair(flist, null));
      }
      return pairs;
    }

    Set<RawDataFile> dataFiles = new HashSet<>();
    for (FeatureList peakList : chromatograms) {
      dataFiles.add(peakList.getRawDataFile(0));
    }
    for (FeatureList peakList : peaks) {
      dataFiles.add(peakList.getRawDataFile(0));
    }

    for (RawDataFile dataFile : dataFiles) {
      FeatureList chromatogram = Arrays.stream(chromatograms)
          .filter(c -> c.getRawDataFile(0) == dataFile).findFirst().orElse(null);
      FeatureList peak = Arrays.stream(peaks).filter(c -> c.getRawDataFile(0) == dataFile)
          .findFirst().orElse(null);
      if (chromatogram != null && peak != null) {
        pairs.put(dataFile, new ChromatogramPeakPair(peak, chromatogram));
      }
    }

    return pairs;
  }
}
