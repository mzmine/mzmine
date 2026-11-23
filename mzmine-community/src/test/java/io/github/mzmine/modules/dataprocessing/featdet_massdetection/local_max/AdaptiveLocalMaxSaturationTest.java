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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimitType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimits;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Saturation threshold of the {@link AdaptiveLocalMaxMassDetector}: manual for all spectra, raw
 * file metadata for mobility scans, the saturation plateau per spectrum type, and the parameter
 * with restricted options.
 */
class AdaptiveLocalMaxSaturationTest {

  private static final double CEILING = 50_000;
  private static final double LIMIT = 52_224;

  /**
   * Tailing profile peak (fronting Gaussian half width 0.008, tailing 0.016) of height 100k at m/z
   * 500, clipped at the detector ceiling with a small ripple (real clipped tops are not perfectly
   * flat) and zero end points like profile data.
   */
  private static @NotNull SimpleMassSpectrum clippedPeak() {
    final int n = 101;
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    // TOF grid: equidistant in sqrt(m/z), ~0.002 spacing at m/z 500
    final double sqrtStart = Math.sqrt(499.9);
    final double sqrtStep = 0.002 / (2 * Math.sqrt(500));
    for (int i = 0; i < n; i++) {
      final double mz = Math.pow(sqrtStart + i * sqrtStep, 2);
      final double delta = mz - 500.0;
      final double width = delta < 0 ? 0.008 : 0.016;
      mzs[i] = mz;
      final double ceiling = CEILING - 25 * (i % 3);
      intensities[i] = Math.min(ceiling,
          100_000 * Math.exp(-0.5 * (delta / width) * (delta / width)));
    }
    intensities[0] = 0;
    intensities[n - 1] = 0;
    return new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE);
  }

  private static @NotNull AdaptiveLocalMaxMassDetector detector(
      @NotNull final SaturationThresholdValue saturation) {
    return new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
        new LocalMaxNoSmoothingModule(), Double.POSITIVE_INFINITY, saturation);
  }

  private static double centroid(final double[][] values) {
    Assertions.assertEquals(1, values[0].length, "expected one centroid");
    return values[0][0];
  }

  private static @NotNull AdaptiveLocalMaxMassDetector fixed(final double threshold) {
    return new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
        new LocalMaxNoSmoothingModule(), threshold);
  }

  @Test
  void manualThresholdAppliesToAllSpectra() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    final AdaptiveLocalMaxMassDetector manual = detector(
        new SaturationThresholdValue(SaturationThresholdOption.MANUAL, 25_000));
    // same threshold as fixed constructor value
    final double fixedMz = centroid(fixed(25_000).getMassValues(spectrum));

    Assertions.assertNotEquals(
        centroid(detector(SaturationThresholdValue.OFF).getMassValues(spectrum)), fixedMz, 1e-9,
        "saturation branch was not applied");
    // mobility scans, frames and other spectra
    Assertions.assertEquals(fixedMz,
        centroid(manual.getMobilityScanMassValues(List.of(spectrum)).getFirst()), 1e-12);
    Assertions.assertEquals(fixedMz, centroid(manual.getMassValues(spectrum)), 1e-12);
  }

  /**
   * Narrow profile peaks on a TOF grid (~0.0002 spacing at m/z 500). Each peak is (center, sigma,
   * height), clipped with a small ripple at the ceiling.
   */
  private static @NotNull SimpleMassSpectrum narrowPeaks(final double ceiling,
      final double @NotNull []... peaks) {
    final int n = 151;
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    final double sqrtStart = Math.sqrt(499.99);
    final double sqrtStep = 0.0002 / (2 * Math.sqrt(500));
    for (int i = 0; i < n; i++) {
      mzs[i] = Math.pow(sqrtStart + i * sqrtStep, 2);
      double intensity = 0;
      for (final double[] peak : peaks) {
        final double delta = (mzs[i] - peak[0]) / peak[1];
        intensity += peak[2] * Math.exp(-0.5 * delta * delta);
      }
      intensities[i] = Math.min(ceiling - 25 * (i % 3), intensity);
    }
    intensities[0] = 0;
    intensities[n - 1] = 0;
    return new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE);
  }

  /**
   * Saturated centroids of a frame get the m/z of the unsaturated flank scans of the same ion. The
   * correction moves ion A (distorted to 500.006 in the saturated scans) below ion B at 500.003, so
   * the scan is sorted again and each intensity must stay with its m/z.
   */
  @Test
  void saturatedMobilityScansGetTheFlankMzAndStaySorted() {
    final double threshold = 50_000;
    final double ceiling = 60_000;
    final double[] flankA = {500.000, 0.0004, 20_000};
    final double[] saturatedA = {500.006, 0.0004, 200_000};
    final double[] ionB = {500.003, 0.0004, 20_000};
    final SimpleMassSpectrum flank = narrowPeaks(ceiling, flankA);
    final SimpleMassSpectrum saturated = narrowPeaks(ceiling, saturatedA, ionB);
    final List<SimpleMassSpectrum> scans = List.of(flank, flank, saturated, saturated, flank,
        flank);

    final List<double[][]> values = detector(
        new SaturationThresholdValue(SaturationThresholdOption.MANUAL,
            threshold)).getMobilityScanMassValues(scans);

    final double flankMz = centroid(values.getFirst());
    Assertions.assertEquals(500.000, flankMz, 1e-4);
    for (final int scan : new int[]{2, 3}) {
      final double[] mzs = values.get(scan)[0];
      final double[] intensities = values.get(scan)[1];
      Assertions.assertEquals(2, mzs.length, "expected ion A and B in scan " + scan);
      Assertions.assertTrue(MobilityFlankConsolidator.isSorted(mzs), Arrays.toString(mzs));
      // corrected ion A is first now and keeps its saturated height
      Assertions.assertEquals(flankMz, mzs[0], 1e-9);
      Assertions.assertTrue(intensities[0] >= threshold, Arrays.toString(intensities));
      // ion B is unchanged and keeps its height
      Assertions.assertEquals(500.003, mzs[1], 1e-4);
      Assertions.assertEquals(20_000, intensities[1], 500);
    }
  }

  @Test
  void metadataThresholdIsHalfTheAbundanceLimitOfTheMsLevel() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    final IMSRawDataFile file = Mockito.mock(IMSRawDataFile.class);
    final SimpleModularDataModel metadata = new SimpleModularDataModel();
    // MS1 limit unknown, MS2 limit known
    metadata.set(MobilityScanAbundanceLimitType.class,
        new MobilityScanAbundanceLimits(Double.NaN, LIMIT));
    Mockito.when(file.getFileMetadata()).thenReturn(metadata);

    final AdaptiveLocalMaxMassDetector fromMetadata = detector(
        new SaturationThresholdValue(SaturationThresholdOption.RAW_FILE_METADATA, 0));
    final AdaptiveLocalMaxMassDetector fixed = new AdaptiveLocalMaxMassDetector(0,
        AbundanceMeasure.Height, 3, new LocalMaxNoSmoothingModule(),
        AdaptiveLocalMaxMassDetector.SATURATION_LIMIT_FRACTION * LIMIT);
    final double unsaturatedMz = centroid(
        detector(SaturationThresholdValue.OFF).getMassValues(spectrum));

    Assertions.assertEquals(centroid(fixed.getMassValues(spectrum)), centroid(
            fromMetadata.getMobilityScanMassValues(mobilityScans(file, 2, spectrum)).getFirst()),
        1e-12);
    // no limit for MS1 -> auto, the file has no mobility scan plateau -> no saturation handling
    Assertions.assertEquals(unsaturatedMz, centroid(
            fromMetadata.getMobilityScanMassValues(mobilityScans(file, 1, spectrum)).getFirst()),
        1e-12);
    // plain spectra without raw file (import) -> no saturation handling
    Assertions.assertEquals(unsaturatedMz,
        centroid(fromMetadata.getMobilityScanMassValues(List.of(spectrum)).getFirst()), 1e-12);
  }

  /**
   * Frames are saturated if the mobility scans of the ms level have a saturation plateau, the
   * threshold is then half the highest frame base peak (not the mobility scan limit). Same rule for
   * raw file metadata and auto.
   */
  @Test
  void framesUseTheHighestFrameIfMobilityScansAreSaturated() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    // frames sum the mobility scans and reach higher intensities, no plateau of their own
    final double frameMax = 80_000;
    final IMSRawDataFile file = imsFile(plateau(49_500), frameMax, 60_000, 40_000);
    final Frame frame = spectrumMock(Frame.class, file, 1, spectrum);

    final double frameMz = centroid(
        fixed(AdaptiveLocalMaxMassDetector.SATURATION_LIMIT_FRACTION * frameMax).getMassValues(
            spectrum));
    Assertions.assertNotEquals(frameMz, centroid(
        fixed(AdaptiveLocalMaxMassDetector.SATURATION_LIMIT_FRACTION * LIMIT).getMassValues(
            spectrum)), 1e-9, "the test cannot tell the two thresholds apart");

    for (final SaturationThresholdOption option : List.of(SaturationThresholdOption.AUTO,
        SaturationThresholdOption.RAW_FILE_METADATA)) {
      Assertions.assertEquals(frameMz,
          centroid(detector(new SaturationThresholdValue(option, 0)).getMassValues(frame)), 1e-12,
          option.toString());
    }
  }

  /**
   * Without a mobility scan plateau the frames are not saturated, even with a mobility scan limit
   * in the metadata
   */
  @Test
  void framesWithoutMobilityScanPlateauAreNotSaturated() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    final double[] mobilityScanBasePeaks = new double[SaturationPlateauEstimator.MIN_PLATEAU_SCANS];
    for (int i = 0; i < mobilityScanBasePeaks.length; i++) {
      mobilityScanBasePeaks[i] = 49_500 - 1000 * i;
    }
    final IMSRawDataFile file = imsFile(mobilityScanBasePeaks, 80_000, 60_000, 40_000);
    final Frame frame = spectrumMock(Frame.class, file, 1, spectrum);
    final double unsaturatedMz = centroid(
        detector(SaturationThresholdValue.OFF).getMassValues(spectrum));

    for (final SaturationThresholdOption option : List.of(SaturationThresholdOption.AUTO,
        SaturationThresholdOption.RAW_FILE_METADATA)) {
      Assertions.assertEquals(unsaturatedMz,
          centroid(detector(new SaturationThresholdValue(option, 0)).getMassValues(frame)), 1e-12,
          option.toString());
    }
  }

  /**
   * @param mobilityScanBasePeaks base peaks of the mobility scans of one MS1 frame
   * @param frameBasePeaks        base peaks of the MS1 frames
   * @return IMS file mock with these base peaks and a mobility scan limit {@link #LIMIT} in the
   * metadata
   */
  private static @NotNull IMSRawDataFile imsFile(final double @NotNull [] mobilityScanBasePeaks,
      final double... frameBasePeaks) {
    final IMSRawDataFile file = Mockito.mock(IMSRawDataFile.class);
    final SimpleModularDataModel metadata = new SimpleModularDataModel();
    metadata.set(MobilityScanAbundanceLimitType.class,
        new MobilityScanAbundanceLimits(LIMIT, LIMIT));
    Mockito.when(file.getFileMetadata()).thenReturn(metadata);

    final List<MobilityScan> mobilityScans = new ArrayList<>();
    for (final double basePeak : mobilityScanBasePeaks) {
      final MobilityScan scan = Mockito.mock(MobilityScan.class);
      Mockito.when(scan.getBasePeakIntensity()).thenReturn(basePeak);
      mobilityScans.add(scan);
    }
    final Frame mobilityFrame = Mockito.mock(Frame.class);
    Mockito.when(mobilityFrame.getMSLevel()).thenReturn(1);
    Mockito.when(mobilityFrame.getMobilityScans()).thenReturn(mobilityScans);
    Mockito.doReturn(List.of(mobilityFrame)).when(file).getFrames();

    final ObservableList<Scan> frames = basePeakScans(1, frameBasePeaks);
    Mockito.when(file.getScans()).thenReturn(frames);
    return file;
  }

  /**
   * Auto on scans of a file without repeated maximum, also through the data access of the mass
   * detection task
   */
  @Test
  void scansWithoutPlateauAreNotSaturated() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    final double[] basePeaks = new double[SaturationPlateauEstimator.MIN_PLATEAU_SCANS * 2];
    for (int i = 0; i < basePeaks.length; i++) {
      basePeaks[i] = 50_000 - 1000 * i;
    }
    final ObservableList<Scan> scans = basePeakScans(1, basePeaks);
    Mockito.when(file.getScans()).thenReturn(scans);
    final Scan scan = spectrumMock(Scan.class, file, 1, spectrum);
    final AdaptiveLocalMaxMassDetector auto = detector(
        new SaturationThresholdValue(SaturationThresholdOption.AUTO, 0));
    final double unsaturatedMz = centroid(
        detector(SaturationThresholdValue.OFF).getMassValues(spectrum));

    Assertions.assertEquals(unsaturatedMz, centroid(auto.getMassValues(scan)), 1e-12);

    // the same file with a plateau, passed as data access like in the mass detection task
    final ObservableList<Scan> plateauScans = basePeakScans(1, plateau(50_000));
    Mockito.when(file.getScans()).thenReturn(plateauScans);
    final ScanDataAccess access = spectrumMock(ScanDataAccess.class, file, 1, spectrum);
    Mockito.when(access.getCurrentScan()).thenReturn(scan);
    Assertions.assertEquals(centroid(fixed(25_000).getMassValues(spectrum)), centroid(
        detector(new SaturationThresholdValue(SaturationThresholdOption.AUTO, 0)).getMassValues(
            access)), 1e-12);
  }

  /**
   * @return base peaks that form a saturation plateau at this intensity
   */
  private static double @NotNull [] plateau(final double intensity) {
    final double[] basePeaks = new double[SaturationPlateauEstimator.MIN_PLATEAU_SCANS];
    Arrays.fill(basePeaks, intensity);
    return basePeaks;
  }

  /**
   * @return scans of the ms level with these base peaks
   */
  private static @NotNull ObservableList<Scan> basePeakScans(final int msLevel,
      final double... basePeaks) {
    final ObservableList<Scan> scans = FXCollections.observableArrayList();
    for (final double basePeak : basePeaks) {
      final Scan scan = Mockito.mock(Scan.class);
      Mockito.when(scan.getMSLevel()).thenReturn(msLevel);
      Mockito.when(scan.getBasePeakIntensity()).thenReturn(basePeak);
      scans.add(scan);
    }
    return scans;
  }

  /**
   * Scan mock of the file and ms level that delegates the data to the spectrum
   */
  private static <T extends Scan> @NotNull T spectrumMock(@NotNull final Class<T> type,
      @NotNull final RawDataFile file, final int msLevel, @NotNull final MassSpectrum spectrum) {
    final T scan = Mockito.mock(type);
    Mockito.when(scan.getDataFile()).thenReturn(file);
    Mockito.when(scan.getMSLevel()).thenReturn(msLevel);
    Mockito.when(scan.getNumberOfDataPoints()).thenReturn(spectrum.getNumberOfDataPoints());
    Mockito.when(scan.getMzValue(Mockito.anyInt()))
        .thenAnswer(inv -> spectrum.getMzValue(inv.getArgument(0)));
    Mockito.when(scan.getIntensityValue(Mockito.anyInt()))
        .thenAnswer(inv -> spectrum.getIntensityValue(inv.getArgument(0)));
    return scan;
  }

  /**
   * Mobility scan mocks of one frame that delegate the data to the spectrum
   */
  private static @NotNull List<MobilityScan> mobilityScans(@NotNull final IMSRawDataFile file,
      final int msLevel, @NotNull final MassSpectrum spectrum) {
    final Frame frame = Mockito.mock(Frame.class);
    Mockito.when(frame.getMSLevel()).thenReturn(msLevel);
    final MobilityScan scan = spectrumMock(MobilityScan.class, file, msLevel, spectrum);
    Mockito.when(scan.getFrame()).thenReturn(frame);
    return List.of(scan);
  }

  /**
   * The preview creates a new detector for each parameter change. The new detector takes over the
   * file cache and does not scan all mobility scans of the file again.
   */
  @Test
  void takenOverFileCacheSkipsTheFileScan() {
    final SimpleMassSpectrum spectrum = clippedPeak();
    final IMSRawDataFile file = imsFile(plateau(49_500), 80_000, 60_000, 40_000);
    final Frame frame = spectrumMock(Frame.class, file, 1, spectrum);

    final AdaptiveLocalMaxMassDetector first = detector(
        new SaturationThresholdValue(SaturationThresholdOption.AUTO, 0));
    final double expectedMz = centroid(first.getMassValues(frame));

    final AdaptiveLocalMaxMassDetector sameOption = detector(
        new SaturationThresholdValue(SaturationThresholdOption.AUTO, 0));
    sameOption.takeOverFileCache(first);
    Assertions.assertEquals(expectedMz, centroid(sameOption.getMassValues(frame)), 1e-12);
    Mockito.verify(file, Mockito.times(1)).getScans();

    // other option: the file thresholds are resolved again, the mobility scan plateau is reused
    final AdaptiveLocalMaxMassDetector otherOption = detector(
        new SaturationThresholdValue(SaturationThresholdOption.RAW_FILE_METADATA, 0));
    otherOption.takeOverFileCache(sameOption);
    Assertions.assertEquals(expectedMz, centroid(otherOption.getMassValues(frame)), 1e-12);
    Mockito.verify(file, Mockito.times(2)).getScans();
    Mockito.verify(file, Mockito.times(1)).getFrames();
  }

  @Test
  void plateauNeedsRepeatedMaximum() {
    final List<Double> values = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      values.add(1000d + i);
    }
    // single highest point -> no plateau
    values.add(40_000d);
    Assertions.assertTrue(Double.isNaN(SaturationPlateauEstimator.estimatePlateau(
        values.stream().mapToDouble(Double::doubleValue).toArray())));

    // clipped points close to the ceiling
    for (int i = 0; i < SaturationPlateauEstimator.MIN_PLATEAU_SCANS; i++) {
      values.add(49_500d - i * 10);
    }
    Assertions.assertEquals(49_500d, SaturationPlateauEstimator.estimatePlateau(
        values.stream().mapToDouble(Double::doubleValue).toArray()));
  }

  @Test
  void restrictedOptionsSurviveCloneAndRejectHiddenValues() {
    final SaturationThresholdParameter full = new SaturationThresholdParameter();
    Assertions.assertEquals(SaturationThresholdParameter.ALL_OPTIONS, full.getOptions());

    final SaturationThresholdParameter restricted = full.withOptions(
        SaturationThresholdParameter.IMPORT_OPTIONS);
    Assertions.assertEquals(SaturationThresholdParameter.IMPORT_OPTIONS,
        restricted.cloneParameter().getOptions());

    restricted.setValue(
        new SaturationThresholdValue(SaturationThresholdOption.RAW_FILE_METADATA, 0));
    Assertions.assertEquals(SaturationThresholdOption.OFF, restricted.getValue().option());
    restricted.setValue(new SaturationThresholdValue(SaturationThresholdOption.MANUAL, 1234));
    Assertions.assertEquals(SaturationThresholdOption.MANUAL, restricted.getValue().option());
    Assertions.assertEquals(1234, restricted.cloneParameter().getValue().manual());
  }

  @Test
  void importParametersOnlyOfferImportOptions() {
    final AdaptiveLocalMaxImportParameters params = (AdaptiveLocalMaxImportParameters) new AdaptiveLocalMaxImportParameters().cloneParameterSet();
    Assertions.assertEquals(SaturationThresholdParameter.IMPORT_OPTIONS,
        params.getParameter(AdaptiveLocalMaxMassDetectorParameters.saturationThreshold)
            .getOptions());
    final AdaptiveLocalMaxMassDetectorParameters full = (AdaptiveLocalMaxMassDetectorParameters) new AdaptiveLocalMaxMassDetectorParameters().cloneParameterSet();
    Assertions.assertEquals(SaturationThresholdParameter.ALL_OPTIONS,
        full.getParameter(AdaptiveLocalMaxMassDetectorParameters.saturationThreshold).getOptions());
  }

  @Test
  void abundanceLimitsSaveString() {
    final MobilityScanAbundanceLimits limits = new MobilityScanAbundanceLimits(LIMIT, Double.NaN);
    Assertions.assertEquals(limits, MobilityScanAbundanceLimits.parse(limits.toSaveString()));
    Assertions.assertEquals(LIMIT, limits.forMsLevel(1));
    Assertions.assertTrue(Double.isNaN(limits.forMsLevel(2)));
    Assertions.assertEquals(new MobilityScanAbundanceLimits(LIMIT, 1d), limits.withMsLevel(2, 1));
  }
}
