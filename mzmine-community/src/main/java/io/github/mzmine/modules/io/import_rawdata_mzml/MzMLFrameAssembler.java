/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawFileWarning;
import io.github.mzmine.datamodel.RawFileWarningType;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MobilitySpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups individual mobility spectra and packs accepted frames in acquisition-axis order.
 */
final class MzMLFrameAssembler {

  private final List<BuildingMobilityScanStorage> frames;
  private final ScanImportProcessorConfig config;
  private final @Nullable MemoryMapStorage storage;
  private final List<BuildingMzMLMsScan> current = new ArrayList<>();
  private @Nullable BuildingMzMLMsScan firstSpectrum;
  private boolean decodeFrame;
  private int frameNumber;
  private int dropped;
  private int affectedFrames;
  private int firstAffectedFrame;
  private double firstDuplicateMobility;

  MzMLFrameAssembler(@NotNull final List<BuildingMobilityScanStorage> frames,
      @NotNull final ScanImportProcessorConfig config, @Nullable final MemoryMapStorage storage) {
    this.frames = frames;
    this.config = config;
    this.storage = storage;
  }

  /**
   * @param scan next mobility spectrum in acquisition order
   */
  void accept(@NotNull final BuildingMzMLMsScan scan) {
    if (scan.isMergedMobilitySpectrum()) {
      finishFrame();
      frameNumber++;
      if (!config.scanFilter().matchesFrameMetadata(scan, frameNumber)) {
        return;
      }
      final var bins = scan.splitMergedMobilityScans();
      if (prepareFrame(scan, bins, MobilitySpectralArrays::mobility)) {
        add(scan.packMergedMobilityScans(storage, config, bins));
      }
      return;
    }
    if (firstSpectrum != null && (firstSpectrum.getRetentionTime() != scan.getRetentionTime()
        || watersFunction(firstSpectrum.getId()) != watersFunction(scan.getId()))) {
      finishFrame();
    }
    if (firstSpectrum == null) {
      firstSpectrum = scan;
      frameNumber++;
      decodeFrame = config.scanFilter().matchesFrameMetadata(scan, frameNumber);
      if (!decodeFrame) {
        // Only the RT and native ID are needed to find the next frame boundary.
        scan.clearUnusedData();
      }
    }
    if (decodeFrame && scan.loadProcessMemMapMzData(storage, config)) {
      current.add(scan);
    }
  }

  /**
   * Closes the current individual-spectra frame at a boundary or at the end of input.
   */
  void finishFrame() {
    final var metadata = firstSpectrum;
    firstSpectrum = null;
    if (metadata == null || current.isEmpty()) {
      return;
    }
    if (prepareFrame(metadata, current, scan -> scan.getMobility().mobility())) {
      BuildingMzMLMsScan.processMobilityScansOfFrame(metadata, current, config);
      add(new BuildingMobilityScanStorage(storage, metadata, current));
    }
    current.clear();
  }

  private void add(@NotNull final BuildingMobilityScanStorage frame) {
    frame.setSourceFrameNumber(frameNumber);
    frames.add(frame);
  }

  /**
   * @param metadata first acquired spectrum
   * @param bins     measured bins in acquisition order, sorted in place for storage
   * @param mobility extracts the coordinate of a bin
   * @return whether the frame overlaps the requested mobility range
   */
  private <T> boolean prepareFrame(@NotNull final BuildingMzMLMsScan metadata,
      @NotNull final List<T> bins, @NotNull final ToDoubleFunction<T> mobility) {
    if (bins.isEmpty()) {
      return false;
    }
    final MobilityType type = metadata.getMobility().mobilityType();
    final double tolerance = MzMLMobilityAxis.binTolerance(type);
    double min = mobility.applyAsDouble(bins.getFirst());
    double max = min;
    boolean checkDuplicates = false;
    for (int i = 1; i < bins.size(); i++) {
      final double value = mobility.applyAsDouble(bins.get(i));
      // Coordinates outside the previous span by more than the tolerance cannot repeat a bin.
      checkDuplicates |= value >= min - tolerance && value <= max + tolerance;
      min = Math.min(min, value);
      max = Math.max(max, value);
    }
    if (!config.scanFilter().matchesFrameMobilityRange(min, max)) {
      return false;
    }
    if (checkDuplicates) {
      discardRepeatedBins(bins, mobility, tolerance);
    }
    final var order = Comparator.comparingDouble(mobility);
    bins.sort(type == MobilityType.TIMS ? order.reversed() : order);
    return true;
  }

