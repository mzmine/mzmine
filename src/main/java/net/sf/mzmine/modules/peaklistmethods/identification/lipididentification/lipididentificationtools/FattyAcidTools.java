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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import java.util.ArrayList;
import net.sf.mzmine.datamodel.PeakIdentity;

/**
 * This class contains methods to build fatty acids for MS/MS identification of lipids
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FattyAcidTools {

  /**
   * This method calculates all possible fatty acids formulas for a selected annotated lipid
   */
  public ArrayList<String> calculateFattyAcidFormulas(PeakIdentity peakIdentity) {
    ArrayList<String> fattyAcidFormulas = new ArrayList<String>();
    LipidTools lipidTools = new LipidTools();
    int minFattyAcidLength = 1;
    int maxFattyAcidLength = lipidTools.getNumberOfCAtoms(peakIdentity.getName());
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = lipidTools.getNumberOfDB(peakIdentity.getName());

    for (int fattyAcidLength =
        minFattyAcidLength; fattyAcidLength <= maxFattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          minNumberOfDoubleBonds; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        if (((fattyAcidDoubleBonds >= 0)
            && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
          fattyAcidFormulas.add(calculateFattyAcidFormula(fattyAcidLength, fattyAcidDoubleBonds));
        }
      }
    }

    return fattyAcidFormulas;
  }

  /**
   * This method creates all possible fatty acid names for a selected annotated lipid
   */
  public ArrayList<String> getFattyAcidNames(PeakIdentity peakIdentity) {

    ArrayList<String> fattyAcidNames = new ArrayList<String>();
    LipidTools lipidTools = new LipidTools();
    int minFattyAcidLength = 1;
    int maxFattyAcidLength = lipidTools.getNumberOfCAtoms(peakIdentity.getName());
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = lipidTools.getNumberOfDB(peakIdentity.getName());

    for (int fattyAcidLength =
        minFattyAcidLength; fattyAcidLength <= maxFattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          minNumberOfDoubleBonds; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        if (((fattyAcidDoubleBonds >= 0)
            && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
          fattyAcidNames.add(getFattyAcidName(fattyAcidLength, fattyAcidDoubleBonds));
        }
      }
    }

    return fattyAcidNames;
  }

  /**
   * This method creates a String sum formula for a fatty acid
   */
  public String calculateFattyAcidFormula(int fattyAcidLength, int fattyAcidDoubleBonds) {
    int numberOfHydrogens = fattyAcidLength * 2 - fattyAcidDoubleBonds * 2;
    String fattyAcidFormula = "C" + fattyAcidLength + 'H' + numberOfHydrogens + 'O' + 2;

    return fattyAcidFormula;
  }

  /**
   * This method creates the systematic name of a fatty acid
   */
  public String getFattyAcidName(int fattyAcidLength, int fattyAcidDoubleBonds) {
    return new String("(" + fattyAcidLength + ":" + fattyAcidDoubleBonds + ")");
  }

}
