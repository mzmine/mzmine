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
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Mobilogram;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.Frame
 */
public class StorableFrame extends StorableScan implements Frame {

  private static Logger logger = Logger.getLogger(Frame.class.getName());

  /**
   * key = scan num, value = mobility scan
   */
  private final Map<Integer, MobilityScan> mobilitySubScans;
  private final Set<ImsMsMsInfo> precursorInfos;
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
  public StorableFrame(Frame originalFrame, RawDataFileImpl rawDataFile, int numberOfDataPoints,
      int storageID) throws IOException {
    super(originalFrame, rawDataFile, numberOfDataPoints, storageID);

    mobilitySubScans = new HashMap<>(originalFrame.getNumberOfMobilityScans());
    mobilograms = new ArrayList<>();
    mobilityRange = null;
    precursorInfos = originalFrame.getImsMsMsInfos();
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
    return mobilitySubScans.size();
  }

  @Override
  public Set<Integer> getMobilityScanNumbers() {
    return mobilitySubScans.keySet();
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
  public MobilityScan getMobilityScan(int num) {
    return Objects.requireNonNull(mobilitySubScans.get(num));
  }

  @Nonnull
  @Override
  public Collection<MobilityScan> getMobilityScans() {
    return mobilitySubScans.values();
  }

  /**
   * Not to be used during processing. Can only be called during raw data file reading before
   * finishWriting() was called.
   *
   * @param originalMobilityScan The mobility scan to store.
   */
  public final void addMobilityScan(MobilityScan originalMobilityScan) {
    try {
      final int storageId =
          ((IMSRawDataFileImpl) rawDataFile).storeDataPoints(originalMobilityScan.getDataPoints());

      if (mobilityRange == null) {
        mobilityRange = Range.singleton(originalMobilityScan.getMobility());
      } else if (!mobilityRange.contains(originalMobilityScan.getMobility())) {
        mobilityRange = mobilityRange.span(Range.singleton(originalMobilityScan.getMobility()));
      }

      StorableMobilityScan storableMobilityScan =
          new StorableMobilityScan(originalMobilityScan, storageId);
      mobilitySubScans.put(originalMobilityScan.getMobilityScamNumber(), storableMobilityScan);

    } catch (IOException e) {
      e.printStackTrace();
      logger.warning(() -> "Mobility scan " + originalMobilityScan.getMobilityScamNumber()
          + " for frame " + getFrameId() + " not stored.");
    }
  }

  /**
   * @param mobilityScanIndex
   * @return
   * @see io.github.mzmine.datamodel.IMSRawDataFile#getMobilityForMobilitySpectrum(int, int)
   */
  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return ((IMSRawDataFileImpl) rawDataFile).getMobilityForMobilitySpectrum(getScanNumber(),
        mobilityScanIndex);
  }

  /**
   * @return
   * @see IMSRawDataFileImpl#getMobilitiesForFrame(int)
   */
  @Nullable
  @Override
  public Map<Integer, Double> getMobilities() {
    return ((IMSRawDataFileImpl) rawDataFile).getMobilitiesForFrame(getScanNumber());
  }

  @Nonnull
  @Override
  public Set<ImsMsMsInfo> getImsMsMsInfos() {
    return Objects.requireNonNullElse(precursorInfos, Collections.emptySet());
  }

  @Nullable
  @Override
  public ImsMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    Optional<ImsMsMsInfo> pcInfo = precursorInfos.stream()
        .filter(info -> info.getSpectrumNumberRange().contains(mobilityScanNumber)).findFirst();
    return pcInfo.orElse(null);
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
        && getNumberOfDataPoints() == that.getNumberOfDataPoints()
        && getStorageID() == that.getStorageID()
        && Double.compare(that.getMobility(), getMobility()) == 0
        && Objects.equals(getDataPointMZRange(), that.getDataPointMZRange())
        && Objects.equals(getHighestDataPoint(), that.getHighestDataPoint())
        && Double.compare(getTIC(), that.getTIC()) == 0
        && getSpectrumType() == that.getSpectrumType() && getDataFile().equals(that.getDataFile())
        && Objects.equals(getMassLists(), that.getMassLists())
        && getPolarity() == that.getPolarity()
        && Objects.equals(getScanDefinition(), that.getScanDefinition())
        && getScanningMZRange().equals(that.getScanningMZRange())
        && getMobilityType() == that.getMobilityType() && getFrameId() == that.getFrameId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScanNumber(), getMSLevel(), getPrecursorMZ(), getPrecursorCharge(),
        getRetentionTime(), getDataPointMZRange(), getHighestDataPoint(), getTIC(),
        getSpectrumType(), getNumberOfDataPoints(), getDataFile(), getMassLists(), getPolarity(),
        getScanDefinition(), getScanningMZRange(), getStorageID(), getMobility(), getMobilityType(),
        getFrameId());
  }
}
