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
import org.jfree.data.xy.AbstractXYDataset;

/*
 * XYDataset for Van Krevelen diagram
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
class VanKrevelenDiagramXYDataset extends AbstractXYDataset {

  private static final long serialVersionUID = 1L;

  private FeatureListRow filteredRows[];
  private int numberOfDatapoints = 0;
  private double[] xValues;
  private double[] yValues;

  public VanKrevelenDiagramXYDataset(FeatureListRow[] filteredRows) {

    this.filteredRows = filteredRows;

    ArrayList<Integer> numberOfCAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfOAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfHAtoms = new ArrayList<Integer>();
    // get number of atoms
    for (int i = 0; i < filteredRows.length; i++) {
      if (getNumberOfCAtoms(filteredRows[i]) != 0 && getNumberOfOAtoms(filteredRows[i]) != 0
          && getNumberOfHAtoms(filteredRows[i]) != 0) {
        numberOfCAtoms.add(getNumberOfCAtoms(filteredRows[i]));
        numberOfOAtoms.add(getNumberOfOAtoms(filteredRows[i]));
        numberOfHAtoms.add(getNumberOfHAtoms(filteredRows[i]));
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
      // calc the ratio of O/C
      yValues[i] = (double) numberOfHAtoms.get(i) / numberOfCAtoms.get(i);
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

}
