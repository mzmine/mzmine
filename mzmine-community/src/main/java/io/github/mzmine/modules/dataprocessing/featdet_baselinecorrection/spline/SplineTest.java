/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.spline;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class SplineTest {

  public static void main(String[] args) throws IOException, CsvException {
    // Example data
    final List<String[]> strings = CSVParsingUtils.readData(
        new File("C:\\Users\\Steffen\\Desktop\\baseline.csv"), ";");

    final double[] x = new double[strings.size()];
    final double[] y = new double[strings.size()];
    for (int i = 1; i < strings.size(); i++) {
      x[i] = Double.parseDouble(strings.get(i)[0]);
      y[i] = Double.parseDouble(strings.get(i)[1]);
    }

//    SplineInterpolator interpolator = new SplineInterpolator();
//    PolynomialSplineFunction splineFunction = interpolator.interpolate(subsample(x, 10),
//        subsample(y, 10));

    LoessInterpolator interpolator = new LoessInterpolator();
    final PolynomialSplineFunction splineFunction = interpolator.interpolate(subsample(x, 50),
        subsample(y, 50));

    System.out.println("Corrected values:");
    for (int i = 0; i < x.length; i++) {
      System.out.println(y[i] - splineFunction.value(x[i]));
    }
  }

  public static double[] subsample(double[] array, int numSamples) {
    final int increment = array.length / numSamples;

    final double[] result = new double[numSamples + 1];
    for (int i = 0; i < numSamples; i++) {
      result[i] = array[i * increment];
    }

    result[numSamples] = array[array.length - 1];

    return result;
  }
}
