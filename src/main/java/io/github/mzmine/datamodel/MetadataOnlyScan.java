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

package io.github.mzmine.datamodel;

import java.util.Iterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Only used to build scans and contain metadata, e.g., during loading of scans
 */
public abstract class MetadataOnlyScan implements Scan {


  @Override
  public int getNumberOfDataPoints() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public @Nullable Double getTIC() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public double[] getMzValues(@NotNull final double[] dst) {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public double[] getIntensityValues(@NotNull final double[] dst) {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public double getMzValue(final int index) {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public double getIntensityValue(final int index) {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public @Nullable Double getBasePeakMz() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public @Nullable Double getBasePeakIntensity() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public @Nullable MassList getMassList() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public void addMassList(@NotNull final MassList massList) {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @Override
  public boolean isEmptyScan() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "This scan contains no data, only metadata and is only used to build a scan while reading data.");
  }
}
