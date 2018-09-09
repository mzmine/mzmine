/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils;

import java.util.Map;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.util.FormulaUtils;

/**
 * lipid identity to annotate features as lipids in feature list
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidIdentity extends SimplePeakIdentity {

  private double exactMass;
  private String sumFormula;
  private LipidClasses lipidClass;
  private static LipidChainBuilder chainBuilder = new LipidChainBuilder();

  public LipidIdentity(final LipidClasses lipidClass, final int chainLength,
      final int chainDoubleBonds, final int numberOfAcylChains, final int numberOfAlkylChains) {
    this(
        lipidClass.getName() + " " + lipidClass.getAbbr() + '(' + chainLength + ':'
            + chainDoubleBonds + ')',
        lipidClass.getBackBoneFormula() + chainBuilder.calculateChainFormula(chainLength,
            chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains));
    this.lipidClass = lipidClass;

  }

  private LipidIdentity(final String name, final String formula) {
    super(name);
    // Parse formula
    Map<String, Integer> parsedFormula = FormulaUtils.parseFormula(formula);
    // Rearrange formula
    sumFormula = FormulaUtils.formatFormula(parsedFormula);
    exactMass = FormulaUtils.calculateExactMass(sumFormula);
    setPropertyValue(PROPERTY_NAME, name);
    setPropertyValue(PROPERTY_FORMULA, sumFormula);
    setPropertyValue(PROPERTY_METHOD, "Lipid identification");
  }

  public double getMass() {
    return exactMass;
  }

  public String getFormula() {
    return sumFormula;
  }

  public LipidClasses getLipidClass() {
    return lipidClass;
  }

  @Override
  public @Nonnull Object clone() {
    return new LipidIdentity(getName(), getPropertyValue(PROPERTY_FORMULA));
  }
}
