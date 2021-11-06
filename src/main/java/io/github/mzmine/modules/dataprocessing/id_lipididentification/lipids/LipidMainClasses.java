/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

/**
 * Enum that contains all lipid main classes. Each enum contains information on the name and
 * lipidCoreClass
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidMainClasses {

  // Fatty Acyls
  RHAMNOLIPIDS("Rhamnolipids", LipidCategories.FATTYACYLS), //

  // Glycerolipids
  MONORADYLGLYCEROLS("Monoradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  DIRADYLGLYCEROLS("Diradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  TRIRADYLGLYCEROLS("Triradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  GLYCOSYLDIACYLGLYCEROLS("Glycosyldiacylglycerols", LipidCategories.GLYCEROLIPIDS), //
  GLYCOSYLMONOACYLGLYCEROLS("Glycosylmonoacylglycerols", LipidCategories.GLYCEROLIPIDS), //
  OTHERGLYCEROLIPIDS("Other glycerolipids", LipidCategories.GLYCEROLIPIDS), //

  // Glycerophospholipids
  PHOSPHATIDYLCHOLINE("Phosphatidylcholine", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOETHANOLAMINES("Glycerophosphoethanolamines", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOSERINES("Glycerophosphoserines", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROLS("Glycerophosphoglycerols", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROPHOSPHATES("Glycerophosphoglycerophosphates",
      LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLS("Glycerophosphoinositols", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLGLYCANS("Glycerophosphoinositolglycans",
      LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHATES("Glycerophosphates", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  CARDIOLIPIN("Cardiolipin", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  CDPGLYCEROLS("CDP-Glycerols", LipidCategories.GLYCEROPHOSPHOLIPIDS); //

  private String name;
  private LipidCategories coreClass;

  LipidMainClasses(String name, LipidCategories coreClass) {
    this.name = name;
    this.coreClass = coreClass;
  }

  public String getName() {
    return name;
  }

  public LipidCategories getCoreClass() {
    return coreClass;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
