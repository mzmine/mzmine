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
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;

public class WavelengthSpectrum implements OtherSpectrum {

  private final OtherSpectralData spectralData;
  private final DoubleBuffer wavelengths;
  private final DoubleBuffer intensities;
  private final MassSpectrumType spectrumType;
  private final float rt;

  public WavelengthSpectrum(final OtherSpectralData spectralData, DoubleBuffer wavelengths,
      DoubleBuffer intensities, MassSpectrumType spectrumType, float rt) {
    this.spectralData = spectralData;
    this.wavelengths = wavelengths;
    this.intensities = intensities;
    this.spectrumType = spectrumType;
    this.rt = rt;
  }

  public WavelengthSpectrum(OtherSpectralData spectralData, MemoryMapStorage storage,
      double[] wavelengths, double[] intensities, MassSpectrumType spectrumType, float rt) {
    this.spectralData = spectralData;
    this.spectrumType = spectrumType;
    this.rt = rt;

    if (wavelengths.length != intensities.length) {
      throw new RuntimeException("Wavelength and intensity arrays must have the same length");
    }

    this.wavelengths = StorageUtils.storeValuesToDoubleBuffer(storage, wavelengths);
    this.intensities = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  @Override
  public double getDomainValue(int index) {
    return wavelengths.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return intensities.get(index);
  }

  @Override
  public int getNumberOfValues() {
    return intensities.limit();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  @Override
  public float getRetentionTime() {
    return rt;
  }

  @Override
  public OtherDataFile getOtherDataFile() {
    return getOtherSpectralData().getOtherDataFile();
  }

  @Override
  public OtherSpectralData getOtherSpectralData() {
    return spectralData;
  }
}
