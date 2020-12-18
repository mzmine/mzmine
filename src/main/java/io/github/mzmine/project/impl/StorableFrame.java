/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.project.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityMassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class StorableFrame extends StorableScan implements Frame {

  private static Logger logger = Logger.getLogger(Frame.class.getName());

  /**
   * key = scan num, value = mobility mass spectrum // TODO do we need this?
   */
  private final Map<Integer, MobilityMassSpectrum> mobilityMassSpectra;
//  private final Map<Integer, Double> mobilities;
  private final List<StorableMobilogram> mobilograms;
  /**
   * Mobility range of this frame. Updated when a scan is added.
   */
  private Range<Double> mobilityRange;


  /**
   * Creates a storable frame and also stores the mobility resolved scans.
   *
   * @param originalFrame
   * @param rawDataFile
   * @param numberOfDataPoints
   * @param storageID
   */
  public StorableFrame(Frame originalFrame,
      RawDataFileImpl rawDataFile, int numberOfDataPoints, int storageID) throws IOException {
    super(originalFrame, rawDataFile, numberOfDataPoints, storageID);

//    mobilities = new HashMap<>(originalFrame.getNumberOfMobilityScans());
    mobilityMassSpectra = new HashMap<>(originalFrame.getNumberOfMobilityScans());
    mobilograms = new ArrayList<>();
    mobilityScans = new TreeMap<>();
    mobilityRange = null;

//    for(Integer num : originalFrame.getMobilityScanNumbers()) {
//      mobilities.put(num, originalFrame.getMobilityForSubSpectrum(num));
//    }

    // TODO subspectra
    /*for (int spectrumNum : originalFrame.getMobilityScanNumbers()) {
      MobilityMassSpectrum spectrum = originalFrame.getMobilityScan(spectrumNum);
      if (spectrum != null) {
        addMobilityScan(spectrum);
      }
    }*/

  }

  /*public StorableFrame(RawDataFileImpl rawDataFile, int storageID, int numberOfDataPoints,
      int scanNumber, int msLevel, float retentionTime, double precursorMZ,
      int precursorCharge,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, @Nonnull MobilityType mobilityType,
      @Nonnull Range<Double> mobilityRange, @Nonnull List<Integer> mobilityScanNumbers) {

    super(rawDataFile, storageID, numberOfDataPoints, scanNumber, msLevel, retentionTime,
        precursorMZ, precursorCharge, spectrumType, polarity, scanDefinition,
        scanMZRange);

    this.frameId = frameId;
    this.mobilityRange = mobilityRange;
    this.mobilityType = mobilityType;

    mobilograms = new ArrayList<>();

    mobilityScans = new TreeMap<>();
    for (int scannum : mobilityScanNumbers) {
      Scan scan = rawDataFile.getScan(scannum);
      if (scan != null) {
        addMobilityScan(scan);
      }
    }
  }*/

  @Override
  public int getNumberOfMobilityScans() {
    return mobilityMassSpectra.size();
  }

  @Override
  public Set<Integer> getMobilityScanNumbers() {
//    return mobilityMassSpectra.keySet();
    return ((IMSRawDataFileImpl) rawDataFile).getMobilitiesForFrame(getScanNumber()).keySet();
  }

  @Nonnull
  @Override
  public Range<Double> getMobilityRange() {
    if (mobilityRange != null) {
      return mobilityRange;
    }
    return Range.singleton(0.0);
  }

  @Nonnull
  @Override
  public MobilityMassSpectrum getMobilityScan(int num) {
    return Objects.requireNonNull(mobilityMassSpectra.get(num));
  }

  @Nonnull
  @Override
  public List<MobilityMassSpectrum> getMobilityScans() {
    return new ArrayList<>(mobilityMassSpectra.values());
  }

  /**
   * Not to be used during processing. Can only be called during raw data file reading before
   * finishWriting() was called.
   *
   * @param originalMobilityMassSpectrum The spectrum to store.
   */
  public final void addMobilityScan(MobilityMassSpectrum originalMobilityMassSpectrum) {
    try {
      final int storageId =
          rawDataFile.storeDataPoints(originalMobilityMassSpectrum.getDataPoints());

      if (mobilityRange == null) {
        mobilityRange = Range.singleton(originalMobilityMassSpectrum.getMobility());
      } else if (!mobilityRange.contains(originalMobilityMassSpectrum.getMobility())) {
        mobilityRange = mobilityRange
            .span(Range.singleton(originalMobilityMassSpectrum.getMobility()));
      }

      StorableMobilityMassSpectrum storableSpectrum =
          new StorableMobilityMassSpectrum(originalMobilityMassSpectrum, storageId);
      mobilityMassSpectra
          .put(originalMobilityMassSpectrum.getSpectrumNumber(), storableSpectrum);

    } catch (IOException e) {
      e.printStackTrace();
      logger.warning(() -> "Mobility spectrum " + originalMobilityMassSpectrum.getSpectrumNumber() +
          " for frame " + getFrameId() + " not stored.");
    }
  }

  @Override
  public double getMobilityForSubSpectrum(int subSpectrumIndex) {
    return ((IMSRawDataFileImpl) rawDataFile)
        .getMobilityForMobilitySpectrum(getScanNumber(), subSpectrumIndex);
  }

  @Override
  public Map<Integer, Double> getMobilities() {
    return ((IMSRawDataFileImpl) rawDataFile).getMobilitiesForFrame(getScanNumber());
  }

  @Override
  public ImmutableList<Mobilogram> getMobilograms() {
    return ImmutableList.copyOf(mobilograms);
  }

  /**
   * @param mobilogram
   * @return the storage id, -1 on error
   */
  @Override
  public int addMobilogram(Mobilogram mobilogram) {

    if (mobilogram instanceof StorableMobilogram && !mobilogram.getRawDataFile()
        .equals(rawDataFile)) {

      logger.warning(() -> "Cannot add mobilogram of " + mobilogram.getRawDataFile().getName() +
          " to Frame of " + rawDataFile.getName());
      return -1;

    } else if (mobilogram instanceof StorableMobilogram && mobilogram.getRawDataFile()
        .equals(rawDataFile)) {

      logger.fine(() -> "Mobilogram already stored in this raw data file.");
      if (!mobilograms.contains(mobilogram)) {
        mobilograms.add((StorableMobilogram) mobilogram);
      }
      return ((StorableMobilogram) mobilogram).getStorageID();

    } else {

      try {
        final int storageId = ((IMSRawDataFileImpl) rawDataFile)
            .storeDataPointsForMobilogram(mobilogram.getDataPoints());

        StorableMobilogram storableMobilogram = new StorableMobilogram(mobilogram,
            (IMSRawDataFileImpl) rawDataFile, storageId);

        mobilograms.add(storableMobilogram);
        return storageId;
      } catch (IOException | ClassCastException e) {
        e.printStackTrace();
        return -1;
      }
    }
  }

  @Override
  public void clearMobilograms() {
    mobilograms.forEach(mob -> ((IMSRawDataFileImpl) rawDataFile)
        .removeDataPointsForMobilogram(mob.getStorageID()));
    mobilograms.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StorableFrame)) {
      return false;
    }
    StorableFrame that = (StorableFrame) o;
    return getScanNumber() == that.getScanNumber() && getMSLevel() == that.getMSLevel()
        && Double.compare(that.getPrecursorMZ(), getPrecursorMZ()) == 0
        && getPrecursorCharge() == that.getPrecursorCharge()
        && Float.compare(that.getRetentionTime(), getRetentionTime()) == 0
        && getNumberOfDataPoints() == that.getNumberOfDataPoints() && getStorageID() == that
        .getStorageID() && Double.compare(that.getMobility(), getMobility()) == 0
        && Objects.equals(getDataPointMZRange(), that.getDataPointMZRange()) && Objects
        .equals(getHighestDataPoint(), that.getHighestDataPoint()) && Double.compare(getTIC(),
        that.getTIC()) == 0
        && getSpectrumType() == that.getSpectrumType() && getDataFile().equals(that.getDataFile())
        && Objects.equals(getMassLists(), that.getMassLists()) && getPolarity() == that
        .getPolarity() && Objects.equals(getScanDefinition(), that.getScanDefinition())
        && getScanningMZRange().equals(that.getScanningMZRange()) && getMobilityType() == that
        .getMobilityType() && getFrameId() == that.getFrameId();
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getScanNumber(), getMSLevel(), getPrecursorMZ(), getPrecursorCharge(), getRetentionTime(),
            getDataPointMZRange(), getHighestDataPoint(), getTIC(), getSpectrumType(),
            getNumberOfDataPoints(),
            getDataFile(), getMassLists(), getPolarity(), getScanDefinition(), getScanningMZRange(),
            getStorageID(), getMobility(), getMobilityType(), getFrameId());
  }
}
