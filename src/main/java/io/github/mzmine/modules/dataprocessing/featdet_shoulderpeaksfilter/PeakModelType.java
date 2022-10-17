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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.peakmodels.ExtendedLorentzianPeak;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.peakmodels.GaussPeak;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.peakmodels.LorentzianPeak;

public enum PeakModelType {

  GAUSS("Gaussian", GaussPeak.class), //
  LORENTZ("Lorentzian", LorentzianPeak.class), //
  LORENTZEXTENDED("Lorentzian extended", ExtendedLorentzianPeak.class);

  private final String modelName;
  private final Class<?> modelClass;

  PeakModelType(String modelName, Class<?> modelClass) {
    this.modelName = modelName;
    this.modelClass = modelClass;
  }

  public String toString() {
    return modelName;
  }

  public Class<?> getModelClass() {
    return modelClass;
  }

}
