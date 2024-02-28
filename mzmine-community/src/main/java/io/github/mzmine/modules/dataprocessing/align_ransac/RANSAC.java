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
package io.github.mzmine.modules.dataprocessing.align_ransac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;

import com.google.common.collect.Range;

import io.github.mzmine.parameters.ParameterSet;

public class RANSAC {

  /**
   * input: data - a set of observed data points n - the minimum number of data values required to
   * fit the model k - the maximum number of iterations allowed in the algorithm t - a threshold
   * value for determining when a data point fits a model d - the number of close data values
   * required to assert that a model fits well to data
   *
   * output: model which best fit the data
   */
  private int n;
  private double d = 1;
  private int k = 0;
  private int AlsoNumber;
  private double numRatePoints, t;
  private boolean Linear;

  public RANSAC(ParameterSet parameters) {

    this.numRatePoints = parameters.getParameter(RansacAlignerParameters.NMinPoints).getValue();

    this.t = parameters.getParameter(RansacAlignerParameters.Margin).getValue();

    this.k = parameters.getParameter(RansacAlignerParameters.Iterations).getValue();

    this.Linear = parameters.getParameter(RansacAlignerParameters.Linear).getValue();

  }

  /**
   * Set all parameters and start ransac.
   * 
   * @param data vector with the points which represent all possible alignments.
   */
  public void alignment(List<AlignStructMol> data) {
    try {
      // If the model is non linear 4 points are taken to build the model,
      // if it is linear only 2 points are taken.
      if (!Linear) {
        n = 4;
      } else {
        n = 2;
      }

      // Minimun number of points required to assert that a model fits
      // well to data
      if (data.size() < 10) {
        d = 3;
      } else {
        d = data.size() * numRatePoints;
      }

      // Calculate the number of trials if the user has not define them
      if (k == 0) {
        k = (int) getK();
      }

      ransac(data);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Calculate k (number of trials)
   * 
   * @return number of trials "k" required to select a subset of n good data points.
   */
  private double getK() {
    double w = numRatePoints;
    double b = Math.pow(w, n);
    return Math.log10(1 - 0.99) / Math.log10(1 - b) + (Math.sqrt(1 - b) / b);
  }

  /**
   * RANSAC algorithm
   * 
   * @param data vector with the points which represent all possible alignments.
   */
  public void ransac(List<AlignStructMol> data) {
    double besterr = 9.9E99;

    for (int iterations = 0; iterations < k; iterations++) {
      AlsoNumber = n;
      // Get the initial points
      boolean initN = getInitN(data);
      if (!initN) {
        continue;
      }

      // Calculate the model
      fittPolinomialFunction(data, Linear);

      // If the model has the minimun number of points
      if (AlsoNumber >= d) {
        // Get the error of the model based on the number of points
        double error = 9.9E99;
        try {
          error = newError(data);
        } catch (Exception ex) {
          Logger.getLogger(RANSAC.class.getName()).log(Level.SEVERE, null, ex);
        }

        // If the error is less than the error of the last model
        if (error < besterr) {
          besterr = error;
          for (int i = 0; i < data.size(); i++) {
            AlignStructMol alignStruct = data.get(i);
            if (alignStruct.ransacAlsoInLiers || alignStruct.ransacMaybeInLiers) {
              alignStruct.Aligned = true;
            } else {
              alignStruct.Aligned = false;
            }

            alignStruct.ransacAlsoInLiers = false;
            alignStruct.ransacMaybeInLiers = false;

          }
        }
      }
      // remove the model
      for (int i = 0; i < data.size(); i++) {
        AlignStructMol alignStruct = data.get(i);
        alignStruct.ransacAlsoInLiers = false;
        alignStruct.ransacMaybeInLiers = false;
      }

    }
  }

  /**
   * Take the initial points ramdoly. The points are divided by the initial number of points. If the
   * fractions contain enough number of points took one point from each part.
   * 
   * @param data vector with the points which represent all possible alignments.
   * @return false if there is any problem.
   */
  private boolean getInitN(List<AlignStructMol> data) {
    if (data.size() > n) {
      Collections.sort(data, new AlignStructMol());
      double min = data.get(0).RT;
      double max = data.get(data.size() - 1).RT;

      Range<Double> rtRange = Range.closed(min, ((max - min) / 2) + min);

      int cont = 0, bucle = 0;
      while (cont < n / 2 && bucle < 1000) {
        int index = (int) (data.size() * Math.random());
        if (!data.get(index).ransacMaybeInLiers && rtRange.contains(data.get(index).RT)) {
          data.get(index).ransacMaybeInLiers = true;
          cont++;

        }

        bucle++;
      }
      if (bucle >= 1000) {
        getN(data, (n / 2) - cont);
      }

      bucle = 0;
      rtRange = Range.closed(((max - min) / 2) + min, max);

      while (cont < n && bucle < 1000) {

        int index = (int) (data.size() * Math.random());
        if (!data.get(index).ransacMaybeInLiers && rtRange.contains(data.get(index).RT)) {
          data.get(index).ransacMaybeInLiers = true;
          cont++;
        }
        bucle++;
      }
      if (bucle >= 1000) {
        getN(data, n - cont);
      }
      return true;
    } else {
      return false;
    }
  }

  private void getN(List<AlignStructMol> data, int newN) {
    if (newN < 1) {
      return;
    }
    int cont = 0;
    while (cont < newN) {
      int index = (int) (data.size() * Math.random());
      if (!data.get(index).ransacMaybeInLiers) {
        data.get(index).ransacMaybeInLiers = true;
        cont++;
      }
    }
  }

  private void fittPolinomialFunction(List<AlignStructMol> data, boolean linear) {
    List<AlignStructMol> points = new ArrayList<AlignStructMol>();

    int degree = 3;
    if (linear) {
      degree = 1;
    }

    PolynomialFitter fitter = new PolynomialFitter(degree, new GaussNewtonOptimizer(true));
    for (int i = 0; i < data.size(); i++) {
      AlignStructMol point = data.get(i);
      if (point.ransacMaybeInLiers) {
        points.add(point);
        fitter.addObservedPoint(1, point.RT, point.RT2);
      }
    }
    try {
      PolynomialFunction function = fitter.fit();
      for (AlignStructMol point : data) {
        double y = point.RT2;
        double bestY = function.value(point.RT);
        if (Math.abs(y - bestY) < t) {
          point.ransacAlsoInLiers = true;
          AlsoNumber++;
        } else {
          point.ransacAlsoInLiers = false;
        }
      }
    } catch (Exception ex) {
    }
  }

  /**
   * calculate the error in the model
   * 
   * @param data vector with the points which represent all possible alignments.
   * @param regression regression of the alignment points
   * @return the error in the model
   */
  private double newError(List<AlignStructMol> data) throws Exception {

    double numT = 1;
    for (int i = 0; i < data.size(); i++) {
      if (data.get(i).ransacAlsoInLiers || data.get(i).ransacMaybeInLiers) {
        numT++;
      }
    }
    return 1 / numT;

  }
}
