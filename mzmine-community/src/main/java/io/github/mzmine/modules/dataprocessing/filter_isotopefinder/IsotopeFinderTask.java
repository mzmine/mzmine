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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.CrossScanRefiner;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.PatternAnchor;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.RatioAggregation;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Detects isotope patterns and charge states per feature. Starts at the feature m/z, searches the
 * most intense MS1 (or best mobility) scan bidirectionally, selects the most probable charge via the
 * {@link IsotopeFinderEngine}, and optionally refines the pattern across the scans within the
 * feature FWHM.
 */
class IsotopeFinderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IsotopeFinderTask.class.getName());
  private final ModularFeatureList featureList;

  // parameter values
  private final ParameterSet parameters;
  // only used to report which elements were searched when nothing could be found
  private final String isotopes;

  private final IsotopeFinderEngine engine;

  // FWHM cross-scan refinement
  private final boolean fwhmRefineEnabled;
  private final MZTolerance refineMzTolerance;
  private final RatioAggregation ratioAggregation;
  private final int minScansPresent;

  private final AtomicLong processedRows = new AtomicLong(0);
  private long totalRows;
  // guard against log spam: the charge/pattern disagreement below is a bug signal, one line is enough
  private final AtomicBoolean chargeMismatchLogged = new AtomicBoolean(false);
  // first error of any sample thread, stops all other threads and errors the task
  private final AtomicReference<String> errorMessage = new AtomicReference<>();

  /**
   * @param parameters    the top-level {@link IsotopeFinderParameters}, only stored as the applied
   *                      method of the feature list.
   * @param algo          the full detection setup this task runs with.
   * @param algorithmName name of the algorithm that created this task, only used for reporting.
   */
  IsotopeFinderTask(@NotNull MZmineProject project, @NotNull ModularFeatureList featureList,
      @NotNull ParameterSet parameters, @NotNull CarbonAveragineAlgorithmParameters algo,
      @NotNull String algorithmName, @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureList;
    this.parameters = parameters;

    final List<Element> isotopeElements = algo.getValue(
        CarbonAveragineAlgorithmParameters.elements);
    isotopes = isotopeElements.stream().map(Objects::toString).collect(Collectors.joining(","));

    // build the detection engine (envelope model, charge scoring, element auto-detection) from the
    // algorithm parameters
    this.engine = IsotopeFinderEngineFactory.create(algo, algorithmName);

    // FWHM refinement parameters
    this.fwhmRefineEnabled = algo.getValue(CarbonAveragineAlgorithmParameters.fwhmRefine);
    final ParameterSet refineParams = algo.getParameter(
        CarbonAveragineAlgorithmParameters.fwhmRefine).getEmbeddedParameters();
    this.refineMzTolerance = refineParams.getValue(FwhmRefineParameters.refineMzTolerance);
    this.ratioAggregation = refineParams.getValue(FwhmRefineParameters.ratioAggregation);
    this.minScansPresent = refineParams.getValue(FwhmRefineParameters.minScansPresent);
  }

  @Override
  public String getTaskDescription() {
    return "Isotope pattern finder on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0d;
    }
    return (double) processedRows.get() / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running isotope pattern finder on " + featureList);

    if (!engine.hasIsotopeDiffs()) {
      setErrorMessage("No isotopes found for elements: " + isotopes);
      setStatus(TaskStatus.ERROR);
      return;
    }

    // start processing, all samples are processed in parallel
    final List<RawDataFile> raws = featureList.getRawDataFiles();
    totalRows = (long) featureList.getNumberOfRows() * raws.size();
    processedRows.set(0);

    final long detected = raws.parallelStream().mapToLong(this::processRawDataFile).sum();

    if (isCanceled()) {
      return;
    }
    final String error = errorMessage.get();
    if (error != null) {
      setErrorMessage(error);
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (detected > 0) {
      logger.info(String.format("Found %d isotope pattern in %s", detected, featureList));
    }
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotope finder module", IsotopeFinderModule.class,
            parameters, getModuleCallDate()));

    logger.info("Finished isotope pattern finder on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Detects isotope patterns for all features of a single sample. Runs on its own thread with its
   * own data access, so nothing here may be shared with other samples.
   *
   * @return the number of features with a detected isotope pattern.
   */
  private long processRawDataFile(@NotNull final RawDataFile raw) {
    final ScanDataAccess scans = EfficientDataAccess.of(raw, ScanDataType.MASS_LIST,
        featureList.getSeletedScans(raw));
    final MobilityScanDataAccess mobScans = initMobilityScanDataAccess(raw);

    long detected = 0;

    try {
      for (FeatureListRow row : featureList.getRows()) {
        if (isCanceled() || errorMessage.get() != null) {
          return detected;
        }

        final Feature feature = row.getFeature(raw);
        if (feature == null || feature.getRepresentativeScan() == null) {
          processedRows.incrementAndGet();
          continue;
        }

        final double mz = feature.getMZ();
        final Float heightValue = feature.getHeight();
        final double height = heightValue == null ? 0d : heightValue;
        PolarityType polarity = feature.getRepresentativePolarity();
        if (polarity == null) {
          polarity = PolarityType.UNKNOWN;
        }

        Scan spectrum = findBestScanOrMobilityScan(scans, mobScans, feature);
        DetectionResult result = engine.detect(spectrum, mz, height, polarity);
        if (result == null && mobScans != null) {
          // for IMS features, do a second attempt in the frame if nothing was found in mobility
          spectrum = findBestScanOrMobilityScan(scans, null, feature);
          result = engine.detect(spectrum, mz, height, polarity);
        }
        if (result == null) {
          processedRows.incrementAndGet();
          continue;
        }

        List<IsotopePattern> patterns = result.patterns();
        // refine across FWHM scans (LC-MS only for now)
        if (fwhmRefineEnabled && feature.getMobility() == null) {
          final List<MassSpectrum> fwhmScans = collectFwhmMassLists(feature);
          if (fwhmScans.size() > 1) {
            final List<IsotopePattern> refined = new ArrayList<>(patterns.size());
            final List<PatternAnchor> anchors = result.anchors();
            for (int i = 0; i < patterns.size(); i++) {
              // the anchor lets the refiner reject recovered offsets the envelope does not predict
              final PatternAnchor anchor = i < anchors.size() ? anchors.get(i) : null;
              refined.add(CrossScanRefiner.refine(patterns.get(i), fwhmScans, refineMzTolerance,
                  ratioAggregation, minScansPresent, anchor));
            }
            patterns = refined;
          }
        }

        final IsotopePattern assembled = IsotopeFinderEngine.assemble(patterns);
        // the feature charge and the preferred pattern's charge must agree - downstream consumers
        // (formula prediction, CCS) read one or the other. Log instead of throwing so a single odd
        // feature cannot abort the whole run.
        if (assembled.getCharge() != result.bestCharge() && chargeMismatchLogged.compareAndSet(false,
            true)) {
          logger.warning(String.format(
              "Isotope finder: preferred pattern charge %d disagrees with the selected charge %d "
                  + "(feature m/z %.4f). Further occurrences are not logged.", assembled.getCharge(),
              result.bestCharge(), mz));
        }
        feature.setIsotopePattern(assembled);
        feature.setCharge(result.bestCharge());

        // CCS calculation for IMS features using the selected charge
        final RawDataFile data = feature.getRawDataFile();
        final Float mobility = feature.getMobility();
        final MobilityType mobilityType = feature.getMobilityUnit();
        if (data instanceof IMSRawDataFile imsfile && CCSUtils.hasValidMobilityType(imsfile)
            && mobility != null && result.bestCharge() > 0 && mobilityType != null) {
          final Float ccs = CCSUtils.calcCCS(mz, mobility, mobilityType, result.bestCharge(),
              imsfile);
          if (ccs != null) {
            feature.setCCS(ccs);
          }
        }
        detected++;
        processedRows.incrementAndGet();
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error in isotope finder " + ex.getMessage(), ex);
      // only keep the first error, all other sample threads stop on it
      errorMessage.compareAndSet(null,
          "Error in isotope finder on %s: %s".formatted(raw.getName(), ex.getMessage()));
    }
    return detected;
  }

  /**
   * @return the mass lists of all scans of the feature within +/- FWHM/2 of the apex RT (or all
   * feature scans if no FWHM is available).
   */
  private @NotNull List<MassSpectrum> collectFwhmMassLists(@NotNull final Feature feature) {
    final List<MassSpectrum> result = new ArrayList<>();
    final Float rt = feature.getRT();
    if (rt == null) {
      return result;
    }
    final Float fwhm = feature.getFWHM();
    final float halfWidth = fwhm != null ? fwhm / 2f : Float.MAX_VALUE;
    for (final Scan scan : feature.getScanNumbers()) {
      if (scan == null || Math.abs(scan.getRetentionTime() - rt) > halfWidth) {
        continue;
      }
      final MassList massList = scan.getMassList();
      if (massList != null && massList.getNumberOfDataPoints() > 0) {
        result.add(massList);
      }
    }
    return result;
  }

  @NotNull
  private Scan findBestScanOrMobilityScan(ScanDataAccess scans,
      @Nullable MobilityScanDataAccess mobScans, @NotNull Feature feature) {

    final Scan maxScan = feature.getRepresentativeScan();
    final int scanIndex = scans.indexOf(maxScan);
    scans.jumpToIndex(scanIndex);

    final boolean mobility = feature.getMobility() != null;
    MobilityScan mobilityScan = null;
    if (mobility && mobScans != null) {
      final MobilityScan bestMobilityScan = IonMobilityUtils.getBestMobilityScan(feature);
      if (bestMobilityScan != null) {
        mobilityScan = mobScans.jumpToMobilityScan(bestMobilityScan);
      }
    }

    return mobilityScan != null ? mobScans : scans;
  }

  @Nullable
  private MobilityScanDataAccess initMobilityScanDataAccess(RawDataFile raw) {
    return
        raw instanceof IMSRawDataFile imsFile && featureList.hasFeatureType(MobilityUnitType.class)
            ? new MobilityScanDataAccess(imsFile, MobilityScanDataType.MASS_LIST,
            (List<Frame>) featureList.getSeletedScans(imsFile)) : null;
  }
}
