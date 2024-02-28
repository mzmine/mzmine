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
 * @description Local Minima + LOESS (smoothed low-percentile intensity) baseline corrector. Uses
 *              "bslnoff" feature from "PROcess" R/Bioconductor package
 *              (http://bioconductor.org/packages/release/ bioc/manuals/PROcess/man/PROcess.pdf).
 * 
 */
public class LocMinLoessCorrector extends BaselineCorrector {

  private static final double BW_MIN_VAL = 0.001d;

  @Override
  public String[] getRequiredRPackages() {
    return new String[] { /* "rJava", "Rserve", */"PROcess"};
  }

  @Override
  public double[] computeBaseline(final RSessionWrapper rSession, final RawDataFile origDataFile,
      double[] chromatogram, ParameterSet parameters) throws RSessionWrapperException {

    // Local Minima parameters.
    String method = parameters.getParameter(LocMinLoessCorrectorParameters.METHOD).getValue();
    double bw = parameters.getParameter(LocMinLoessCorrectorParameters.BW).getValue();
    int breaks = parameters.getParameter(LocMinLoessCorrectorParameters.BREAKS).getValue();
    int breaks_width =
        parameters.getParameter(LocMinLoessCorrectorParameters.BREAK_WIDTH).getValue();
    double qntl = parameters.getParameter(LocMinLoessCorrectorParameters.QNTL).getValue();

    final double[] baseline;

    // Set chromatogram.
    rSession.assign("chromatogram", chromatogram);
    // Transform chromatogram.
    int mini = 1;
    int maxi = chromatogram.length;
    rSession.eval("mat <- cbind(matrix(seq(" + ((double) mini) + ", " + ((double) maxi)
        + ", by = 1.0), ncol=1), " + "matrix(chromatogram[" + mini + ":" + maxi + "], ncol=1))");
    // Breaks
    rSession.eval("breaks <- "
        + ((breaks_width > 0) ? (int) Math.round((double) (maxi - mini) / (double) breaks_width)
            : breaks));
    // Calculate baseline.
    // + Seems like "loess" method doesn't support "bw=0.0"
    rSession.eval("bseoff <- bslnoff(mat, method=\"" + method + "\", bw="
        + ((method.equals("approx") || bw >= BW_MIN_VAL) ? bw : BW_MIN_VAL)
        + ", breaks=breaks, qntl=" + qntl + ")");
    rSession.eval("baseline <- mat[,2] - bseoff[,2]");
    baseline = (double[]) rSession.collect("baseline");
    // Done: Refresh R code stack
    rSession.clearCode();

    return baseline;
  }

  @Override
  public @NotNull String getName() {
    return "Local minima + LOESS baseline corrector";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LocMinLoessCorrectorParameters.class;
  }

}
