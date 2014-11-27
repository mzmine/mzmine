/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
 * @description Peak Detection baseline corrector.
 * Peak detection is done in several steps sorting out real peaks through different criteria. Peaks are removed from 
 * spectra and minimums and medians are used to smooth the remaining parts of the spectra.
 * Uses "peakDetection" feature from "baseline" R-package (http://cran.r-project.org/web/packages/baseline/baseline.pdf).
 * (A translation from Kevin R. Coombes et al.'s MATLAB code for detecting peaks and removing baselines).
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class PeakDetectionCorrector extends BaselineCorrector {

	@Override
	public String[] getRequiredRPackages() {
		return new String[] { "rJava", "baseline" };
	}

	@Override
	public double[] computeBaseline(final RSession rSession, final RawDataFile origDataFile, double[] chromatogram, ParameterSet parameters) {

		// Peak Detection parameters.
		int left = parameters.getParameter(PeakDetectionCorrectorParameters.LEFT).getValue();
		int right = parameters.getParameter(PeakDetectionCorrectorParameters.RIGHT).getValue();
		int lwin = parameters.getParameter(PeakDetectionCorrectorParameters.LWIN).getValue();
		int rwin = parameters.getParameter(PeakDetectionCorrectorParameters.RWIN).getValue();
		double snminimum = parameters.getParameter(PeakDetectionCorrectorParameters.SNMINIMUM).getValue();
		double mono = parameters.getParameter(PeakDetectionCorrectorParameters.MONO).getValue();
		double multiplier = parameters.getParameter(PeakDetectionCorrectorParameters.MULTIPLIER).getValue();


		final double[] baseline;
		synchronized (RUtilities.R_SEMAPHORE) {

			try {
				// Set chromatogram.
				rSession.assignDoubleArray("chromatogram", chromatogram);
				// Transform chromatogram.
				rSession.eval("mat = matrix(chromatogram, nrow=1)");

				// Calculate baseline.
				rSession.eval("bl = NULL");
				// This method can fail for some bins when "useBins" is enabled, or more generally speaking for
				// abusive parameter set
				String cmd = "tryCatch({" +
						"bl = baseline(mat, left=" + left + ", right=" + right + 
						", lwin=" + lwin + ", rwin=" + rwin + ", snminimum=" + snminimum + 
						", mono=" + mono + ", multiplier=" + multiplier + ", method='peakDetection')" +
						"}, warning = function(war) {" +
						"message(\"<R warning>: \", war);" +
						"}, error = function(err) {" +
						"message(\"<R error>: \", err);" +
						"}, finally = {" +
						//"" +
						"})";
				rSession.eval(cmd);
				// Return a flat baseline (passing by the lowest intensity scan - "min(chromatogram)") in case of failure
				// Anyway, this usually happens when "chromatogram" is fully flat and zeroed.
				rSession.eval(
						"if (!is.null(bl)) { baseline <- getBaseline(bl); } else { baseline <- matrix(rep(min(chromatogram), length(chromatogram)), nrow=1); }"
						);
				baseline = rSession.collectDoubleArray("baseline");
			}
			catch (Throwable t) {
				//t.printStackTrace();
				throw new IllegalStateException("R error during baseline correction (" + this.getName() + ").", t);
			}
		}
		return baseline;
	}


	@Override
	public @Nonnull String getName() {
		return "PeakDetection baseline corrector";
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return PeakDetectionCorrectorParameters.class;
	}

}
