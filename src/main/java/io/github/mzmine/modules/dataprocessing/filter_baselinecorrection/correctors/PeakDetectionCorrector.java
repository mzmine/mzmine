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
 * @description Feature Detection baseline corrector. Feature detection is done in several steps
 *              sorting out real peaks through different criteria. Peaks are removed from spectra
 *              and minimums and medians are used to smooth the remaining parts of the spectra. Uses
 *              "peakDetection" feature from "baseline" R-package
 *              (http://cran.r-project.org/web/packages/baseline/baseline.pdf). (A translation from
 *              Kevin R. Coombes et al.'s MATLAB code for detecting peaks and removing baselines).
 * 
 */
public class PeakDetectionCorrector extends BaselineCorrector {

  @Override
  public String[] getRequiredRPackages() {
    return new String[] { /* "rJava", "Rserve", */"baseline"};
  }

  @Override
  public double[] computeBaseline(final RSessionWrapper rSession, final RawDataFile origDataFile,
      double[] chromatogram, ParameterSet parameters) throws RSessionWrapperException {

    // Feature Detection parameters.
    int left = parameters.getParameter(PeakDetectionCorrectorParameters.LEFT).getValue();
    int right = parameters.getParameter(PeakDetectionCorrectorParameters.RIGHT).getValue();
    int lwin = parameters.getParameter(PeakDetectionCorrectorParameters.LWIN).getValue();
    int rwin = parameters.getParameter(PeakDetectionCorrectorParameters.RWIN).getValue();
    double snminimum =
        parameters.getParameter(PeakDetectionCorrectorParameters.SNMINIMUM).getValue();
    double mono = parameters.getParameter(PeakDetectionCorrectorParameters.MONO).getValue();
    double multiplier =
        parameters.getParameter(PeakDetectionCorrectorParameters.MULTIPLIER).getValue();

    final double[] baseline;

    // Set chromatogram.
    rSession.assign("chromatogram", chromatogram);
    // Transform chromatogram.
    rSession.eval("mat <- matrix(chromatogram, nrow=1)");

    // Calculate baseline.
    rSession.eval("bl <- NULL");
    // This method can fail for some bins when "useBins" is enabled, or more
    // generally speaking for
    // abusive parameter set
    String cmd = "tryCatch({" + "bl <- baseline(mat, left=" + left + ", right=" + right + ", lwin="
        + lwin + ", rwin=" + rwin + ", snminimum=" + snminimum + ", mono=" + mono + ", multiplier="
        + multiplier + ", method='peakDetection')" + "}, warning = function(war) {"
        + "message(\"<R warning>: \", war);" + "}, error = function(err) {"
        + "message(\"<R error>: \", err);" + "}, finally = {" +
        // "" +
        "})";
    rSession.eval(cmd);
    // Return a flat baseline (passing by the lowest intensity scan -
    // "min(chromatogram)") in case of failure
    // Anyway, this usually happens when "chromatogram" is fully flat and
    // zeroed.
    rSession.eval(
        "if (!is.null(bl)) { baseline <- getBaseline(bl); } else { baseline <- matrix(rep(min(chromatogram), length(chromatogram)), nrow=1); }");
    baseline = ((double[][]) rSession.collect("baseline"))[0];
    // Done: Refresh R code stack
    rSession.clearCode();

    return baseline;
  }

  @Override
  public @NotNull String getName() {
    return "PeakDetection baseline corrector";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return PeakDetectionCorrectorParameters.class;
  }

}
