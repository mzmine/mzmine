/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleComboParameter;

public class AdvancedSpectraImportParameters extends SimpleParameterSet {

  public static final OptionalParameter<ModuleComboParameter<MassDetector>> msMassDetection = new OptionalParameter<>(
      new ModuleComboParameter<MassDetector>("MS1 detector (Advanced)",
          "Algorithm to use on MS1 scans for mass detection and its parameters",
          MassDetectionParameters.massDetectors, MassDetectionParameters.massDetectors[0]));

  public static final OptionalParameter<ModuleComboParameter<MassDetector>> ms2MassDetection = new OptionalParameter<>(
      new ModuleComboParameter<MassDetector>("MS2 detector (Advanced)",
          "Algorithm to use on MS2 scans for mass detection and its parameters",
          MassDetectionParameters.massDetectors, MassDetectionParameters.massDetectors[0]));

  public static final BooleanParameter denormalizeMSnScans = new BooleanParameter(
      "Denormalize fragment scans (traps)", """
      Denormalize MS2 (MSn) scans by multiplying with the injection time. Encouraged before spectral merging.
      (only available in trap-based systems, like Orbitraps, trapped ion mobility spectrometry (tims), etc)
      This reduces the intensity differences between spectra acquired with different injection times
      and reverts to "raw" intensities.""", false);

  public AdvancedSpectraImportParameters() {
    super(msMassDetection, ms2MassDetection, denormalizeMSnScans);
  }

}
