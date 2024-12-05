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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SampleHandling;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SpectraMergingParameters extends SimpleParameterSet {

  public static final ComboParameter<SampleHandling> mergeAcrossSamples = new ComboParameter<>(
      "Merge spectra", """
      Either merge within each sample (intra sample) or merge across all samples.""",
      SampleHandling.values(), SampleHandling.ACROSS_SAMPLES);
  public static final ComboParameter<IntensityMergingType> intensityMergeType = new ComboParameter<>(
      "Intensity merge mode", """
                                  Defines the way intensity values are merged:
                                  """ + Arrays.stream(IntensityMergingType.values())
                                  .map(IntensityMergingType::getDescription)
                                  .collect(Collectors.joining("\n")), IntensityMergingType.values(),
      IntensityMergingType.MAXIMUM);

  public static final MZToleranceParameter mergeMzTolerance = new MZToleranceParameter(
      "m/z tolerance", "The tolerance used to group signals during merging of spectra", 0.008, 25);

//  public static final PercentParameter signalCountFilter = new PercentParameter(
//      "Signal detections (%)",
//      "A signal is removed from the merged spectrum if it was detected in <X% of the total source scans.",
//      0.2, 0d, 1d);

  public SpectraMergingParameters() {
    super(mergeMzTolerance, mergeAcrossSamples, intensityMergeType);
  }

  public void setAll(final SampleHandling sampleHandling, final MZTolerance mzTol,
      final IntensityMergingType intensityMerging) {
    setParameter(mergeMzTolerance, mzTol);
    setParameter(mergeAcrossSamples, sampleHandling);
    setParameter(intensityMergeType, intensityMerging);
  }


  public SpectraMerger create() {
    SampleHandling sampleHandling = getValue(mergeAcrossSamples);
    MZTolerance mzTol = getValue(mergeMzTolerance);
    var intensityMerging = getValue(intensityMergeType);
    return new SpectraMerger(sampleHandling, mzTol, intensityMerging);
  }
}
