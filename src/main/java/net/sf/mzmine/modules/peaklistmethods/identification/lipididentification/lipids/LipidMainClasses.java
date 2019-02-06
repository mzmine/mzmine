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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

/**
 * Enum that contains all lipid main classes. Each enum contains information on the name and
 * lipidCoreClass
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidMainClasses {

  // Fattyacyls
  FATTYACIDS("Fatty acids", LipidCoreClasses.FATTYACYLS), //
  FATTYALCOHOLS("Fatty alcohols", LipidCoreClasses.FATTYACYLS), //
  FATTYALDEHYDES("Fatty aldehydes", LipidCoreClasses.FATTYACYLS), //
  FATTYESTERS("Fatty esters", LipidCoreClasses.FATTYACYLS), //
  FATTYAMIDS("Fatty amids", LipidCoreClasses.FATTYACYLS), //
  FATTYNITRILES("Fatty esters", LipidCoreClasses.FATTYACYLS), //
  FATTYETHERS("Fatty ehters", LipidCoreClasses.FATTYACYLS), //
  HYDROCARBONS("Hydrocarbons", LipidCoreClasses.FATTYACYLS), //
  RHAMNOLIPIDS("Rhamnolipids", LipidCoreClasses.FATTYACYLS), //
  SOPHOROLIPIDS("Sophorolipds", LipidCoreClasses.FATTYACYLS), //

  // Glycerolipids
  MONORADYLGLYCEROLS("Monoradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  DIRADYLGLYCEROLS("Diradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  TRIRADYLGLYCEROLS("Triradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  GLYCOSYLDIACYLGLYCEROLS("Glycosyldiacylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  GLYCOSYLMONOACYLGLYCEROLS("Glycosylmonoacylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  OTHERGLYCEROLIPIDS("Other glycerolipids", LipidCoreClasses.GLYCEROLIPIDS), //

  // Glycerophospholipids
  PHOSPHATIDYLCHOLINE("Phosphatidylcholine", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOETHANOLAMINES("Glycerophosphoethanolamines", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOSERINES("Glycerophosphoserines", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROLS("Glycerophosphoglycerols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROPHOSPHATES("Glycerophosphoglycerophosphates",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLS("Glycerophosphoinositols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHATES("Glycerophosphates", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  CARDIOLIPIN("Cardiolipin", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  CDPGLYCEROLS("CDP-Glycerols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //

  // Sphingolipids
  CERAMIDES("Ceramides", LipidCoreClasses.SPHINGOLIPIDS), //
  PHOSPHOSPHINGOLIPIDS("Phosphosphingolipids", LipidCoreClasses.SPHINGOLIPIDS);//


  private String name;
  private LipidCoreClasses coreClass;

  LipidMainClasses(String name, LipidCoreClasses coreClass) {
    this.name = name;
    this.coreClass = coreClass;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LipidCoreClasses getCoreClass() {
    return coreClass;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
