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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTMeasure;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

class InternalRtStandard extends RtStandard {

  @NotNull
  private final CompoundDBAnnotation internalStd;

  InternalRtStandard(HashMap<RawDataFile, FeatureListRow> standards, CompoundDBAnnotation internalStd) {
    super(standards);
    this.internalStd = internalStd;
    float internalStdRt = internalStd.getRT();
    this.avgRt = internalStdRt;
    this.medianRt = internalStdRt;
  }

  @Override
  public String toString() {
    return "InternalRtStandard{" + "avgRt=" + avgRt + ", medianRt=" + medianRt + ", standards="
        + standards + '}';
  }

  @Override
  public CompoundDBAnnotation toAnnotation(RTMeasure measure, int standardIndex) {
    var a = super.toAnnotation(measure, standardIndex);
    a.putIfNotNull(RTType.class, internalStd.getRT());
    a.putIfNotNull(PrecursorMZType.class, internalStd.getPrecursorMZ());
    a.putIfNotNull(CompoundNameType.class, internalStd.getCompoundName());
    return a;
  }
}
