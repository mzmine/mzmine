/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.isotopes.deisotoper;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class IsotopeGrouperParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final String ChooseTopIntensity = "Most intense";
    public static final String ChooseLowestMZ = "Lowest m/z";

    public static final Object[] representativeIsotopeValues = {
            ChooseTopIntensity, ChooseLowestMZ };

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "deisotoped", null);

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Maximum distance in M/Z from the expected location of a peak",
            "m/z", new Double(0.05), new Double(0.0), null,
            MZmineCore.getMZFormat());

    public static final Parameter rtTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "RT tolerance",
            "Maximum distance in RT from the expected location of a peak",
            null, new Double(5.0), new Double(0.0), null,
            MZmineCore.getRTFormat());

    public static final Parameter monotonicShape = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Monotonic shape",
            "If true, then monotonically decreasing height of isotope pattern is required (monoisotopic peak is strongest).",
            new Boolean(true));

    public static final Parameter maximumCharge = new SimpleParameter(
            ParameterType.INTEGER, "Maximum charge", "Maximum charge", "",
            new Integer(1), new Integer(1), null);

    public static final Parameter representativeIsotope = new SimpleParameter(
            ParameterType.STRING,
            "Representative isotope",
            "Which peak should represent the whole isotope pattern",
            ChooseTopIntensity, representativeIsotopeValues);

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peaklist",
            "If checked, original peaklist will be removed and only deisotoped version remains",
            new Boolean(true));

    public IsotopeGrouperParameters() {
        super(new Parameter[] { suffix, mzTolerance, rtTolerance,
                monotonicShape, maximumCharge, representativeIsotope,
                autoRemove });
    }

}
