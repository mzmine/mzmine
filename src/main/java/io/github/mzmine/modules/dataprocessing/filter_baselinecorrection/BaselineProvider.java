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

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;

/**
 * @description Base interface for providing a new way for computing baselines.
 * 
 */
public interface BaselineProvider {

    /**
     * Gets R required packages for the corrector's method, if applicable
     */
    public String[] getRequiredRPackages();

    /**
     * Returns a baseline for correcting the given chromatogram using R
     * 
     * @throws RSessionWrapperException
     */
    public double[] computeBaseline(final RSessionWrapper rSession,
            final RawDataFile origDataFile, final double[] chromatogram,
            ParameterSet parameters) throws RSessionWrapperException;

}
