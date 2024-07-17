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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;

/**
 * Basic interface of spectral data. Does not have to be mass spectral data.
 */
public interface OtherSpectrum {

  double getDomainValue(int index);

  double getRangeValue(int index);

  int getNumberOfValues();

  MassSpectrumType getSpectrumType();

  float getRetentionTime();

  default String getDomainUnit() {
    return getOtherSpectralData().getSpectraDomainUnit();
  }

  default String getRangeUnit() {
    return getOtherSpectralData().getSpectraRangeUnit();
  }

  default String getRangeLabel() {
    return getOtherSpectralData().getSpectraRangeLabel();
  }

  default String getDomainLabel() {
    return getOtherSpectralData().getSpectraDomainLabel();
  }

  default RawDataFile getMsRawDataFile() {
    return getOtherDataFile().getCorrespondingRawDataFile();
  }

  default OtherDataFile getOtherDataFile() {
    return getOtherSpectralData().getOtherDataFile();
  }

  OtherSpectralData getOtherSpectralData();
}
