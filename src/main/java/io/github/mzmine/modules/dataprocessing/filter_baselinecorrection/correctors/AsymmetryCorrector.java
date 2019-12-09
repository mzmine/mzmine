/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;

/**
 * @description Asymmetric baseline corrector. Estimates a trend based on
 *              asymmetric least squares. Uses "asysm" feature from "ptw"
 *              R-package (http://cran.r-project.org/web/packages/ptw/ptw.pdf).
 * 
 */
public class AsymmetryCorrector extends BaselineCorrector {

    @Override
    public String[] getRequiredRPackages() {
        return new String[] { /* "rJava", "Rserve", */"ptw" };
    }

    @Override
    public double[] computeBaseline(final RSessionWrapper rSession,
            final RawDataFile origDataFile, double[] chromatogram,
            ParameterSet parameters) throws RSessionWrapperException {

        // Smoothing and asymmetry parameters.
        final double smoothing = parameters
                .getParameter(AsymmetryCorrectorParameters.SMOOTHING)
                .getValue();
        final double asymmetry = parameters
                .getParameter(AsymmetryCorrectorParameters.ASYMMETRY)
                .getValue();

        // Compute baseline.
        final double[] baseline;

        // try {
        // Set chromatogram.
        // rSession.assignDoubleArray("chromatogram", chromatogram);
        rSession.assign("chromatogram", chromatogram);
        // Calculate baseline.
        rSession.eval("baseline <- asysm(chromatogram," + smoothing + ','
                + asymmetry + ')');
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
    public @Nonnull String getName() {
        return "Asymmetric baseline corrector";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return AsymmetryCorrectorParameters.class;
    }

}
