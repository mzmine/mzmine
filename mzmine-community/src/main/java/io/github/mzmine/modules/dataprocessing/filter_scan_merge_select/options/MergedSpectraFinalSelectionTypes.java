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
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Final selection types that direct the selection / filtering of merged scans. Many of these types
 * must be seen in combination like ACROSS_SAMPLES and MSN_TREE would mean there is one MSN_TREE
 * merged across samples. By adding also EACH_SAMPLE there will be additional trees for each sample.
 * By adding EACH_ENERGY there will be additional scans for each fragmentation energy.
 * <p>
 * ACROSS_SAMPLES and ACROSS_ENERGIES will result in one scan merged across all
 * <p>
 * MSN_PSEUDO_MS2 will generate one MS2 scan from all MSn without the need to retain the whole tree
 */
public enum MergedSpectraFinalSelectionTypes implements UniqueIdSupplier {
  // across samples
  ACROSS_SAMPLES, EACH_SAMPLE,

  // fragmentation energies
  ACROSS_ENERGIES, EACH_ENERGY,

  // MSn
  MSN_TREE, MSN_PSEUDO_MS2;

  public static boolean isActive(Collection<MergedSpectraFinalSelectionTypes> types) {
    return !isEmptySelection(types) &&
           // at least one of the merging types
           (types.contains(ACROSS_ENERGIES) || types.contains(EACH_ENERGY) //
            || types.contains(MSN_TREE) || types.contains(MSN_PSEUDO_MS2));
  }

  public static boolean containsSampleDefinition(
      Collection<MergedSpectraFinalSelectionTypes> types) {
    if (types == null) {
      return false;
    }
    return types.stream().anyMatch(MergedSpectraFinalSelectionTypes::isSampleDefinition);
  }

  public static boolean isEmptySelection(Collection<MergedSpectraFinalSelectionTypes> types) {
    return types == null || types.isEmpty();
  }

  /**
   * @return true if empty and is allowed or if a matching combination was selected
   */
  public static boolean isValidSelection(Collection<MergedSpectraFinalSelectionTypes> types,
      boolean allowEmptySelection) {
    // valid is if inactive or if active and one of sample option is selected
    return (allowEmptySelection && isEmptySelection(types))
           // true selection
           || (isActive(types) && containsSampleDefinition(types));
  }

  public static String getAdvancedValidDescription() {
    return """
        When merging and selecting fragmentation spectra in advanced mode, make sure to select at
        least one combination of SAMPLE and ENERGY handling like %s with %s to have a valid selection.
        Add more options to include more scans in the final selection.
        MSn types only affect MSn data.
        Otherwise, merging will be turned off.""".formatted(ACROSS_SAMPLES, ACROSS_ENERGIES);
  }

  @Override
  public String toString() {
    return switch (this) {
      case ACROSS_SAMPLES -> "Across samples";
      case EACH_SAMPLE -> "Each sample";
      case ACROSS_ENERGIES -> "Across energies";
      case EACH_ENERGY -> "Each energy";
      case MSN_TREE -> "MSn tree";
      case MSN_PSEUDO_MS2 -> "MSn to pseudo MS2";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case ACROSS_SAMPLES -> "across_samples";
      case EACH_SAMPLE -> "each_sample";
      case ACROSS_ENERGIES -> "across_energies";
      case EACH_ENERGY -> "each_energy";
      case MSN_TREE -> "msn_trees";
      case MSN_PSEUDO_MS2 -> "msn_pseudo_ms2";
    };
  }

  public boolean isSampleDefinition() {
    return switch (this) {
      case ACROSS_SAMPLES, EACH_SAMPLE -> true;
      case EACH_ENERGY, ACROSS_ENERGIES, MSN_TREE, MSN_PSEUDO_MS2 -> false;
    };
  }

  public boolean isRequireMSn() {
    return switch (this) {
      case MSN_TREE, MSN_PSEUDO_MS2 -> true;
      case ACROSS_SAMPLES, EACH_SAMPLE, ACROSS_ENERGIES, EACH_ENERGY -> false;
    };
  }
}