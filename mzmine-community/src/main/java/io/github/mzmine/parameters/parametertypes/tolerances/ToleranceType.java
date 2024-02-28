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

package io.github.mzmine.parameters.parametertypes.tolerances;

/**
 * Defines different types of tolerances, e.g., for {@link MZTolerance}. We use intra-sample and
 * sample-to-sample to have a greater difference than when using inter and intra sampple
 */
public enum ToleranceType {
  SCAN_TO_SCAN("scan-to-scan"), INTRA_SAMPLE("intra-sample"), SAMPLE_TO_SAMPLE(
      "sample-to-sample"), FEATURE_TO_SCAN("feature-to-scan"), EXPERIMENTAL_TO_CALCULATED(
      "experimental-to-calculated");


  private final String title;

  ToleranceType(final String title) {

    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }

  public String getDescription() {
    return switch (this) {
      case INTRA_SAMPLE -> "Intra sample tolerances can be smaller due to higher accuracy.";
      case SAMPLE_TO_SAMPLE ->
          "Sample-to-sample (inter-sample) tolerances may be wider if drifts or other issues occur.";
      case SCAN_TO_SCAN ->
          "Scan-to-scan tolerances may be wider considering signals rather than averaged values.";
      case FEATURE_TO_SCAN ->
          "Feature-to-scan tolerances compare the average value of a feature to scans.";
      case EXPERIMENTAL_TO_CALCULATED -> "Comparing the experimental value to the theoretical.";
    };
  }
}
