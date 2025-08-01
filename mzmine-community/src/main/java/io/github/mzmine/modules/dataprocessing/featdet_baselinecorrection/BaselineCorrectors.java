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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.akimaspline.AkimaSplineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.chang.ChangBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.divideddifference.DividedDifferenceCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.loess.LoessBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.neville.NevilleBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.polynomial.PolynomialBaselineCorrection;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.spline.SplineBaselineCorrector;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum BaselineCorrectors implements ModuleOptionsEnum<BaselineCorrector> {
  LOESS, SPLINE, AKIMA, DIVIDED_DIFFERENCE, NEVILLE, CHANG, POLYNOMIAL;

  @Override
  public Class<? extends BaselineCorrector> getModuleClass() {
    return switch (this) {
      case LOESS -> LoessBaselineCorrector.class;
      case SPLINE -> SplineBaselineCorrector.class;
      case AKIMA -> AkimaSplineCorrector.class;
      case DIVIDED_DIFFERENCE -> DividedDifferenceCorrector.class;
      case NEVILLE -> NevilleBaselineCorrector.class;
      case CHANG -> ChangBaselineCorrector.class;
      case POLYNOMIAL -> PolynomialBaselineCorrection.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case LOESS -> "loess_baseline_corrector";
      case SPLINE -> "spline_corrector";
      case AKIMA -> "akima_corrector";
      case DIVIDED_DIFFERENCE -> "divided_difference_corrector";
      case NEVILLE -> "neville_corrector";
      case CHANG -> "chang_corrector";
      case POLYNOMIAL -> "polynomial_corrector";
    };
  }
}
