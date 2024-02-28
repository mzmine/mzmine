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
 * @description Rubber Band baseline corrector. Estimates a trend based on Rubber Band algorithm
 *              (which determines a convex envelope for the spectra - underneath side). Uses
 *              "spc.rubberband" feature from "hyperSpec" R-package
 *              (http://cran.r-project.org/web/packages /hyperSpec/vignettes/baseline.pdf).
 * 
 */
public class RubberBandCorrector extends BaselineCorrector {

  @Override
  public String[] getRequiredRPackages() {
    return new String[] { /* "rJava", "Rserve", */"hyperSpec"};
  }

  @Override
  public double[] computeBaseline(final RSessionWrapper rSession, final RawDataFile origDataFile,
      double[] chromatogram, ParameterSet parameters) throws RSessionWrapperException {

    // Rubber Band parameters.
    double noise = parameters.getParameter(RubberBandCorrectorParameters.NOISE).getValue();
    boolean autoNoise =
        parameters.getParameter(RubberBandCorrectorParameters.AUTO_NOISE).getValue();
    double df = parameters.getParameter(RubberBandCorrectorParameters.DF).getValue();
    boolean spline = parameters.getParameter(RubberBandCorrectorParameters.SPLINE).getValue();
    double bend = parameters.getParameter(RubberBandCorrectorParameters.BEND_FACTOR).getValue();

    final double[] baseline;

    // Set chromatogram.
    rSession.assign("chromatogram", chromatogram);
    // Transform chromatogram.
    rSession.eval("mat <- matrix(chromatogram, nrow=1)");
    rSession.eval("spc <- new (\"hyperSpec\", spc = mat, wavelength = as.numeric(seq(" + 1 + ", "
        + chromatogram.length + ")))");
    // Auto noise ?
    rSession.eval("noise <- " + ((autoNoise) ? "min(mat)" : "" + noise));
    // Bend
    rSession.eval("bend <- " + bend + " * wl.eval(spc, function(x) x^2, normalize.wl=normalize01)");
    // Calculate baseline.
    rSession.eval("baseline <- spc.rubberband(spc + bend, noise = noise, df = " + df + ", spline="
        + (spline ? "T" : "F") + ") - bend");
    // 'NA' might appear in 'baseline' array when 'spline' parameter set to
    // 'FALSE',
    // So handle them properly if necessary...
    rSession.eval(
        "if (is.na(baseline)) { baseline[is.na(baseline)] <- " + RSessionWrapper.NA_DOUBLE + " }");
    rSession.eval("baseline <- orderwl(baseline)[[1]]");
    baseline = ((double[][]) rSession.collect("baseline"))[0];
    // Done: Refresh R code stack
    rSession.clearCode();

    return baseline;
  }

  @Override
  public @NotNull String getName() {
    return "RubberBand baseline corrector";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return RubberBandCorrectorParameters.class;
  }

}
