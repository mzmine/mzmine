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

package io.github.mzmine.modules.dataprocessing.align_gc;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignMissingNumFeaturesType;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignShiftedNumFeaturesType;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureAlignmentPostProcessor;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Post processor of the {@link GCAlignerTask} for GC-EI feature lists that hold a
 * {@link CompoundList} from the spectral deconvolution. Only the representative rows (with pseudo
 * spectra) were aligned by spectral similarity. This post processor expands each aligned row into
 * all rows of the aligned source compounds:
 * <ul>
 *   <li>The features of all source compound members are aligned by m/z within the compound.</li>
 *   <li>The m/z found in most samples (then highest summed height) becomes the representative row.
 *   Its features keep the pseudo spectrum of their sample.</li>
 *   <li>Features of the representative are only re-extracted from raw data when a sample did not
 *   detect this m/z as a feature of the compound.</li>
 * </ul>
 * The rows of each aligned compound are collected in {@link #getAlignedCompounds()} to create the
 * compound list of the aligned feature list after the final row IDs were set.
 */
public class GCCompoundAlignerPostProcessor implements FeatureAlignmentPostProcessor {

  private static final Logger logger = Logger.getLogger(
      GCCompoundAlignerPostProcessor.class.getName());

  private final MZTolerance mzTol;
  // links a pseudo spectrum back to its source compound. Features copied during alignment keep the
  // same pseudo spectrum instance
  private final Map<Scan, CompoundRow> compoundBySpectrum;
  private final List<List<FeatureListRow>> alignedCompounds = new ArrayList<>();
  private final AtomicInteger extractedFeatures = new AtomicInteger(0);
  private final AtomicInteger shiftedFeatures = new AtomicInteger(0);

  /**
   * @param mzTol        tolerance to align member features by m/z
   * @param featureLists the source feature lists with compound lists
   */
  public GCCompoundAlignerPostProcessor(@NotNull final MZTolerance mzTol,
      @NotNull final List<FeatureList> featureLists) {
    this.mzTol = mzTol;
    compoundBySpectrum = new IdentityHashMap<>();
    for (final FeatureList flist : featureLists) {
      final CompoundList compoundList = flist.getCompoundList();
      if (compoundList == null) {
        continue;
      }
      for (final ModularCompoundRow compound : compoundList.getRows()) {
        compound.getPreferredRow().streamFeatures().map(ModularFeature::getMostIntenseFragmentScan)
            .filter(Objects::nonNull)
            .forEach(spectrum -> compoundBySpectrum.put(spectrum, compound));
      }
    }
  }

  /**
   * @return true if all feature lists have a valid compound list. Empty feature lists have no
   * compound list after deconvolution and are ignored.
   */
  public static boolean hasCompoundLists(@NotNull final List<FeatureList> featureLists) {
    return featureLists.stream().anyMatch(FeatureList::hasCompoundList) && featureLists.stream()
        .allMatch(flist -> flist.isEmpty() || flist.hasCompoundList());
  }

  /**
   * @return the representative rows of all source compounds that have a pseudo spectrum
   */
  public static @NotNull List<FeatureListRow> getRowsToAlign(@NotNull final FeatureList flist) {
    final CompoundList compoundList = flist.getCompoundList();
    if (compoundList == null) {
      return List.of();
    }
    final List<FeatureListRow> rows = new ArrayList<>(compoundList.size());
    for (final ModularCompoundRow compound : compoundList.getRows()) {
      // assumption: the representative lost its pseudo spectrum if the original representative was
      // removed by a filter. Those compounds were removed by filters in the previous workflow
      final FeatureListRow representative = compound.getPreferredRow();
      if (representative.hasMs2Fragmentation()) {
        rows.add(representative);
      }
    }
    return rows;
  }

  @Override
  public void handlePostAlignment(final ModularFeatureList flist) {
    // debugging info types - kept for now so that users may provide more info for debugging
    flist.addRowType(DataTypes.get(GcAlignMissingNumFeaturesType.class));
    flist.addRowType(DataTypes.get(GcAlignShiftedNumFeaturesType.class));

    final AtomicInteger nextRowId = new AtomicInteger(
        flist.stream().mapToInt(FeatureListRow::getID).max().orElse(0) + 1);
    final List<List<FeatureListRow>> compounds = flist.getRowsCopy().parallelStream()
        .map(row -> expandAlignedRow(flist, (ModularFeatureListRow) row, nextRowId)).toList();

    alignedCompounds.clear();
    alignedCompounds.addAll(compounds);
    // new member rows are added here, this also applies the row bindings to all rows
    flist.setRowsApplySort(compounds.stream().flatMap(Collection::stream)
        .toArray(FeatureListRow[]::new));

    logger.fine("""
        GC-EI compound alignment created %d compounds with %d rows. \
        %d representative features were extracted from raw data, because the consensus m/z was not \
        a feature of the compound in this sample. %d of them were shifted to the closest signal.""".formatted(
        compounds.size(), flist.getNumberOfRows(), extractedFeatures.get(),
        shiftedFeatures.get()));
  }

  /**
   * @return the rows of each aligned compound, the representative row first. Available after
   * {@link #handlePostAlignment(ModularFeatureList)}.
   */
  public @NotNull List<List<FeatureListRow>> getAlignedCompounds() {
    return alignedCompounds;
  }

  /**
   * Expand an aligned representative row into the rows of all members of the aligned source
   * compounds.
   *
   * @param alignedRow the aligned row, will become the representative row
   * @return all rows of this compound, the representative first
   */
  private @NotNull List<FeatureListRow> expandAlignedRow(@NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow alignedRow, @NotNull final AtomicInteger nextRowId) {
    final List<ModularFeature> alignedFeatures = List.copyOf(alignedRow.getFeatures());

    final List<MemberCluster> clusters = clusterMemberFeatures(alignedFeatures);
    // decision: same consensus as the previous pseudo spectrum based approach
    // most detections across samples and then highest summed height
    final MemberCluster consensus = clusters.stream().max(
        Comparator.comparingInt(MemberCluster::numDetections)
            .thenComparingDouble(MemberCluster::sumHeight)).orElseThrow();

    final List<FeatureListRow> rows = new ArrayList<>(clusters.size());
    rows.add(alignedRow);
    setRepresentativeFeatures(flist, alignedRow, alignedFeatures, consensus);

    for (final MemberCluster cluster : clusters) {
      if (cluster == consensus) {
        continue;
      }
      final ModularFeatureListRow memberRow = new ModularFeatureListRow(flist,
          nextRowId.getAndIncrement());
      for (final ModularFeature feature : cluster.features().values()) {
        final ModularFeature copy = new ModularFeature(flist, feature);
        // only the representative row holds the pseudo spectrum
        copy.setAllMS2FragmentScans(null);
        memberRow.addFeature(copy.getRawDataFile(), copy, false);
      }
      rows.add(memberRow);
    }
    return rows;
  }

  /**
   * Replace the features of the aligned row by the features of the consensus m/z. Each feature
   * keeps the pseudo spectrum of the aligned representative feature of its sample.
   */
  private void setRepresentativeFeatures(@NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow alignedRow,
      @NotNull final List<ModularFeature> alignedFeatures,
      @NotNull final MemberCluster consensus) {
    final Range<Double> mzTolRange = mzTol.getToleranceRange(consensus.getAverageMz());
    final double mzTolRangeLength = RangeUtils.rangeLength(mzTolRange);

    int missing = 0;
    int shifted = 0;
    final List<ModularFeature> newFeatures = new ArrayList<>(alignedFeatures.size());
    for (final ModularFeature alignedFeature : alignedFeatures) {
      final RawDataFile raw = alignedFeature.getRawDataFile();
      final ModularFeature member = consensus.features().get(raw);
      if (member == alignedFeature) {
        // aligned feature was already the consensus feature
        newFeatures.add(alignedFeature);
        continue;
      }
      if (member != null) {
        final ModularFeature copy = new ModularFeature(flist, member);
        copy.setAllMS2FragmentScans(alignedFeature.getAllMS2FragmentScans());
        newFeatures.add(copy);
        continue;
      }

      // the consensus m/z was not detected as a feature of this compound in this sample
      ModularFeature extracted = GCConsensusAlignerPostProcessor.extractNewFeature(flist,
          alignedFeature, mzTolRange);
      if (extracted == null) {
        // try to shift to the closest signal and then use a range around this
        extracted = GCConsensusAlignerPostProcessor.tryRecenterMzToClosestSignal(flist,
            alignedFeature, consensus.getAverageMz(), mzTolRangeLength, mzTol);
        if (extracted != null) {
          shifted++;
        }
      }
      if (extracted != null) {
        extractedFeatures.incrementAndGet();
        newFeatures.add(extracted);
      } else {
        missing++;
      }
    }

    alignedRow.clearFeatures(false);
    for (final ModularFeature feature : newFeatures) {
      // row bindings are applied when all rows are set
      alignedRow.addFeature(feature.getRawDataFile(), feature, false);
    }
    shiftedFeatures.addAndGet(shifted);
    alignedRow.set(GcAlignMissingNumFeaturesType.class, missing);
    alignedRow.set(GcAlignShiftedNumFeaturesType.class, shifted);
  }

  /**
   * Align all member features of the source compounds by m/z. Starts with the highest feature, a
   * cluster takes only one feature per sample.
   *
   * @param alignedFeatures the aligned representative features, one per sample
   * @return the member clusters
   */
  private @NotNull List<MemberCluster> clusterMemberFeatures(
      @NotNull final List<ModularFeature> alignedFeatures) {
    final List<ModularFeature> memberFeatures = new ArrayList<>();
    for (final ModularFeature alignedFeature : alignedFeatures) {
      final RawDataFile raw = alignedFeature.getRawDataFile();
      final Scan spectrum = alignedFeature.getMostIntenseFragmentScan();
      final CompoundRow source = spectrum == null ? null : compoundBySpectrum.get(spectrum);
      if (source == null) {
        // assumption: should not happen as only representatives with pseudo spectra are aligned
        memberFeatures.add(alignedFeature);
        continue;
      }
      for (final FeatureListRow memberRow : source.getMemberRows()) {
        final Feature member = memberRow.getFeature(raw);
        if (member == null) {
          continue;
        }
        // use the aligned copy for the representative to keep its row values
        final boolean isRepresentative = memberRow == source.getPreferredRow();
        memberFeatures.add(isRepresentative ? alignedFeature : (ModularFeature) member);
      }
    }

    final RangeMap<Double, MemberCluster> clusters = TreeRangeMap.create();
    memberFeatures.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());
    for (final ModularFeature feature : memberFeatures) {
      final double mz = feature.getMZ();
      final MemberCluster cluster = clusters.get(mz);
      if (cluster != null) {
        // decision: a lower feature of the same sample at the same m/z is dropped
        cluster.features().putIfAbsent(feature.getRawDataFile(), feature);
      } else {
        // use smaller range in case of overlaps
        final Range<Double> mzRange = SpectraMerging.createNewNonOverlappingRange(clusters,
            mzTol.getToleranceRange(mz));
        final Map<RawDataFile, ModularFeature> features = HashMap.newHashMap(
            alignedFeatures.size());
        features.put(feature.getRawDataFile(), feature);
        clusters.put(mzRange, new MemberCluster(features));
      }
    }
    return new ArrayList<>(clusters.asMapOfRanges().values());
  }

  /**
   * Features of one m/z within an aligned compound
   *
   * @param features one feature per sample
   */
  private record MemberCluster(@NotNull Map<RawDataFile, ModularFeature> features) {

    int numDetections() {
      return features.size();
    }

    double sumHeight() {
      double sum = 0;
      for (final ModularFeature feature : features.values()) {
        sum += feature.getHeight();
      }
      return sum;
    }

    double getAverageMz() {
      return features.values().stream().mapToDouble(ModularFeature::getMZ).average().orElse(0d);
    }
  }
}
