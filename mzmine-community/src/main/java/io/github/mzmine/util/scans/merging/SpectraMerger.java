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

package io.github.mzmine.util.scans.merging;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import org.jetbrains.annotations.NotNull;

public class SpectraMerger {

  private final @NotNull SampleHandling sampleHandling;
  private final @NotNull MZTolerance mzTol;
  private final @NotNull IntensityMergingType intensityMerging;
  private final @NotNull FragmentScanSelection.IncludeInputSpectra inputSpectra;

  /**
   * @param sampleHandling
   * @param mzTol
   * @param intensityMerging
   * @param inputSpectra     keep input spectra in the final list or only use representative
   *                         spectra
   */
  public SpectraMerger(final @NotNull SampleHandling sampleHandling,
      final @NotNull MZTolerance mzTol, final @NotNull IntensityMergingType intensityMerging,
      final @NotNull FragmentScanSelection.IncludeInputSpectra inputSpectra) {

    this.sampleHandling = sampleHandling;
    this.mzTol = mzTol;
    this.intensityMerging = intensityMerging;
    this.inputSpectra = inputSpectra;
  }

  @NotNull
  public SampleHandling getSampleHandling() {
    return sampleHandling;
  }

  @NotNull
  public MZTolerance getMzTol() {
    return mzTol;
  }

  @NotNull
  public IntensityMergingType getIntensityMerging() {
    return intensityMerging;
  }

  public boolean isMergeAcrossEnergies() {
    return true;
  }

  public @NotNull FragmentScanSelection.IncludeInputSpectra getInputSpectra() {
    return inputSpectra;
  }
}
