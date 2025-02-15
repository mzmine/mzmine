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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SpectralDeconvolutionUtils {

  /**
   * Generates pseudo spectra for a list of grouped features, excluding those within specified m/z
   * ranges. The features in each group need to be sorted in descending order by feature height.
   *
   * @param groupedFeatures  A list of groups, where each group is a list of {@link ModularFeature}
   *                         objects.
   * @param featureList      The {@link FeatureList} to which the pseudo spectra will be added.
   * @param mzValuesToIgnore A list of {@link Range} objects representing m/z ranges to be
   *                         excluded.
   * @return A list of {@link FeatureListRow} objects representing the deconvoluted feature list
   * rows.
   */
  public static List<FeatureListRow> generatePseudoSpectra(
      List<List<ModularFeature>> groupedFeatures, FeatureList featureList,
      List<Range<Double>> mzValuesToIgnore) {
    List<FeatureListRow> deconvolutedFeatureListRowsByRtOnly = new ArrayList<>();
    List<Range<Double>> adjustedRanges = getAdjustedRanges(mzValuesToIgnore);
    for (List<ModularFeature> group : groupedFeatures) {
      // find main feature as representative feature in new feature list
      ModularFeature mainFeature = getMainFeature(group, adjustedRanges);

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

  /**
   * Retrieves the main feature from a list of features, excluding those within specified m/z
   * ranges. The features in the list should be sorted in descending order by feature height.
   *
   * @param groups         A list of {@link ModularFeature} objects sorted in descending order by
   *                       feature height.
   * @param adjustedRanges A list of {@link Range} objects representing m/z ranges to be excluded.
   * @return The first highest {@link ModularFeature} not within the excluded m/z ranges, or the highest
   * feature if all features are within the excluded ranges.
   */
  @NotNull
  public static ModularFeature getMainFeature(List<ModularFeature> groups,
      List<Range<Double>> adjustedRanges) {
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
    // if no feature matched then return the first (highest) even though it was excluded as main feature
    return groups.getFirst();
  }

  /**
   * Adjusts the given m/z ranges to ensure that ranges with equal minimum and maximum values are
   * expanded.
   *
   * @param mzValuesToIgnore A list of {@link Range} objects representing m/z ranges to be
   *                         adjusted.
   * @return A list of adjusted {@link Range} objects.
   */
  public static @NotNull List<Range<Double>> getAdjustedRanges(
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
    return adjustedRanges;
  }

}
