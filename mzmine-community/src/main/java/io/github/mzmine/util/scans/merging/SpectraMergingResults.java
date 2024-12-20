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

import io.github.mzmine.datamodel.Scan;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Merging results for one MS2 precursor or one MSn tree
 *
 * @param bySample      one MS2 node per sample or one MSn node per sample and MSn tree node
 * @param acrossSamples one node for an MS2 precursor or one for every MSn tree node - if merging
 *                      across samples was off, or only one sample, then bySample is also used as
 *                      across samples to facilitate downstream selection of scans
 * @param msnPseudoMs2  a spectrum merged all MSn to a pseudo MS2 - only for MSn data
 */
public record SpectraMergingResults(@NotNull List<SpectraMergingResultsNode> bySample,
                                    @NotNull List<SpectraMergingResultsNode> acrossSamples,
                                    @Nullable Scan msnPseudoMs2) {

  /**
   * Merging results for one MS2 precursor or one MSn tree
   *
   * @param bySample      one MS2 node per sample or one MSn node per sample and MSn tree node
   * @param acrossSamples merged across samples results. If null - across samples was off, or only
   *                      one sample, then bySample is also used as across samples to facilitate
   *                      downstream selection of scans used instead also as across samples. Final
   *                      acrossSamples of this object is non-null
   * @param msnPseudoMs2  a spectrum merged all MSn to a pseudo MS2 - only for MSn data
   */
  public SpectraMergingResults(@NotNull final List<SpectraMergingResultsNode> bySample,
      @Nullable List<SpectraMergingResultsNode> acrossSamples, @Nullable final Scan msnPseudoMs2) {
    this.bySample = bySample;
    this.msnPseudoMs2 = msnPseudoMs2;

    // replace empty across samples with by sample to have representative scans during selection
    if (acrossSamples == null || acrossSamples.isEmpty()) {
      acrossSamples = bySample;
    }
    this.acrossSamples = acrossSamples;
  }

  public SpectraMergingResults(final List<SpectraMergingResultsNode> bySample) {
    this(bySample, null, null);
  }

  /**
   * Constructor for MS2 data
   */
  public SpectraMergingResults(final List<SpectraMergingResultsNode> bySample,
      final @Nullable SpectraMergingResultsNode acrossSamples) {
    this(bySample, acrossSamples == null ? null : List.of(acrossSamples), null);
  }


  /**
   * Map a single node
   *
   * @return the map contains one scan, allEnergiesScan is null
   */
  @NotNull
  public static SpectraMergingResults ofSingleNode(@NotNull SpectraMergingResultsNode node) {
    return new SpectraMergingResults(List.of(node));
  }

  /**
   * Map a single scan to its collision energy group
   *
   * @return the map contains one scan, allEnergiesScan is null
   */
  @NotNull
  public static SpectraMergingResults ofSingleScan(@NotNull Scan scan) {
    return new SpectraMergingResults(List.of(SpectraMergingResultsNode.ofSingleScan(scan)));
  }

  /**
   * Map a single scan to its collision energy group
   *
   * @return the map contains one scan, allEnergiesScan is null
   */
  @NotNull
  public static SpectraMergingResults ofEmpty() {
    return new SpectraMergingResults(List.of());
  }
}
