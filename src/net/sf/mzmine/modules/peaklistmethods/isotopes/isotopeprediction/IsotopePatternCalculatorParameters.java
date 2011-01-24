/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.Polarity;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class IsotopePatternCalculatorParameters extends SimpleParameterSet {

	public static final Parameter formula = new SimpleParameter(
			ParameterType.STRING, "Chemical formula",
			"empirical formula of a chemical compound", null, null, null);

	public static final Parameter charge = new SimpleParameter(
			ParameterType.INTEGER, "Charge", "Charge of the molecule", "z",
			new Integer(1), new Integer(0), null);

	public static final Parameter polarity = new SimpleParameter(
			ParameterType.STRING, "Polarity",
			"Set positive or negative the charge of the molecule ", null,
			Polarity.Positive, Polarity.values(), null);

	public IsotopePatternCalculatorParameters() {
		super(new Parameter[] { formula, charge, polarity });
	}

}
