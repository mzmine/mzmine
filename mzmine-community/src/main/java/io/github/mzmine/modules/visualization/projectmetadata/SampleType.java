/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public enum SampleType {
  BLANK, SAMPLE, QC, CALIBRATION;

  /**
   * checks for naming patterns in the sample name. default return is {@link #SAMPLE}.
   */
  public static SampleType ofString(String sampleType) {
    final String name = sampleType.toLowerCase();
    final Pattern qcPattern = Pattern.compile(".*(?<![a-z])*([-_0-9]*qc[-_0-9]*)(?![a-z])*.*");
    final Pattern blankPattern = Pattern.compile(
        ".*(?<![a-z])*([-_0-9]*blank|media|blk[-_0-9]*)(?![a-z])*.*");
    final Pattern calPattern = Pattern.compile(
        ".*(?<![a-z])*([-_0-9]*cal|quant[-_0-9]*)(?![a-z])*.*");

    if (qcPattern.matcher(name).matches()) {
      return QC;
    }
    if (blankPattern.matcher(name).matches()) {
      return BLANK;
    }
    if (calPattern.matcher(name).matches()) {
      return CALIBRATION;
    }
    return SAMPLE;
  }

  /**
   * checks for naming patterns in the sample name. default return is {@link #SAMPLE}.
   */
  @NotNull
  public static SampleType ofFile(@NotNull final RawDataFile file) {
    return ofString(file.getName());
  }

  @Override
  public String toString() {
    return switch (this) {
      case BLANK -> "blank";
      case SAMPLE -> "sample";
      case QC -> "qc";
      case CALIBRATION -> "calibration";
    };
  }
}
