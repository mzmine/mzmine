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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.StoredMobilityScan;
import io.github.mzmine.datamodel.impl.masslist.StoredMobilityScanMassList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder object during MzML import of Ion mobility data. Will be transformed to
 * {@link MobilityScanStorage} later. The issue is that when loading - not all mobility values
 * actually have a scan. So empty scans may be missing from the file but the final store needs to
 * contain all ion mobility scans + placeholders for empty data. Memory efficient storage of
 * {@link MobilityScan}s. Methods return an instance of {@link StoredMobilityScan} or
 * {@link StoredMobilityScanMassList} which is garbage collected if not used anymore.
 */
public class BuildingMobilityScanStorage {

  private final MemorySegment mzValues;
  private final MemorySegment intensityValues;
  /**
   * Per scan
   */
  private final int[] storageOffsets;
  private final List<BuildingMzMLMobilityScan> mobilityScans;
  /**
   * Per scan
   */
  private final int[] basePeakIndices;
  private int maxNumPoints;

  // frame information
  private final int msLevel;
  private final float retentionTime;
  private final @NotNull MassSpectrumType spectrumType;
  private final @NotNull PolarityType polarity;
  private final String scanDefinition;
  private final @NotNull Range<Double> scanningMZRange;


  /**
   * @param storage
   * @param mobilityScans will be stored internally. Loaded data is memory mapped and then removed
   *                      from these instances
   */
  public BuildingMobilityScanStorage(@Nullable MemoryMapStorage storage,
      @NotNull List<BuildingMzMLMsScan> mobilityScans) {
    this.mobilityScans = mobilityScans.stream().map(BuildingMzMLMobilityScan::create).toList();
    storageOffsets = new int[mobilityScans.size()];
    int numDp = fillDataOffsetsGetTotalDataPoints(mobilityScans);

    mzValues = memoryMap(storage, numDp, mobilityScans, SimpleSpectralArrays::mzs);
    intensityValues = memoryMap(storage, numDp, mobilityScans, SimpleSpectralArrays::intensities);

    this.basePeakIndices = findBasePeakIndices(mobilityScans, storageOffsets);

    // extract some values for the frame from the first mob scan
    var firstScan = mobilityScans.getFirst();
    msLevel = firstScan.getMSLevel();
    retentionTime = firstScan.getRetentionTime();
    // maybe always centroid? the frame will always be calculated
    spectrumType = firstScan.getSpectrumType();
    polarity = firstScan.getPolarity();
    scanDefinition = firstScan.getScanDefinition();
    scanningMZRange = firstScan.getScanningMZRange();

    // clear all data from all scans
    mobilityScans.forEach(BuildingMzMLMsScan::clearMobilityData);
  }

  public List<BuildingMzMLMobilityScan> getMobilityScans() {
    return mobilityScans;
  }

  private int[] findBasePeakIndices(final List<BuildingMzMLMsScan> scans, final int[] offsets) {
    int[] basePeakIndices = new int[scans.size()];

    for (int scanI = 0; scanI < scans.size(); scanI++) {
      var scan = scans.get(scanI);
      int offset = offsets[scanI];
      double[] intensities = scan.getMobilityScanSimpleSpectralData().intensities();

      if (intensities.length == 0) {
        basePeakIndices[scanI] = -1;
        continue;
      }

      double maxIntensity = Double.NEGATIVE_INFINITY;
      for (int dp = 0; dp < intensities.length; dp++) {
        double intensity = intensities[dp];
        if (intensity > maxIntensity) {
          basePeakIndices[scanI] = offset + dp;
          maxIntensity = intensity;
        }
      }
    }
    return basePeakIndices;
  }

  private MemorySegment memoryMap(final @Nullable MemoryMapStorage storage, final int numDp,
      final List<BuildingMzMLMsScan> mobilityScans,
      final Function<SimpleSpectralArrays, double[]> dataSupplier) {
    final double[] result = new double[numDp];
    int offset = 0;
    for (final BuildingMzMLMsScan scan : mobilityScans) {
      double[] data = dataSupplier.apply(scan.getMobilityScanSimpleSpectralData());
      System.arraycopy(data, 0, result, offset, data.length);
      offset += data.length;
    }
    return StorageUtils.storeValuesToDoubleBuffer(storage, result);
  }

  private int fillDataOffsetsGetTotalDataPoints(
      final @NotNull List<BuildingMzMLMsScan> mobilityScans) {
    int lastOffset = 0;
    for (int i = 0; i < mobilityScans.size(); i++) {
      final BuildingMzMLMsScan scan = mobilityScans.get(i);
      SimpleSpectralArrays data = scan.getMobilityScanSimpleSpectralData();
      int numDP = data.getNumberOfDataPoints();
      storageOffsets[i] = lastOffset;
      lastOffset += numDP;
      maxNumPoints = Math.max(maxNumPoints, numDP);
    }
    return lastOffset;
  }

  public int getNumberOfMobilityScans() {
    return storageOffsets.length;
  }

  /**
   * @return The maximum number of data points in a single mobility scan.
   */
  public int getMaxNumPoints() {
    return maxNumPoints;
  }

  /**
   * @return The total number of points in this {@link  MobilityScanStorage}.
   */
  public int getRawTotalNumPoints() {
    return (int) StorageUtils.numDoubles(mzValues);
  }


  public MemorySegment getMzValues() {
    return mzValues;
  }

  public MemorySegment getIntensityValues() {
    return intensityValues;
  }

  public int getStorageOffset(int i) {
    return storageOffsets[i];
  }

  public int getBasePeakIndex(int i) {
    return basePeakIndices[i];
  }

  public int[] getStorageOffsets() {
    return storageOffsets;
  }

  public int[] getBasePeakIndices() {
    return basePeakIndices;
  }

  public int getMsLevel() {
    return msLevel;
  }

  public float getRetentionTime() {
    return retentionTime;
  }

  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  public PolarityType getPolarity() {
    return polarity;
  }

  public String getScanDefinition() {
    return scanDefinition;
  }

  public Range<Double> getScanningMZRange() {
    return scanningMZRange;
  }

  public SimpleFrame createFrame(final IMSRawDataFileImpl newImsFile, final int frameNumber) {
    MobilityType mobilityType = mobilityScans.getFirst().mobilityType();
    return new SimpleFrame(newImsFile, frameNumber, getMsLevel(), getRetentionTime(), null, null,
        getSpectrumType(), getPolarity(), getScanDefinition(), getScanningMZRange(), mobilityType,
        null, null);
  }
}
