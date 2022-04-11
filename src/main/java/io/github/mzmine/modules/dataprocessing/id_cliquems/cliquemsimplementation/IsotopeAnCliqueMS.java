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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */


package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;

/**
 * Calculates features for isotope calculation.
 * <p>
 * See https://github.com/osenan/cliqueMS/blob/master/src/isotopesanCliqueMSR.h for Rcpp code
 * corresponding to this class
 */
public class IsotopeAnCliqueMS {

  private final List<Pair<Double, Pair<Double, Integer>>> isoData;

  private final List<Integer> pfeature = new ArrayList<>();
  private final List<Integer> ifeature = new ArrayList<>();
  private final List<Integer> pcharge = new ArrayList<>();
  private final List<Integer> icharge = new ArrayList<>();

  public IsotopeAnCliqueMS(List<Pair<Double, Pair<Double, Integer>>> isoData) {
    this.isoData = isoData;
  }

  /**
   * Function to determine whether two masses are isotope or not
   *
   * @param mz1            first mass
   * @param mz2            second mass
   * @param reference      The reference mass difference between two features to be considered
   *                       isotopes
   * @param isoMZTolerance mass tolerance for considering isotopes
   * @return
   */
  private boolean errorRange(Double mz1, Double mz2, Double reference, MZTolerance isoMZTolerance) {
    boolean result = false;
    MZTolerance mzTolerance_mod = new MZTolerance(isoMZTolerance.getMzTolerance() * Math.sqrt(2.0),
        isoMZTolerance.getPpmTolerance() * Math.sqrt(2.0)); // modified value acc. to algorithm
    Range<Double> mzRange = mzTolerance_mod.getToleranceRange(mz1 + reference);
    if (mzRange.contains(mz2)) {
      result = true;
    }
    return result;
  }

  /**
   * Function to determine whether two masses are isotope or not, if yes assign the parent and child
   * mass
   *
   * @param mz1            first mass
   * @param mz2            second mass
   * @param maxCharge      maximum charge to be anntotated to a isotope
   * @param isoMZTolerance mass tolerance for considering isotopes
   * @param isom           The reference mass difference between two features to be considered
   *                       isotopes
   * @return
   */
  private IsoTest isIsotope(Double mz1, Double mz2, Integer maxCharge, MZTolerance isoMZTolerance,
      Double isom) {

    IsoTest finalIso = new IsoTest();

    for (int charge1 = 1; charge1 <= maxCharge; charge1++) {
      for (int charge2 = 1; charge2 <= maxCharge; charge2++) {
        IsoTest currentIso = new IsoTest();
        Double cmz1, cmz2;
        cmz1 = mz1 * charge1;
        cmz2 = mz2 * charge2;
        // if isotope mass is bigger than parental mass
        if (cmz2 > cmz1) {
          currentIso.isIso = errorRange(cmz1, cmz2, isom, isoMZTolerance);
          if (currentIso.isIso) {
            finalIso.isIso = true;
            finalIso.pCharge = charge1;
            finalIso.iCharge = charge2;
          }
        }

      }
    }
    return finalIso;
  }


  /**
   * Function to calculate parent feature (pfeature), isotope corresponding to the parent(ifeature),
   * charge and grade corresponding to the features
   *
   * @param maxCharge      maximum charge to be annotated to each feature
   * @param isoMZTolerance mass tolerance for considering isotopes
   * @param isom           The reference mass difference between two features to be considered
   *                       isotopes
   */
  public void getIsotopes(Integer maxCharge, MZTolerance isoMZTolerance, Double isom) {

    for (int id1 = 0; id1 < isoData.size(); id1++) {
      for (int id2 = 1; id2 < isoData.size(); id2++) {
        if (id2 > id1) {
          Double mz1 = isoData.get(id1).getValue().getKey();
          Double mz2 = isoData.get(id2).getValue().getKey();
          IsoTest iTest = isIsotope(mz1, mz2, maxCharge, isoMZTolerance, isom);
          if (iTest.isIso) {
            pfeature.add(isoData.get(id1).getValue().getValue());
            ifeature.add(isoData.get(id2).getValue().getValue());
            pcharge.add(iTest.pCharge);
            icharge.add(iTest.iCharge);
          }
        }
      }
    }

  }


  public List<Integer> getPfeature() {
    return pfeature;
  }

  public List<Integer> getIfeature() {
    return ifeature;
  }

  public List<Integer> getPcharge() {
    return pcharge;
  }

  public List<Integer> getIcharge() {
    return icharge;
  }

  /**
   * Temp class to hold isotope data
   */
  private class IsoTest {

    boolean isIso = false;
    Integer pCharge;
    Integer iCharge;
  }

}
