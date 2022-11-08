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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class LibraryExportQualityParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> minNumSignals = new OptionalParameter<>(
      new IntegerParameter("Minimum number of signals",
          "Number of signals an MS2 must contain to be exported.", 3), true);

  public static final OptionalParameter<PercentParameter> minExplainedSignals = new OptionalParameter<>(
      new PercentParameter("Minimum explained signals (%)",
          "Minimum number of explained signals in an MSMS.", 0.4d), false);

  public static final OptionalParameter<PercentParameter> minExplainedIntensity = new OptionalParameter<>(
      new PercentParameter("Minimum explained intensity (%)",
          "Minimum explained intensity in an MSMS.", 0.4d), false);

  public static final MZToleranceParameter formulaTolerance = new MZToleranceParameter(
      "Formula m/z tolerance", "m/z tolerance to assign MSMS signals to a formula.", 0.003, 10.0d);

  public static final BooleanParameter exportExplainedSignalsOnly = new BooleanParameter(
      "Export explained signals only",
      "Only explained signals will be exported to the library spectrum.", false);

  public static final BooleanParameter exportFlistNameMatchOnly = new BooleanParameter(
      "Match compound and feature list name",
      "Only export MS/MS spectra if the feature list name contains the compound name.");

  public LibraryExportQualityParameters() {
    super(
        new Parameter[]{minNumSignals, minExplainedSignals, minExplainedIntensity, formulaTolerance,
            exportExplainedSignalsOnly, exportFlistNameMatchOnly});
  }

  public MsMsQualityChecker toQualityChecker() {
    return new MsMsQualityChecker(
        getValue(minNumSignals) ? getEmbeddedParameterValue(minNumSignals) : null,
        getValue(minExplainedSignals) ? getEmbeddedParameterValue(minExplainedSignals) : null,
        getValue(minExplainedIntensity) ? getEmbeddedParameterValue(minExplainedIntensity) : null,
        getValue(formulaTolerance), getValue(exportExplainedSignalsOnly),
        getValue(exportFlistNameMatchOnly));
  }
}
