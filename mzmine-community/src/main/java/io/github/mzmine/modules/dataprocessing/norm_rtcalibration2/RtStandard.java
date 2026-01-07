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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RtStandard {

  private final HashMap<@NotNull RawDataFile, @Nullable FeatureListRow> standards; // must be a hash map. supports null values.
  private Float medianRt = null;
  private Float avgRt = null;

  public RtStandard(HashMap<RawDataFile, FeatureListRow> standards) {
    this.standards = standards;
  }

  public RtStandard(List<FeatureList> standards) {
    final HashMap<RawDataFile, FeatureListRow> map = new HashMap<>();
    standards.forEach(standard -> map.put(standard.getRawDataFile(0), null));
    this(map);
  }

  public boolean isValid() {
    return standards.values().stream().allMatch(Objects::nonNull);
  }

  public int getNumberOfMatches() {
    return (int) standards.values().stream().filter(Objects::nonNull).count();
  }

  private float getMedianRt() {
    if (medianRt == null) {
      medianRt = (float) MathUtils.calcQuantileSorted(
          standards.values().stream().filter(Objects::nonNull)
              .mapToDouble(FeatureListRow::getAverageRT).sorted().toArray(), 0.5);
    }
    return medianRt;
  }

  private float getAverageRt() {
    if (avgRt == null) {
      avgRt = (float) standards.values().stream().filter(Objects::nonNull)
          .mapToDouble(FeatureListRow::getAverageRT).average().getAsDouble();
    }
    return avgRt;
  }

  public float getRt(RTMeasure measure) {
    return switch (measure) {
      case MEDIAN -> getMedianRt();
      case AVERAGE -> getAverageRt();
    };
  }

  public HashMap<RawDataFile, FeatureListRow> standards() {
    return standards;
  }

  @Override
  public String toString() {
    return "RtStandard[" + "standards=" + standards + ']';
  }

}
