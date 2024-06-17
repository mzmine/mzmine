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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SpectralDeconvolutionTools {

  public static List<FeatureListRow> generatePseudoSpectra(
      List<List<ModularFeature>> groupedFeatures, FeatureList featureList,
      List<Range<Double>> mzValuesToIgnore) {
    List<FeatureListRow> deconvolutedFeatureListRowsByRtOnly = new ArrayList<>();
    for (List<ModularFeature> group : groupedFeatures) {
      // find main feature as representative feature in new feature list
      ModularFeature mainFeature = getMainFeature(group, mzValuesToIgnore);

      group.sort(Comparator.comparingDouble(ModularFeature::getMZ));
      double[] mzs = new double[group.size()];
      double[] intensities = new double[group.size()];
      for (int i = 0; i < group.size(); i++) {
        mzs[i] = group.get(i).getMZ();
        intensities[i] = group.get(i).getHeight();
      }
      // Create PseudoSpectrum
      PseudoSpectrum pseudoSpectrum = new SimplePseudoSpectrum(featureList.getRawDataFile(0), 1,
          // MS Level
          mainFeature.getRT(), null, // No MsMsInfo for pseudo spectrum
          mzs, intensities, mainFeature.getRepresentativeScan().getPolarity(),
          "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);

      mainFeature.setAllMS2FragmentScans(List.of(pseudoSpectrum));
      deconvolutedFeatureListRowsByRtOnly.add(mainFeature.getRow());
    }
    return deconvolutedFeatureListRowsByRtOnly;
  }

  public static ModularFeature getMainFeature(List<ModularFeature> groups,
      List<Range<Double>> mzValuesToIgnore) {
    List<Range<Double>> adjustedRanges = new ArrayList<>();
    if (mzValuesToIgnore != null) {
      // Adjust ranges if min and max values are the same
      for (Range<Double> range : mzValuesToIgnore) {
        if (range.lowerEndpoint().equals(range.upperEndpoint())) {
          double minValue = range.lowerEndpoint();
          double maxValue = minValue + 1.0;
          adjustedRanges.add(Range.closed(minValue, maxValue));
        } else {
          adjustedRanges.add(range);
        }
      }
    }

    for (ModularFeature feature : groups) {
      double mz = feature.getMZ();
      boolean isIgnored = false;
      if (!adjustedRanges.isEmpty()) {
        for (Range<Double> range : adjustedRanges) {
          if (range.contains(mz)) {
            isIgnored = true;
            break;
          }
        }
      }
      if (!isIgnored) {
        return feature;
      }
    }
    return null; // Return null if all features are in the ignored ranges
  }


  @NotNull
  public static SpectralDeconvolutionAlgorithm createSpectralDeconvolutionAlgorithm(
      final MZmineProcessingStep<SpectralDeconvolutionAlgorithm> spectralDeconvolutionAlgorithmStep) {
    ParameterSet parameterSet = spectralDeconvolutionAlgorithmStep.getParameterSet();
    // derive new mass detector with parameters
    return spectralDeconvolutionAlgorithmStep.getModule().create(parameterSet);
  }

}
