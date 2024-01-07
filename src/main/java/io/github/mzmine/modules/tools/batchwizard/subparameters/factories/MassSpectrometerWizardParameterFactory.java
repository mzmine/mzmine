/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 */
public enum MassSpectrometerWizardParameterFactory implements WizardParameterFactory {
  Orbitrap, QTOF, FTICR, LOW_RES;

  /**
   * Special presets derived from IMS go here
   */
  public static MassSpectrometerWizardParameters createForIms(
      IonMobilityWizardParameterFactory ims) {
    return switch (ims) {
      case NO_IMS, IMS, DTIMS, TWIMS -> null;
      case TIMS ->
          new MassSpectrometerWizardParameters(QTOF, MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL,
              500, 1E2, 1.0E3, new MZTolerance(0.005, 20), new MZTolerance(0.0015, 3),
              new MZTolerance(0.004, 8));
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case Orbitrap, QTOF, FTICR -> super.toString();
      case LOW_RES -> "Low res.";
    };
  }

  @Override
  public String getUniqueId() {
    return name();
  }

  /**
   * User options for instruments go here
   */
  @Override
  public WizardStepParameters create() {
    return switch (this) {
      case QTOF ->
          new MassSpectrometerWizardParameters(this, MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL,
              5E2, 1E2, 1E3, new MZTolerance(0.005, 20), new MZTolerance(0.0015, 3),
              new MZTolerance(0.004, 8));
      case Orbitrap -> new MassSpectrometerWizardParameters(this,
          MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 5, 2.5, 5E4,
          new MZTolerance(0.002, 10), new MZTolerance(0.0015, 3), new MZTolerance(0.0015, 5));
      // TODO optimize some defaults
      case FTICR -> new MassSpectrometerWizardParameters(this,
          MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 5, 2.5, 1E3,
          new MZTolerance(0.0005, 5), new MZTolerance(0.0005, 2), new MZTolerance(0.0005, 3.5));
      case LOW_RES ->
          new MassSpectrometerWizardParameters(this, MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL,
              0, 0, 0, new MZTolerance(0.5, 0), new MZTolerance(0.5, 0), new MZTolerance(0.5, 0));
    };
  }
}
