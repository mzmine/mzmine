/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
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

package io.github.mzmine.modules.dataprocessing.adap_mcr;

import io.github.mzmine.datamodel.features.FeatureList;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;

import java.util.*;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class ChromatogramPeakPair {
  public final FeatureList chromatograms;
  public final FeatureList peaks;

  private ChromatogramPeakPair(@NotNull FeatureList chromatograms, @NotNull FeatureList peaks) {
    this.chromatograms = chromatograms;
    this.peaks = peaks;
  }

  @Override
  public String toString() {
    return chromatograms.getName() + " / " + peaks.getName();
  }

  public static Map<RawDataFile, ChromatogramPeakPair> fromParameterSet(
      @NotNull ParameterSet parameterSet) {
    Map<RawDataFile, ChromatogramPeakPair> pairs = new HashMap<>();

    FeatureList[] chromatograms =
        parameterSet.getParameter(ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS).getValue()
            .getMatchingFeatureLists();
    FeatureList[] peaks = parameterSet.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();
    if (chromatograms == null || chromatograms.length == 0 || peaks == null || peaks.length == 0)
      return pairs;

    Set<RawDataFile> dataFiles = new HashSet<>();
    for (FeatureList peakList : chromatograms)
      dataFiles.add(peakList.getRawDataFile(0));
    for (FeatureList peakList : peaks)
      dataFiles.add(peakList.getRawDataFile(0));

    for (RawDataFile dataFile : dataFiles) {
      FeatureList chromatogram = Arrays.stream(chromatograms)
          .filter(c -> c.getRawDataFile(0) == dataFile).findFirst().orElse(null);
      FeatureList peak = Arrays.stream(peaks).filter(c -> c.getRawDataFile(0) == dataFile).findFirst()
          .orElse(null);
      if (chromatogram != null && peak != null)
        pairs.put(dataFile, new ChromatogramPeakPair(chromatogram, peak));
    }

    return pairs;
  }
}
