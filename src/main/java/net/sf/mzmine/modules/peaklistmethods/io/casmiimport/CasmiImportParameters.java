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

package net.sf.mzmine.modules.peaklistmethods.io.casmiimport;

import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.TextParameter;

public class CasmiImportParameters extends SimpleParameterSet {

    public static final StringParameter casmiProblemName = new StringParameter(
	    "Name",
	    "This name will be used for the generated raw data file and peak list",
	    "CASMI problem #1");

    public static final TextParameter msSpectrum = new TextParameter(
	    "MS spectrum",
	    "MS spectrum in text format, consisting of two values (m/z and intensity) on each line");

    public static final TextParameter msMsSpectrum = new TextParameter(
	    "MS/MS spectrum",
	    "MS/MS spectrum in text format, consisting of two values (m/z and intensity) on each line");

    public static final ComboParameter<PolarityType> polarity = new ComboParameter<>(
	    "Polarity", "Polarity - positive or negative?",
	    PolarityType.values());

    public CasmiImportParameters() {
	super(new Parameter[] { casmiProblemName, msSpectrum, msMsSpectrum,
		polarity });
    }

}
