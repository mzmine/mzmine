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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.RangeUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OverlappingCompoundAnnotation {

  final List<CompoundDBAnnotation> annotations = new ArrayList<>();
  private final Range<Double> mzRange;
  private final Range<Float> rtRange;
  private final Range<Float> mobRange;

  public OverlappingCompoundAnnotation(final CompoundDBAnnotation annotation,
      @NotNull final MZTolerance mzTolerance, @Nullable final RTTolerance rtTolerance,
      @Nullable final MobilityTolerance mobilityTolerance) {
    this(annotation, mzTolerance, rtTolerance, mobilityTolerance, 0.8, 0.8f, 0.8f);
  }

  /**
   * @param mzRangeOverlapThreshold Threshold of how far the exact mass of a potential isobaric
   *                                overlap may fall into the mzRange of this annotation.
   */
  public OverlappingCompoundAnnotation(final CompoundDBAnnotation annotation,
      @NotNull final MZTolerance mzTolerance, @Nullable final RTTolerance rtTolerance,
      @Nullable final MobilityTolerance mobilityTolerance, final double mzRangeOverlapThreshold,
      @Nullable final Float rtRangeOverlapThreshold,
      @Nullable final Float mobilityOverlapThreshold) {

    final Double precursorMZ = annotation.getPrecursorMZ();
    this.mzRange = RangeUtils.rangeAround(precursorMZ,
        RangeUtils.rangeLength(mzTolerance.getToleranceRange(precursorMZ))
            * mzRangeOverlapThreshold);

    final Float rt = annotation.getRT();
    if (rt != null && rtTolerance != null && rtRangeOverlapThreshold != null) {
      this.rtRange = RangeUtils.rangeAround(rt,
          RangeUtils.rangeLength(rtTolerance.getToleranceRange(rt)) * rtRangeOverlapThreshold);
    } else {
      rtRange = TargetedFeatureDetectionModuleTask.floatInfiniteRange;
    }

    final Float mobility = annotation.get(MobilityType.class);
    if (mobility != null && mobilityTolerance != null && mobilityOverlapThreshold != null) {
      mobRange = RangeUtils.rangeAround(mobility,
          RangeUtils.rangeLength(mobilityTolerance.getToleranceRange(mobility))
              * mobilityOverlapThreshold);
    } else {
      mobRange = TargetedFeatureDetectionModuleTask.floatInfiniteRange;
    }

    annotations.add(annotation);
  }

  public boolean offerAnnotation(final CompoundDBAnnotation annotation) {
    final Double precursorMZ = annotation.getPrecursorMZ();
    if (precursorMZ == null || !(mzRange.contains(precursorMZ))) {
      return false;
    }

    // if there is no rt given, we assume a match
    final Float rt = annotation.getRT();
    if (rt != null && !rtRange.contains(rt)) {
      return false;
    } else if (rt != null && rtRange.contains(rt)) {

      // if there is no mobility given, we assume a match
      final Float mobility = annotation.get(MobilityType.class);
      if (mobility != null && !mobRange.contains(mobility)) {
        return false;
      }
    }

    annotations.add(annotation);
    // ensure sorted state
    annotations.sort(Comparator.comparingDouble(CompoundDBAnnotation::getPrecursorMZ));
    return true;
  }

  public List<CompoundDBAnnotation> getAnnotations() {
    return annotations;
  }

  public Range<Double> evaluateMergedToleranceRange(@NotNull MZTolerance mzTolerance) {
    final Double lower = annotations.get(0).getPrecursorMZ();
    final double upper = annotations.get(annotations.size() - 1).getPrecursorMZ();
    return mzTolerance.getToleranceRange(Range.closed(lower, upper));
  }

  public Range<Float> evaluateMergedRtToleranceRange(@Nullable final RTTolerance rtTol) {
    if (rtTol == null) {
      return TargetedFeatureDetectionModuleTask.floatInfiniteRange;
    }
    Range<Float> rtRange = null;
    // not all annotations might have a rt range
    for (CompoundDBAnnotation annotation : annotations) {
      final Float rt = annotation.getRT();
      if (rt != null) {
        if (rtRange == null) {
          rtRange = rtTol.getToleranceRange(rt);
        } else {
          rtRange = rtRange.span(rtTol.getToleranceRange(rt));
        }
      } else {
        // as soon as we have one target with no rt, we assume a overlap for all of them
        return TargetedFeatureDetectionModuleTask.floatInfiniteRange;
      }
    }
    return rtRange;
  }

  public Range<Float> evaluateMergedMobilityToleranceRange(
      @Nullable final MobilityTolerance mobTol) {

    if (mobTol == null) {
      return TargetedFeatureDetectionModuleTask.floatInfiniteRange;
    }
    Range<Float> mobRange = null;
    // not all annotations might have a mobility range
    for (CompoundDBAnnotation annotation : annotations) {
      final Float mobility = annotation.get(MobilityType.class);
      if (mobility != null) {
        if (mobRange == null) {
          mobRange = mobTol.getToleranceRange(mobility);
        } else {
          mobRange = mobRange.span(mobTol.getToleranceRange(mobility));
        }
      } else {
        // as soon as we have one target with no mobility, we assume a overlap for all of them
        return TargetedFeatureDetectionModuleTask.floatInfiniteRange;
      }
    }
    return mobRange;
  }
}
