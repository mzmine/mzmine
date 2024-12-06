/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancedSpectraImportParameters extends SimpleParameterSet {

  public static final ScanSelectionParameter scanFilter = new ScanSelectionParameter();

  public static final OptionalParameter<DoubleRangeParameter> mzRange = new OptionalParameter<>(
      new DoubleRangeParameter("Crop MS1 m/z", "m/z boundary of the cropped region",
          MZmineCore.getConfiguration().getMZFormat()), false);

  public static final OptionalParameter<ModuleOptionsEnumComboParameter<MassDetectors>> msMassDetection = new OptionalParameter<>(
      new ModuleOptionsEnumComboParameter<>("MS1 detector (Advanced)",
          "Algorithm to use on MS1 scans for mass detection and its parameters",
          MassDetectors.FACTOR_OF_LOWEST), false);

  public static final OptionalParameter<ModuleOptionsEnumComboParameter<MassDetectors>> ms2MassDetection = new OptionalParameter<>(
      new ModuleOptionsEnumComboParameter<>("MS2 detector (Advanced)",
          "Algorithm to use on MS2 scans for mass detection and its parameters",
          MassDetectors.FACTOR_OF_LOWEST), false);

  public static final BooleanParameter denormalizeMSnScans = new BooleanParameter(
      "Denormalize fragment scans (traps)", """
      Denormalize MS2 (MSn) scans by multiplying with the injection time. Encouraged before spectral merging.
      (only available in trap-based systems, like Orbitraps, trapped ion mobility spectrometry (tims), etc)
      This reduces the intensity differences between spectra acquired with different injection times
      and reverts to "raw" intensities.""", false);


  public AdvancedSpectraImportParameters() {
    super(scanFilter, mzRange, msMassDetection, ms2MassDetection, denormalizeMSnScans);
  }

  /**
   * Create new instance copy and set all values
   */
  @NotNull
  public static AdvancedSpectraImportParameters create(@NotNull MassDetectorWizardOptions detector,
      @Nullable Double ms1NoiseLevel, @Nullable Double ms2NoiseLevel,
      @Nullable Range<Double> mzRangeFilter, @NotNull ScanSelection scanFilter,
      boolean denormMsnScans) {
    var params = (AdvancedSpectraImportParameters) new AdvancedSpectraImportParameters().cloneParameterSet();

    params.setParameter(AdvancedSpectraImportParameters.msMassDetection, ms1NoiseLevel != null);
    params.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, ms2NoiseLevel != null);
    params.setParameter(AdvancedSpectraImportParameters.mzRange, mzRangeFilter != null,
        mzRangeFilter);
    params.setParameter(AdvancedSpectraImportParameters.scanFilter, scanFilter);
    params.setParameter(AdvancedSpectraImportParameters.denormalizeMSnScans, denormMsnScans);
    // create centroid mass detectors
    if (ms1NoiseLevel != null) {
      var mdParam = detector.createMassDetectorParameters(ms1NoiseLevel);
      params.getParameter(AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter()
          .setValue(mdParam.value(), mdParam.parameters());

    }
    if (ms2NoiseLevel != null) {
      var mdParam = detector.createMassDetectorParameters(ms2NoiseLevel);
      params.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter()
          .setValue(mdParam.value(), mdParam.parameters());
    }

    return params;
  }

}
