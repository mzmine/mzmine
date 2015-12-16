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
package net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class FormulaPredictionPeakListParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final IntegerParameter charge = new IntegerParameter(
	    "Charge", "Charge");

    public static final ComboParameter<IonizationType> ionization = new ComboParameter<IonizationType>(
	    "Ionization type", "Ionization type", IonizationType.values());

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final ElementsParameter elements = new ElementsParameter(
	    "Elements", "Elements and ranges");

    public static final OptionalModuleParameter elementalRatios = new OptionalModuleParameter(
	    "Element count heuristics",
	    "Restrict formulas by heuristic restrictions of elemental counts and ratios",
	    new ElementalHeuristicParameters());

    public static final OptionalModuleParameter rdbeRestrictions = new OptionalModuleParameter(
	    "RDBE restrictions",
	    "Search only for formulas which correspond to the given RDBE restrictions",
	    new RDBERestrictionParameters());

    public static final OptionalModuleParameter isotopeFilter = new OptionalModuleParameter(
	    "Isotope pattern filter",
	    "Search only for formulas with a isotope pattern similar",
	    new IsotopePatternScoreParameters());

    public static final OptionalModuleParameter msmsFilter = new OptionalModuleParameter(
	    "MS/MS filter", "Check MS/MS data", new MSMSScoreParameters());

    public FormulaPredictionPeakListParameters() {
	super(new Parameter[] { charge, ionization, PEAK_LISTS, mzTolerance,
		elements, elementalRatios, rdbeRestrictions, isotopeFilter,
		msmsFilter });
    }
}
