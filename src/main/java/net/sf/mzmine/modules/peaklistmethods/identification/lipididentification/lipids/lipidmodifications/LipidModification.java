package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.mzmine.util.FormulaUtils;

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
