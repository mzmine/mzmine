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

package io.github.mzmine.util.scans.merging;

import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.FloatGrouping.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spectral merging - options include:
 *
 * <pre>
 * MS2
 *   Each Sample:
 *   1. split by samples and merge for each sample first - this may increase quality of scans within samples
 *   2. Split by fragmentation energy - merge each energy
 *   3. Merge across energies - one spec per precursor and sample
 *   Across samples:
 *   4. Merge SINGLE_ENERGY across samples - can then be filtered for min detection rate for each signal
 *   5. Merge all SINGLE_ENERGY across samples to create ALL_ENERGIES after signal filters
 *
 * MSn
 *   1. Create MSn tree
 *   2. Run MS2 workflow on each precursor node on MSn
 *   3. Merge ALL_ENERGIES of all nodes into PSEUDO MS2
 * </pre>
 */
public class SpectraMerger {

  private static final Logger logger = Logger.getLogger(SpectraMerger.class.getName());
  private final @NotNull SampleHandling sampleHandling;
  private final @NotNull MZTolerance mzTol;
  private final @NotNull IntensityMergingType intensityMerging;
  private final @NotNull FragmentScanSelection.IncludeInputSpectra inputSpectra;
  private @Nullable MemoryMapStorage storage;

  /**
   * @param sampleHandling
   * @param mzTol
   * @param intensityMerging
   * @param inputSpectra     keep input spectra in the final list or only use representative
   *                         spectra
   */
  public SpectraMerger(final @NotNull SampleHandling sampleHandling,
      final @NotNull MZTolerance mzTol, final @NotNull IntensityMergingType intensityMerging,
      final @NotNull FragmentScanSelection.IncludeInputSpectra inputSpectra) {

    this.sampleHandling = sampleHandling;
    this.mzTol = mzTol;
    this.intensityMerging = intensityMerging;
    this.inputSpectra = inputSpectra;
  }


  public @NotNull SpectraMergingResults getAllFragmentSpectra(final FeatureListRow row) {
    return getAllFragmentSpectra(row.getAllFragmentScans());
  }

  /**
   * List of spectra merged on different MSn levels, energies, total merged, single most abundant,
   * ...
   *
   * @param scans prefiltered list of scans
   * @return list of merged and single scans
   */
  @NotNull
  public SpectraMergingResults getAllFragmentSpectra(final List<Scan> scans) {
    if (scans.isEmpty()) {
      return SpectraMergingResults.ofEmpty();
    }
    if (scans.size() == 1) {
      return SpectraMergingResults.ofSingleScan(scans.getFirst());
    }
    boolean hasMSn = scans.stream().anyMatch(s -> s.getMSLevel() > 2);
    // result
    List<SpectraMergingResultsNode> mergedBySample = new ArrayList<>(scans.size());

    // always split by sample and merge for each individually, then maybe merge across samples if selected
    final Map<String, List<Scan>> bySamples = ScanUtils.splitBySample(scans);

    for (final Entry<String, List<Scan>> group : bySamples.entrySet()) {
      List<Scan> sampleScans = group.getValue();
      // get merged or best single scans on different MSn levels, and energies
      if (hasMSn) {
        mergedBySample.addAll(computeAllMergedOrBestFromMSn(sampleScans, false));
      } else {
        mergedBySample.add(mergeByFragmentationEnergy(sampleScans, true, false));
      }
    }

    // merge across samples using the list of spectra obtained from all samples
    SpectraMergingResultsNode mergedAcrossSamples = null;
    if (sampleHandling == SampleHandling.ACROSS_SAMPLES && bySamples.size() > 1) {
      // collect all the scans with the same energy across samples
      var byEnergyAcrossSamples = SpectraMergingResultsNode.collectScanByEnergy(mergedBySample);
      // remove those with only one scan - this means only present in one sample and those are already represented
      byEnergyAcrossSamples.entrySet().removeIf(entry -> entry.getValue().size() <= 1);

      if (!byEnergyAcrossSamples.isEmpty()) {
        logger.fine("Merging scans from %d different energy groups across samples".formatted(
            byEnergyAcrossSamples.size()));
        mergedAcrossSamples = mergeByFragmentationEnergy(byEnergyAcrossSamples, true, true);
      }
    }

    return new SpectraMergingResults(mergedBySample, mergedAcrossSamples, null);
  }

  private List<SpectraMergingResultsNode> computeAllMergedOrBestFromMSn(final List<Scan> scans,
      final boolean isAcrossSamples) {
    // get all merged spectra on MSn tree nodes, energies, and then one spectrum for all merged into one
    // for MS2, that means that all spectra are merged first for individual energies and then all of them to one
    // Empty list if only one spectrum
    // get best tree - there should only be one
    List<PrecursorIonTree> msnTrees = ScanUtils.getMSnFragmentTrees(scans, getMzTol(), null);
    if (msnTrees.size() > 1) {
      logger.finer(() -> String.format(
          "List of scans had more than 1 MSn Tree (%d). MZtolerance might be too low, will only use the biggest tree for now",
          msnTrees.size()));
    }
    PrecursorIonTree tree = msnTrees.stream()
        .max(Comparator.comparingInt(PrecursorIonTree::countPrecursor)).orElse(null);

    return tree == null ? List.of() : getAllFragmentSpectra(tree, isAcrossSamples);
  }

  @NotNull
  public List<SpectraMergingResultsNode> getAllFragmentSpectra(final PrecursorIonTree tree,
      final boolean isAcrossSamples) {
    return getAllFragmentSpectra(tree.getRoot(), isAcrossSamples);
  }

