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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.camera;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.R.REngineType;

/**
 * Parameters for a <code>CameraSearchTask</code>.
 *
 */
public class CameraSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();


    // Sigma.
    public static final DoubleParameter FWHM_SIGMA = new DoubleParameter(
            "FWHM sigma",
            "Fitted peak (Gaussian) width multiplier used when grouping peaks by RT",
            NumberFormat.getNumberInstance(), 0.2, 0.0, null);

    // Percentage of FWHM.
    public static final PercentParameter FWHM_PERCENTAGE = new PercentParameter(
            "FWHM percentage",
            "Percentage of the FWHM of a peak used when grouping peaks by RT",
            0.01, 0.0, 1.0);

    // Max charge.
    public static final IntegerParameter ISOTOPES_MAX_CHARGE = new IntegerParameter(
            "Isotopes max. charge",
            "The maximum charge considered when identifying isotopes", 3, 1,
            null);

    // Max isotopes.
    public static final IntegerParameter ISOTOPES_MAXIMUM = new IntegerParameter(
            "Isotopes max. per cluster",
            "The maximum number of isotopes per cluster", 4, 0, null);

    // Isotope m/z tolerance.
    public static final MZToleranceParameter ISOTOPES_MZ_TOLERANCE = new MZToleranceParameter(
            "Isotopes mass tolerance",
            "Mass tolerance used when identifying isotopes (both values required)");

    // Correlation threshold.
    public static final DoubleParameter CORRELATION_THRESHOLD = new DoubleParameter(
            "Correlation threshold",
            "Minimum correlation required between two peaks' EICs when grouping by peak shape",
            NumberFormat.getNumberInstance(), 0.9, 0.0, 1.0);

    // Correlation threshold.
    public static final DoubleParameter CORRELATION_P_VALUE = new DoubleParameter(
            "Correlation p-value",
            "Required p-value when testing the significance of peak shape correlation",
            NumberFormat.getNumberInstance(), 0.05, 0.0, 1.0);

    // Ionization Polarity
    public static final ComboParameter POLARITY = 
            new ComboParameter("Ionization Polarity", "Ionization Polarity",
            new Object[] {"positive", "negative"}, "positive");
    
    public static final BooleanParameter DONT_SPLIT_ISOTOPES =
            new BooleanParameter("Do not split isotopes", 
                    "If checked, isotopes will not be split even if shape-similarity is low",
                    true);
    
    public static final String FIND_ISOTOPES_FIRST = "Perform Isotope search before Shape correlation";
    public static final String GROUP_CORR_FIRST = "Perform Shape correlation before Isotope search";
    
    public static final ComboParameter <String> ORDER = 
            new ComboParameter <> ("Order", 
                    "Order of Isotope search and Shape correlation steps",
                    new String[] {FIND_ISOTOPES_FIRST, GROUP_CORR_FIRST},
                    FIND_ISOTOPES_FIRST);
    
    public static final BooleanParameter CREATE_NEW_LIST = new BooleanParameter(
            "Create new list", "If checked, a new list will be created", true);
    
    public static final String GROUP_BY_ISOTOPE = "Isotope ID";
    public static final String GROUP_BY_PCGROUP = "PCGroup ID";
    
    public static final ComboParameter <String> GROUP_BY = 
            new ComboParameter <> ("Group peaks by", 
                    "Order of Isotope search and Shape correlation steps",
                    new String[] {GROUP_BY_ISOTOPE, GROUP_BY_PCGROUP},
                    GROUP_BY_ISOTOPE);
    
    public static final BooleanParameter INCLUDE_SINGLETONS =
            new BooleanParameter("Include singletons", "If checked, features with no found isotope pattern will be included", 
                    false);
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "CAMERA");

    /**
     * R engine type.
     */
    public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
            "R engine",
            "The R engine to be used for communicating with R.",
            REngineType.values(), REngineType.RCALLER);

    
    public CameraSearchParameters() {

        super(new Parameter[] { PEAK_LISTS, FWHM_SIGMA, FWHM_PERCENTAGE,
                ISOTOPES_MAX_CHARGE, ISOTOPES_MAXIMUM, ISOTOPES_MZ_TOLERANCE,
                CORRELATION_THRESHOLD, CORRELATION_P_VALUE, POLARITY, 
                DONT_SPLIT_ISOTOPES, ORDER, CREATE_NEW_LIST, GROUP_BY, 
                INCLUDE_SINGLETONS, SUFFIX,
                RENGINE_TYPE});
    }
}
