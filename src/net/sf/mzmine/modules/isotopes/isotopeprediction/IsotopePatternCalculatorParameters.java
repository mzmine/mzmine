/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */


package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class IsotopePatternCalculatorParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    public static final NumberFormat integerFormat = NumberFormat.getIntegerInstance();

    public static final Parameter formula = new SimpleParameter(
            ParameterType.STRING, "Chemical formula",
            "empirical formula of a chemical compound", null, null, null);

    public static final Parameter minimalAbundance = new SimpleParameter(
            ParameterType.DOUBLE, "Minimum % of relative abundance", "Mininum relative % of abundance per peak", "%",
            new Double(0.01f), new Double(0.0001f), new Double(0.1f),
            null);

    public static final Parameter charge = new SimpleParameter(
            ParameterType.INTEGER, "Charge distribution", "Charge distribution of the molecule", "z",
            new Integer(0), new Integer(0), null,
            integerFormat);
    
    public static final Parameter signOfCharge = new SimpleParameter(
            ParameterType.STRING, "Sign of the charge", "Set positive or negative the charge of the molecule ", null,
            new String("Negative"), new String[]{"Negative", "Positive"},
            null);

    public static final Parameter autoHeight = new SimpleParameter(
			ParameterType.BOOLEAN, "Set automatic height",
			"Set automatic height of most abundance isotope", null, null,
			null, null, null);
	
    public static final Parameter isotopeHeight = new SimpleParameter(
            ParameterType.DOUBLE, "Height of most abundance isotope", "Height of most abundance isotope", "absolute",
            new Double(10000.0f), new Double(1.0f), null,
            MZmineCore.getIntensityFormat());

    public IsotopePatternCalculatorParameters() {
        super(new Parameter[] { formula, minimalAbundance, charge, signOfCharge, autoHeight, isotopeHeight });
    }

}
