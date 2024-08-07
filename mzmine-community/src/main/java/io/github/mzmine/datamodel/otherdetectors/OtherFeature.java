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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import org.jetbrains.annotations.Nullable;

public interface OtherFeature extends ModularDataModel {

  @Nullable
  default OtherTimeSeries getFeatureData() {
    return get(OtherFeatureDataType.class);
  }

  /**
   * @return Creates a new feature with the same properties, except the actual time series data. If
   * this feature is the "raw" trace, this is used as RawTraceType. If this feature is already
   * processed, the original RawTraceType is used.
   */
  default OtherFeature createSubFeature() {
    final OtherFeatureImpl newFeature = new OtherFeatureImpl();

    //copy everything except the OtherFeatureDataType and the RawTraceType
    getMap().entrySet().stream().filter(
            e -> !(e.getValue() instanceof OtherTimeSeries || e.getValue() instanceof OtherFeature))
        .forEach(e -> newFeature.set(e.getKey(), e.getValue()));

    // either this is already a sub feature, or this is the original one
    final OtherFeature raw = get(RawTraceType.class) != null ? get(RawTraceType.class) : this;
    if (raw != null) {
      newFeature.set(RawTraceType.class, raw);
    }
    return newFeature;
  }
}
