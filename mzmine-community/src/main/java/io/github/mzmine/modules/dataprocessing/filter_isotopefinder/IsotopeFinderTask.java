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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
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
import io.github.mzmine.util.scans.SpectraMerging;
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
 * Detects isotope patterns and charge states per feature. Starts at the feature m/z, searches one
 * spectrum bidirectionally, selects the most probable charge via the {@link IsotopeFinderEngine},
 * and optionally refines the pattern across the scans within the feature FWHM.
 * <p>
 * The searched spectrum is the most intense MS1 scan, or, for ion mobility features, the mobility
 * scans within the mobility FWHM merged into one mobility resolved spectrum - see
 * {@link #mergeMobilityFwhmSpectrum}.
 */
class IsotopeFinderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IsotopeFinderTask.class.getName());

  /**
   * The retention time scope IMS features are merged over. Not a user parameter - flip this to
   * compare the two on real data.
   */
  static final ImsMergeScope IMS_MERGE_SCOPE = ImsMergeScope.RT_FWHM;

  /**
   * Half width of the m/z window the IMS merge is restricted to, around the feature m/z. Generous
   * on purpose: the pattern search walks outwards from the feature signal as long as it keeps
   * finding plausible isotope spacings, so this must comfortably exceed the widest pattern to be
   * found.
   */
  private static final double IMS_MERGE_MZ_WINDOW_DA = 50d;

  private final ModularFeatureList featureList;

  private final ParameterSet parameters;
  // only used to report which elements were searched when nothing could be found
  private final String isotopes;

  private final IsotopeFinderEngine engine;

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
   * @param parameters    the top-level set, stored only as the feature list's applied method.
   * @param algo          the full detection setup this task runs with.
   * @param algorithmName only used for reporting.
   */
  IsotopeFinderTask(@NotNull MZmineProject project, @NotNull ModularFeatureList featureList,
      @NotNull ParameterSet parameters, @NotNull CarbonModelAlgorithmParameters algo,
      @NotNull String algorithmName, @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureList;
    this.parameters = parameters;

    final List<Element> isotopeElements = algo.getValue(CarbonModelAlgorithmParameters.elements);
    isotopes = isotopeElements.stream().map(Objects::toString).collect(Collectors.joining(","));

    // envelope model, charge scoring and element auto-detection all come from the algo parameters
    this.engine = IsotopeFinderEngineFactory.create(algo, algorithmName);

    this.fwhmRefineEnabled = algo.getValue(CarbonModelAlgorithmParameters.fwhmRefine);
    final ParameterSet refineParams = algo.getParameter(CarbonModelAlgorithmParameters.fwhmRefine)
        .getEmbeddedParameters();
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
   * Detects the patterns of ONE sample, on its own thread with its own data access - nothing here
   * may be shared with another sample.
   *
   * @return the number of features with a detected isotope pattern.
   */
  private long processRawDataFile(@NotNull final RawDataFile raw) {
    final ScanDataAccess scans = EfficientDataAccess.of(raw, ScanDataType.MASS_LIST,
        featureList.getSeletedScans(raw));
    final boolean imsEnabled = raw instanceof IMSRawDataFile && featureList.hasFeatureType(
        io.github.mzmine.datamodel.features.types.numbers.MobilityType.class);

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

        // IMS features are searched in a mobility resolved spectrum merged over the mobility FWHM,
        // whose intensities cover only a part of the feature -> normalize back to the feature height
        final MassSpectrum mergedMobility =
            imsEnabled ? mergeMobilityFwhmSpectrum(feature, mz) : null;
        final MassSpectrum spectrum =
            mergedMobility != null ? mergedMobility : positionScanAccess(scans, feature);
        final DetectionResult result = engine.detect(spectrum, mz, height, polarity,
            mergedMobility != null);
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
        // these must agree: downstream consumers (formula prediction, CCS) read one or the other.
        // Logged rather than thrown so one odd feature cannot abort the run.
        if (assembled.getCharge() != result.bestCharge() && chargeMismatchLogged.compareAndSet(
            false, true)) {
          logger.warning(String.format(
              "Isotope finder: preferred pattern charge %d disagrees with the selected charge %d "
                  + "(feature m/z %.4f). Further occurrences are not logged.",
              assembled.getCharge(), result.bestCharge(), mz));
        }
        feature.setIsotopePattern(assembled);
        feature.setCharge(result.bestCharge());

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
      errorMessage.compareAndSet(null,
          "Error in isotope finder on %s: %s".formatted(raw.getName(), ex.getMessage()));
    }
    return detected;
  }

  /**
   * @return the mass lists within +/- FWHM/2 of the apex RT, or all feature scans if no FWHM is
   * available.
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

  /**
   * @return the data access, positioned on the feature's representative (apex) scan. For IMS this
   * is the apex frame, which is NOT mobility resolved - only used when no mobility merged spectrum
   * can be built.
   */
  @NotNull
  private Scan positionScanAccess(@NotNull ScanDataAccess scans, @NotNull Feature feature) {
    scans.jumpToIndex(scans.indexOf(feature.getRepresentativeScan()));
    return scans;
  }

  /**
   * The mobility resolved spectrum an IMS feature is searched in: the mass lists of the feature's
   * mobility scans within the mobility FWHM, merged over {@link #IMS_MERGE_SCOPE} frames.
   * <p>
   * A single mobility scan carries too little signal for weak M+1/M+2 peaks to clear mass
   * detection, and the frame spectrum is not mobility resolved, so it drags in every co-eluting
   * ion. Merging the FWHM keeps the mobility selectivity and recovers the counting statistics.
   *
   * @param mz the feature m/z, the merge is restricted to a window around it - see
   *           {@link #IMS_MERGE_MZ_WINDOW_DA}.
   * @return the merged spectrum, or null if the feature is not ion mobility data or none of its
   * mobility scans has a mass list.
   */
  @Nullable
  private MassSpectrum mergeMobilityFwhmSpectrum(@NotNull final Feature feature, final double mz) {
    if (feature.getMobility() == null
        || !(feature.getFeatureData() instanceof IonMobilogramTimeSeries series)) {
      return null;
    }

    // no FWHM (e.g. a mobilogram of one or two points) -> merge everything the feature covers.
    // extractSummedMobilityScanFromMassLists skips mobility scans without feature intensity, so the
    // full range still only contains scans this feature is actually present in.
    final Range<Float> mobilityRange = Objects.requireNonNullElse(
        IonMobilityUtils.getMobilityFWHM(series.getSummedMobilogram()), Range.all());

    final Range<Float> rtRange = switch (IMS_MERGE_SCOPE) {
      case APEX_FRAME -> {
        final Scan apex = feature.getRepresentativeScan();
        // a closed range on the exact apex RT, so only that frame's mobility scans pass
        yield apex == null ? Range.all() : Range.singleton(apex.getRetentionTime());
      }
      case RT_FWHM -> {
        final Float rt = feature.getRT();
        final Float fwhm = feature.getFWHM();
        yield rt == null || fwhm == null ? Range.all()
            : Range.closed(rt - fwhm / 2f, rt + fwhm / 2f);
      }
    };

    // the engine only reads around the feature m/z, and merging is by far the most expensive step
    // per feature, so hand it only that window
    final Range<Double> mzRange = Range.closed(mz - IMS_MERGE_MZ_WINDOW_DA,
        mz + IMS_MERGE_MZ_WINDOW_DA);

    // no memory map storage: the merged spectrum is per feature scratch data, not kept anywhere
    return SpectraMerging.extractSummedMobilityScanFromMassLists(feature,
        SpectraMerging.defaultMs1MergeTol, mobilityRange, rtRange, mzRange, null);
  }
}
