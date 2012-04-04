/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.AdductsParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;

public class AdductSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final DoubleParameter RT_TOLERANCE = new DoubleParameter(
            "RT tolerance",
            "Maximum allowed difference of retention time to set a relationship between peaks",
            MZmineCore.getConfiguration().getRTFormat());

    public static final AdductsParameter ADDUCTS = new AdductsParameter(
            "Adducts",
            "List of adducts, each one refers a specific distance in m/z axis between related peaks");

    public static final DoubleParameter MZ_TOLERANCE = new DoubleParameter(
            "m/z tolerance",
            "Tolerance value of the m/z difference between peaks",
            MZmineCore.getConfiguration().getMZFormat());

    public static final DoubleParameter MAX_ADDUCT_HEIGHT = new DoubleParameter(
            "Max relative adduct peak height",
            "Maximum height of the recognized adduct peak, relative to the main peak");

    public AdductSearchParameters() {

        super(new Parameter[]{PEAK_LISTS, RT_TOLERANCE, ADDUCTS, MZ_TOLERANCE, MAX_ADDUCT_HEIGHT});
    }
}
