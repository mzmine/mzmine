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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.modules.dataprocessing.id_formulaprediction.elements.ElementsParameter;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.NeutralMassParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class FormulaPredictionParameters extends SimpleParameterSet {

    public static final NeutralMassParameter neutralMass = new NeutralMassParameter(
            "Neutral mass", "Original neutral mass");

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

    public FormulaPredictionParameters() {
        super(new Parameter[] { neutralMass, mzTolerance, elements,
                elementalRatios, rdbeRestrictions, isotopeFilter, msmsFilter });
    }

}
