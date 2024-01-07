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

package io.github.mzmine.util.spectraldb.entry;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps an {@link SpectralLibraryEntry} to form a Scan
 */
public class LibraryEntryWrappedScan implements Scan {

  @NotNull
  private final SpectralLibraryDataFile dataFile;
  @NotNull
  private final SpectralLibraryEntry entry;
  private final int scanNumber;
  private final @NotNull String definition;
  private final DDAMsMsInfoImpl msMsInfo;
  private MassList massList;

  public LibraryEntryWrappedScan(@NotNull SpectralLibraryDataFile dataFile,
      @NotNull SpectralLibraryEntry entry, int scanNumber) {
    this.dataFile = dataFile;
    this.entry = entry;
    this.scanNumber = scanNumber;

    int msLevel = getMSLevel();
    var mz = entry.getPrecursorMz();

    if (msLevel > 1 && mz != null) {
      var charge = (Integer) entry.getOrElse(DBEntryField.CHARGE, null);
      msMsInfo = new DDAMsMsInfoImpl(mz, charge, msLevel);
    } else {
      msMsInfo = null;
    }

    massList = new ScanPointerMassList(this);
    // last
    definition = ScanUtils.scanToString(this);
  }

  @Override
  public int getNumberOfDataPoints() {
    return entry.getNumberOfDataPoints();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return entry.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull final double[] dst) {
    return entry.getMzValues(dst);
  }

  @Override
  public double[] getIntensityValues(@NotNull final double[] dst) {
    return entry.getIntensityValues(dst);
  }

  @Override
  public double getMzValue(final int index) {
    return entry.getMzValue(index);
  }

  @Override
  public double getIntensityValue(final int index) {
    return entry.getIntensityValue(index);
  }

  @Override
  public @Nullable Double getBasePeakMz() {
    return entry.getBasePeakMz();
  }

  @Override
  public @Nullable Double getBasePeakIntensity() {
    return entry.getBasePeakIntensity();
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    return entry.getBasePeakIndex();
  }

  @Override
  public @Nullable Range<Double> getDataPointMZRange() {
    return entry.getDataPointMZRange();
  }

  @Override
  public @Nullable Double getTIC() {
    return entry.getTIC();
  }

  @Override
  public @NotNull RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  @Override
  public @NotNull String getScanDefinition() {
    return entry.toString();
  }

  @Override
  public int getMSLevel() {
    return entry.getMsLevel().orElse(0);
  }

  @Override
  public float getRetentionTime() {
    return entry.getAsFloat(DBEntryField.RT).orElse(-1f);
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return null;
  }

  @Override
  public @NotNull Range<Double> getScanningMZRange() {
    return requireNonNullElse(getDataPointMZRange(), Range.singleton(0d));
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    return msMsInfo;
  }

  @Override
  public @NotNull PolarityType getPolarity() {
    return entry.getPolarity();
  }

  @Override
  public @Nullable MassList getMassList() {
    return massList;
  }

  @Override
  public void addMassList(@NotNull final MassList massList) {
    var tmp = this.massList;
    this.massList = massList;
    dataFile.applyMassListChanged(this, tmp, massList);
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return entry.iterator();
  }

  public @NotNull SpectralLibraryEntry getEntry() {
    return entry;
  }
}
