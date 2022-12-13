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
package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay;

public final class SGCoefficients {

  public static final double[][] SGCoefficientsFirstDerivative = {{0.0}, {0.0, 0.500},
      {0.0, 0.100, 0.200}, {0.0, 0.036, 0.071, 0.107}, {0.0, 0.017, 0.033, 0.050, 0.067},
      {0.0, 0.009, 0.018, 0.027, 0.036, 0.045}, {0.0, 0.005, 0.011, 0.016, 0.022, 0.027, 0.033},
      {0.0, 0.004, 0.007, 0.011, 0.014, 0.018, 0.021, 0.025},
      {0.0, 0.002, 0.005, 0.007, 0.010, 0.012, 0.015, 0.017, 0.020},
      {0.0, 0.002, 0.004, 0.005, 0.007, 0.009, 0.011, 0.012, 0.014, 0.016},
      {0.0, 0.001, 0.003, 0.004, 0.005, 0.006, 0.008, 0.009, 0.010, 0.012, 0.013},
      {0.0, 0.001, 0.002, 0.003, 0.004, 0.005, 0.006, 0.007, 0.008, 0.009, 0.010, 0.011},
      {0.0, 0.001, 0.002, 0.002, 0.003, 0.004, 0.005, 0.005, 0.006, 0.007, 0.008, 0.008, 0.009}};

  public static final double[][] SGCoefficientsFirstDerivativeQuartic = {{0.0}, {0.0, 0.667},
      {0.0, 0.667, -0.083}, {0.0, 0.230, 0.266, -0.087}, {0.0, 0.106, 0.162, 0.120, -0.072},
      {0.0, 0.057, 0.098, 0.103, 0.057, -0.058}, {0.0, 0.035, 0.062, 0.075, 0.066, 0.027, -0.047},
      {0.0, 0.022, 0.041, 0.053, 0.055, 0.042, 0.012, -0.039},
      {0.0, 0.015, 0.029, 0.039, 0.043, 0.040, 0.028, 0.004, -0.032},
      {0.0, 0.011, 0.021, 0.029, 0.034, 0.034, 0.029, 0.018, 0.000, -0.027},
      {0.0, 0.008, 0.016, 0.022, 0.026, 0.028, 0.027, 0.022, 0.012, -0.003, -0.023},
      {0.0, 0.006, 0.012, 0.017, 0.021, 0.023, 0.023, 0.021, 0.016, 0.008, -0.004, -0.020},
      {0.0, 0.006, 0.009, 0.013, 0.017, 0.019, 0.020, 0.019, 0.016, 0.012, 0.005, -0.005, -0.017}};

  public static final double[][] SGCoefficientsSecondDerivative =
      {{0.0}, {-1.0, 0.5}, {-0.143, -0.071, 0.143}, {-0.048, -0.036, 0.0, 0.060},
          {-0.022, -0.018, -0.009, 0.008, 0.030}, {-0.012, -0.010, -0.007, -0.001, 0.007, 0.017},
          {-0.007, -0.006, -0.005, -0.002, 0.001, 0.005, 0.011},
          {-0.005, -0.004, -0.004, -0.002, -0.001, 0.002, 0.004, 0.007},
          {-0.003, -0.003, -0.003, -0.002, -0.001, 0.000, 0.002, 0.003, 0.005},
          {-0.002, -0.002, -0.002, -0.002, -0.001, 0.000, 0.000, 0.001, 0.003, 0.004},
          {-0.002, -0.002, -0.001, -0.001, -0.001, -0.001, 0.000, 0.001, 0.001, 0.002, 0.003},
          {-0.001, -0.001, -0.001, -0.001, -0.001, -0.001, 0.000, 0.000, 0.001, 0.001, 0.002,
              0.002},
          {-0.001, -0.001, -0.001, -0.001, -0.001, -0.001, 0.000, 0.000, 0.000, 0.001, 0.001, 0.001,
              0.002}};

  public static final double[][] SGCoefficientsSecondDerivativeQuartic =
      {{0.0}, {-1.250, 0.567}, {-1.250, 0.567, -0.042}, {-0.265, -0.072, 0.254, -0.049},
          {-0.108, -0.061, -0.044, 0.108, -0.037}, {-0.055, -0.040, 0.000, 0.043, 0.051, -0.026},
          {-0.032, -0.026, -0.008, 0.014, 0.030, 0.025, -0.019},
          {-0.021, -0.018, -0.009, 0.003, 0.014, 0.020, 0.013, -0.014},
          {-0.014, -0.012, -0.008, -0.001, 0.006, 0.012, 0.013, 0.007, -0.011},
          {-0.010, -0.009, -0.006, -0.002, 0.002, 0.007, 0.009, 0.009, 0.004, -0.008},
          {-0.007, -0.007, -0.005, -0.003, 0.000, 0.003, 0.006, 0.007, 0.006, 0.002, -0.006},
          {-0.006, -0.005, -0.004, -0.003, -0.001, 0.002, 0.004, 0.005, 0.005, 0.004, 0.001,
              -0.005},
          {-0.004, -0.004, -0.003, -0.002, -0.001, 0.001, 0.002, 0.003, 0.004, 0.004, 0.003, 0.000,
              -0.004}};

}
