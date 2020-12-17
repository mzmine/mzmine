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
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class StorableFrame extends StorableScan implements Frame {

  private static final Logger logger = Logger.getLogger(Frame.class.getName());

  private final int frameId;

  /**
   * key = scan num, value = mobility scan // TODO do we need this?
   */
  private final SortedMap<Integer, Scan> mobilityScans;
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

    frameId = originalFrame.getFrameId();
    mobilograms = new ArrayList<>();
    mobilityScans = new TreeMap<>();
    mobilityRange = null;

    for (int scannum : originalFrame.getMobilityScanNumbers()) {
      Scan scan = rawDataFile.getScan(scannum);
      if (scan != null) {
        addMobilityScan(scan);
      }
    }
  }

  public StorableFrame(RawDataFileImpl rawDataFile, int storageID, int numberOfDataPoints,
      int scanNumber, int msLevel, float retentionTime, double precursorMZ,
      int precursorCharge, int[] fragmentScans,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, @Nonnull MobilityType mobilityType,
      @Nonnull Range<Double> mobilityRange, @Nonnull List<Integer> mobilityScanNumbers) {

    super(rawDataFile, storageID, numberOfDataPoints, scanNumber, msLevel, retentionTime,
        precursorMZ, precursorCharge, fragmentScans, spectrumType, polarity, scanDefinition,
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
  }

  @Override
  public int getFrameId() {
    return frameId;
  }

  @Override
  public int getNumberOfMobilityScans() {
    return mobilityScans.size();
  }

  @Override
  public Set<Integer> getMobilityScanNumbers() {
    return mobilityScans.keySet();
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
  public Scan getMobilityScan(int num) {
    return Objects.requireNonNull(
        mobilityScans.computeIfAbsent(num, i -> rawDataFile.getScan(num)));
  }

  @Nonnull
  @Override
  public List<Scan> getMobilityScans() {
    return new ArrayList<>(mobilityScans.values());
  }

  protected final void addMobilityScan(Scan mobilityScan) {
    if (mobilityRange == null) {
      mobilityRange = Range.singleton(mobilityScan.getMobility());
    } else if (!mobilityRange.contains(mobilityScan.getMobility())) {
      mobilityRange = mobilityRange.span(Range.singleton(mobilityScan.getMobility()));
    }

    mobilityScans.put(mobilityScan.getScanNumber(), mobilityScan);
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
}
