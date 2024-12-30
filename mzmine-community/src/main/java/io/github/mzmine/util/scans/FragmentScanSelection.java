/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans;

import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.scans.merging.ScanSelectionFilter;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import io.github.mzmine.util.scans.merging.SpectraMergingResults;
import io.github.mzmine.util.scans.merging.SpectraMergingResultsNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drives the selection of fragmentation spectra. Based on input spectra (MS2 or MSn) merged spectra
 * are generated.
 * <p>
 * See {@link SpectraMergeSelectParameter} for many options with presets or advanced setup
 *
 * <pre>
 *   Options to choose from and combined:
 *   - merging: Across samples, same sample, Across Energies, Same Energy
 *   - MS level: MS1, MS2, MSn, MSn to pseudo MS2
 *   - Intensity merging
 *   - mz tolerance
 *
 *   Simple presets:
 *   - Single merged scan: merged across samples and energies
 *   - Representative scans: merged across samples & energies, each energy
 *   - MSn tree as pseudo MS2 = Single merged scan
 *   - MSn tree: merged across samples & energies
 *   - Representative MSn tree: merged across samples & energies, each energy
 *   - Single most intense scan (no merging, MS level?)
 *   - All input scans (MS level?)
 *   - Advanced
 *
 * Future ideas for integration and parameters:
 *   Pre merging filter:
 *   - chimeric? - isolation widow offset - width
 *   - Cosine threshold? (only for same energy) this could be applied when merging across samples to generate multiple different spectra if they are too different
 *
 *   Post merging data filter:
 *   - signal count filter: how many / % samples detected the same point
 *
 * </pre>
 */
public final class FragmentScanSelection {

  private static final Logger logger = Logger.getLogger(FragmentScanSelection.class.getName());
  private final @Nullable SpectraMerger merger;
  private final EnumSet<MergedSpectraFinalSelectionTypes> finalScanSelection;
  // eg ScanSelectionFilter
  private final @NotNull ScanSelectionFilter postMergingScanFilter;
  private final @Nullable MemoryMapStorage storage;
  private final boolean includeMSn;

  /**
   * @param merger                performs spectral merging
   * @param postMergingScanFilter Post merging scan filters like
   *                              {@link ScanSelectionFilter#matchesAllOf(ScanSelectionFilter...)}
   *                              are applied last after all merging is done. Options to filter for
   *                              specific {@link MergingType} or MS level like MSn data this would
   *                              mean that either all MSn levels are used or only the pseudo MS2
   *                              scan merged from all is kept when using MS2 only
   */
  public FragmentScanSelection(@Nullable MemoryMapStorage storage, @Nullable SpectraMerger merger,
      @NotNull final List<MergedSpectraFinalSelectionTypes> finalScanSelection,
      final @NotNull ScanSelectionFilter postMergingScanFilter) {
    this.merger = merger;
    this.finalScanSelection = EnumSet.copyOf(finalScanSelection);
    this.postMergingScanFilter = postMergingScanFilter;
    this.storage = storage;
    this.includeMSn = finalScanSelection.contains(MergedSpectraFinalSelectionTypes.MSN_TREE);
  }

  public FragmentScanSelection(@Nullable final MemoryMapStorage storage,
      @Nullable final SpectraMerger merger,
      @NotNull final List<MergedSpectraFinalSelectionTypes> finalScanSelection) {
    this(storage, merger, finalScanSelection, ScanSelectionFilter.all());
  }

  public static FragmentScanSelection createAllInputFragmentScansSelect(
      final @Nullable MemoryMapStorage storage) {
    return new FragmentScanSelection(storage, null,
        List.of(MergedSpectraFinalSelectionTypes.ALL_INPUT_SCANS));
  }


  public List<Scan> getAllFragmentSpectra(final FeatureListRow row) {
    return getAllFragmentSpectra(row.getAllFragmentScans());
  }

  /**
   * List of spectra merged on different MSn levels, energies, total merged, single most abundant,
   * ...
   *
   * @param scans prefiltered list of scans
   * @return modifiable list of merged and single scans
   */
  public @NotNull List<Scan> getAllFragmentSpectra(final List<Scan> scans) {
    if (scans.size() <= 1) {
      return new ArrayList<>(scans);
    }

    // use set for uniqueness and linked for insertion order
    Set<Scan> result = new LinkedHashSet<>();

    if (merger != null) {
      // merge scans and generate results
      SpectraMergingResults merged = merger.getAllFragmentSpectra(scans);

      // add scans based on selection types
      // need to combine the various selection types like SAMPLES / ENERGIES
      if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.MSN_PSEUDO_MS2)) {
        addIfNotNull(result, merged.msnPseudoMs2());
      }

