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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;

/**
 * @param filter      defines how to apply the filter
 * @param rtTolerance only used with {@link FeatureLimitOptions#USE_TOLERANCE}
 */
public record RtLimitsFilter(FeatureLimitOptions filter, RTTolerance rtTolerance) implements
    ComboWithInputValue<FeatureLimitOptions, RTTolerance> {

  @Override
  public FeatureLimitOptions getSelectedOption() {
    return filter;
  }

  @Override
  public RTTolerance getEmbeddedValue() {
    return rtTolerance;
  }

  /**
   * Test either if testedRt is within the RT range of a feature or matches the RT center of a
   * feature +- rtTolerance
   *
   * @param feature  tested feature
   * @param testedRt tested retention time
   * @return true if tested rt is matching the feature's retention time
   */
  public boolean accept(final ModularFeature feature, final float testedRt) {
    return switch (filter) {
      case USE_FEATURE_EDGES -> {
        // don't use shortcut as this returns a non-null singleton range.
        // Range<Float> rtRange = feature.getRawDataPointsRTRange();
        // true if no range means that there was no retention time like in IMS-MS data without time component
        Range<Float> rtRange = feature.get(RTRangeType.class);
        yield rtRange == null || rtRange.contains(testedRt);
      }
      case USE_TOLERANCE -> {
        Float rt = feature.getRT();
        yield rt == null || rtTolerance.checkWithinTolerance(rt, testedRt);
      }
    };
  }
}
