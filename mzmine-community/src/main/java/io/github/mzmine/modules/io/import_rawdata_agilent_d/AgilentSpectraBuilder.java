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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimitType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimits;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.DIAImsMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DIAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max.AdaptiveLocalMaxMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max.LocalMaxGaussianModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds mzmine frames and scans from the spectra of an Agilent .d file, independent of the reader
 * that provides them: scan filter, mzmine centroiding, saturation thresholds, spectral processors,
 * mass lists and MS/MS info. Shared by all native Agilent import paths so they behave the same. One
 * instance per imported file.
 */
@NotThreadSafe
public final class AgilentSpectraBuilder {

  private static final Logger logger = Logger.getLogger(AgilentSpectraBuilder.class.getName());

  private final @NotNull String fileName;
  private final @Nullable ScanImportProcessorConfig processor;

  // decision: adaptive local maximum (benchmarked on Agilent 6560 IMS data), same smoothing as the
  // previous local maximum centroider
  /**
   * Centroids frames (total frame spectra). No saturation handling, the abundance limit only
   * applies to mobility scans.
   */
  private final AdaptiveLocalMaxMassDetector frameCentroider = new AdaptiveLocalMaxMassDetector(0,
      AbundanceMeasure.Height, 3, new LocalMaxGaussianModule(7));
  /**
   * Centroids mobility scans per ms level, with the saturation threshold derived from the mobility
   * scan abundance limit of the first frame of this ms level.
   */
  private final Map<Integer, AdaptiveLocalMaxMassDetector> mobilityScanCentroiders = new HashMap<>();
  /**
   * Centroids non-IMS profile scans, per abundance limit (saturation threshold) of the scan.
   */
  private final Map<Double, AdaptiveLocalMaxMassDetector> scanCentroiders = new HashMap<>();

  public AgilentSpectraBuilder(@NotNull String fileName,
      @Nullable ScanImportProcessorConfig processor) {
    this.fileName = fileName;
    this.processor = processor;
  }

  /**
   * @return true if the scan selection of the import excludes a spectrum with this metadata; its
   * data does not need to be read then
   */
  public boolean isFilteredOut(int msLevel, @NotNull PolarityType polarity, float rt,
      double precursorMz) {
    if (processor == null || !processor.hasProcessors() || processor.scanFilter() == null
        || !processor.scanFilter().isActiveFilter()) {
      return false;
    }
    final ScanSelection selection = processor.scanFilter();
    return !selection.matches(
        new SimpleBuildingScan(0, msLevel, polarity, MassSpectrumType.PROFILE, rt,
            (float) precursorMz, 0));
  }

  public boolean isMassDetectActive(int msLevel) {
    return processor != null && processor.isMassDetectActive(msLevel);
  }

  /**
   * Builds one IMS frame: the total frame spectrum becomes the {@link SimpleFrame}, the mobility
   * scans its {@link BuildingMobilityScan}s.
   *
   * @param mobilityScans     one entry per drift bin (empty arrays for empty bins), profile data
   * @param mzmineCentroiding centroid a profile TFS and all mobility scans with mzmine (mobility
   *                          scans with the mobility-aware saturation handling)
   * @param driftTimes        drift time per drift bin (ms)
   */
  public @NotNull SimpleFrame buildFrame(@NotNull IMSRawDataFileImpl file, int frameId,
      @NotNull AgilentFrameInfo info, double @NotNull [] tfsMzs, double @NotNull [] tfsIntensities,
      boolean tfsCentroided, @NotNull List<SimpleSpectralArrays> mobilityScans,
      boolean mzmineCentroiding, double @NotNull [] driftTimes) {
    final int msLevel = info.msLevel();
    double[] mzs = tfsMzs;
    double[] intensities = tfsIntensities;
    boolean centroided = tfsCentroided;
    if (!centroided && mzmineCentroiding) {
      final double[][] c = frameCentroider.getMassValues(
          new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE));
      mzs = c[0];
      intensities = c[1];
      centroided = true;
    }

    // the processors receive the actual input type; mass detection marks the metadata centroided
    final var meta = new SimpleBuildingScan(0, msLevel, info.polarity(), inputType(centroided),
        info.rt(), 0, 0);
    final SimpleSpectralArrays frameData = applyProcessor(meta, mzs, intensities);
    final MassSpectrumType type = meta.getSpectrumType();
    final boolean massDetect = isMassDetectActive(msLevel);
    final Set<IonMobilityMsMsInfo> precursorInfos =
        msLevel > 1 && info.msMsInfo() != null ? Set.of(info.msMsInfo()) : null;

    final SimpleFrame frame = new SimpleFrame(file, frameId, msLevel, info.rt(), frameData.mzs(),
        frameData.intensities(), type, info.polarity(), "frame=" + frameId,
        mzRange(frameData.mzs()), MobilityType.DRIFT_TUBE, precursorInfos, null);
    if (massDetect) {
      frame.addMassList(new ScanPointerMassList(frame));
    }

