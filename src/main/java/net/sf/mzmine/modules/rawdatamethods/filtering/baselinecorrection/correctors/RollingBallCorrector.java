/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

/**
 * @description Rolling Ball baseline corrector. Estimates a trend based on
 *              Rolling Ball algorithm. Uses "rollingBall" feature from
 *              "baseline" R-package
 *              (http://cran.r-project.org/web/packages/baseline/baseline.pdf).
 *              (Ideas from Rolling Ball algorithm for X-ray spectra by
 *              M.A.Kneen and H.J. Annegarn. Variable window width has been left
 *              out).
 * 
 */
public class RollingBallCorrector extends BaselineCorrector {

    @Override
    public String[] getRequiredRPackages() {
        return new String[] { /* "rJava", "Rserve", */"baseline" };
    }

    @Override
    public double[] computeBaseline(final RSessionWrapper rSession,
            final RawDataFile origDataFile, double[] chromatogram,
            ParameterSet parameters) throws RSessionWrapperException {

        // Rolling Ball parameters.
        double wm = parameters.getParameter(
                RollingBallCorrectorParameters.MIN_MAX_WIDTH).getValue();
        double ws = parameters.getParameter(
                RollingBallCorrectorParameters.SMOOTHING).getValue();

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
        String cmd = "tryCatch({" + "bl <- baseline(mat, wm=" + wm + ", ws="
                + ws + ", method='rollingBall')"
                + "}, warning = function(war) {"
                + "message(\"<R warning>: \", war);"
                + "}, error = function(err) {"
                + "message(\"<R error>: \", err);" + "}, finally = {" +
                // "" +
                "})";
        rSession.eval(cmd);
        // Return a flat baseline (passing by the lowest intensity scan -
        // "min(chromatogram)") in case of failure
        // Anyway, this usually happens when "chromatogram" is fully flat and
        // zeroed.
        rSession.eval("if (!is.null(bl)) { baseline <- getBaseline(bl); } else { baseline <- matrix(rep(min(chromatogram), length(chromatogram)), nrow=1); }");
        baseline = ((double[][]) rSession.collect("baseline"))[0];
        // Done: Refresh R code stack
        rSession.clearCode();

        return baseline;
    }

    @Override
    public @Nonnull
    String getName() {
        return "RollingBall baseline corrector";
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return RollingBallCorrectorParameters.class;
    }

}
