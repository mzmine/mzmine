/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import io.github.mzmine.datamodel.PolarityType;

public enum WizardMsPolarity {
  Positive, Negative, No_filter//, Polarity_switching; // add later
  ;

  public static WizardMsPolarity valueOf(final PolarityType polarity) {
    return switch (polarity){
      case NEGATIVE -> Negative;
      case POSITIVE-> Positive;
      case ANY, NEUTRAL, UNKNOWN-> No_filter;
      case null -> No_filter;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case Negative, Positive -> name();
      case No_filter -> "No filter";
    };
  }

  public PolarityType toScanPolaritySelection() {
    return switch (this) {
      case Negative -> PolarityType.NEGATIVE;
      case Positive -> PolarityType.POSITIVE;
      case No_filter -> PolarityType.ANY;
    };
  }
}
