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

package net.sf.mzmine.modules.peaklistmethods.alignment.path.scorer;

import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.PathAlignerParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.functions.AlignmentPath;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.functions.ScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;

import com.google.common.collect.Range;

public class RTScore implements ScoreCalculator {

    MZTolerance mzTolerance;
    RTTolerance rtTolerance;
    private final static double WORST_SCORE = Double.MAX_VALUE;

    public double calculateScore(AlignmentPath path, PeakListRow peak,
	    ParameterSet parameters) {
	try {
	    rtTolerance = parameters.getParameter(
		    PathAlignerParameters.RTTolerance).getValue();
	    mzTolerance = parameters.getParameter(
		    PathAlignerParameters.MZTolerance).getValue();
	    Range<Double> rtRange = rtTolerance.getToleranceRange(path.getRT());
	    Range<Double> mzRange = mzTolerance.getToleranceRange(path.getMZ());

	    if (!rtRange.contains(peak.getAverageRT())
		    || !mzRange.contains(peak.getAverageMZ())) {
		return WORST_SCORE;
	    }

	    double mzDiff = Math.abs(path.getMZ() - peak.getAverageMZ());

	    double rtDiff = Math.abs(path.getRT() - peak.getAverageRT());

	    double score = ((mzDiff / (RangeUtils.rangeLength(mzRange) / 2.0)))
		    + ((rtDiff / (RangeUtils.rangeLength(rtRange) / 2.0)));

	    if (parameters.getParameter(
		    PathAlignerParameters.SameChargeRequired).getValue()) {
		if (!PeakUtils.compareChargeState(
			path.convertToAlignmentRow(0), peak)) {
		    return WORST_SCORE;
		}
	    }

	    if (parameters.getParameter(PathAlignerParameters.SameIDRequired)
		    .getValue()) {
		if (!PeakUtils.compareIdentities(path.convertToAlignmentRow(0),
			peak)) {
		    return WORST_SCORE;
		}
	    }

	    if (parameters.getParameter(
		    PathAlignerParameters.compareIsotopePattern).getValue()) {
		IsotopePattern ip1 = path.convertToAlignmentRow(0)
			.getBestIsotopePattern();
		IsotopePattern ip2 = peak.getBestIsotopePattern();

		if ((ip1 != null) && (ip2 != null)) {
		    ParameterSet isotopeParams = parameters.getParameter(
			    PathAlignerParameters.compareIsotopePattern)
			    .getEmbeddedParameters();

		    if (!IsotopePatternScoreCalculator.checkMatch(ip1, ip2,
			    isotopeParams)) {
			return WORST_SCORE;
		    }
		}
	    }

	    return score;
	} catch (NullPointerException e) {
	    e.printStackTrace();
	    return WORST_SCORE;
	}
    }

    public boolean matches(AlignmentPath path, PeakListRow peak,
	    ParameterSet parameters) {
	rtTolerance = parameters
		.getParameter(PathAlignerParameters.RTTolerance).getValue();
	mzTolerance = parameters
		.getParameter(PathAlignerParameters.MZTolerance).getValue();
	Range<Double> rtRange = rtTolerance.getToleranceRange(path.getRT());
	Range<Double> mzRange = mzTolerance.getToleranceRange(path.getMZ());

	if (!rtRange.contains(peak.getAverageRT())
		|| !mzRange.contains(peak.getAverageMZ())) {
	    return false;
	}
	return true;
    }

    public double getWorstScore() {
	return WORST_SCORE;
    }

    public boolean isValid(PeakListRow peak) {
	return true;
    }

    public String name() {
	return "Uses MZ and retention time";
    }
}
