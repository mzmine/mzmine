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

package io.github.mzmine.modules.dataprocessing.id_camera;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A task to perform a CAMERA search.
 *
 */
@Deprecated
public class CameraSearchTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(CameraSearchTask.class.getName());

  // Required version of CAMERA.
  private static final String CAMERA_VERSION = "1.12";

  // Minutes to seconds conversion factor.
  private static final double SECONDS_PER_MINUTE = 60.0;

  // The MS-level processed by this module.
  private static final int MS_LEVEL = 1;

  // Isotope regular expression.
  private static final Pattern ISOTOPE_PATTERN = Pattern.compile("\\[\\d+\\](.*)");

  // Peak signal to noise ratio.
  private static final double SIGNAL_TO_NOISE = 10.0;

  // Data point sorter.
  private static final DataPointSorter ASCENDING_MASS_SORTER =
      new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);

  // Feature list to process.
  private final FeatureList peakList;

  // Task progress.
  private double progress;
  // Project
  MZmineProject project;

  // R session.
  private RSessionWrapper rSession;
  private String errorMsg;
  private boolean userCanceled;

  // Parameters.
  private final ParameterSet parameters;
  private final Double fwhmSigma;
  private final Double fwhmPercentage;
  private final Integer isoMaxCharge;
  private final Integer isoMaxCount;
  private final MZTolerance isoMassTolerance;
  private final Double corrThreshold;
  private final Double corrPValue;
  private final String polarity;
  private final Boolean calcIso;
  private final String groupBy;
  private final Boolean includeSingletons;

  private REngineType rEngineType;

  public CameraSearchTask(final MZmineProject project, final ParameterSet parameters,
      final FeatureList list, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    peakList = list;
    progress = 0.0;

    this.project = project;

    // Parameters.
    this.parameters = parameters;

    this.rEngineType = parameters.getParameter(CameraSearchParameters.RENGINE_TYPE).getValue();

    fwhmSigma = parameters.getParameter(CameraSearchParameters.FWHM_SIGMA).getValue();
    fwhmPercentage = parameters.getParameter(CameraSearchParameters.FWHM_PERCENTAGE).getValue();
    isoMaxCharge = parameters.getParameter(CameraSearchParameters.ISOTOPES_MAX_CHARGE).getValue();
    isoMaxCount = parameters.getParameter(CameraSearchParameters.ISOTOPES_MAXIMUM).getValue();
    isoMassTolerance =
        parameters.getParameter(CameraSearchParameters.ISOTOPES_MZ_TOLERANCE).getValue();
    corrThreshold =
        parameters.getParameter(CameraSearchParameters.CORRELATION_THRESHOLD).getValue();
    corrPValue = parameters.getParameter(CameraSearchParameters.CORRELATION_P_VALUE).getValue();
    polarity = parameters.getParameter(CameraSearchParameters.POLARITY).getValue().toString();
    calcIso = parameters.getParameter(CameraSearchParameters.DONT_SPLIT_ISOTOPES).getValue();
    groupBy = parameters.getParameter(CameraSearchParameters.GROUP_BY).getValue();
    includeSingletons =
        parameters.getParameter(CameraSearchParameters.INCLUDE_SINGLETONS).getValue();

    this.userCanceled = false;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of pseudo-spectra in " + peakList;
  }

  @Override
  public double getFinishedPercentage() {

    return progress;
  }

  @Override
  public void run() {

    try {

      setStatus(TaskStatus.PROCESSING);

      // Check number of raw data files.
      if (peakList.getNumberOfRawDataFiles() != 1) {

        throw new IllegalStateException(
            "CAMERA can only process feature lists for a single raw data file, i.e. non-aligned feature lists.");
      }

      // Run the search.
      cameraSearch(peakList.getRawDataFile(0));

      // Create new list with IsotopePattern information
      FeatureList newPeakList = null;

      if (parameters.getParameter(CameraSearchParameters.CREATE_NEW_LIST).getValue()) {
        switch (groupBy) {
          case CameraSearchParameters.GROUP_BY_PCGROUP:
            newPeakList = groupPeaksByPCGroup(peakList);
            break;
          default:
            newPeakList = groupPeaksByIsotope(peakList);
        }
      }

      if (!isCanceled()) {

        if (newPeakList != null) {
          project.addFeatureList(newPeakList);

          //QualityParameters.calculateQualityParameters(newPeakList);
        }

        // Finished.
        setStatus(TaskStatus.FINISHED);
        logger.info("CAMERA Search completed");
      }

      // Repaint the window to reflect the change in the feature list
      Desktop desktop = MZmineCore.getDesktop();
      // if (!(desktop instanceof HeadLessDesktop))
      // desktop.getMainWindow().repaint();

    } catch (Throwable t) {

      logger.log(Level.SEVERE, "CAMERA Search error", t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * Perform CAMERA search.
   *
   * @param rawFile raw data file of feature list to process.
   */
  private void cameraSearch(final RawDataFile rawFile) {

    logger.finest("Detecting peaks.");

    errorMsg = null;
    try {

      String[] reqPackages = {"CAMERA"};
      String[] reqPackagesVersions = {CAMERA_VERSION};
      this.rSession = new RSessionWrapper(this.rEngineType, "Camera search feature", reqPackages,
          reqPackagesVersions);
      this.rSession.open();

      // Create empty peaks matrix.
      this.rSession.eval(
          "columnHeadings <- c('mz','mzmin','mzmax','rt','rtmin','rtmax','into','intb','maxo','sn')");
      this.rSession.eval("peaks <- matrix(nrow=0, ncol=length(columnHeadings))");
      this.rSession.eval("colnames(peaks) <- columnHeadings");

      // Initialize.
      final Feature[] peaks = peakList.getFeatures(rawFile).toArray(Feature[]::new);
      progress = 0.0;

      // Initialize scan map.
      final Map<Scan, Set<DataPoint>> peakDataPointsByScan =
          new HashMap<Scan, Set<DataPoint>>(rawFile.getNumOfScans(MS_LEVEL));
      int dataPointCount = 0;
      for (final Scan scan : rawFile.getScanNumbers(MS_LEVEL)) {

        // Create a set to hold data points (sorted by m/z).
        final Set<DataPoint> dataPoints = new TreeSet<DataPoint>(ASCENDING_MASS_SORTER);

        // Add a dummy data point.
        dataPoints.add(new SimpleDataPoint(0.0, 0.0));
        dataPointCount++;

        // Map the set.
        peakDataPointsByScan.put(scan, dataPoints);
      }

      // Add peaks.
      // 80 percents for building peaks list.
      double progressInc = 0.8 / peaks.length;
      for (final Feature peak : peaks) {

        // Get peak data.
        Range<Float> rtRange = null;
        Range<Double> intRange = null;
        final double mz = peak.getMZ();

        // Get the peak's data points per scan.
        for (int i=0; i<peak.getNumberOfDataPoints(); i++){
          Scan scan = peak.getScanAtIndex(i);
          if (scan.getMSLevel() != MS_LEVEL) {
            throw new IllegalStateException(
                "CAMERA can only process feature lists from MS-level " + MS_LEVEL);
          }

          // Copy the data point.
          final DataPoint dataPoint = peak.getDataPointAtIndex(i);
          if (dataPoint != null) {

            final double intensity = dataPoint.getIntensity();
            peakDataPointsByScan.get(scan).add(new SimpleDataPoint(mz, intensity));
            dataPointCount++;

            // Update RT & intensity range.
            final float rt = scan.getRetentionTime();
            if (rtRange == null) {
              rtRange = Range.singleton(rt);
              intRange = Range.singleton(intensity);
            } else {
              rtRange = rtRange.span(Range.singleton(rt));
              intRange = intRange.span(Range.singleton(intensity));
            }

          }
        }

        // Set peak values.
        final double area = peak.getArea();
        final double maxo = intRange == null ? peak.getHeight() : intRange.upperEndpoint();
        final float rtMin =
            (rtRange == null ? peak.getRawDataPointsRTRange() : rtRange).lowerEndpoint();
        final float rtMax =
            (rtRange == null ? peak.getRawDataPointsRTRange() : rtRange).upperEndpoint();

        // Add peak row.
        this.rSession.eval("peaks <- rbind(peaks, c(" + mz + ", " // mz
            + mz + ", " // mzmin: use the same as mz.
            + mz + ", " // mzmax: use the same as mz.
            + peak.getRT() + ", " // rt
            + rtMin + ", " // rtmin
            + rtMax + ", " // rtmax
            + area + ", " // into: peak area.
            + area + ", " // intb: doesn't affect result, use area.
            + maxo + ", " // maxo
            + SIGNAL_TO_NOISE + "))", false);

        progress += progressInc;
      }

      // 20 percents (5*4) for building pseudo-isotopes groups.
      progressInc = 0.05;

      // Create R vectors.
      final int scanCount = peakDataPointsByScan.size();
      final double[] scanTimes = new double[scanCount];
      final int[] scanIndices = new int[scanCount];
      final double[] masses = new double[dataPointCount];
      final double[] intensities = new double[dataPointCount];

      // Fill vectors.
      int scanIndex = 0;
      int pointIndex = 0;
      for (final Scan scan : rawFile.getScanNumbers(MS_LEVEL)) {
        scanTimes[scanIndex] = scan.getRetentionTime();
        scanIndices[scanIndex] = pointIndex + 1;
        scanIndex++;

        for (final DataPoint dataPoint : peakDataPointsByScan.get(scan)) {
          masses[pointIndex] = dataPoint.getMZ();
          intensities[pointIndex] = dataPoint.getIntensity();
          pointIndex++;
        }
      }

      // Set vectors.
      this.rSession.assign("scantime", scanTimes);
      this.rSession.assign("scanindex", scanIndices);
      this.rSession.assign("mass", masses);
      this.rSession.assign("intensity", intensities);

      // Construct xcmsRaw object
      this.rSession.eval("xRaw <- new(\"xcmsRaw\")");
      this.rSession.eval("xRaw@tic <- intensity");
      this.rSession.eval("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE);
      this.rSession.eval("xRaw@scanindex <- as.integer(scanindex)");
      this.rSession.eval("xRaw@env$mz <- mass");
      this.rSession.eval("xRaw@env$intensity <- intensity");

      // Create the xcmsSet object.
      this.rSession.eval("xs <- new(\"xcmsSet\")");

      // Set peaks.
      this.rSession.eval("xs@peaks <- peaks");

      // Set file (dummy) file path.
      this.rSession.eval("xs@filepaths  <- ''");

      // Set sample name.
      this.rSession.assign("sampleName", peakList.getName());
      this.rSession.eval("sampnames(xs) <- sampleName");

      // Create an empty xsAnnotate.
      this.rSession.eval("an <- xsAnnotate(xs, sample=1)");

      // Group by RT.
      this.rSession
          .eval("an <- groupFWHM(an, sigma=" + fwhmSigma + ", perfwhm=" + fwhmPercentage + ')');
      progress += progressInc;

      // this.rSession.eval("write.csv(getPeaklist(an),
      // file='/Users/aleksandrsmirnov/table.csv',
      // row.names=FALSE)");

      switch (parameters.getParameter(CameraSearchParameters.ORDER).getValue()) {
        case CameraSearchParameters.GROUP_CORR_FIRST:
          // Split groups by correlating peak shape (need to set xraw to
          // raw
          // data).
          this.rSession.eval("an <- groupCorr(an, calcIso=FALSE, xraw=xRaw, cor_eic_th="
              + corrThreshold + ", pval=" + corrPValue + ')');
          progress += progressInc;

          // Identify isotopes.
          this.rSession.eval("an <- findIsotopes(an, maxcharge=" + isoMaxCharge + ", maxiso="
              + isoMaxCount + ", ppm=" + isoMassTolerance.getPpmTolerance() + ", mzabs="
              + isoMassTolerance.getMzTolerance() + ')');

          progress += progressInc;
          break;

        default: // case CameraSearchParameters.FIND_ISOTOPES_FIRST
          // Identify isotopes.
          this.rSession.eval("an <- findIsotopes(an, maxcharge=" + isoMaxCharge + ", maxiso="
              + isoMaxCount + ", ppm=" + isoMassTolerance.getPpmTolerance() + ", mzabs="
              + isoMassTolerance.getMzTolerance() + ')');
          // this.rSession.eval("write.csv(getPeaklist(an),
          // file='/Users/aleksandrsmirnov/test_camera.csv')");
          progress += progressInc;

          // Split groups by correlating peak shape (need to set xraw to
          // raw
          // data).
          this.rSession.eval("an <- groupCorr(an, calcIso=" + String.valueOf(calcIso).toUpperCase()
              + ", xraw=xRaw, cor_eic_th=" + corrThreshold + ", pval=" + corrPValue + ')');
          progress += progressInc;
      }

      this.rSession.eval("an <- findAdducts(an, polarity='" + polarity + "')");

      // Get the feature list.
      this.rSession.eval("peakList <- getPeaklist(an)");

      // Extract the pseudo-spectra and isotope annotations from the peak
      // list.
      rSession.eval("pcgroup <- as.integer(peakList$pcgroup)");
      rSession.eval("isotopes <- peakList$isotopes");

      rSession.eval("adducts <- peakList$adduct");
      final int[] spectra = (int[]) rSession.collect("pcgroup");
      final String[] isotopes = (String[]) rSession.collect("isotopes");
      final String[] adducts = (String[]) rSession.collect("adducts");
      // Done: Refresh R code stack
      this.rSession.clearCode();

      // Add identities.
      if (spectra != null) {

        addPseudoSpectraIdentities(peaks, spectra, isotopes, adducts);
      }
      progress += progressInc;
      // Turn off R instance, once task ended gracefully.
      if (!this.userCanceled)
        this.rSession.close(false);

    } catch (RSessionWrapperException e) {
      if (!this.userCanceled) {
        errorMsg = "'R computing error' during CAMERA search. \n" + e.getMessage();
        e.printStackTrace();
      }
    } catch (Exception e) {
      if (!this.userCanceled) {
        errorMsg = "'Unknown error' during CAMERA search. \n" + e.getMessage();
        e.printStackTrace();
      }
    }

    // Turn off R instance, once task ended UNgracefully.
    try {
      if (!this.userCanceled)
        this.rSession.close(this.userCanceled);
    } catch (RSessionWrapperException e) {
      if (!this.userCanceled) {
        // Do not override potential previous error message.
        if (errorMsg == null) {
          errorMsg = e.getMessage();
        }
      } else {
        // User canceled: Silent.
      }
    }

    // Report error.
    if (errorMsg != null) {
      setErrorMessage(errorMsg);
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * Add pseudo-spectra identities.
   *
   * @param peaks peaks to annotate with identities.
   * @param spectra the pseudo-spectra ids vector.
   * @param isotopes the isotopes vector.
   */
  private void addPseudoSpectraIdentities(final Feature[] peaks, final int[] spectra,
      final String[] isotopes, final String[] adducts) {

    // Add identities for each peak.
    int peakIndex = 0;
    for (final Feature peak : peaks) {

      // Create pseudo-spectrum identity
      final SimpleFeatureIdentity identity =
          new SimpleFeatureIdentity("Pseudo-spectrum #" + String.format("%03d", spectra[peakIndex]));
      identity.setPropertyValue(FeatureIdentity.PROPERTY_METHOD, "Bioconductor CAMERA");

      // Add isotope info, if any.
      if (isotopes != null) {

        final String isotope = isotopes[peakIndex].trim();
        if (isotope.length() > 0) {

          // Parse the isotope pattern.
          final Matcher matcher = ISOTOPE_PATTERN.matcher(isotope);
          if (matcher.matches()) {

            // identity.setPropertyValue("Isotope",
            // matcher.group(1));
            identity.setPropertyValue("Isotope", isotope);

          } else {

            logger.warning("Irregular isotope value: " + isotope);
          }
        }
      }

      if (adducts != null) {
        final String adduct = adducts[peakIndex].trim();
        if (adduct.length() > 0)
          identity.setPropertyValue("Adduct", adduct);
      }

      // Add identity to peak's row.
      FeatureListRow row = peakList.getFeatureRow(peak);
      for (FeatureIdentity peakIdentity : row.getPeakIdentities())
        row.removeFeatureIdentity(peakIdentity);
      peakList.getFeatureRow(peak).addFeatureIdentity(identity, true);
      peakIndex++;
    }
  }

  @Override
  public void cancel() {

    this.userCanceled = true;

    super.cancel();

    // Turn off R instance, if already existing.
    try {
      if (this.rSession != null)
        this.rSession.close(true);
    } catch (RSessionWrapperException e) {
      // Silent, always...
    }
  }

  /**
   * Uses Isotope-field in PeakIdentity to group isotopes and build spectrum
   *
   * @param peakList PeakList object
   * @return new PeakList object
   */

  private FeatureList groupPeaksByIsotope(FeatureList peakList) {
    // Create new feature list.
    final FeatureList combinedPeakList = new ModularFeatureList(
        peakList + " " + parameters.getParameter(CameraSearchParameters.SUFFIX).getValue(),
        getMemoryMapStorage(), peakList.getRawDataFiles());

    // Load previous applied methods.
    for (final FeatureList.FeatureListAppliedMethod method : peakList.getAppliedMethods()) {
      combinedPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    combinedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Bioconductor CAMERA", CameraSearchModule.class,
            parameters, getModuleCallDate()));

    // ------------------------------------------------
    // Find unique isotopes belonging to the same group
    // ------------------------------------------------

    Set<String> isotopeGroups = new HashSet<>();

    for (FeatureListRow row : peakList.getRows()) {
      FeatureIdentity identity = row.getPreferredFeatureIdentity();
      if (identity == null)
        continue;

      String isotope = identity.getPropertyValue("Isotope");
      if (isotope == null)
        continue;

      String isotopeGroup = isotope.substring(1, isotope.indexOf("]"));
      if (isotopeGroup == null || isotopeGroup.length() == 0)
        continue;

      isotopeGroups.add(isotopeGroup);
    }

    List<FeatureListRow> groupRows = new ArrayList<>();
    Set<String> groupNames = new HashSet<>();
    Map<Double, Double> spectrum = new HashMap<>();
    List<FeatureListRow> newPeakListRows = new ArrayList<>();

    for (String isotopeGroup : isotopeGroups) {
      // -----------------------------------------
      // Find all peaks belonging to isotopeGroups
      // -----------------------------------------

      groupRows.clear();
      groupNames.clear();
      spectrum.clear();

      int minLength = Integer.MAX_VALUE;
      FeatureListRow groupRow = null;

      for (FeatureListRow row : peakList.getRows()) {
        FeatureIdentity identity = row.getPreferredFeatureIdentity();
        if (identity == null)
          continue;

        String isotope = identity.getPropertyValue("Isotope");
        if (isotope == null)
          continue;

        String isoGroup = isotope.substring(1, isotope.indexOf("]"));
        if (isoGroup == null)
          continue;

        if (isoGroup.equals(isotopeGroup)) {
          groupRows.add(row);
          groupNames.add(identity.getName());
          spectrum.put(row.getAverageMZ(), row.getAverageHeight().doubleValue());

          if (isoGroup.length() < minLength) {
            minLength = isoGroup.length();
            groupRow = row;
          }
        }
      }

      // Skip peaks that have different identity names (belong to
      // different pcgroup)
      if (groupRow == null || groupNames.size() != 1)
        continue;

      // -------------------------------------------------
      // Save the row and the spectrum to combinedPeakList
      // -------------------------------------------------

      if (groupRow == null)
        continue;

      FeatureIdentity identity = groupRow.getPreferredFeatureIdentity();
      if (identity == null)
        continue;

      DataPoint[] dataPoints = new DataPoint[spectrum.size()];
      int count = 0;
      for (Entry<Double, Double> e : spectrum.entrySet())
        dataPoints[count++] = new SimpleDataPoint(e.getKey(), e.getValue());

      IsotopePattern pattern = new SimpleIsotopePattern(dataPoints, -1,
          IsotopePatternStatus.PREDICTED, "Spectrum");

      groupRow.getBestFeature().setIsotopePattern(pattern);

      // combinedPeakList.addRow(groupRow);
      newPeakListRows.add(groupRow);
    }

    // -----------------------------------
    // Add peaks with no isotope annotated
    // -----------------------------------

    if (includeSingletons) {
      for (FeatureListRow row : peakList.getRows()) {
        FeatureIdentity identity = row.getPreferredFeatureIdentity();
        if (identity == null)
          continue;

        String isotope = identity.getPropertyValue("Isotope");
        if (isotope == null || isotope.length() == 0) {
          DataPoint[] dataPoints = new DataPoint[1];
          dataPoints[0] = new SimpleDataPoint(row.getAverageMZ(), row.getAverageHeight());

          IsotopePattern pattern = new SimpleIsotopePattern(dataPoints, -1,
              IsotopePatternStatus.PREDICTED, "Spectrum");

          row.getBestFeature().setIsotopePattern(pattern);

          newPeakListRows.add(row);
        }
      }
    }

    // ------------------------------------
    // Sort new peak rows by retention time
    // ------------------------------------

    Collections.sort(newPeakListRows, new Comparator<FeatureListRow>() {
      @Override
      public int compare(FeatureListRow row1, FeatureListRow row2) {
        double retTime1 = row1.getAverageRT();
        double retTime2 = row2.getAverageRT();

        return Double.compare(retTime1, retTime2);
      }
    });

    for (FeatureListRow row : newPeakListRows)
      combinedPeakList.addRow(row);

    return combinedPeakList;
  }

  /**
   * Uses PCGroup-field in PeakIdentity to group peaks and build spectrum
   *
   * @param peakList a PeakList object
   * @return a new PeakList object
   */

  private FeatureList groupPeaksByPCGroup(FeatureList peakList) {
    // Create new feature list.
    final FeatureList combinedPeakList = new ModularFeatureList(
        peakList + " " + parameters.getParameter(CameraSearchParameters.SUFFIX).getValue(),
        getMemoryMapStorage(), peakList.getRawDataFiles());

    // Load previous applied methods.
    for (final FeatureList.FeatureListAppliedMethod method : peakList.getAppliedMethods()) {
      combinedPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    combinedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Bioconductor CAMERA", CameraSearchModule.class,
            parameters, getModuleCallDate()));

    // --------------------
    // Find unique PCGroups
    // --------------------

    Set<String> pcGroups = new HashSet<>();

    for (FeatureListRow row : peakList.getRows()) {
      FeatureIdentity identity = row.getPreferredFeatureIdentity();
      if (identity == null)
        continue;

      String groupName = identity.getName();
      if (groupName == null || groupName.length() == 0)
        continue;

      pcGroups.add(groupName);
    }

    List<FeatureListRow> groupRows = new ArrayList<>();
    // Set <String> groupNames = new HashSet <> ();
    Map<Double, Double> spectrum = new HashMap<>();
    List<FeatureListRow> newPeakListRows = new ArrayList<>();

    for (String groupName : pcGroups) {
      // -----------------------------------------
      // Find all peaks belonging to isotopeGroups
      // -----------------------------------------

      groupRows.clear();
      // groupNames.clear();
      spectrum.clear();

      double maxIntensity = 0.0;
      FeatureListRow groupRow = null;

      for (FeatureListRow row : peakList.getRows()) {
        FeatureIdentity identity = row.getPreferredFeatureIdentity();
        if (identity == null)
          continue;

        String name = identity.getName();

        if (name.equals(groupName)) {
          double intensity = row.getAverageHeight();

          groupRows.add(row);
          // groupNames.add(name);
          spectrum.put(row.getAverageMZ(), intensity);

          if (intensity > maxIntensity) {
            maxIntensity = intensity;
            groupRow = row;
          }
        }
      }

      // -------------------------------------------------
      // Save the row and the spectrum to combinedPeakList
      // -------------------------------------------------

      if (groupRow == null || spectrum.size() <= 1)
        continue;

      FeatureIdentity identity = groupRow.getPreferredFeatureIdentity();
      if (identity == null)
        continue;

      DataPoint[] dataPoints = new DataPoint[spectrum.size()];
      int count = 0;
      for (Entry<Double, Double> e : spectrum.entrySet())
        dataPoints[count++] = new SimpleDataPoint(e.getKey(), e.getValue());

      IsotopePattern pattern = new SimpleIsotopePattern(dataPoints, -1,
          IsotopePatternStatus.PREDICTED, "Spectrum");

      groupRow.getBestFeature().setIsotopePattern(pattern);

      // combinedPeakList.addRow(groupRow);
      newPeakListRows.add(groupRow);
    }

    // ------------------------------------
    // Sort new peak rows by retention time
    // ------------------------------------

    Collections.sort(newPeakListRows, new Comparator<FeatureListRow>() {
      @Override
      public int compare(FeatureListRow row1, FeatureListRow row2) {
        double retTime1 = row1.getAverageRT();
        double retTime2 = row2.getAverageRT();

        return Double.compare(retTime1, retTime2);
      }
    });

    for (FeatureListRow row : newPeakListRows)
      combinedPeakList.addRow(row);

    return combinedPeakList;
  }
}
