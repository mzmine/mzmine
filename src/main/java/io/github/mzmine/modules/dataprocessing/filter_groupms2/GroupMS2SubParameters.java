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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class GroupMS2SubParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(
      "MS1 to MS2 precursor tolerance (m/z)",
      "Describes the tolerance between the precursor ion in a MS1 scan and the precursor "
          + "m/z assigned to the MS2 scan.", 0.01, 10);

  public static final RTToleranceParameter rtTol = new RTToleranceParameter(
      "Retention time tolerance",
      "The maximum offset between the highest point of the chromatographic peak and the time the MS2 was acquired.",
      new RTTolerance(0.2f, Unit.MINUTES));

  public static final BooleanParameter limitRTByFeature = new BooleanParameter("Limit by RT edges",
      "Use the feature's edges (retention time) as a filter.", true);

  public static final BooleanParameter combineTimsMsMs = new BooleanParameter(
      "Combine MS/MS spectra (TIMS)",
      "If checked, all MS/MS spectra assigned to a feature will be merged into a single spectrum.",
      false);

  public static final BooleanParameter lockMS2ToFeatureMobilityRange = new BooleanParameter(
      "Lock to feature mobility range",
      "If checked, only mobility scans from the mobility range of the feature will be merged.\n"
          + "This is usually not needed. However, if isomers/isobars elute at the same retention time and are close in mobility, "
          + "the MS/MS window might be larger than the peak in mobility dimension and thus cause chimeric MS/MS spectra.\n"
          + "This can be investigated in the \"All MS MS\" window", false);

  public static final OptionalParameter<DoubleParameter> outputNoiseLevel = new OptionalParameter<>(
      new DoubleParameter("Minimum merged intensity (absolute, IMS)",
          "If an ion mobility spectrometry (IMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.",
          MZmineCore.getConfiguration().getIntensityFormat(), 250d, 0d, Double.MAX_VALUE));

  public static final OptionalParameter<PercentParameter> outputNoiseLevelRelative = new OptionalParameter<>(
      new PercentParameter("Minimum merged intensity (relative, IMS)",
          "If an ion mobility spectrometry (IMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.", 0.01d), true);

  public GroupMS2SubParameters() {
    super(new Parameter[]{rtTol, mzTol, limitRTByFeature, combineTimsMsMs,
        lockMS2ToFeatureMobilityRange, outputNoiseLevel, outputNoiseLevelRelative});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
