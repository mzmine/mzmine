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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;

public class ExportFragmentScansFeatureParameters extends SimpleParameterSet {

  public static final OptionalModuleParameter<HandleChimericMsMsParameters> handleChimerics = new OptionalModuleParameter<>(
      "Handle chimeric spectra",
      "Options to identify and handle chimeric spectra with multiple MS1 signals in the precursor ion selection",
      new HandleChimericMsMsParameters(), true);

  public static final SpectraMergeSelectParameter merging = SpectraMergeSelectParameter.createFullSetupWithSimplePreset(
      SpectraMergeSelectPresets.REPRESENTATIVE_MSn_TREE);

  public static final IntegerParameter minSignals = new IntegerParameter("Minimum signals",
      "Minimum number of signals filter to skip export of spectra with less data points. This is done after potential merging.",
      1, true, 0, null);

  public ExportFragmentScansFeatureParameters() {
    super(handleChimerics, merging, minSignals);
  }
}