  @NotNull
  public List<SpectraMergingResultsNode> getAllFragmentSpectra(final PrecursorIonTreeNode root,
      final boolean isAcrossSamples) {
    // merge each MSn node by energy and across energies
    List<SpectraMergingResultsNode> mergedPerTreeNode = root.streamWholeTree()
        .map(PrecursorIonTreeNode::getFragmentScans)
        .map(scans -> mergeByFragmentationEnergy(scans, true, isAcrossSamples)).toList();

    // merge all to pseudo MS2
    List<Scan> representativeMergedScans = mergedPerTreeNode.stream()
        .map(SpectraMergingResultsNode::getAcrossEnergiesOrSingleScan).toList();

    Scan pseudoMs2 = null;
    if (representativeMergedScans.size() > 1) {
      pseudoMs2 = mergeSpectra(representativeMergedScans, MergingType.ALL_MSN_TO_PSEUDO_MS2);
    }

    // TODO how to return this result - extra class?

    List<Scan> allScans = new ArrayList<>();
    allScans.add(pseudoMs2);
    mergedPerTreeNode.forEach(allScans::addAll);

    if (postMergingScanFilter.isFilter()) {
      allScans.removeIf(postMergingScanFilter::matchesNot);
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
      return scans.getFirst();
    }
    scans.sort(FragmentScanSorter.DEFAULT_TIC);
    return scans.get(0);
  }

  /**
   * Merge list of spectra or return a single spectrum if list has only one element
   *
   * @param scans input list of spectra to be merged
   * @return merged spectrum or a mass spectrum
   */
  public Scan mergeSpectra(final List<? extends Scan> scans, MergingType mergeType) {
    if (scans.size() == 1) {
      return scans.getFirst();
    }
    return SpectraMerging.mergeSpectra(scans, mzTol, mergeType, intensityMerging, storage);
  }

  /**
   * Merges all scans of the same energy. If there is only one scan with a specific energy, this
   * scan is used.Optionally merge across energies.
   *
   * @param inputScans          list of input scans, usually already separated for MSn levels
   * @param mergeAcrossEnergies true to also merge across energies after merging each
   * @param isAcrossSamples     flag if this is across samples or within
   * @return map of energy to one scan per fragmentation energy: either the single scan or merged
   */
  public SpectraMergingResultsNode mergeByFragmentationEnergy(final List<Scan> inputScans,
      final boolean mergeAcrossEnergies, final boolean isAcrossSamples) {
    if (inputScans.size() == 1) {
      return SpectraMergingResultsNode.ofSingleScan(inputScans.getFirst(), isAcrossSamples);
    }

    return mergeByFragmentationEnergy(ScanUtils.splitByFragmentationEnergy(inputScans),
        mergeAcrossEnergies, isAcrossSamples);
  }

  /**
   * Merges all scans of the same energy. If there is only one scan with a specific energy, this
   * scan is used. Optionally merge across energies.
   *
   * @param perEnergy           map of scans by fragmentation energy
   * @param mergeAcrossEnergies true to also merge across energies after merging each
   * @param isAcrossSamples     flag if this is across samples or within
   * @return map of energy to one scan per fragmentation energy: either the single scan or merged
   */
  private SpectraMergingResultsNode mergeByFragmentationEnergy(
      final Map<FloatGrouping, List<Scan>> perEnergy, final boolean mergeAcrossEnergies,
      final boolean isAcrossSamples) {
    Map<FloatGrouping, Scan> results = HashMap.newHashMap(perEnergy.size());
    for (final var entry : perEnergy.entrySet()) {
      FloatGrouping grouping = entry.getKey();
      final List<Scan> scans = entry.getValue();
      if (scans.size() > 1) {
        // may
        MergingType mergeType;
        if (grouping.type() == Type.SINGLE_VALUE) {
          mergeType = MergingType.SAME_ENERGY;
        } else {
          // set the expected merging type based on grouping.
          if (grouping.type() == Type.UNDEFINED) {
            mergeType = MergingType.UNDEFINED_ENERGY;
          } else {
            mergeType = MergingType.ALL_ENERGIES;
          }
          // then see if there is a merging type present with larger ordinal
          for (final Scan scan : scans) {
            MergingType sourceMergingType = ScanUtils.getMergingType(scan);
            if (sourceMergingType.ordinal() > mergeType.ordinal()) {
              mergeType = sourceMergingType;
            }
          }
        }
        // finally merge and add scan
        results.put(grouping, mergeSpectra(scans, mergeType));
      } else {
        results.put(grouping, scans.getFirst());
      }
    }

    Scan allEnergiesScan = null;
    if (mergeAcrossEnergies && perEnergy.size() > 1) {
      // first entry should be the mergeAll
      allEnergiesScan = mergeSpectra(new ArrayList<>(results.values()), MergingType.ALL_ENERGIES);
    }

    return new SpectraMergingResultsNode(isAcrossSamples, results, allEnergiesScan);
  }


  @NotNull
  public SampleHandling getSampleHandling() {
    return sampleHandling;
  }

  @NotNull
  public MZTolerance getMzTol() {
    return mzTol;
  }

  @NotNull
  public IntensityMergingType getIntensityMerging() {
    return intensityMerging;
  }

  public boolean isMergeAcrossEnergies() {
    return true;
  }

  public @NotNull FragmentScanSelection.IncludeInputSpectra getInputSpectra() {
    return inputSpectra;
  }

  public @Nullable MemoryMapStorage getStorage() {
    return storage;
  }

  public void setStorage(final @Nullable MemoryMapStorage storage) {
    this.storage = storage;
  }
}
