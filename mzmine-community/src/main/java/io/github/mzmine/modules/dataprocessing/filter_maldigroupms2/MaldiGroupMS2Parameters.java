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

package io.github.mzmine.modules.dataprocessing.filter_maldigroupms2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class MaldiGroupMS2Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();
  public static final MZToleranceParameter mzTol = new MZToleranceParameter(
      "MS1 to MS2 precursor tolerance (m/z)",
      "Describes the tolerance between the precursor ion in a MS1 scan and the precursor "
          + "m/z assigned to the MS2 scan.", 0.01, 10);
  public static final BooleanParameter combineTimsMsMs = new BooleanParameter(
      "Combine MS/MS spectra (TIMS)",
      "If checked, all assigned MS/MS spectra with will also be merged into a single MS/MS spectrum.",
      false);
  public static final BooleanParameter lockMS2ToFeatureMobilityRange = new BooleanParameter(
      "Lock to feature mobility range",
      "If checked, only mobility scans from the mobility range of the feature will be merged.\n"
          + "This is usually not needed. However, if isomers/isobars elute at the same retention time and are close in mobility, "
          + "the MS/MS window might be larger than the peak in mobility dimension and thus cause chimeric MS/MS spectra.\n"
          + "This can be investigated in hte \"All MS MS\" window", false);
  public static final OptionalParameter<DoubleParameter> outputNoiseLevel = new OptionalParameter<>(
      new DoubleParameter("Minimum merged intensity (IMS)",
          "If an ion mobility spectrometry (IMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.",
          MZmineCore.getConfiguration().getIntensityFormat(), 250d, 0d, Double.MAX_VALUE));
  public static final OptionalParameter<PercentParameter> outputNoiseLevelRelative = new OptionalParameter<>(
      new PercentParameter("Minimum merged intensity (relative, IMS)",
          "If an ion mobility spectrometry (IMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.", 0.01d));

  // disabled for now, messes with project save/load because one ms2 spectrum has more than 1 raw data file.
  /*  public static final OptionalParameter<IntegerParameter> consensusMinSignals = new OptionalParameter<>(
      new IntegerParameter("Create consensus MS2 with min. signals",
          "If enabled, a consensus MS2 spectrum across all collision energies will be created, with the given minimum signals for each peak.",
          3));*/
  private static final RawDataFilesSelection rawSelection = new RawDataFilesSelection(
      RawDataFilesSelectionType.NAME_PATTERN);
  public static final RawDataFilesParameter files = new RawDataFilesParameter(rawSelection);

  static {
    rawSelection.setNamePattern("*_MSMS*");
  }

  public MaldiGroupMS2Parameters() {
    super(new Parameter[]{PEAK_LISTS, files, mzTol, combineTimsMsMs, lockMS2ToFeatureMobilityRange,
            outputNoiseLevel, outputNoiseLevelRelative/*, consensusMinSignals*/},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ms2_scan_pairing/ms2_scan_pairing.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
