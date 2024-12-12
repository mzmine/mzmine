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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.ScanSelectionFilter;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drives the selection of fragmentation spectra. Based on input spectra (MS2 or MSn) merged spectra
 * are generated.
 */
public final class FragmentScanSelection {

  private static final Logger logger = Logger.getLogger(FragmentScanSelection.class.getName());
  private final @Nullable SpectraMerger merger;
  // eg ScanSelectionFilter
  private final @NotNull ScanSelectionFilter postMergingScanFilter;
  private final @Nullable MemoryMapStorage storage;

  /**
   * @param merger                performs spectral merging
   * @param postMergingScanFilter Post merging scan filters like
   *                              {@link ScanSelectionFilter#matchesAllOf(ScanSelectionFilter...)}
   *                              are applied last after all merging is done. Options to filter for
   *                              specific {@link MergingType} or MS level like MSn data this would
   *                              mean that either all MSn levels are used or only the pseudo MS2
   *                              scan merged from all is kept when using MS2 only
   */
  public FragmentScanSelection(@Nullable SpectraMerger merger,
      final @NotNull ScanSelectionFilter postMergingScanFilter,
      @Nullable MemoryMapStorage storage) {
    this.merger = merger;
    this.postMergingScanFilter = postMergingScanFilter;
    this.storage = storage;
  }

  public FragmentScanSelection(@Nullable SpectraMerger merger,
      final @NotNull ScanSelectionFilter postMergingScanFilter) {
    this(merger, postMergingScanFilter, null);
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

    var allScans = merger.getAllFragmentSpectra(scans);

    // TODO move at right position
    // filter out duplicates from the original scans list, same energy
//    switch (inputSpectra) {
//      case NONE -> {
//      }
//      case ALL -> allScans.addAll(noMergedScans);
//      case HIGHEST_TIC_PER_ENERGY ->
//          allScans.addAll(filterBestScansPerEnergy(byFragmentationEnergy.values()));
//    }

    if (postMergingScanFilter.isFilter()) {
      allScans.removeIf(postMergingScanFilter::matchesNot);
    }
    return allScans;
  }

  public boolean isMerging() {
    return merger != null;
  }

  public @Nullable IntensityMergingType intensityMergeType() {
    return merger != null ? merger.getIntensityMerging() : null;
  }

  public @Nullable SpectraMerger merger() {
    return merger;
  }

  public @NotNull IncludeInputSpectra inputSpectra() {
    return inputSpectra;
  }

  public @NotNull MsLevelFilter msLevelFilter() {
    return msLevelFilter;
  }

  public @Nullable MemoryMapStorage storage() {
    return storage;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FragmentScanSelection) obj;
    return Objects.equals(this.merger, that.merger) && Objects.equals(this.inputSpectra,
        that.inputSpectra) && Objects.equals(this.msLevelFilter, that.msLevelFilter)
           && Objects.equals(this.storage, that.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(merger, inputSpectra, msLevelFilter, storage);
  }

  @Override
  public String toString() {
    return "FragmentScanSelection[" + "merger=" + merger + ", " + "inputSpectra=" + inputSpectra
           + ", " + "msLevelFilter=" + msLevelFilter + ", " + "storage=" + storage + ']';
  }


  public enum IncludeInputSpectra {
    HIGHEST_TIC_PER_ENERGY, ALL, NONE
  }


}
