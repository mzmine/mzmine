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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.mzmine.util.FormulaUtils;

/**
 * This class represents a lipid modification
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidModification {

  private String lipidModification;

  public LipidModification(String lipidModification) {
    this.lipidModification = lipidModification;
  }

  public String getLipidModificatio() {
    return lipidModification;
  }

  public void setLipidModification(String newLipidModification) {
    lipidModification = newLipidModification;
  }

  /**
   * This method calculates the exact mass of lipid modification
   */
  public double getModificationMass() {
    Double lipidModificationMass = 0.0;
    ArrayList<String> subModificationList = new ArrayList<String>();
    ArrayList<String> subModificationListSign = new ArrayList<String>();
    // split modification string at - oder +
    for (int i = 0; i < lipidModification.length(); i++) {
      // search for mathematical sign
      if (lipidModification.charAt(i) == '+') {
        subModificationListSign.add("+");
        // search for next mathematical sign or last char
        for (int j = i + 1; j < lipidModification.length(); j++) {
          if (lipidModification.charAt(j) == '+') {
            subModificationList.add(lipidModification.substring(i + 1, j));
            break;
          } else if (lipidModification.charAt(j) == '-') {
            subModificationList.add(lipidModification.substring(i + 1, j));
            break;
          } else if (j + 1 == lipidModification.length()) {
            subModificationList.add(lipidModification.substring(i + 1, lipidModification.length()));
            break;
          }
        }
      }
      if (lipidModification.charAt(i) == '-') {
        subModificationListSign.add("-");
        for (int j = i + 1; j < lipidModification.length(); j++) {
          if (lipidModification.charAt(j) == '+') {
            subModificationList.add(lipidModification.substring(i + 1, j));
            break;
          } else if (lipidModification.charAt(j) == '-') {
            subModificationList.add(lipidModification.substring(i + 1, j));
            break;
          } else if (j + 1 == lipidModification.length()) {
            subModificationList.add(lipidModification.substring(i + 1, lipidModification.length()));
            break;
          }
        }
      }
    }

    // Calculate masses for sub modifications
    for (int i = 0; i < subModificationList.size(); i++) {
      if (subModificationListSign.get(i) == "+") {
        lipidModificationMass =
            lipidModificationMass + FormulaUtils.calculateExactMass(subModificationList.get(i));
      } else if (subModificationListSign.get(i) == "-") {
        lipidModificationMass = lipidModificationMass
            + FormulaUtils.calculateExactMass(subModificationList.get(i)) * (-1);
      } else {
        lipidModificationMass =
            lipidModificationMass + FormulaUtils.calculateExactMass(subModificationList.get(i));
      }
    }
    return lipidModificationMass;
  }

  @Override
  public String toString() {
    NumberFormat format = new DecimalFormat("0.0000");
    return "Modify lipid with [" + lipidModification + "] (" + format.format(getModificationMass())
        + ")";
  }

}
