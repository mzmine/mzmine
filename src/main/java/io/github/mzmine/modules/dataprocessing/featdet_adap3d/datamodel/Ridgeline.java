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
package io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel;

import java.util.ArrayList;


/**
 * <p>
 * Ridgeline class.
 * </p>
 *
 * @author owen myers Modified by Dharak Shah to include in MSDK
 */
public class Ridgeline {
  public int totalNumberOfScales;
  public int curBestInd = 0;
  public double curBestScale = 0.0;
  public double maxCorVal = 0.0;



  // These are public for priting reasons and debuging. When everything is
  // done they can be made private.
  public ArrayList<Double> scales_ = new ArrayList<Double>();
  public ArrayList<Integer> indecies_ = new ArrayList<Integer>();

  // Correlation values
  private ArrayList<Double> corValues_ = new ArrayList<Double>();
  private int curRunningGap_;

  /**
   * <p>
   * Constructor for Ridgeline.
   * </p>
   *
   * @param firstScale a double.
   * @param firstIndex a int.
   * @param corValue a double.
   * @param NScales a int.
   */
  public Ridgeline(double firstScale, int firstIndex, double corValue, int NScales) {
    scales_.add(firstScale);
    indecies_.add(firstIndex);
    corValues_.add(corValue);
    curRunningGap_ = 0;
    totalNumberOfScales = NScales;
  }

  /**
   * <p>
   * getRunningGapNum.
   * </p>
   *
   * @return a int.
   */
  public int getRunningGapNum() {
    return curRunningGap_;
  }

  /**
   * <p>
   * getRidgeLength.
   * </p>
   *
   * @return a int.
   */
  public int getRidgeLength() {
    return scales_.size();
  }

  /**
   * <p>
   * getRidgeStartScale.
   * </p>
   *
   * @return a double.
   */
  public double getRidgeStartScale() {
    return scales_.get(0);
  }

  /**
   * <p>
   * getRidgeEndScale.
   * </p>
   *
   * @return a double.
   */
  public double getRidgeEndScale() {
    int l = scales_.size();
    return scales_.get(l - 1);
  }

  /**
   * <p>
   * findBestValues.
   * </p>
   */
  public void findBestValues() {

    int index = 0;
    int count = 0;
    double[] arrayMaxCorVal = new double[corValues_.size()];
    int[] arrayCurBestInd = new int[indecies_.size()];
    double[] arrayCurBestScale = new double[scales_.size()];

    for (int i = 0; i < indecies_.size(); i++) {
      double curCor = corValues_.get(i);
      double previousCurCor = 0;
      double nextCurCor = 0;

      if (i - 1 >= 0) {
        previousCurCor = corValues_.get(i - 1);
      }
      if (i + 1 < indecies_.size()) {
        nextCurCor = corValues_.get(i + 1);
      }


      if (curCor > previousCurCor && curCor > nextCurCor && i - 1 >= 0
          && i + 1 <= indecies_.size()) {
        arrayMaxCorVal[count] = curCor;
        arrayCurBestInd[count] = indecies_.get(i);
        arrayCurBestScale[count] = scales_.get(i);
        count++;
      }
    }

    double minValue = arrayCurBestScale[0];
    for (int i = 1; i < arrayCurBestScale.length; i++) {
      if (arrayCurBestScale[i] < minValue && arrayCurBestScale[i] != 0.0) {
        minValue = arrayCurBestScale[i];
        index++;
      }
    }
    maxCorVal = arrayMaxCorVal[index];
    curBestInd = arrayCurBestInd[index];
    curBestScale = minValue;
  }


  /**
   * <p>
   * tryAddPoint.
   * </p>
   *
   * @param scale a double.
   * @param index a int.
   * @param corValue a double.
   * @return a boolean.
   */
  public boolean tryAddPoint(double scale, int index, double corValue) {
    // see if where this index is in relation to the last added
    int lastAddedInd = indecies_.get(indecies_.size() - 1);
    int indexDiff = Math.abs(lastAddedInd - index);

    int indexTol = (int) Math.round(findIndexTolFromScale(scale));



    // Need to see if something has already been added for this scale
    boolean haveThisScaleAlready = false;
    double epsilon = 0.000000001;
    if ((scales_.get(scales_.size() - 1) <= (scale + epsilon))
        && (scales_.get(scales_.size() - 1) >= (scale - epsilon))) {
      haveThisScaleAlready = true;
    }
    if (!haveThisScaleAlready) {

      // times 2 for pluss minus tollerance
      if (indexDiff < (2 * indexTol)) {

        scales_.add(scale);
        indecies_.add(index);
        corValues_.add(corValue);
        curRunningGap_ = 0;
        return true;
      } else {
        curRunningGap_++;
        return false;
      }

    } else {
      // two things to check
      // 1) is it closer in endex to previous?
      // 2) is it larger or smaller correlation value
      // For now lets just take the closest point unless this the first scale still.
      // If it is the first scale then lets pick the largest value.
      if (scales_.size() > 1) {
        // Lets try taking the largest one instead
        // int prevCor = corValues_[corValues_.size()-1];
        // int curCor = corValue;


        // if (curCor>prevCor){
        // indecies_[indecies_.size()-1]=index;
        // corValues_[indecies_.size()-1]=corValue;
        // return true;
        // }


        int prevIndexDiff =
            Math.abs(indecies_.get(indecies_.size() - 2) - indecies_.get(indecies_.size() - 1));
        int curIndexDiff = Math.abs(indecies_.get(indecies_.size() - 2) - index);

        if (prevIndexDiff > curIndexDiff) {
          indecies_.set(indecies_.size() - 1, index);
          corValues_.set(indecies_.size() - 1, corValue);
          return true;
        }
      } else {
        // only compare magnitued if they are close points
        if (indexDiff < (2 * indexTol)) {
          double prevCor = corValues_.get(0);
          if (corValue > prevCor) {
            indecies_.set(indecies_.size() - 1, index);
            corValues_.set(indecies_.size() - 1, corValue);
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * <p>
   * findIndexTolFromScale.
   * </p>
   *
   * @param scale a double.
   * @return a double.
   */
  public double findIndexTolFromScale(double scale) {
    // This is probably too simple but it apears to work well enough. Use for now ans then look into
    // correct value

    // window size for scale = 1 is [-5,5]
    // return scale*5;
    //
    // I think above is much to big. Going to try this
    return scale;
    // return 2;
  }



}
