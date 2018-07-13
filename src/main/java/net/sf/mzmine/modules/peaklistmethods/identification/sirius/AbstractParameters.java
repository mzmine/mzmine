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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import java.io.IOException;
import java.text.DecimalFormat;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;

public abstract class AbstractParameters extends SimpleParameterSet {
  private static final int ISOTOPE_MAX = 100;
  private static final int ISOTOPE_MIN = 0;

 public static final DoubleParameter MZ_TOLERANCE = new DoubleParameter("Deviation",
      "Speficy the deviation value in ppm s",
      new DecimalFormat("#0.00"),
      10.0d);

  public static final ElementsParameter ELEMENTS = new ElementsParameter(
      "Elements", "Elements and ranges");

  static {
    ELEMENTS.setValue(createDefaultElements());
  }

  public AbstractParameters(Parameter[] params) {
    super(params);
  }

  private static MolecularFormulaRange createDefaultElements() {
    MolecularFormulaRange range = new MolecularFormulaRange();
    try {
      IsotopeFactory iFac = Isotopes.getInstance();
      range.addIsotope(iFac.getMajorIsotope("C"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("H"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("N"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("O"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("P"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("S"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("F"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("B"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("I"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Br"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Se"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Cl"), AbstractParameters.ISOTOPE_MIN,
          AbstractParameters.ISOTOPE_MIN);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return range;
  }
}
