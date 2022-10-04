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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class RecursiveIMSBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "The m/z tolerance to build ion traces. The tolerance is specified as a +- tolerance. "
          + "m/z 500.000 with a tolerance of 0.01 will allow m/z 499.99 to 501.01.",0.005, 15);

  public static final IntegerParameter minNumConsecutive = new IntegerParameter(
      "Minimum consecutive retention time data points",
      "The minimum number of consecutive detections in frames (retention time dimension).", 5);

  public static final IntegerParameter minNumDatapoints = new IntegerParameter(
      "Minimum number of data points",
      "The minimum number of consecutive detections in frames (retention time dimension).", 100);

  public static final ParameterSetParameter advancedParameters =
      new ParameterSetParameter("Advanced parameters",
          "Allows adjustment of internal binning parameters for mobilograms",
          new RecursiveIMSBuilderAdvancedParameters());

  public RecursiveIMSBuilderParameters() {
    super(new Parameter[]{rawDataFiles, scanSelection, mzTolerance, minNumConsecutive,
        minNumDatapoints, advancedParameters},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ims-ms_featdet/recursive_ims_builder/recursive-ims-builder.html");
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
