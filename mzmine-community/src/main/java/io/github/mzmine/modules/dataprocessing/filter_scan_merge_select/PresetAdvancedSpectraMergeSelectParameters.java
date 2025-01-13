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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select;

import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresetsParameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;


public class PresetAdvancedSpectraMergeSelectParameters extends SimpleParameterSet {

  public static final SpectraMergeSelectPresetsParameter preset = new SpectraMergeSelectPresetsParameter();

  public static final CheckComboParameter<MergedSpectraFinalSelectionTypes> sampleHandling = new CheckComboParameter<>(
      "Merge", """
      Merge spectra across all samples (default) and / or for each sample.
      If both options are included then spectra will be merged for each sample and across all samples.""",
      List.of(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES,
          MergedSpectraFinalSelectionTypes.EACH_SAMPLE),
      List.of(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES), true);

  public static final ComboParameter<IntensityMergingType> intensityMergeType = new ComboParameter<>(
      "Intensity merge mode", """
                                  Defines the way intensity values are merged:
                                  """ + Arrays.stream(IntensityMergingType.values())
                                  .map(IntensityMergingType::getDescription)
                                  .collect(Collectors.joining("\n")), IntensityMergingType.values(),
      IntensityMergingType.MAXIMUM);

  public static final MZToleranceParameter mergeMzTolerance = new MZToleranceParameter(
      "Merging m/z tolerance", "The tolerance used to group signals during merging of spectra",
      0.008, 25);

//  public static final AbsoluteAndRelativeIntParameter signalCountFilter = new AbsoluteAndRelativeIntParameter(
//      "Signal count filter",
//      "A signal is removed from the merged spectrum if it was detected in <X% or <N of the total source scans. If signals are present in all scans they are always retained, even if N is greater than the number of source scans. This is only applied on a per fragmentation energy level.",
//      null, new AbsoluteAndRelativeInt(1, 0.2f));

  public PresetAdvancedSpectraMergeSelectParameters() {
    super(preset, mergeMzTolerance, sampleHandling, intensityMergeType);
  }

  /**
   * Set all values in the parameter set
   *
   * @param params usually an instance of this {@link PresetAdvancedSpectraMergeSelectParameters}
   */
  public static void setAll(final @NotNull ParameterSet params, SpectraMergeSelectPresets preset,
      MZTolerance mzTol, List<MergedSpectraFinalSelectionTypes> sampleHandling,
      IntensityMergingType intensityMergingType) {
    for (final MergedSpectraFinalSelectionTypes handling : sampleHandling) {
      if (!handling.isSampleDefinition()) {
        throw new IllegalArgumentException(
            "Sample handling list should only contain values to define each or all samples");
      }
    }

    params.setParameter(PresetAdvancedSpectraMergeSelectParameters.preset, preset);
    params.setParameter(PresetAdvancedSpectraMergeSelectParameters.mergeMzTolerance, mzTol);
    params.setParameter(PresetAdvancedSpectraMergeSelectParameters.sampleHandling, sampleHandling);
    params.setParameter(PresetAdvancedSpectraMergeSelectParameters.intensityMergeType,
        intensityMergingType);
  }

}
