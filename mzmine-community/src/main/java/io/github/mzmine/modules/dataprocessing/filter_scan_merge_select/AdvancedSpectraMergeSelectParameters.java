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

import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypesParameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;


public class AdvancedSpectraMergeSelectParameters extends SimpleParameterSet {


  public static ComboParameter<SelectInputScans> includeInputScans = new ComboParameter<>(
      "Also include input scans", """
      Include input scans without merging in the final scan selection. Options:
      None (only use the merging settings below), single most intense scan, or all fragment scans.
      This parameter does only affect the final scan selection. The merging always uses all scans available.""",
      SelectInputScans.values(), SelectInputScans.NONE);

  public static final MergedSpectraFinalSelectionTypesParameter mergingOptions = new MergedSpectraFinalSelectionTypesParameter(
      MergedSpectraFinalSelectionTypes.values(),
      List.of(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES,
          MergedSpectraFinalSelectionTypes.ACROSS_ENERGIES));

  public static final ComboParameter<IntensityMergingType> intensityMergeType = new ComboParameter<>(
      "Intensity merge mode", "Defines the way intensity values are merged:\n" + Arrays.stream(
          IntensityMergingType.values()).map(IntensityMergingType::getDescription)
      .collect(Collectors.joining("\n")), IntensityMergingType.values(),
      IntensityMergingType.MAXIMUM);

  public static final MZToleranceParameter mergeMzTolerance = new MZToleranceParameter(
      "m/z tolerance", "The tolerance used to group signals during merging of spectra", 0.008, 25);

//  public static final AbsoluteAndRelativeIntParameter signalCountFilter = new AbsoluteAndRelativeIntParameter(
//      "Signal count filter",
//      "A signal is removed from the merged spectrum if it was detected in <X% or <N of the total source scans. If signals are present in all scans they are always retained, even if N is greater than the number of source scans. This is only applied on a per fragmentation energy level.",
//      null, new AbsoluteAndRelativeInt(1, 0.2f));

  public AdvancedSpectraMergeSelectParameters() {
    super(mergingOptions, mergeMzTolerance, intensityMergeType,
        // input spectra last - otherwise users may think this is the input into the merging
        includeInputScans);
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {
    var result = super.checkParameterValues(errorMessages);
    // check that combination is valid - either source scans or merged
    final List<MergedSpectraFinalSelectionTypes> finalSelection = getValue(mergingOptions);
    final boolean isNoInputScans = getValue(includeInputScans) == SelectInputScans.NONE;
    // only requires a valid merge selection if input scans are NONE
    if (isNoInputScans && !MergedSpectraFinalSelectionTypes.isValidSelection(finalSelection,
        false)) {

      errorMessages.add(
          "For fragment scan selection, either choose to include input scans (before merging) or valid merging parameters.");
      return false;
    }
    return result;
  }

  public static ParameterSet createInputScanParams(
      final @NotNull InputSpectraSelectParameters.SelectInputScans inputScans) {
    // mz tol and other merging specific values cannot be null but need to be set
    // will not be used when only input scans are selected
    return createParams(inputScans, List.of(), MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA,
        IntensityMergingType.MAXIMUM);
  }

  public static ParameterSet createParams(
      final @NotNull InputSpectraSelectParameters.SelectInputScans inputScans,
      final List<MergedSpectraFinalSelectionTypes> scanSelection, final MZTolerance mzTol,
      final IntensityMergingType intensityMerging) {
    var params = new AdvancedSpectraMergeSelectParameters().cloneParameterSet();

    params.setParameter(includeInputScans, inputScans);
    params.setParameter(mergeMzTolerance, mzTol);
    params.setParameter(mergingOptions, scanSelection);
    params.setParameter(intensityMergeType, intensityMerging);
    return params;
  }

  public SpectraMerger createMerger() {
    var scanSelection = getValue(mergingOptions);
    MZTolerance mzTol = getValue(mergeMzTolerance);
    var intensityMerging = getValue(intensityMergeType);
    return new SpectraMerger(scanSelection, mzTol, intensityMerging);
  }
}
