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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.AdductsParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class AdductSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter(
            "RT tolerance",
            "Maximum allowed difference of retention time to set a relationship between peaks");

    public static final AdductsParameter ADDUCTS = new AdductsParameter(
            "Adducts",
            "List of adducts, each one refers a specific distance in m/z axis between related peaks");

    public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
            "m/z tolerance",
            "Tolerance value of the m/z difference between peaks");

    /*
     * Max value 10000% so even high-intensity adducts can be searched for.
     */
    public static final PercentParameter MAX_ADDUCT_HEIGHT = new PercentParameter(
            "Max relative adduct peak height",
            "Maximum height of the recognized adduct peak, relative to the main peak",
            0.5, 0, 100);

    public AdductSearchParameters() {

        super(new Parameter[] { PEAK_LISTS, RT_TOLERANCE, ADDUCTS,
                MZ_TOLERANCE, MAX_ADDUCT_HEIGHT });
    }
}
