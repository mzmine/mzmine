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

package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchMode;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The recommended settings of one of the two NIST workflows.
 * <p>
 * Kept in one place because two things need them and must not drift apart: the presets offered by
 * the presets button of the setup dialog, see
 * {@link NistMsSearchParameters#createDefaultPresets()}, and the migration of parameters saved
 * before the search type and the tolerances existed, see
 * {@link NistMsSearchParameters#handleLoadedParameters}.
 *
 * @param presetName         the name the preset is offered under.
 * @param mode               the search type, which also picks the libraries.
 * @param minSimilarity      the minimum similarity on mzmine's 0 to 1 scale.
 * @param precursorTolerance precursor m/z tolerance, ignored by the GC-EI searches.
 * @param fragmentTolerance  fragment m/z tolerance, ignored by the GC-EI searches.
 * @param integerMz          the unit mass merging to switch on, or null to leave it off.
 * @param mergePreset        how the spectra of a row are merged and selected.
 * @param mergeTolerance     the m/z tolerance used while merging.
 */
public record NistSearchDefaults(@NotNull String presetName, @NotNull NistSearchMode mode,
                                 double minSimilarity, @NotNull MZTolerance precursorTolerance,
                                 @NotNull MZTolerance fragmentTolerance,
                                 @Nullable IntegerMode integerMz,
                                 @NotNull SpectraMergeSelectPresets mergePreset,
                                 @NotNull MZTolerance mergeTolerance) {

  /**
   * Unit mass EI spectra against the EI libraries. Everything is unit mass, so a nominal mass is
   * one bin everywhere.
   */
  public static final NistSearchDefaults GC_EI = new NistSearchDefaults("GC-EI (low resolution)",
      NistSearchMode.GC_EI_IDENTITY, 0.75, MZTolerance.UNIT_MASS_TOLERANCE,
      MZTolerance.UNIT_MASS_TOLERANCE, IntegerMode.SUM,
      SpectraMergeSelectPresets.SINGLE_MERGED_SCAN, MZTolerance.UNIT_MASS_TOLERANCE);

  /**
   * Accurate mass MS/MS spectra against the tandem libraries. The spectra must not be binned, so
   * {@link #integerMz()} stays off.
   */
  public static final NistSearchDefaults MSMS = new NistSearchDefaults("MS/MS (high resolution)",
      NistSearchMode.MSMS_HIRES, 0.7, MZTolerance.WIDE_25_PPM_OR_10_MDA, new MZTolerance(0.01, 40),
      null, SpectraMergeSelectPresets.REPRESENTATIVE_SCANS, MZTolerance.WIDE_25_PPM_OR_10_MDA);

  /**
   * @return both workflows, in the order the presets are offered.
   */
  public static @NotNull NistSearchDefaults[] values() {
    return new NistSearchDefaults[]{GC_EI, MSMS};
  }
}