    // the limit of the first frame per ms level is stored in the raw file metadata
    final double fileAbundLimit = updateMobilityScanAbundanceLimit(file, msLevel,
        info.mobilityScanAbundLimit());

    List<SimpleSpectralArrays> mobilityScanData = mobilityScans;
    if (!Double.isNaN(fileAbundLimit)) {
      double maxIntensity = 0;
      for (final SimpleSpectralArrays data : mobilityScans) {
        for (final double intensity : data.intensities()) {
          maxIntensity = Math.max(maxIntensity, intensity);
        }
      }
      if (maxIntensity > fileAbundLimit) {
        // TODO separate PR: warn and disable the saturation handling for this file. The metadata
        //  does not match the data, e.g., PNNL PreProcessor output keeps the transients and
        //  abundance limit of the raw file although the intensities were summed.
      }
    }

    if (mzmineCentroiding) {
      final List<SimpleMassSpectrum> profiles = new ArrayList<>(mobilityScans.size());
      for (final SimpleSpectralArrays data : mobilityScans) {
        profiles.add(
            new SimpleMassSpectrum(data.mzs(), data.intensities(), MassSpectrumType.PROFILE));
      }
      // all mobility scans of the frame at once
      final List<double[][]> centroidedScans = mobilityScanCentroider(msLevel,
          fileAbundLimit).getMobilityScanMassValues(profiles);
      mobilityScanData = new ArrayList<>(centroidedScans.size());
      for (final double[][] values : centroidedScans) {
        mobilityScanData.add(new SimpleSpectralArrays(values[0], values[1]));
      }
    }

    // mobility scans are profile unless centroided by mzmine or by mass detection on import
    final var mobMeta = new SimpleBuildingScan(0, msLevel, info.polarity(),
        inputType(mzmineCentroiding), info.rt(), 0, 0);
    final List<SimpleSpectralArrays> processed = applyMobilityScanProcessor(mobMeta,
        mobilityScanData);
    final MassSpectrumType mobType = mobMeta.getSpectrumType();
    final List<BuildingMobilityScan> mobScans = new ArrayList<>(processed.size());
    for (int i = 0; i < processed.size(); i++) {
      final SimpleSpectralArrays data = processed.get(i);
      mobScans.add(new BuildingMobilityScan(i, data.mzs(), data.intensities(), mobType));
    }

