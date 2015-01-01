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
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.RSession;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RUtilities;

/**
 * @description Local Minima + LOESS (smoothed low-percentile intensity)
 *              baseline corrector. Uses "bslnoff" feature from "PROcess"
 *              R/Bioconductor package
 *              (http://bioconductor.org/packages/release/
 *              bioc/manuals/PROcess/man/PROcess.pdf).
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class LocMinLoessCorrector extends BaselineCorrector {

    @Override
    public String[] getRequiredRPackages() {
	return new String[] { "rJava", "PROcess" };
    }

    @Override
    public double[] computeBaseline(final RSession rSession,
	    final RawDataFile origDataFile, double[] chromatogram,
	    ParameterSet parameters) {

	// Local Minima parameters.
	String method = parameters.getParameter(
		LocMinLoessCorrectorParameters.METHOD).getValue();
	double bw = parameters.getParameter(LocMinLoessCorrectorParameters.BW)
		.getValue();
	int breaks = parameters.getParameter(
		LocMinLoessCorrectorParameters.BREAKS).getValue();
	int breaks_width = parameters.getParameter(
		LocMinLoessCorrectorParameters.BREAK_WIDTH).getValue();
	double qntl = parameters.getParameter(
		LocMinLoessCorrectorParameters.QNTL).getValue();

	final double[] baseline;
	synchronized (RUtilities.R_SEMAPHORE) {

	    try {
		// Set chromatogram.
		rSession.assignDoubleArray("chromatogram", chromatogram);
		// Transform chromatogram.
		int mini = 1;
		int maxi = chromatogram.length;
		rSession.eval("mat = cbind(matrix(seq(" + ((double) mini)
			+ ", " + ((double) maxi) + ", by = 1.0), ncol=1), "
			+ "matrix(chromatogram[" + mini + ":" + maxi
			+ "], ncol=1))");
		// Breaks
		rSession.eval("breaks <- "
			+ ((breaks_width > 0) ? (int) Math
				.round((double) (maxi - mini)
					/ (double) breaks_width) : breaks));
		// Calculate baseline.
		rSession.eval("bseoff <- bslnoff(mat, method=\"" + method
			+ "\", bw=" + bw + ", breaks=breaks, qntl=" + qntl
			+ ")");
		rSession.eval("baseline <- mat[,2] - bseoff[,2]");
		baseline = rSession.collectDoubleArray("baseline");
	    } catch (Throwable t) {
		// t.printStackTrace();
		throw new IllegalStateException(
			"R error during baseline correction (" + this.getName()
				+ ").", t);
	    }
	}
	return baseline;
    }

    @Override
    public @Nonnull String getName() {
	return "Local minima + LOESS baseline corrector";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return LocMinLoessCorrectorParameters.class;
    }

}
