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

package io.github.mzmine.datamodel.impl.builders;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Only used to build scans and contain metadata, e.g., during loading of scans
 */
public class SimpleBuildingScan extends MetadataOnlyScan {

  public String scanId;
  public int scanNumber;
  public int msLevel = 1;
  public PolarityType polarity = PolarityType.UNKNOWN;
  public MassSpectrumType spectrumType;
  public Float retentionTime;

  public double precursorMz = 0d;
  public int precursorCharge = 0;


  public SimpleBuildingScan(final int scanNumber, final int msLevel, final PolarityType polarity,
      final MassSpectrumType spectrumType, final Float retentionTime, final double precursorMz,
      final int precursorCharge) {
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.polarity = polarity;
    this.spectrumType = spectrumType;
    this.retentionTime = retentionTime;
    this.precursorMz = precursorMz;
    this.precursorCharge = precursorCharge;
  }

  public SimpleBuildingScan() {
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  @Override
  public @Nullable Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException("This method is not supported");
  }

  @Override
  public @NotNull RawDataFile getDataFile() {
    throw new UnsupportedOperationException("This method is not supported");
  }

  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  @Override
  public @NotNull String getScanDefinition() {
    return "";
  }

  @Override
  public int getMSLevel() {
    return msLevel;
  }

  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return null;
  }

  @Override
  public @NotNull Range<Double> getScanningMZRange() {
    throw new UnsupportedOperationException("This method is not supported");
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    if (msLevel != 1 && precursorMz > 0) {
      return new DDAMsMsInfoImpl(precursorMz, precursorCharge, null, null, null, msLevel,
          ActivationMethod.UNKNOWN, null);
    }
    return null;
  }

  @Override
  public @NotNull PolarityType getPolarity() {
    return polarity;
  }
}
