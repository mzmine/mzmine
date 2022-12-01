/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import org.jfree.data.xy.AbstractXYZDataset;

/*
 * XYZDataset for Van Krevelen diagram
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
class VanKrevelenDiagramXYZDataset extends AbstractXYZDataset {

  private static final long serialVersionUID = 1L;

  private FeatureListRow filteredRows[];
  private int numberOfDatapoints = 0;
  private double[] xValues;
  private double[] yValues;
  private double[] zValues;

  public VanKrevelenDiagramXYZDataset(String zAxisLabel, FeatureListRow[] filteredRows) {

    this.filteredRows = filteredRows;

    ArrayList<Integer> numberOfCAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfOAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfHAtoms = new ArrayList<Integer>();
    ArrayList<Double> zValuesList = new ArrayList<Double>();
    // get number of atoms
    int atomsC = 0;
    int atomsO = 0;
    int atomsH = 0;
    for (int i = 0; i < filteredRows.length; i++) {
      atomsC = getNumberOfCAtoms(filteredRows[i]);
      atomsO = getNumberOfOAtoms(filteredRows[i]);
      atomsH = getNumberOfHAtoms(filteredRows[i]);
      if (atomsC != 0 && atomsO != 0 && atomsH != 0) {
        numberOfCAtoms.add(atomsC);
        numberOfOAtoms.add(atomsO);
        numberOfHAtoms.add(atomsH);
        // plot selected feature characteristic as z Axis
        if (zAxisLabel.equals("Retention time")) {
          zValuesList.add((double) filteredRows[i].getAverageRT());
        } else if (zAxisLabel.equals("Intensity")) {
          zValuesList.add(filteredRows[i].getAverageHeight().doubleValue());
        } else if (zAxisLabel.equals("Area")) {
          zValuesList.add(filteredRows[i].getAverageArea().doubleValue());
        } else if (zAxisLabel.equals("Tailing factor")) {
          zValuesList.add((double) filteredRows[i].getBestFeature().getTailingFactor());
        } else if (zAxisLabel.equals("Asymmetry factor")) {
          zValuesList.add((double) filteredRows[i].getBestFeature().getAsymmetryFactor());
        } else if (zAxisLabel.equals("FWHM")) {
          zValuesList.add((double) filteredRows[i].getBestFeature().getFWHM());
        } else if (zAxisLabel.equals("m/z")) {
          zValuesList.add(filteredRows[i].getBestFeature().getMZ());
        }

      }
    }
    numberOfDatapoints = numberOfCAtoms.size();
    // Calc xValues
    xValues = new double[numberOfCAtoms.size()];
    for (int i = 0; i < numberOfCAtoms.size(); i++) {
      // calc the ratio of O/C
      xValues[i] = (double) numberOfOAtoms.get(i) / numberOfCAtoms.get(i);
    } // Calc yValues
    yValues = new double[numberOfCAtoms.size()];
    for (int i = 0; i < numberOfCAtoms.size(); i++) {
      // calc the ratio of H/C
      yValues[i] = (double) numberOfHAtoms.get(i) / numberOfCAtoms.get(i);
    }
    zValues = new double[numberOfCAtoms.size()];
    for (int i = 0; i < numberOfCAtoms.size(); i++) {
      // get intensity
      zValues[i] = zValuesList.get(i);
    }
  }

  private int getNumberOfCAtoms(FeatureListRow row) {
    int numberOfCAtoms = 0;
    if (row.getPreferredFeatureIdentity() != null) {
      String rowName = row.getPreferredFeatureIdentity().getPropertyValue("Molecular formula");
      int indexC = 0;
      int indexNextAtom = 0;
      int nextAtomCounter = 0;
      String numberOfC = null;
      boolean hasC = false;
      // Loop through every char and check for "C"
      for (int i = 0; i < rowName.length(); i++) {
        // get C index
        if (rowName.charAt(i) == 'C') {
          hasC = true;
          indexC = i;
          // get index of next Atom
          for (int j = i + 1; j < rowName.length(); j++) {
            if (Character.isAlphabetic(rowName.charAt(j)) && nextAtomCounter == 0) {
              indexNextAtom = j;
              nextAtomCounter++;
            }
          }
          // check if searched atom number is last atom of formula
          if (nextAtomCounter == 0) {
            // check how many digits for last Atom index
            indexNextAtom = rowName.length();
          }
        }

      }
      if (hasC == true) {
        numberOfC = rowName.substring(indexC + 1, indexNextAtom);
        if (numberOfC.equals("") == true) {
          numberOfCAtoms = 1;
        } else {
          numberOfCAtoms = Integer.parseInt(numberOfC);
        }
      } else {
        numberOfCAtoms = 0;
      }
      return numberOfCAtoms;
    }

    return numberOfCAtoms;
  }

  private int getNumberOfOAtoms(FeatureListRow row) {
    int numberOfOAtoms = 0;
    if (row.getPreferredFeatureIdentity() != null) {
      String rowName = row.getPreferredFeatureIdentity().getPropertyValue("Molecular formula");
      int indexO = 0;
      int indexNextAtom = 0;
      int nextAtomCounter = 0;
      String numberOfO = null;
      boolean hasO = false;
      // Loop through every char and check for "C"
      for (int i = 0; i < rowName.length(); i++) {
        // get C index
        if (rowName.charAt(i) == 'O') {
          hasO = true;
          indexO = i;
          // get index of next Atom
          for (int j = i + 1; j < rowName.length(); j++) {
            if (Character.isAlphabetic(rowName.charAt(j)) && nextAtomCounter == 0) {
              indexNextAtom = j;
              nextAtomCounter++;
            }
          }
          // check if searched atom number is last atom of formula
          if (nextAtomCounter == 0) {
            // check how many digits for last Atom index
            indexNextAtom = rowName.length();
          }
        }

      }
      if (hasO == true) {
        numberOfO = rowName.substring(indexO + 1, indexNextAtom);
        if (numberOfO.equals("") == true) {
          numberOfOAtoms = 1;
        } else {
          numberOfOAtoms = Integer.parseInt(numberOfO);
        }
      } else {
        numberOfOAtoms = 0;
      }
      return numberOfOAtoms;
    }

    return numberOfOAtoms;
  }

  private int getNumberOfHAtoms(FeatureListRow row) {
    int numberOfHAtoms = 0;
    if (row.getPreferredFeatureIdentity() != null) {
      String rowName = row.getPreferredFeatureIdentity().getPropertyValue("Molecular formula");
      int indexH = 0;
      int indexNextAtom = 0;
      int nextAtomCounter = 0;
      String numberOfH = null;
      boolean hasC = false;
      // Loop through every char and check for "C"
      for (int i = 0; i < rowName.length(); i++) {
        // get C index
        if (rowName.charAt(i) == 'H') {
          hasC = true;
          indexH = i;
          // get index of next Atom
          for (int j = i + 1; j < rowName.length(); j++) {
            if (Character.isAlphabetic(rowName.charAt(j)) && nextAtomCounter == 0) {
              indexNextAtom = j;
              nextAtomCounter++;
            }
          }
          // check if searched atom number is last atom of formula
          if (nextAtomCounter == 0) {
            // check how many digits for last Atom index
            indexNextAtom = rowName.length();
          }
        }

      }
      if (hasC == true) {
        numberOfH = rowName.substring(indexH + 1, indexNextAtom);
        if (numberOfH.equals("") == true) {
          numberOfHAtoms = 1;
        } else {
          numberOfHAtoms = Integer.parseInt(numberOfH);
        }
      } else {
        numberOfHAtoms = 0;
      }
      return numberOfHAtoms;
    }

    return numberOfHAtoms;
  }

  @Override
  public int getItemCount(int series) {
    return numberOfDatapoints;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int row) {
    return filteredRows[row].toString();
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return getRowKey(series);
  }

  public double[] getxValues() {
    return xValues;
  }

  public double[] getyValues() {
    return yValues;
  }

  public double[] getzValues() {
    return zValues;
  }

}
