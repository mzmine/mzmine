/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drives the selection of fragmentation spectra. Based on input spectra (MS2 or MSn) merged spectra
 * are generated.
 *
 * @param mzTol                 tolerance to merge signals
 * @param mergeSeparateEnergies the final list will contain one representative spectrum for each
 *                              energy
 * @param inputSpectra          keep input spectra in the final list or only use representative
 *                              spectra
 * @param msLevelFilter         is applied last after all merging is done. for MSn data this would
 *                              mean that either all MSn levels are used or only the pseudo MS2 scan
 *                              merged from all is kept when using MS2 only
 */
public record FragmentScanSelection(@NotNull MZTolerance mzTol, boolean mergeSeparateEnergies,
                                    @NotNull IncludeInputSpectra inputSpectra,
                                    @NotNull IntensityMergingType intensityMergeType,
                                    @NotNull MsLevelFilter msLevelFilter,
                                    @Nullable MemoryMapStorage storage) {

  private static final Logger logger = Logger.getLogger(FragmentScanSelection.class.getName());

  public FragmentScanSelection(@NotNull MZTolerance mzTol, boolean mergeSeparateEnergies,
      @NotNull IncludeInputSpectra inputSpectra, @NotNull IntensityMergingType intensityMergeType,
      @NotNull MsLevelFilter msLevelFilter) {
    this(mzTol, mergeSeparateEnergies, inputSpectra, intensityMergeType, msLevelFilter, null);
  }

  public List<Scan> getAllFragmentSpectra(final FeatureListRow row) {
    return getAllFragmentSpectra(row.getAllFragmentScans());
  }

  /**
   * List of spectra merged on different MSn levels, energies, total merged, single most abundant,
   * ...
   *
   * @param scans prefiltered list of scans
   * @return list of merged and single scans
   */
  public @NotNull List<Scan> getAllFragmentSpectra(final List<Scan> scans) {
    if (scans.size() <= 1) {
      return scans;
    }

    boolean hasMSn = scans.stream().anyMatch(s -> s.getMSLevel() > 2);
    // make sure its mutable
    var allScans = new ArrayList<>(hasMSn ? getAllFromMSn(scans) : computeAllScans(scans));

    if (msLevelFilter.isFilter()) {
      allScans.removeIf(msLevelFilter::notMatch);
    }
    return allScans;
  }

  /**
   * Applies the selection to a list of MS spectra from the same MS level. The first scan is the
   * representative scan that merges all
   *
   * @param scans all scans from the same level. Already merged scans are removed.
   * @return list of merged and single spectra. Previously merged scans are removed.
   */
  private List<Scan> computeAllScans(final List<Scan> scans) {
    if (scans.size() == 1) {
      return scans;
    }

    final List<Scan> noMergedScans = scans.stream().filter(
        scan -> !(scan instanceof MergedMassSpectrum merged)
            || merged.getMergingType() == MergingType.PASEF_SINGLE).toList();

    if (noMergedScans.isEmpty()) {
      return scans;
    }

    Map<Float, List<Scan>> byFragmentationEnergy = ScanUtils.splitByFragmentationEnergy(
        noMergedScans);
    List<Scan> allScans = new ArrayList<>();
    // merge by energies separately and then all together
    List<Scan> mergedByEnergy = mergeByFragmentationEnergy(byFragmentationEnergy);
    // first entry should be the mergeAll
    allScans.add(mergeSpectra(mergedByEnergy, MergingType.ALL_ENERGIES));
    addIf(mergeSeparateEnergies, allScans, mergedByEnergy);

    // filter out duplicates from the original scans list, same energy
    switch (inputSpectra) {
      case NONE -> {
      }
      case ALL -> allScans.addAll(noMergedScans);
      case HIGHEST_TIC_PER_ENERGY ->
          allScans.addAll(filterBestScansPerEnergy(byFragmentationEnergy.values()));
    }
    // make sure to have unique scans - they might have been added on multiple stages as single spectra
    return allScans.stream().distinct().toList();
  }

  private List<Scan> getAllFromMSn(final List<Scan> scans) {
    // get all merged spectra on MSn tree nodes, energies, and then one spectrum for all merged into one
    // for MS2, that means that all spectra are merged first for individual energies and then all of them to one
    // Empty list if only one spectrum
    // get best tree - there should only be one
    List<PrecursorIonTree> msnTrees = ScanUtils.getMSnFragmentTrees(scans, mzTol, null);
    if (msnTrees.size() > 1) {
      logger.finer(() -> String.format(
          "List of scans had more than 1 MSn Tree (%d). MZtolerance might be too low, will only use the biggest tree for now",
          msnTrees.size()));
    }
    PrecursorIonTree tree = msnTrees.stream()
        .max(Comparator.comparingInt(PrecursorIonTree::countPrecursor)).orElse(null);

    return tree == null ? List.of() : getAllFragmentSpectra(tree);
  }

  @NotNull
  public List<Scan> getAllFragmentSpectra(final PrecursorIonTree tree) {
    return getAllFragmentSpectra(tree.getRoot());
  }

  @NotNull
  public List<Scan> getAllFragmentSpectra(final PrecursorIonTreeNode root) {
    // merge each MSn node and add all selected scans
    List<List<Scan>> mergedPerTreeNode = root.streamWholeTree()
        .map(PrecursorIonTreeNode::getFragmentScans).map(this::computeAllScans).toList();

    // first scan of each list is the representative scan (merged from all other or a single if solitary)
    List<Scan> representativeMergedScans = mergedPerTreeNode.stream().map(list -> list.get(0))
        .toList();

    Scan allMerged = mergeSpectra(representativeMergedScans, MergingType.ALL_MSN_TO_PSEUDO_MS2);

    List<Scan> allScans = new ArrayList<>();
    allScans.add(allMerged);
    mergedPerTreeNode.forEach(allScans::addAll);

    if (msLevelFilter.isFilter()) {
      allScans.removeIf(msLevelFilter::notMatch);
    }
    return allScans;
  }

  /**
   * For each energy, retain one scan - with the highest TIC
   *
   * @param byFragmentationEnergy scans split by their fragmentation energy build a group
   * @return list of scans, for each energy one
   */
  private Collection<? extends Scan> filterBestScansPerEnergy(
      final Collection<List<Scan>> byFragmentationEnergy) {
    return byFragmentationEnergy.stream().map(this::getBestScan).toList();
  }

  /**
   * @param scans group of scans
   * @return the scan with the highest TIC, {@link FragmentScanSorter#DEFAULT_TIC}
   */
  public <T extends Scan> T getBestScan(final List<T> scans) {
    if (scans.size() == 1) {
      return scans.get(0);
    }
    scans.sort(FragmentScanSorter.DEFAULT_TIC);
    return scans.get(0);
  }

  /**
   * Merges all scans of the same energy. If there is only one scan with a specific energy, this
   * scan is used.
   *
   * @param inputScans list of input scans, usually already separated for MSn levels
   * @return list of one scan per fragmentation energy.
   */
  public List<Scan> mergeByFragmentationEnergy(final List<Scan> inputScans) {
    return mergeByFragmentationEnergy(ScanUtils.splitByFragmentationEnergy(inputScans));
  }

  /**
   * Merges all scans of the same energy. If there is only one scan with a specific energy, this
   * scan is used.
   *
   * @param perEnergy map of scans by fragmentation energy
   * @return list of one scan per fragmentation energy.
   */
  private List<Scan> mergeByFragmentationEnergy(final Map<Float, List<Scan>> perEnergy) {
    List<Scan> list = new ArrayList<>(perEnergy.size());
    for (final List<Scan> scans : perEnergy.values()) {
      if (scans.size() > 1) {
        list.add(mergeSpectra(scans, MergingType.SAME_ENERGY));
      } else {
        list.add(scans.get(0));
      }
    }

    return list;
  }

  /**
   * Merge list of spectra or return a single spectrum if list has only one element
   *
   * @param scans input list of spectra to be merged
   * @return merged spectrum or a mass spectrum
   */
  private Scan mergeSpectra(final List<? extends Scan> scans, MergingType mergeType) {
    if (scans.size() == 1) {
      return scans.get(0);
    }
    return SpectraMerging.mergeSpectra(scans, mzTol, mergeType, intensityMergeType, storage);
  }

  private void addIf(boolean condition, final List<Scan> targetList, final Object scans) {
    if (condition) {
      if (scans instanceof Scan scan) {
        targetList.add(scan);
      } else if (scans instanceof Collection<?> collection) {
        targetList.addAll((Collection<? extends Scan>) collection);
      }
    }
  }

  public enum IncludeInputSpectra {
    HIGHEST_TIC_PER_ENERGY, ALL, NONE
  }


}
