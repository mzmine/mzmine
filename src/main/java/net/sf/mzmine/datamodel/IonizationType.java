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

package net.sf.mzmine.datamodel;

public enum IonizationType {

  NO_IONIZATION("No ionization", "", 0, PolarityType.NEUTRAL), //
  POSITIVE("[M]+", "+", -0.00054857990946, PolarityType.POSITIVE), //
  NEGATIVE("[M]-", "-", 0.00054857990946, PolarityType.NEGATIVE), //
  POSITIVE_HYDROGEN("[M+H]+", "H", 1.00728, PolarityType.POSITIVE), //
  NEGATIVE_HYDROGEN("[M-H]-", "H-1", -1.00728, PolarityType.NEGATIVE), //
  POTASSIUM("[M+K]+", "K", 38.96316, PolarityType.POSITIVE), //
  SODIUM("[M+Na]+", "Na", 22.98922, PolarityType.POSITIVE), //
  LITHIUM("[M+Li]+", "Li", 7.01546, PolarityType.POSITIVE), //
  AMMONIUM("[M+NH4]+", "NH4", 18.03383, PolarityType.POSITIVE), //
  CARBONATE("[M+CO3]-", "CO3", 59.98529, PolarityType.NEGATIVE), //
  PHOSPHATE("[M+H2PO4]-", "H2PO4", 96.96962, PolarityType.NEGATIVE), //
  FORMATE("[M+HCOO]-", "HCOO", 44.998201, PolarityType.NEGATIVE), //
  ACETATE("[M+CH3COO]-", "CH3COO", 59.013851, PolarityType.NEGATIVE), //
  TRIFLUORACETATE("[M+CF3COO]-", "CF3COO", 112.985586, PolarityType.NEGATIVE), //
  CHLORIDE("[M+Cl]-", "Cl", 34.969402, PolarityType.NEGATIVE), //
  BROMIDE("[M+Br]-", "Br", 78.918885, PolarityType.NEGATIVE); //


  private final String name, adductFormula;
  private final PolarityType polarity;
  private final double addedMass;

  IonizationType(String name, String adductFormula, double addedMass, PolarityType polarity) {

    this.name = name;
    this.adductFormula = adductFormula;
    this.addedMass = addedMass;
    this.polarity = polarity;
  }

  public String getAdduct() {
    return adductFormula;
  }

  public double getAddedMass() {
    return addedMass;
  }

  public PolarityType getPolarity() {
    return polarity;
  }

  @Override
  public String toString() {
    return name;
  }

}
