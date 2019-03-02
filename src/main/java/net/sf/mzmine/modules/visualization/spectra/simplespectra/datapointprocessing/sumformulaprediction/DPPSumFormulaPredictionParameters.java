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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.sumformulaprediction;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 */
public class DPPSumFormulaPredictionParameters extends SimpleParameterSet {

  public static final IntegerParameter charge = new IntegerParameter("Charge", "Charge");

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Minimum intensity of a data point to predict a sum formula for.");

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

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> isotopeFilter =
      new OptionalModuleParameter("Isotope pattern filter",
          "Search only for formulas with a isotope pattern similar",
          new IsotopePatternScoreParameters());

  public static final OptionalParameter<IntegerParameter> displayResults =
      new OptionalParameter<IntegerParameter>(new IntegerParameter("Display results (#)",
          "Check if you want to display the sum formula prediction results in the plot. "
          + "Displaying too much datasets might decrease clarity.\nPlease enter the number "
          + "of predicted sum formulas, you would like to display.", 1));

  public DPPSumFormulaPredictionParameters() {
    super(new Parameter[] {charge, noiseLevel, ionization, mzTolerance, elements, elementalRatios,
        rdbeRestrictions, isotopeFilter, displayResults});
  }
}
