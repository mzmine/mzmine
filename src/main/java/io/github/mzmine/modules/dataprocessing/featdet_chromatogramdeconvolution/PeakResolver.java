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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;

public interface PeakResolver extends MZmineModule {

    /**
     * Gets if resolver requires R, if applicable
     */
    public boolean getRequiresR();

    /**
     * Gets R required packages for the resolver's method, if applicable
     */
    public String[] getRequiredRPackages();

    /**
     * Gets R required packages versions for the resolver's method, if
     * applicable
     */
    public String[] getRequiredRPackagesVersions();

    /**
     * Gets R engine type, if applicable
     */
    public REngineType getREngineType(final ParameterSet parameters);

    /**
     * Resolve a peaks found within given chromatogram. For easy use, three
     * arrays (scanNumbers, retentionTimes and intensities) are provided,
     * although the contents of these arrays can also be obtained from the
     * chromatogram itself. The size of these arrays must be same, and must be
     * equal to the number of scans covered by given chromatogram.
     * 
     * @param mzCenterFunction
     * 
     * @param rTRangeMSMS
     * @param msmsRange
     * @param rTRangeMSMS
     * @param msmsRange
     * @param rTRangeMSMS
     * @param msmsRange
     * 
     * @throws RSessionWrapperException
     */
    public ResolvedPeak[] resolvePeaks(Feature chromatogram,
            ParameterSet parameters, RSessionWrapper rSession,
            CenterFunction mzCenterFunction, double msmsRange,
            double rTRangeMSMS) throws RSessionWrapperException;

}
