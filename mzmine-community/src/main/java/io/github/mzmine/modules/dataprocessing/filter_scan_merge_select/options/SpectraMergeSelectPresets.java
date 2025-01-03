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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.AdvancedSpectraMergeSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetAdvancedSpectraMergeSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetSimpleSpectraMergeSelectModule;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * These presets are used by different merging and select modules:
 * {@link PresetAdvancedSpectraMergeSelectModule} and {@link PresetSimpleSpectraMergeSelectModule}
 * <p>
 * They are shortcuts to setting up the advanced {@link AdvancedSpectraMergeSelectModule} with
 * predefined target spectra.
 */
public enum SpectraMergeSelectPresets implements UniqueIdSupplier {
  SINGLE_MERGED_SCAN, REPRESENTATIVE_SCANS, MSn_TREE, REPRESENTATIVE_MSn_TREE;

  public static SpectraMergeSelectPresets getDefault() {
    return SINGLE_MERGED_SCAN;
  }

  public static SpectraMergeSelectPresets[] defaultNoMSnTrees() {
    return new SpectraMergeSelectPresets[]{SINGLE_MERGED_SCAN, REPRESENTATIVE_SCANS};
  }

  @Override
  public String toString() {
    return switch (this) {
      case SINGLE_MERGED_SCAN -> "Single scan: Merged across energies";
      case REPRESENTATIVE_SCANS -> "Representative scans: Each energy & merged across energies";
      case MSn_TREE -> "MSn tree: Merged across energies & pseudo MS2";
      case REPRESENTATIVE_MSn_TREE ->
          "Representative scans or MSn tree: Each energy, merged across energies & pseudo MS2";
    };
  }

  public String getDescription() {
    final String description = switch (this) {
      case SINGLE_MERGED_SCAN -> "";
      case REPRESENTATIVE_SCANS -> "";
      case MSn_TREE -> "";
      case REPRESENTATIVE_MSn_TREE -> "";
    };

    return "%s (%s)".formatted(this, description);
  }

  /**
   * The {@link MergedSpectraFinalSelectionTypes} that define the final selection of scans. This
   * method adds scan types based on presets. Sample handling like
   * {@link MergedSpectraFinalSelectionTypes#ACROSS_SAMPLES} and
   * {@link MergedSpectraFinalSelectionTypes#EACH_SAMPLE} need to be added to make this a complete
   * selection.
   */
  public List<MergedSpectraFinalSelectionTypes> listIncludedScanTypes() {
    List<MergedSpectraFinalSelectionTypes> types = new ArrayList<>();
    types.add(MergedSpectraFinalSelectionTypes.ACROSS_ENERGIES); // always across energies
    types.add(MergedSpectraFinalSelectionTypes.MSN_PSEUDO_MS2); // MSn -> pseudo MS2, only MSn data
    // this would now result in one scan merge across samples and energies - all other dropped
    // or 2 scans for MSn
    // add more specific scans
    switch (this) {
      case SINGLE_MERGED_SCAN -> {
      }
      case REPRESENTATIVE_SCANS -> types.add(MergedSpectraFinalSelectionTypes.EACH_ENERGY);
      case MSn_TREE -> {
        types.add(MergedSpectraFinalSelectionTypes.MSN_TREE);
      }
      case REPRESENTATIVE_MSn_TREE -> {
        types.add(MergedSpectraFinalSelectionTypes.MSN_TREE);
        types.add(MergedSpectraFinalSelectionTypes.EACH_ENERGY);
      }
    }
    return types;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case SINGLE_MERGED_SCAN -> "single_merged_scan";
      case REPRESENTATIVE_SCANS -> "representative_scans";
      case MSn_TREE -> "msn_trees";
      case REPRESENTATIVE_MSn_TREE -> "representative_msn_trees";
    };
  }
}
