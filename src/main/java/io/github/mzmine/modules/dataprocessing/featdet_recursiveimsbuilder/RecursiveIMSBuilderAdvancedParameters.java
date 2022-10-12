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

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_DTIMS_BIN_WIDTH;
import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_TIMS_BIN_WIDTH;
import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_TWIMS_BIN_WIDTH;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class RecursiveIMSBuilderAdvancedParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> timsBinningWidth = new OptionalParameter<>(
      new IntegerParameter("Override default TIMS binning width (Vs/cm²)",
          "The binning width in mobility units of the selected raw data file.\n"
              + " The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
          DEFAULT_TIMS_BIN_WIDTH, 1, 1000));

  public static final OptionalParameter<IntegerParameter> twimsBinningWidth = new OptionalParameter(
      new IntegerParameter(
          "Travelling wave binning width (ms)",
          "The binning width in mobility units of the selected raw data file."
              + "The default binning width is " + DEFAULT_TWIMS_BIN_WIDTH + ".",
          DEFAULT_TWIMS_BIN_WIDTH, 1, 1000));

  public static final OptionalParameter<IntegerParameter> dtimsBinningWidth = new OptionalParameter<>(
      new IntegerParameter(
          "Drift tube binning width (ms)",
          "The binning width in mobility units of the selected raw data file.\n"
              + "The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
          DEFAULT_DTIMS_BIN_WIDTH, 1, 1000));

  public RecursiveIMSBuilderAdvancedParameters() {
    super(new Parameter[]{timsBinningWidth, dtimsBinningWidth, twimsBinningWidth});
  }
}

