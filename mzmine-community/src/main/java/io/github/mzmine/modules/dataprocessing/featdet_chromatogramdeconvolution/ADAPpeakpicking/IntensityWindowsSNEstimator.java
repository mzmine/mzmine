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
/*
 * author Owen Myers (Oweenm@gmail.com)
 */
package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.parameters.ParameterSet;

public class IntensityWindowsSNEstimator implements SNEstimatorChoice {
  @Override
  public @NotNull String getName() {
    return "Intensity window SN";
  }

  public String getSNCode() {
    return "Intensity Window Estimator";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IntensityWindowsSNParameters.class;
  }

  @Override
  public boolean getRequiresR() {
    return false;
  }

  @Override
  public String[] getRequiredRPackages() {
    return null;
  }

  @Override
  public String[] getRequiredRPackagesVersions() {
    return null;
  }
}
