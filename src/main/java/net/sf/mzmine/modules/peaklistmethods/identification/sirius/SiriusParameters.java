/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import java.io.IOException;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.NeutralMassParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class SiriusParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final ComboParameter<IonizationType> ionizationType = new ComboParameter<IonizationType>(
	    "Ionization type", "Ionization type", IonizationType.values());

	public static final NeutralMassParameter NEUTRAL_MASS = new NeutralMassParameter(
			"Neutral mass", "Value to use in the search query");

	public static final IntegerParameter MAX_RESULTS = new IntegerParameter(
			"Number of candidates", "Maximum number of results to display", 10);

	public static final DoubleParameter PARENT_MASS = new DoubleParameter(
			"Parent mass", "Mass of the precurosr ion, before fragmentation");

	public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter();

	public static final ElementsParameter ELEMENTS = new ElementsParameter(
			"Elements", "Elements and ranges");
	static {
	  setDefaultCompounds();
  }

	public static final OptionalModuleParameter ISOTOPE_FILTER = new OptionalModuleParameter(
			"Isotope pattern filter",
			"Search only for compounds with a isotope pattern similar",
			new IsotopePatternScoreParameters());

    public SiriusParameters() {
	super(new Parameter[] { peakLists, ionizationType,
		PARENT_MASS,
		MAX_RESULTS,
		MZ_TOLERANCE,
		ELEMENTS,
		ISOTOPE_FILTER });
    }

    private static void setDefaultCompounds() {
      MolecularFormulaRange range = new MolecularFormulaRange();
      try {
        IsotopeFactory iFac = Isotopes.getInstance();
        range.addIsotope(iFac.getMajorIsotope("C"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("H"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("N"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("O"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("P"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("S"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
        range.addIsotope(iFac.getMajorIsotope("F"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
        range.addIsotope(iFac.getMajorIsotope("B"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
        range.addIsotope(iFac.getMajorIsotope("I"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
        range.addIsotope(iFac.getMajorIsotope("Br"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
        range.addIsotope(iFac.getMajorIsotope("Se"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
        range.addIsotope(iFac.getMajorIsotope("Cl"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
      } catch (IOException e) {
        //TODO: throw analogue of MSDKException?
        e.printStackTrace();
      }
      ELEMENTS.setValue(range);
    }

}
