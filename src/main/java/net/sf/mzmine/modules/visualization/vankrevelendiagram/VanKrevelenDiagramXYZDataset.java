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

package net.sf.mzmine.modules.visualization.vankrevelendiagram;

import java.util.ArrayList;
import org.jfree.data.xy.AbstractXYZDataset;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.ParameterSet;

/*
 * XYZDataset for Van Krevelen diagram
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
class VanKrevelenDiagramXYZDataset extends AbstractXYZDataset {

  private static final long serialVersionUID = 1L;

  private PeakListRow selectedRows[];
  private int numberOfDatapoints = 0;
  private double[] xValues;
  private double[] yValues;
  private double[] zValues;
  private String zAxisLabel;

  public VanKrevelenDiagramXYZDataset(ParameterSet parameters) {

    PeakList peakList = parameters.getParameter(VanKrevelenDiagramParameters.peakList).getValue()
        .getMatchingPeakLists()[0];

    zAxisLabel =
        parameters.getParameter(VanKrevelenDiagramParameters.zAxisValues).getValue().toString();

    this.selectedRows = parameters.getParameter(VanKrevelenDiagramParameters.selectedRows)
        .getMatchingRows(peakList);
    ArrayList<Integer> numberOfCAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfOAtoms = new ArrayList<Integer>();
    ArrayList<Integer> numberOfHAtoms = new ArrayList<Integer>();
    ArrayList<Double> zValuesList = new ArrayList<Double>();
    // get number of atoms
    for (int i = 0; i < selectedRows.length; i++) {
      if (getNumberOfCAtoms(selectedRows[i]) != 0 && getNumberOfOAtoms(selectedRows[i]) != 0
          && getNumberOfHAtoms(selectedRows[i]) != 0) {
        numberOfCAtoms.add(getNumberOfCAtoms(selectedRows[i]));
        numberOfOAtoms.add(getNumberOfOAtoms(selectedRows[i]));
        numberOfHAtoms.add(getNumberOfHAtoms(selectedRows[i]));
        // plot selected feature characteristic as z Axis
        if (zAxisLabel.equals("Retention time")) {
          zValuesList.add(selectedRows[i].getAverageRT());
        } else if (zAxisLabel.equals("Intensity")) {
          zValuesList.add(selectedRows[i].getAverageHeight());
        } else if (zAxisLabel.equals("Area")) {
          zValuesList.add(selectedRows[i].getAverageArea());
        } else if (zAxisLabel.equals("Tailing factor")) {
          zValuesList.add(selectedRows[i].getBestPeak().getTailingFactor());
        } else if (zAxisLabel.equals("Asymmetry factor")) {
          zValuesList.add(selectedRows[i].getBestPeak().getAsymmetryFactor());
        } else if (zAxisLabel.equals("FWHM")) {
          zValuesList.add(selectedRows[i].getBestPeak().getFWHM());
        } else if (zAxisLabel.equals("m/z")) {
          zValuesList.add(selectedRows[i].getBestPeak().getMZ());
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
      // calc the ratio of O/C
      yValues[i] = (double) numberOfHAtoms.get(i) / numberOfCAtoms.get(i);
    }
    zValues = new double[numberOfCAtoms.size()];
    for (int i = 0; i < numberOfCAtoms.size(); i++) {
      // get intensity
      zValues[i] = zValuesList.get(i);
    }
  }

  private int getNumberOfCAtoms(PeakListRow row) {
    int numberOfCAtoms = 0;
    if (row.getPreferredPeakIdentity() != null) {
      String rowName = row.getPreferredPeakIdentity().getName();
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

  private int getNumberOfOAtoms(PeakListRow row) {
    int numberOfOAtoms = 0;
    if (row.getPreferredPeakIdentity() != null) {
      String rowName = row.getPreferredPeakIdentity().getName();
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

  private int getNumberOfHAtoms(PeakListRow row) {
    int numberOfHAtoms = 0;
    if (row.getPreferredPeakIdentity() != null) {
      String rowName = row.getPreferredPeakIdentity().getName();
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
    return selectedRows[row].toString();
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
