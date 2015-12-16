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

package net.sf.mzmine.modules.peaklistmethods.identification.fragmentsearch;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class FragmentSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public static final MZToleranceParameter ms2mzTolerance = new MZToleranceParameter(
            "m/z tolerance of MS2 data",
            "Tolerance value of the m/z difference between peaks in MS/MS scans");

    /*
     * Max value 10000% so even high-intensity fragments can be searched for.
     */
    public static final PercentParameter maxFragmentHeight = new PercentParameter(
            "Max fragment peak height",
            "Maximum height of the recognized fragment peak, relative to the main peak",
            0.5, 0, 100);

    public static final DoubleParameter minMS2peakHeight = new DoubleParameter(
            "Min MS2 peak height",
            "Minimum absolute intensity of the MS2 fragment peak", MZmineCore
                    .getConfiguration().getIntensityFormat());

    public FragmentSearchParameters() {
        super(new Parameter[] { peakLists, rtTolerance, ms2mzTolerance,
                maxFragmentHeight, minMS2peakHeight });
    }

}
