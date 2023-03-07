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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsPlaceholder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class ChromatogramPeakPair {

  public final FeatureList chromatograms;
  public final FeatureList peaks;

  private static final Logger logger = Logger.getLogger(ChromatogramPeakPair.class.getName());
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

    var optionalChromatograms = parameterSet.getParameter(
        ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS);
    var useChromatograms = optionalChromatograms.getValue();
    FeatureList[] chromatograms =
        useChromatograms ? optionalChromatograms.getEmbeddedParameter().getValue()
            .getMatchingFeatureLists() : null;
    FeatureList[] peaks = parameterSet.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();

    //if only chromatogram is unavailable find it from peak list.
    if ((chromatograms == null || chromatograms.length == 0) && (peaks != null
        || peaks.length > 0)) {

      for (var peak : peaks) {
        ObservableList<FeatureListAppliedMethod> appliedMethodsList = peak.getAppliedMethods();
        FeatureList chromatogram = null;

        String chromatogramSuffix = getChromatogramSuffixFromAppliedMethods(appliedMethodsList);
        //Get candidate chromatogram based on the suffix, if none is found use the peak list.
        outer:
        for (int j = appliedMethodsList.size() - 1; j >= 0; j--) {
          ParameterSet parameters = appliedMethodsList.get(j).getParameters();
          for (final Parameter<?> param : parameters.getParameters()) {
            if (param instanceof FeatureListsParameter flistParam) {
              FeatureListsPlaceholder[] placeholders = flistParam.getValue()
                  .getCurrentFeatureListsPlaceholders();
              for (int k = placeholders.length - 1; k >= 0; k--) {
                FeatureList candidateList = placeholders[k].getMatchingFeatureList();
//              // feature list is still in memory and was not deleted and already collected by GC
                if (candidateList != null) {
                  String parentName = peak.getName();
                  String candidateName = candidateList.getName();
                  if (parentName.contains(candidateName) && candidateName.endsWith(
                      chromatogramSuffix)) {
                    chromatogram = candidateList;
                    break outer;
                  }
                }
              }
            }
          }
        }
        if (chromatogram != null) {
          pairs.put(peak.getRawDataFile(0), new ChromatogramPeakPair(chromatogram, peak));
        } else {
          pairs.put(peak.getRawDataFile(0), new ChromatogramPeakPair(peak, peak));
          logger.warning("Chromatogram list not found for corresponding peak list");
        }
      }
      return pairs;
    } else if (chromatograms == null || chromatograms.length == 0 || peaks == null
        || peaks.length == 0) {
      return pairs;
    } else {
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
          pairs.put(dataFile, new ChromatogramPeakPair(chromatogram, peak));
        }
      }

      return pairs;
    }
  }

  private static String getChromatogramSuffixFromAppliedMethods (ObservableList<FeatureListAppliedMethod> appliedMethodsList){
    for (var appliedMethod : appliedMethodsList) {
      if(appliedMethod.getDescription().equals("ADAP Chromatogram Builder")){
        ParameterSet parameters = appliedMethod.getParameters();
        for(var parameter : parameters.getParameters()){
          if(parameter instanceof StringParameter sp && sp.getName().equals("Suffix")){
            return sp.getValue();
          }
        }
      }
    }
    return null;
  }
}
