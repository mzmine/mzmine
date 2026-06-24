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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A SamplesBatch like a list of RawDataFiles all run as a single batch.
 */
public final class SamplesBatch {

  private final List<RawDataFile> raws;
  private final @Nullable Object groupMetadataValue;
  private double medianNormMetric = Double.NaN;

  SamplesBatch(List<RawDataFile> raws, @Nullable Object groupMetadataValue) {
    this.raws = raws;
    this.groupMetadataValue = groupMetadataValue;
  }

  public SamplesBatch(List<RawDataFile> raws) {
    this(raws, null);
  }

  public List<RawDataFile> getRaws() {
    return raws;
  }

  public @Nullable Object getGroupMetadataValue() {
    return groupMetadataValue;
  }

  @NotNull
  public String getGroupMetadataValueStr() {
    return groupMetadataValue == null ? "UNNAMED BATCH" : groupMetadataValue.toString();
  }

  /**
   * The median intensity of all reference samples in this batch. This is set by the sample intra
   * batch correction step.
   */
  public void setMedianReferenceNormMetric(double medianNormMetric) {
    this.medianNormMetric = medianNormMetric;
  }

  /**
   *
   * @return Double.NaN if invalid or unset. positive otherwise
   */
  public double getMedianReferenceNormMetric() {
    return medianNormMetric;
  }

  public int size() {
    return raws.size();
  }

}
