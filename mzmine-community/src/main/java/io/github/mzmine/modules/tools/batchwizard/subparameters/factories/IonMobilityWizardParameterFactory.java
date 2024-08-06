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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 */
public enum IonMobilityWizardParameterFactory implements WizardParameterFactory {
  NO_IMS,
  /**
   * TIMS actually is a different workflow than the rest. slight changes because of MS2 acquisition
   * in PASEF
   */
  TIMS, IMS, DTIMS, TWIMS;

  @Override
  public String toString() {
    return switch (this) {
      case NO_IMS -> "";
      case TIMS, IMS -> super.toString();
      case DTIMS -> "DTIMS";
      case TWIMS -> "TWIMS";
    };
  }

  /**
   * Create parameters from defaults
   */
  @Override
  public WizardStepParameters create() {
    return switch (this) {
      case NO_IMS -> new IonMobilityWizardParameters(this, 5, 0.01, true, false, MobilityType.NONE);
      case TIMS -> new IonMobilityWizardParameters(this, 5, 0.01, true, true, MobilityType.TIMS);
      case IMS -> new IonMobilityWizardParameters(this, 5, 0.01, true, true, MobilityType.OTHER);
      case DTIMS ->
          new IonMobilityWizardParameters(this, 4, 0.7, true, true, MobilityType.DRIFT_TUBE);
      case TWIMS ->
          new IonMobilityWizardParameters(this, 4, 0.4, true, true, MobilityType.TRAVELING_WAVE);
    };
  }

  @Override
  public String getUniqueId() {
    return name();
  }


  /**
   * Not all combinations work.
   *
   * @return supported combinations
   */
  public MassSpectrometerWizardParameterFactory[] getMatchingMassSpectrometerPresets() {
    return switch (this) {
      case TWIMS, DTIMS ->
          new MassSpectrometerWizardParameterFactory[]{MassSpectrometerWizardParameterFactory.QTOF};
      case TIMS ->
          new MassSpectrometerWizardParameterFactory[]{MassSpectrometerWizardParameterFactory.QTOF,
              MassSpectrometerWizardParameterFactory.FTICR};
      case NO_IMS, IMS -> MassSpectrometerWizardParameterFactory.values();
    };
  }
}
