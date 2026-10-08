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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDeconvolutionUtils {

  /**
   * Score of correlated EI fragment rows in a compound, same as correlated members of the compound
   * grouper.
   */
  private static final float CORRELATED_MEMBER_SCORE = 0.5f;

  /**
   * Generates pseudo spectra for a list of grouped features and sets them as fragment scans of the
   * main feature of each group. This modifies the features of the input feature list, use
   * {@link #createDeconvolutedFeatureList(FeatureList, List, List, String, MemoryMapStorage)} to
   * create a new feature list instead. The features in each group need to be sorted in descending
   * order by feature height.
   *
   * @param groupedFeatures  A list of groups, where each group is a list of {@link ModularFeature}
   *                         objects.
   * @param featureList      The {@link FeatureList} of the features.
   * @param mzValuesToIgnore A list of {@link Range} objects representing m/z ranges to be excluded
   *                         as main feature.
   * @return the rows of the main features
   */
  public static @NotNull List<FeatureListRow> generatePseudoSpectra(
      @NotNull List<List<ModularFeature>> groupedFeatures, @NotNull FeatureList featureList,
      @Nullable List<Range<Double>> mzValuesToIgnore) {
    final List<FeatureListRow> mainRows = new ArrayList<>();
    final List<Range<Double>> adjustedRanges = getAdjustedRanges(mzValuesToIgnore);
    for (List<ModularFeature> group : groupedFeatures) {
      // find main feature as representative feature in new feature list
      final ModularFeature mainFeature = getMainFeature(group, adjustedRanges);
      mainFeature.setAllMS2FragmentScans(List.of(createPseudoSpectrum(group, mainFeature)));
      mainRows.add(mainFeature.getRow());
    }
    return mainRows;
  }

  /**
   * Creates a GC-EI pseudo spectrum from the m/z and height of all features in a group.
   *
   * @param group       all features of one deconvoluted compound
   * @param mainFeature the representative feature that defines RT and polarity
   * @return the pseudo spectrum with signals sorted by m/z
   */
  public static @NotNull PseudoSpectrum createPseudoSpectrum(
      @NotNull List<? extends Feature> group, @NotNull Feature mainFeature) {
    final List<? extends Feature> sortedByMz = group.stream()
        .sorted(Comparator.comparingDouble(Feature::getMZ)).toList();
    final double[] mzs = new double[sortedByMz.size()];
    final double[] intensities = new double[sortedByMz.size()];
    for (int i = 0; i < sortedByMz.size(); i++) {
      mzs[i] = sortedByMz.get(i).getMZ();
      intensities[i] = sortedByMz.get(i).getHeight();
    }
    // MS level 1 and no MsMsInfo for EI pseudo spectra
    return new SimplePseudoSpectrum(mainFeature.getRawDataFile(), 1, mainFeature.getRT(), null,
        mzs, intensities, mainFeature.getRepresentativeScan().getPolarity(),
        "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);
  }

  /**
   * Creates the deconvoluted feature list. All rows of grouped features are kept and each group
   * becomes a {@link ModularCompoundRow} in the {@link CompoundList} of the new feature list. The
   * main feature of each group is the representative of the compound and holds the
   * {@link PseudoSpectrum} of the group as its fragment scan. Features that are not part of any
   * group are not transferred.
   *
   * @param source           the single sample feature list that was grouped
   * @param groupedFeatures  groups of features of the source list, each sorted in descending
   *                         order by feature height
   * @param mzValuesToIgnore m/z ranges that should not be used as the main feature
   * @param suffix           the suffix of the new feature list
   * @param storage          storage of the new feature list
   * @return the new feature list with a compound list
   */
  public static @NotNull ModularFeatureList createDeconvolutedFeatureList(
      @NotNull FeatureList source, @NotNull List<List<ModularFeature>> groupedFeatures,
      @Nullable List<Range<Double>> mzValuesToIgnore, @NotNull String suffix,
      @Nullable MemoryMapStorage storage) {
    final List<Range<Double>> adjustedRanges = getAdjustedRanges(mzValuesToIgnore);

    // representative row first, then all other rows in descending height
    final List<List<FeatureListRow>> sourceGroups = new ArrayList<>(groupedFeatures.size());
    final Map<FeatureListRow, PseudoSpectrum> spectra = new IdentityHashMap<>(
        groupedFeatures.size());
    for (final List<ModularFeature> group : groupedFeatures) {
      final ModularFeature mainFeature = getMainFeature(group, adjustedRanges);
      final List<FeatureListRow> rows = new ArrayList<>(group.size());
      rows.add(mainFeature.getRow());
      for (final ModularFeature feature : group) {
        if (feature != mainFeature) {
          rows.add(feature.getRow());
        }
      }
      sourceGroups.add(rows);
      spectra.put(mainFeature.getRow(), createPseudoSpectrum(group, mainFeature));
    }

    final List<FeatureListRow> allRows = sourceGroups.stream().flatMap(List::stream).toList();
    // single sample: one feature per row
    final ModularFeatureList deconvolutedList = FeatureListUtils.createCopyWithoutRows(source,
        suffix, storage, allRows.size(), allRows.size());
    final Map<FeatureListRow, ModularFeatureListRow> rowMapping = FeatureListUtils.copyRows(
        allRows, deconvolutedList, true);
    FeatureListUtils.transferRowRelationsAndIIN(source, deconvolutedList, rowMapping);

    // set the pseudo spectra on the copies to keep the source feature list unchanged
    final RawDataFile dataFile = source.getRawDataFile(0);
    spectra.forEach((sourceRow, spectrum) -> {
      final ModularFeature mainFeature = rowMapping.get(sourceRow).getFeature(dataFile);
      if (mainFeature != null) {
        mainFeature.setAllMS2FragmentScans(List.of(spectrum));
      }
    });

    final List<List<ModularFeatureListRow>> groups = sourceGroups.stream()
        .map(group -> group.stream().map(rowMapping::get).toList()).toList();
    // set the compound list after all rows were added, adding rows invalidates the compound list
    deconvolutedList.setCompoundList(createCompoundList(deconvolutedList, groups, storage));
    return deconvolutedList;
  }

  /**
   * Groups rows of a GC-EI feature list into compounds. The first row of each group is the
   * representative row that holds the pseudo spectrum. All other rows are correlated members. Must
   * be called after all rows were added to the feature list, because structural changes to the
   * rows invalidate the compound list.
   *
   * @param featureList the feature list that contains all rows of the groups
   * @param groups      rows of each compound, the representative row first
   * @param storage     storage for the compound list
   * @return the compound list or null if there are no groups
   */
  public static @Nullable CompoundList createCompoundList(@NotNull ModularFeatureList featureList,
      @NotNull List<? extends List<? extends FeatureListRow>> groups,
      @Nullable MemoryMapStorage storage) {
    if (groups.isEmpty()) {
      return null;
    }
    // compound IDs follow the representative rows in default row order
    final Comparator<FeatureListRow> rowSorter = FeatureListUtils.getDefaultRowSorter(
        featureList);
    final List<List<? extends FeatureListRow>> sortedGroups = new ArrayList<>(groups.size());
    for (final List<? extends FeatureListRow> group : groups) {
      if (!group.isEmpty()) {
        sortedGroups.add(group);
      }
    }
    sortedGroups.sort((a, b) -> rowSorter.compare(a.getFirst(), b.getFirst()));

    final CompoundList compoundList = new CompoundList(featureList, storage, sortedGroups.size());
    final List<ModularCompoundRow> compounds = new ArrayList<>(sortedGroups.size());
    int compoundId = 1;
    for (final List<? extends FeatureListRow> group : sortedGroups) {
      final List<CompoundFeatureMember> members = new ArrayList<>(group.size());
      for (final FeatureListRow row : group) {
        final boolean representative = members.isEmpty();
        members.add(new CompoundFeatureMember(row,
            representative ? CompoundMemberRole.REPRESENTATIVE : CompoundMemberRole.CORRELATED,
            representative ? 1f : CORRELATED_MEMBER_SCORE));
      }
      // decision: no independent confidence estimate exists for EI deconvolution, groups already
      // passed the correlation and minimum signal criteria of the deconvolution algorithm
      compounds.add(
          new ModularCompoundRow(compoundList, compoundId++, group.getFirst(), members, 1f,
              null));
    }
    compoundList.setRows(compounds);
    return compoundList;
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