    frame.setMobilityScans(mobScans, massDetect);
    frame.setMobilities(driftTimes);
    return frame;
  }

  /**
   * Builds one non-IMS scan.
   *
   * @param centroided whether the data is centroided (stored or mzmine centroids)
   */
  public @NotNull SimpleScan buildScan(@NotNull RawDataFileImpl file, int id, int msLevel, float rt,
      @NotNull PolarityType polarity, double precursorMz, @Nullable MsMsInfo msMsInfo,
      double @NotNull [] mzs, double @NotNull [] intensities, boolean centroided) {
    // the processors receive the actual input type; mass detection marks the metadata centroided
    final var meta = new SimpleBuildingScan(0, msLevel, polarity, inputType(centroided), rt,
        (float) precursorMz, 0);
    final SimpleSpectralArrays data = applyProcessor(meta, mzs, intensities);
    final MassSpectrumType type = meta.getSpectrumType();
    final SimpleScan scan = new SimpleScan(file, id, msLevel, rt, msMsInfo, data.mzs(),
        data.intensities(), type, polarity, "scan=" + id, mzRange(data.mzs()));
    if (isMassDetectActive(msLevel)) {
      scan.addMassList(new ScanPointerMassList(scan));
    }
    return scan;
  }

  /**
   * Centroids a non-IMS profile scan with the same detector as the mobility scans: peaks reaching
   * {@link AdaptiveLocalMaxMassDetector#SATURATION_LIMIT_FRACTION} of the abundance limit get the
   * EMG fit.
   *
   * @param abundanceLimit max possible abundance of a point of this scan, NaN if unknown (no
   *                       saturation handling)
   * @return [mzs, intensities]
   */
  public double @NotNull [][] centroidScan(double @NotNull [] mzs, double @NotNull [] intensities,
      double abundanceLimit) {
    final double key = Double.isNaN(abundanceLimit) ? Double.NaN : abundanceLimit;
    final AdaptiveLocalMaxMassDetector detector = scanCentroiders.computeIfAbsent(key, limit -> {
      final double threshold = Double.isNaN(limit) ? Double.POSITIVE_INFINITY
          : AdaptiveLocalMaxMassDetector.SATURATION_LIMIT_FRACTION * limit;
      return new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
          new LocalMaxGaussianModule(7), threshold);
    });
    return detector.getMassValues(
        new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE));
  }

  /**
   * Ion-mobility MS/MS info for an MS2 frame. All-Ions (DIA) frames have no precursor selection
   * (full-range isolation window = null); DDA frames carry a precursor m/z and isolation window.
   * Mirrors the Waters IMS path, which uses {@link DIAImsMsMsInfoImpl} for all IMS MS/MS.
   */
  public static @NotNull IonMobilityMsMsInfo imsMsMsInfo(double collisionEnergy,
      boolean hasPrecursor, double precursorMz, double isolationWidth, int numDriftBins) {
    final Float ce = collisionEnergy > 0 ? (float) collisionEnergy : null;
    Range<Double> isolationWindow = null;
    if (hasPrecursor && isolationWidth > 0) {
      isolationWindow = Range.closed(precursorMz - isolationWidth / 2,
          precursorMz + isolationWidth / 2);
    }
    return new DIAImsMsMsInfoImpl(Range.closed(0, Math.max(0, numDriftBins - 1)), ce, null,
        isolationWindow);
  }

  /**
   * MS/MS info for a non-IMS scan. A scan with a precursor is targeted MS/MS (DDA); a scan without
   * one but at a non-zero collision energy is All-Ions / MSE (DIA), where the full m/z range is
   * fragmented.
   *
   * @param charge          precursor charge, 0 if unknown
   * @param isolationWindow quad isolation window, null if unknown
   */
  public static @Nullable MsMsInfo scanMsMsInfo(int msLevel, boolean hasPrecursor,
      double precursorMz, int charge, double collisionEnergy, @NotNull ActivationMethod activation,
      @Nullable Range<Double> isolationWindow) {
    if (msLevel <= 1) {
      return null;
    }
    final Float ce = collisionEnergy > 0 ? (float) collisionEnergy : null;
    if (hasPrecursor) {
      return new DDAMsMsInfoImpl(precursorMz, charge > 0 ? charge : null, ce, null, null, msLevel,
          activation, isolationWindow);
    }
    return new DIAMsMsInfoImpl(ce, null, msLevel, activation, null);
  }

  /**
   * Stores the mobility scan abundance limit of this ms level in the raw file metadata if it is not
   * set yet.
   *
   * @return the limit of the file for this ms level (from the first frame of the ms level), NaN if
   * unknown
   */
  private static double updateMobilityScanAbundanceLimit(@NotNull final IMSRawDataFileImpl file,
      final int msLevel, final double frameLimit) {
    final MobilityScanAbundanceLimits limits = Objects.requireNonNullElse(
        file.getFileMetadata().get(MobilityScanAbundanceLimitType.class),
        MobilityScanAbundanceLimits.UNKNOWN);
    final double fileLimit = limits.forMsLevel(msLevel);
    if (Double.isNaN(fileLimit)) {
      if (!Double.isNaN(frameLimit)) {
        file.getFileMetadata()
            .set(MobilityScanAbundanceLimitType.class, limits.withMsLevel(msLevel, frameLimit));
      }
      return frameLimit;
    }
    if (!Double.isNaN(frameLimit) && Double.compare(fileLimit, frameLimit) != 0) {
      // TODO separate PR: warn that the abundance limit changes within the frames of this ms level,
      //  the limit (and saturation threshold) of the first frame is used.
    }
    return fileLimit;
  }

  /**
   * @param abundanceLimit mobility scan abundance limit of the file for this ms level, NaN if
   *                       unknown
   * @return the centroider of this ms level, with the saturation threshold derived from the
   * abundance limit of the first frame of this ms level
   */
  private @NotNull AdaptiveLocalMaxMassDetector mobilityScanCentroider(final int msLevel,
      final double abundanceLimit) {
    return mobilityScanCentroiders.computeIfAbsent(msLevel, level -> {
      final double threshold = Double.isNaN(abundanceLimit) ? Double.POSITIVE_INFINITY
          : AdaptiveLocalMaxMassDetector.SATURATION_LIMIT_FRACTION * abundanceLimit;
      logger.finest("Mobility scan saturation threshold of %s MS%d: %.0f".formatted(fileName, level,
          threshold));
      return new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
          new LocalMaxGaussianModule(7), threshold);
    });
  }

  /**
   * Processes all mobility scans of a frame together, see
   * {@link
   * io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor#processMobilityScans}.
   */
  private @NotNull List<SimpleSpectralArrays> applyMobilityScanProcessor(
      @NotNull final SimpleBuildingScan meta,
      @NotNull final List<SimpleSpectralArrays> mobilityScans) {
    if (processor == null || !processor.hasProcessors()) {
      return mobilityScans;
    }
    return processor.processor().processMobilityScans(meta, mobilityScans);
  }

  private @NotNull SimpleSpectralArrays applyProcessor(@NotNull SimpleBuildingScan meta,
      double @NotNull [] mzs, double @NotNull [] intensities) {
    final SimpleSpectralArrays arrays = new SimpleSpectralArrays(mzs, intensities);
    if (processor == null || !processor.hasProcessors()) {
      return arrays;
    }
    return processor.processor().processScan(meta, arrays);
  }

  /**
   * @param centroided whether the spectrum as read (before the import processors) is centroided
   * @return the spectrum type passed to the import processors
   */
  private static @NotNull MassSpectrumType inputType(final boolean centroided) {
    return centroided ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE;
  }

  private static @NotNull Range<Double> mzRange(double @NotNull [] mzs) {
    return mzs.length > 0 ? Range.closed(mzs[0], mzs[mzs.length - 1]) : Range.closed(0d, 0d);
  }
}