      if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES)) {
        extractEnergyScans(merged.acrossSamples(), result);
      }

      // may have selected both across samples and each sample so keep those if separate
      if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.EACH_SAMPLE)) {
        extractEnergyScans(merged.bySample(), result);
      }
    }

    // add ALL source scans
    addSourceScans(scans, result);

    // set already filters duplicates
    return result.stream().filter(Objects::nonNull).filter(postMergingScanFilter::matches)
        .collect(CollectionUtils.toArrayList());
  }

  private void addSourceScans(List<Scan> source, final Set<Scan> result) {
    boolean addAll = finalScanSelection.contains(MergedSpectraFinalSelectionTypes.ALL_INPUT_SCANS);
    boolean addBest = finalScanSelection.contains(
        MergedSpectraFinalSelectionTypes.SINGLE_MOST_INTENSE_INPUT_SCAN);
    if (!includeMSn && (addAll || addBest)) {
      // remove MSn scans from input
      source = source.stream().filter(scan -> scan.getMSLevel() < 3).toList();
    }

    if (addAll) {
      result.addAll(source);
    } else if (addBest) {
      // skip adding best scans if all were already added
      // skip MSn scans here - too complex
      addMostIntenseSourceScans(source, result);
    }
  }

  private void addMostIntenseSourceScans(List<Scan> source, final Set<Scan> result) {
    // sort best scan first
    source = source.stream().filter(s -> s.getMSLevel() < 3).sorted(FragmentScanSorter.DEFAULT_TIC)
        .toList();

    if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES)) {
      // add best overall scan
      addIfNotNull(result, source.getFirst());

      // add best for each energy
      if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.EACH_ENERGY)) {
        var byEnergy = ScanUtils.splitByFragmentationEnergy(source);
        byEnergy.values().stream().map(List::getFirst).forEach(result::add);
      }
    }
    // can select both across samples and each sample
    if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.EACH_SAMPLE)) {
      var bySample = ScanUtils.splitBySample(source).values();
      // sorting allows that first is always the best scan by sample
      bySample.stream().map(List::getFirst).forEach(result::add);

      // add best for each energy
      if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.EACH_ENERGY)) {
        for (final List<Scan> sampleScans : bySample) {
          var byEnergy = ScanUtils.splitByFragmentationEnergy(sampleScans);
          byEnergy.values().stream().map(List::getFirst).forEach(result::add);
        }
      }
    }
  }

  private void extractEnergyScans(final List<SpectraMergingResultsNode> nodes,
      final Set<Scan> result) {
    if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.ACROSS_ENERGIES)) {
      nodes.stream().map(SpectraMergingResultsNode::getAcrossEnergiesOrSingleScan)
          .filter(scan -> includeMSn || scan.getMSLevel() < 3).forEach(result::add);
    }
    if (finalScanSelection.contains(MergedSpectraFinalSelectionTypes.EACH_ENERGY)) {
      nodes.stream().map(SpectraMergingResultsNode::scanByEnergy).map(Map::values)
          .flatMap(Collection::stream).filter(scan -> includeMSn || scan.getMSLevel() < 3)
          .forEach(result::add);
    }
  }

  private <T extends Collection<Scan>> T addIfNotNull(final T scans, final @Nullable Scan scan) {
    if (scan != null) {
      scans.add(scan);
    }
    return scans;
  }


  public boolean isMerging() {
    return merger != null;
  }

  public @Nullable SpectraMerger merger() {
    return merger;
  }

  public @Nullable MemoryMapStorage storage() {
    return storage;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final FragmentScanSelection that)) {
      return false;
    }

    return Objects.equals(merger, that.merger) && Objects.equals(finalScanSelection,
        that.finalScanSelection) && postMergingScanFilter.equals(that.postMergingScanFilter);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(merger);
    result = 31 * result + Objects.hashCode(finalScanSelection);
    result = 31 * result + postMergingScanFilter.hashCode();
    return result;
  }

  public List<Scan> getAllFragmentSpectra(final PrecursorIonTreeNode root) {
    return getAllFragmentSpectra(root.getAllFragmentScans());
  }

  public enum IncludeInputSpectra {
    HIGHEST_TIC_PER_ENERGY, ALL, NONE
  }


}
