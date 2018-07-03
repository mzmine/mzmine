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
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class IsotopeConstants {
  public static final int ISOTOPE_MAX = 100;
  public static final int ISOTOPE_MIN = 0;

  public static MolecularFormulaRange setDefaultCompounds() {
    MolecularFormulaRange range = new MolecularFormulaRange();
    try {
      IsotopeFactory iFac = Isotopes.getInstance();
      range.addIsotope(iFac.getMajorIsotope("C"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("H"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("N"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("O"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("P"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("S"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("F"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("B"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("I"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Br"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Se"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Cl"), IsotopeConstants.ISOTOPE_MIN,
          IsotopeConstants.ISOTOPE_MIN);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return range;
  }

  private IsotopeConstants() {}
}