  /**
   * @param bins      measured bins in acquisition order
   * @param mobility  extracts the coordinate of a bin
   * @param tolerance inclusive radius around each retained coordinate
   */
  private <T> void discardRepeatedBins(@NotNull final List<T> bins,
      @NotNull final ToDoubleFunction<T> mobility, final double tolerance) {
    final var seen = TreeRangeSet.<Double>create();
    // decision: the first acquired spectrum owns the bin, even if a later one sorts before it.
    final boolean removed = bins.removeIf(bin -> {
      final double value = mobility.applyAsDouble(bin);
      if (!seen.contains(value)) {
        seen.add(Range.closed(value - tolerance, value + tolerance));
        return false;
      }
      if (dropped == 0) {
        firstAffectedFrame = frameNumber;
        firstDuplicateMobility = value;
      }
      dropped++;
      return true;
    });
    if (removed) {
      affectedFrames++;
    }
  }

  /**
   * @return the Waters function number or zero for other native ID formats
   */
  static int watersFunction(@NotNull final String id) {
    if (!id.startsWith("function=")) {
      return 0;
    }
    final int end = id.indexOf(' ');
    if (end <= "function=".length()) {
      return 0;
    }
    try {
      return Integer.parseInt(id, "function=".length(), end, 10);
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  /**
   * Adds an axis-aligned frame using its packed segments. Every placeholder has zero points.
   *
   * @param file         destination file
   * @param packed       measured spectra in axis order
   * @param axis         coordinates shared by this function
   * @param massDetected whether the stored peaks are also the mass list
   */
  static void appendFrame(@NotNull final IMSRawDataFileImpl file,
      @NotNull final BuildingMobilityScanStorage packed, @NotNull final MzMLMobilityAxis axis,
      final boolean massDetected) throws IOException {
    final int frameNumber = packed.getSourceFrameNumber();
    final List<BuildingImsMsMsInfo> precursors = new ArrayList<>();
    final int[] offsets = new int[axis.coordinates().length];
    final int[] basePeaks = new int[offsets.length];
    Arrays.fill(basePeaks, -1);
    int next = 0;
    final var scans = packed.getMobilityScans();
    for (int i = 0; i < scans.size(); i++) {
      final var scan = scans.get(i);
      final Integer bin = axis.indices().get(scan.mobility());
      if (bin == null || bin < next) {
        throw new IOException(
            "Cannot align mobility " + scan.mobility() + " in frame " + frameNumber);
      }
      Arrays.fill(offsets, next, bin + 1, packed.getStorageOffset(i));
      basePeaks[bin] = packed.getBasePeakIndex(i);
      // Leading and interior placeholders inherit the next measured isolation.
      for (; next <= bin; next++) {
        ConversionUtils.extractImsMsMsInfo(scan.precursorList(), precursors, frameNumber, next);
      }
    }
    Arrays.fill(offsets, next, offsets.length, packed.getRawTotalNumPoints());
    if (next < offsets.length) {
      // Only isolations active at the final measured bin extend into trailing placeholders.
      for (final var precursor : precursors) {
        if (precursor.getLastSpectrumNumber() == next - 1) {
          precursor.setLastSpectrumNumber(offsets.length - 1);
        }
      }
    }
    final var frame = packed.createFrame(file, frameNumber);
    frame.setMobilities(axis.coordinates());
    frame.setMobilityScanStorage(
        new MobilityScanStorage(file.getMemoryMapStorage(), frame, packed.getMzValues(),
            packed.getIntensityValues(), packed.getMaxNumPoints(), offsets, basePeaks, massDetected,
            massDetected ? MassSpectrumType.CENTROIDED : scans.getFirst().spectrumType()));
    if (!precursors.isEmpty()) {
      final Set<IonMobilityMsMsInfo> infos = new HashSet<>();
      for (final var precursor : precursors) {
        infos.add(precursor.build(null, frame));
      }
      frame.setPrecursorInfos(infos);
    }
    file.addScan(frame);
  }

  /**
   * @return one file-level warning summarizing discarded duplicates
   */
  @Nullable RawFileWarning warning() {
    return dropped == 0 ? null : new RawFileWarning(RawFileWarningType.DUPLICATE_MOBILITY_SCANS,
        "Dropped %d spectra in %d frames. First occurrence in frame %d at mobility %s.".formatted(
            dropped, affectedFrames, firstAffectedFrame, firstDuplicateMobility));
  }
}
