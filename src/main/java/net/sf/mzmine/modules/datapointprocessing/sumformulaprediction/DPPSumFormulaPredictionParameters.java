/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.datapointprocessing.sumformulaprediction;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 */
public class DPPSumFormulaPredictionParameters extends SimpleParameterSet {
  public static final IntegerParameter charge = new IntegerParameter("Charge", "Charge");

  public static final ComboParameter<IonizationType> ionization =
      new ComboParameter<IonizationType>("Ionization type", "Ionization type",
          IonizationType.values());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final ElementsParameter elements =
      new ElementsParameter("Elements", "Elements and ranges");

  public static final OptionalModuleParameter<ElementalHeuristicParameters> elementalRatios =
      new OptionalModuleParameter<ElementalHeuristicParameters>("Element count heuristics",
          "Restrict formulas by heuristic restrictions of elemental counts and ratios",
          new ElementalHeuristicParameters());

  public static final OptionalModuleParameter<RDBERestrictionParameters> rdbeRestrictions =
      new OptionalModuleParameter<RDBERestrictionParameters>("RDBE restrictions",
          "Search only for formulas which correspond to the given RDBE restrictions",
          new RDBERestrictionParameters());

  public DPPSumFormulaPredictionParameters() {
    super(new Parameter[] {charge, ionization, mzTolerance, elements, elementalRatios,
        rdbeRestrictions});
  }
}
