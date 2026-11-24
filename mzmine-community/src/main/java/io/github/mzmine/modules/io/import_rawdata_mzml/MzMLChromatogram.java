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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLBinaryDataInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLPeaksDecoder;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVGroup;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLProduct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detector trace and transition metadata accumulated inside one chromatogram.
 */
final class MzMLChromatogram extends MzMLCVGroup {

  private final String id;
  private final int length;
  private @Nullable MzMLPrecursorElement precursor;
  private @Nullable MzMLProduct product;
  private @Nullable String intensityUnit;
  private float[] retentionTimes;
  private double[] intensities;

  MzMLChromatogram(@NotNull final String id, final int length) {
    this.id = id;
    this.length = length;
  }

  @NotNull String getId() {
    return id;
  }

  int getNumberOfDataPoints() {
    return length;
  }

  @Nullable String getIntensityUnit() {
    return intensityUnit;
  }

  @Nullable MzMLPrecursorElement getPrecursor() {
    return precursor;
  }

  void setPrecursor(@NotNull final MzMLPrecursorElement precursor) {
    this.precursor = precursor;
  }

  @Nullable MzMLProduct getProduct() {
    return product;
  }

  void setProduct(@NotNull final MzMLProduct product) {
    this.product = product;
  }

  /**
   * @return whether both required arrays were decoded, including zero-length arrays
   */
  boolean hasBinaryArrays() {
    return retentionTimes != null && intensities != null;
  }

  /**
   * @param array binary payload and descriptor belonging to this chromatogram
   */
  void readBinary(@NotNull final MzMLBinaryDataInfo array) {
    switch (array.getArrayType()) {
      case TIME -> {
        retentionTimes = ConversionUtils.convertDoublesToFloats(decode(array));
        if (MzMLCV.cvUnitsSec.equals(array.getUnitAccession())) {
          for (int i = 0; i < length; i++) {
            retentionTimes[i] /= 60f;
          }
        }
      }
      case INTENSITY -> {
        intensityUnit = array.getUnitAccession();
        intensities = decode(array);
      }
      case MZ, WAVELENGTH, ION_MOBILITY -> {
      }
    }
  }

  private double @NotNull [] decode(@NotNull final MzMLBinaryDataInfo array) {
    final double[] values = MzMLPeaksDecoder.decodeToDouble(array);
    if (values.length != length) {
      throw new IllegalArgumentException("Expected " + length + " values in chromatogram " + id);
    }
    return values;
  }

  float @NotNull [] getRetentionTimes() {
    return retentionTimes;
  }

  double @NotNull [] getIntensities() {
    return intensities;
  }

  @NotNull PolarityType getPolarity() {
    if (getCVParamsList().stream()
        .anyMatch(cv -> cv.getAccession().equals(MzMLCV.cvPolarityPositive))) {
      return PolarityType.POSITIVE;
    }
    return getCVParamsList().stream()
        .anyMatch(cv -> cv.getAccession().equals(MzMLCV.cvPolarityNegative)) ? PolarityType.NEGATIVE
        : PolarityType.UNKNOWN;
  }
}
