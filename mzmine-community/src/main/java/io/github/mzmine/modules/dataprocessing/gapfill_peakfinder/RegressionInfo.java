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
package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;

import io.github.mzmine.modules.dataprocessing.align_ransac.RTs;

public class RegressionInfo {

  private List<RTs> data;
  private PolynomialFunction function;

  public RegressionInfo() {
    this.data = new ArrayList<RTs>();

  }

  public void setFunction() {
    function = getPolynomialFunction();
  }

  public double predict(double RT) {
    try {
      return function.value(RT);
    } catch (Exception ex) {
      return -1;
    }
  }

  public void addData(double RT, double RT2) {
    this.data.add(new RTs(RT, RT2));
  }

  private PolynomialFunction getPolynomialFunction() {
    Collections.sort(data, new RTs());
    PolynomialFitter fitter = new PolynomialFitter(3, new GaussNewtonOptimizer(true));
    for (RTs rt : data) {
      fitter.addObservedPoint(1, rt.RT, rt.RT2);
    }
    try {
      return fitter.fit();

    } catch (Exception ex) {
      return null;
    }
  }

}
