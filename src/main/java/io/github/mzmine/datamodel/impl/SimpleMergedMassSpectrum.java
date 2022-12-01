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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a spectrum based on multiple individual mass spectra. Compatible with the {@link Scan}
 * interface. {@link SimpleMergedMassSpectrum#getScanNumber()} will return -1 to represent the
 * artificial state of this spectrum.
 */
public class SimpleMergedMassSpectrum extends AbstractStorableSpectrum implements
    MergedMassSpectrum {

  private static final Logger logger = Logger.getLogger(SimpleMergedMsMsSpectrum.class.getName());

  protected final List<MassSpectrum> sourceSpectra;
  protected final IntensityMergingType intensityMergingType;
  protected final CenterFunction centerFunction;
  protected final RawDataFile rawDataFile;
  protected final float retentionTime;
  protected final int msLevel;
  protected final Range<Double> scanningMzRange;
  protected final PolarityType polarity;
  protected String scanDefinition; // cannot be final due to subclasses
  protected MassList massList = null;
  private final MergingType mergingType;

  /**
   * Construncts a new SimpleMergedMassSpectrum. A {@link ScanPointerMassList} will be created by
   * default.
   *
   * @param storage              The storage to use. may be null.
   * @param mzValues             The merged mz values
   * @param intensityValues      The merged intensities
   * @param msLevel              The ms level
   * @param sourceSpectra        The source spectra used to create this spectrum
   * @param intensityMergingType The merging type this spectrum was created with.
   * @param centerFunction       The center function this spectrum was created with.
   * @param mergingType
   */
  public SimpleMergedMassSpectrum(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, int msLevel,
      @NotNull List<? extends MassSpectrum> sourceSpectra,
      @NotNull SpectraMerging.IntensityMergingType intensityMergingType,
      @NotNull CenterFunction centerFunction, final MergingType mergingType) {
    super(storage, mzValues, intensityValues);
    assert !sourceSpectra.isEmpty();

    this.mergingType = mergingType;
    RawDataFile file = null;
    PolarityType tempPolarity = null;
    Range<Double> tempScanningMzRange = null;
    float tempRt = 0f;
    int numScans = 0;
    for (MassSpectrum spectrum : sourceSpectra) {
      if (spectrum instanceof Scan scan) {
        if (file == null) { // set only once
          file = scan.getDataFile();
          tempPolarity = scan.getPolarity();
          tempScanningMzRange = scan.getScanningMZRange();
        }
        if (file != scan.getDataFile()) {
          logger.warning("Merging spectra with different raw data files");
        }
        numScans++;
        tempRt += scan.getRetentionTime();
      }
    }
    rawDataFile = file;

    retentionTime = tempRt / numScans;
    this.polarity = tempPolarity;
    this.scanningMzRange = tempScanningMzRange;
    this.sourceSpectra = (List<MassSpectrum>) sourceSpectra;
    this.intensityMergingType = intensityMergingType;
    this.centerFunction = centerFunction;
    this.msLevel = msLevel;
    this.scanDefinition = ScanUtils.scanToString(this, true);
    addMassList(new ScanPointerMassList(this));
  }

  @Override
  public List<MassSpectrum> getSourceSpectra() {
    return Collections.unmodifiableList(sourceSpectra);
  }

  @Override
  public IntensityMergingType getIntensityMergingType() {
    return intensityMergingType;
  }

  @Override
  public CenterFunction getCenterFunction() {
    return centerFunction;
  }

  @Override
  public MergingType getMergingType() {
    return mergingType;
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    return rawDataFile;
  }

  @NotNull
  @Override
  public String getScanDefinition() {
    return scanDefinition;
  }

  @Override
  public int getMSLevel() {
    return msLevel;
  }

  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  @NotNull
  @Override
  public Range<Double> getScanningMZRange() {
    return scanningMzRange;
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    return null;
  }

  @NotNull
  @Override
  public PolarityType getPolarity() {
    return polarity;
  }

  @Nullable
  @Override
  public MassList getMassList() {
    return massList;
  }

  @Override
  public synchronized void addMassList(final @NotNull MassList massList) {
    // we are not going into any details if this.massList equals massList
    // do not call listeners if the same object is passed multiple times
    if (this.massList == massList) {
      return;
    }
    MassList old = this.massList;
    this.massList = massList;

    if (rawDataFile != null) {
      rawDataFile.applyMassListChanged(this, old, massList);
    }
  }

  /**
   * @return null, because this spectrum consists of multiple scans.
   */
  @Override
  public @Nullable Float getInjectionTime() {
    return null;
  }
}
