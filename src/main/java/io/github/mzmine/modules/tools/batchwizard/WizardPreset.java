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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardChromatographyParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardIonMobilityParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardMassSpectrometerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;

/**
 * @param name         the name of the preset
 * @param parentPreset name of the parent preset - needs to be one of the defined ones in the enums
 *                     for LC, IMS, MS, ...
 * @param part         to which part of the workflow it belongs
 * @param parameters   the parameters
 */
public record WizardPreset(String name, String parentPreset, WizardPart part,
                           ParameterSet parameters) {

  /**
   * Clones the parameter set to separate it from the static version
   *
   * @param name       name of the preset
   * @param part       of the workflow
   * @param parameters will be cloned
   */
  public WizardPreset(final String name, final String parentPreset, final WizardPart part,
      final ParameterSet parameters) {
    // needs the clone to separate the parameters from the static ones
    this.parameters = parameters.cloneParameterSet();
    this.name = name;
    this.parentPreset = parentPreset;
    this.part = part;
  }

  /**
   * Clones the parameter set to separate it from the static version
   *
   * @param name       name of the preset and its also the parent preset
   * @param part       of the workflow
   * @param parameters will be cloned
   */
  public WizardPreset(final String name, final WizardPart part, final ParameterSet parameters) {
    this(name, name, part, parameters);
  }

  public WizardPreset(ChromatographyDefaults defaults) {
    this(defaults.toString(), WizardPart.SAMPLE_INTRODUCTION_CHROMATOGRAPHY,
        WizardChromatographyParameters.create(defaults));
  }

  public WizardPreset(MsInstrumentDefaults defaults) {
    this(defaults.toString(), WizardPart.MS, WizardMassSpectrometerParameters.create(defaults));
  }

  public WizardPreset(final ImsDefaults defaults) {
    this(defaults.toString(), WizardPart.IMS, WizardIonMobilityParameters.create(defaults));
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Set the corresponding ParameterSetParameter in the wizard parameters to this parameters
   *
   * @param wizardParam the main parameters
   */
  public void setParametersToWizardParameters(final ParameterSet wizardParam) {
    wizardParam.getParameter(part.getParameterSetParameter())
        .setValue(parameters.cloneParameterSet());
  }

  public enum WizardPart {
    // order is important and reflects the order of the elements in the wizard
    DATA_IMPORT, SAMPLE_INTRODUCTION_CHROMATOGRAPHY, IMS, MS, FILTER, EXPORT;

    public Enum<?>[] getDefaultsEnum() {
      return switch (this) {
        // only one option
        case DATA_IMPORT, FILTER, EXPORT -> DefaultOptions.values();
        // multiple options
        case SAMPLE_INTRODUCTION_CHROMATOGRAPHY -> ChromatographyDefaults.values();
        case IMS -> ImsDefaults.values();
        case MS -> MsInstrumentDefaults.values();
      };
    }

    /**
     * @return the corresponding ParameterSetParameter to this preset
     */
    public ParameterSetParameter getParameterSetParameter() {
      return switch (this) {
        case DATA_IMPORT -> BatchWizardParameters.dataInputParams;
        case SAMPLE_INTRODUCTION_CHROMATOGRAPHY -> BatchWizardParameters.hplcParams;
        case IMS -> BatchWizardParameters.imsParameters;
        case MS -> BatchWizardParameters.msParams;
        case FILTER -> BatchWizardParameters.filterParameters;
        case EXPORT -> BatchWizardParameters.exportParameters;
      };
    }
  }

  /**
   * Everything that has only one option should use this enum
   */
  public enum DefaultOptions {
    DEFAULT
  }

  public enum ChromatographyDefaults {
    HPLC, UHPLC, HILIC, GC
  }

  public enum ImsDefaults {
    NO_IMS, tims, IMS;

    @Override
    public String toString() {
      return switch (this) {
        case NO_IMS -> " ";
        case tims, IMS -> super.toString();
      };
    }
  }

  public enum MsInstrumentDefaults {
    Orbitrap, qTOF
  }
}
