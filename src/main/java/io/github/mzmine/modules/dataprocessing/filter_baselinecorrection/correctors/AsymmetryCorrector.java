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

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;

/**
 * @description Asymmetric baseline corrector. Estimates a trend based on asymmetric least squares.
 *              Uses "asysm" feature from "ptw" R-package
 *              (http://cran.r-project.org/web/packages/ptw/ptw.pdf).
 * 
 */
public class AsymmetryCorrector extends BaselineCorrector {

  @Override
  public String[] getRequiredRPackages() {
    return new String[] { /* "rJava", "Rserve", */"ptw"};
  }

  @Override
  public double[] computeBaseline(final RSessionWrapper rSession, final RawDataFile origDataFile,
      double[] chromatogram, ParameterSet parameters) throws RSessionWrapperException {

    // Smoothing and asymmetry parameters.
    final double smoothing =
        parameters.getParameter(AsymmetryCorrectorParameters.SMOOTHING).getValue();
    final double asymmetry =
        parameters.getParameter(AsymmetryCorrectorParameters.ASYMMETRY).getValue();

    // Compute baseline.
    final double[] baseline;

    // try {
    // Set chromatogram.
    // rSession.assignDoubleArray("chromatogram", chromatogram);
    rSession.assign("chromatogram", chromatogram);
    // Calculate baseline.
    rSession.eval("baseline <- asysm(chromatogram," + smoothing + ',' + asymmetry + ')');
    // baseline = rSession.collectDoubleArray("baseline");
    baseline = (double[]) rSession.collect("baseline");
    // Done: Refresh R code stack
    rSession.clearCode();

    // }
    // catch (Throwable t) {
    // //t.printStackTrace();
    // throw new
    // IllegalStateException("R error during baseline correction (" +
    // this.getName() + ").", t);
    // }

    return baseline;
  }

  @Override
  public @NotNull String getName() {
    return "Asymmetric baseline corrector";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return AsymmetryCorrectorParameters.class;
  }

}
