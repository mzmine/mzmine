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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

public enum LipidType {

  PC("Phosphatidylcholine", "PC", "C8H17NO8P", true, true, false), //
  PE("Phosphatidylethanolamine", "PE", "C5H11NO8P", true, true, false), //
  PI("Phosphatidylinositol", "PI", "C9H16O13P", true, true, false), //
  PS("Phosphatidylserine", "PS", "C6H11NO10P", true, true, false), //
  PG("Phosphatidylglycerol", "PG", "C6H12O10P", true, true, false), //
  BMP("Bis(Monoacylglycero)Phosphate", "BMP", "C6H12O9P", true, true, false), //
  CL("Cardiolipin", "CL", "C9H15O17P2", true, true, false), //
  DAG("Diacyglycerol", "DAG", "C3H5O5", true, true, false), //
  TAG("Triacyglycerol", "TAG", "C3H3O6", true, true, false), //
  MGDG("Monogalactosyldiacylglycerol", "MGDG", "C9H15O10", true, true, false), //
  DGDG("Digalactosyldiacylglycerol", "DGDG", "C15H25O15", true, true, false), //
  SQDG("Sulfoquinovosyldiacylglyerol", "SQDG", "C9H15O12S", true, true, false), //
  DGTS("Diacylglyceroltrimethylhomoserin", "DGTS", "C10H18O7N", true, true, false), //
  MELA("Monosylerythrotol lipid A", "MELA", "C14H21O13", true, true, false), //
  MELBC("Monosylerythrotol lipid B/C", "MELBC", "C12H19O12", true, true, false), //
  MELD("Monosylerythrotol lipid D", "MELD", "C10H17O11", true, true, false), //
  mRL("mono-Rhamnolipid", "mRL", "C6H9O9", true, true, false), //
  diRL("di-Rhamnolipid", "diRL", "C12H19O13", true, true, false), //
  HAA("HAA", "HAA", "C0H-1O5", true, true, false); //

  private final String name, abbr, formula;
  private boolean hasAlkylChain, hasAcylChain, hasCustomeChain;


  LipidType(String name, String abbr, String formula, Boolean hasAlkylChain, Boolean hasAcylChain,
      Boolean hasCustomeChain) {
    this.name = name;
    this.abbr = abbr;
    this.formula = formula;
    this.hasAlkylChain = hasAlkylChain;
    this.hasAlkylChain = hasAcylChain;
    this.hasAlkylChain = hasCustomeChain;
  }

  public String getAbbr() {
    return abbr;
  }

  public String getFormula() {
    return formula;
  }

  public String getName() {
    return this.name;
  }

  public boolean hasAlkylChain() {
    return this.hasAlkylChain;
  }

  public boolean hasAcylChain() {
    return this.hasAcylChain;
  }

  public boolean hasCustomeChain() {
    return this.hasCustomeChain;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
