/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;


import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Objects.requireNonNullElse;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ModularADAPChromatogramBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ModularADAPChromatogramBuilderTask.class.getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  // User parameters
  private final String suffix;
  private final MZTolerance mzTolerance;
  // image builder supplies a min number of total scans as well as min consecutive scans
  // ADAPChromatogramBuilder only uses min consecutive scans
  private final int minimumTotalScans;
  private final int minimumConsecutiveScans;
  // Owen added User parameers;
  private final double minGroupIntensity;
  private final double minHighestPoint;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> callingModule;
  private final boolean isImaging;
  private double progress = 0.0;
  private ModularFeatureList newFeatureList;

  /**
   * @param callingModule     {@link ImageBuilderModule} or
   *                          {@link ModularADAPChromatogramBuilderModule}
   * @param minimumTotalScans min total scans is only used in imaging
   * @param minGroupIntensity min group intensity is only used in chromatography
   */
  public ModularADAPChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      Class<? extends MZmineModule> callingModule, @Nullable Integer minimumTotalScans,
      @Nullable Double minGroupIntensity) {
    super(storage, moduleCallDate);
    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getValue(ADAPChromatogramBuilderParameters.scanSelection);

    this.mzTolerance = parameters.getValue(ADAPChromatogramBuilderParameters.mzTolerance);
    this.minimumConsecutiveScans = parameters.getValue(
        ADAPChromatogramBuilderParameters.minimumConsecutiveScans);

    this.suffix = parameters.getValue(ADAPChromatogramBuilderParameters.suffix);

    // Owen added parameters
    this.minGroupIntensity = requireNonNullElse(minGroupIntensity, 0d);

    this.minHighestPoint = parameters.getValue(ADAPChromatogramBuilderParameters.minHighestPoint);
    this.parameters = parameters;
    this.callingModule = callingModule;
    // image builder supplies a min number of total scans as well as min consecutive scans
    // ADAPChromatogramBuilder only uses min consecutive scans
    this.minimumTotalScans = requireNonNullElse(minimumTotalScans, minimumConsecutiveScans);

    isImaging = callingModule.equals(ImageBuilderModule.class);
  }

  public static ModularADAPChromatogramBuilderTask forImaging(MZmineProject project,
      RawDataFile dataFile, ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, Class<? extends MZmineModule> callingModule) {
    var total = parameters.getValue(ImageBuilderParameters.minTotalSignals);
    return new ModularADAPChromatogramBuilderTask(project, dataFile, parameters, storage,
        moduleCallDate, callingModule, total, null);
  }

  public static ModularADAPChromatogramBuilderTask forChromatography(MZmineProject project,
      RawDataFile dataFile, ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, Class<? extends MZmineModule> callingModule) {
    var minGroupIntensity = parameters.getValue(
        ADAPChromatogramBuilderParameters.minGroupIntensity);
    return new ModularADAPChromatogramBuilderTask(project, dataFile, parameters, storage,
        moduleCallDate, callingModule, null, minGroupIntensity);
  }

  @Override
  public String getTaskDescription() {
    return "Detecting %s in %s".formatted(isImaging ? "images" : "chromatograms", dataFile);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(() -> "Started chromatogram builder on " + dataFile);

    Scan[] scans = scanSelection.getMatchingScans(dataFile);
    int emptyScanNumber = 0;

    if (scans.length == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("There are no scans satisfying filtering values. Consider updating filters "
          + "with \"Set filters\" in the \"Scans\" parameter.");
      return;
    }

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY;
    for (Scan s : scans) {
      if (isCanceled()) {
        return;
      }

      if (s.isEmptyScan()) {
        emptyScanNumber++;
        continue;
      }

      if (s.getRetentionTime() < prevRT) {
        setStatus(TaskStatus.ERROR);
        final String msg = "Retention time of scan #" + s.getScanNumber()
            + " is smaller then the retention time of the previous scan."
            + " Please make sure you only use scans with increasing retention times."
            + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
        setErrorMessage(msg);
        return;
      }
      prevRT = s.getRetentionTime();
    }

    if (emptyScanNumber > 0) {
      logger.info(emptyScanNumber + " scans were found to be empty.");
    }

    // Check if the scans are MS1-only or MS2-only.
    int level = scans[0].getMSLevel();
    final PolarityType pol = scans[0].getPolarity();
    for (int i = 1; i < scans.length; i++) {
      if (level != scans[i].getMSLevel()) {
        DesktopService.getDesktop().displayMessage(null,
            "mzmine thinks that you are running ADAP Chromatogram builder on both MS1- and MS2-scans. "
                + "This will likely produce wrong results. "
                + "Please, set the scan filter parameter to a specific MS level");
        break;
      }
      if (pol != scans[i].getPolarity()) {
        DesktopService.getDesktop().displayMessage("""
            mzmine thinks you are processing data of multiple polarities (%s and %s)
            at the same time. This will likely lead to wrong results.
            Set the polarity filter in the wizard or the chromatogram builder step to process each polarity individually.""".formatted(
            pol, scans[i].getPolarity()));
        break;
      }
    }

    // make a list of all the data points
    // sort data points by intensity
    // loop through list
    // add data point to chromatogrm or make new one
    // update mz avg and other stuff
    //

    // map the mz tolerance to chromatograms
    RangeMap<Double, ADAPChromatogram> rangeToChromMap = TreeRangeMap.create();

    // make a list of all the data points
    final int totalDps = Arrays.stream(scans).map(s -> {
      if (s.getMassList() != null) {
        return s.getMassList();
      }
      final MissingMassListException ex = new MissingMassListException(s);
      DesktopService.getDesktop().displayErrorMessage(ex.getMessage());
      throw ex;
    }).mapToInt(MassSpectrum::getNumberOfDataPoints).sum();
    int dpCounter = 0;

    ExpandedDataPoint[] allMzValues = new ExpandedDataPoint[totalDps];

    ScanDataAccess scanData = EfficientDataAccess.of(dataFile, ScanDataType.MASS_LIST,
        scanSelection);

    progress = 0;
    double progressStep = 0.1 / scanData.getNumberOfScans();
    while (scanData.hasNextScan()) {
      if (isCanceled()) {
        return;
      }

      Scan scan;
      try {
        scan = scanData.nextScan();
      } catch (MissingMassListException e) {
        setStatus(TaskStatus.ERROR);
        StringBuilder b = new StringBuilder("Scan #");
        b.append(scanData.getCurrentScan().getScanNumber()).append(" from ");
        b.append(dataFile.getName());
        b.append(
            " does not have a mass list. Please run \"Raw data methods\" -> \"Mass detection\"");
        if (dataFile instanceof IMSRawDataFile) {
          b.append("\nIMS files require mass detection on the frame level (Scan type = \"Frames ");
          b.append("only\" or \"All scan types\"");
        }
        setErrorMessage(b.toString());
        e.printStackTrace();
        return;
      }

      int dps = scanData.getNumberOfDataPoints();
      for (int i = 0; i < dps; i++) {
        ExpandedDataPoint curDatP = new ExpandedDataPoint(scanData.getMzValue(i),
            scanData.getIntensityValue(i), scan);
        allMzValues[dpCounter] = curDatP;
        dpCounter++;
      }
      progress += progressStep;
    }

    // sort data points by intensity
    Arrays.parallelSort(allMzValues,
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));

    // count starts at 1 since we already have added one with a single point.
    progress = 0.1;
    progressStep = (allMzValues.length > 0) ? 0.45 / allMzValues.length : 0.0;

    for (ExpandedDataPoint mzFeature : allMzValues) {

      progress += progressStep;

      if (isCanceled()) {
        return;
      }

      if (mzFeature == null || Double.isNaN(mzFeature.getMZ()) || Double.isNaN(
          mzFeature.getIntensity())) {
        continue;
      }

      final Entry<Range<Double>, ADAPChromatogram> existing = rangeToChromMap.getEntry(
          mzFeature.getMZ());
      if (existing != null) {
        // add data point to chromatogram
        existing.getValue().addMzFeature(mzFeature.getScan(), mzFeature);
      } else {
        // skip it entierly if the intensity is not high enough
        if (mzFeature.getIntensity() < minHighestPoint) {
          continue;
        }
        // add a new chromatogram to the range map - limit ranges to avoid overlap
        startNewChromatogramLimitMzRanges(rangeToChromMap, mzFeature);
      }
    }

    // finish chromatograms sorted by m/z
    final Map<Range<Double>, ADAPChromatogram> finalRangeMap = rangeToChromMap.asMapOfRanges();

    int numChromatograms = finalRangeMap.size();
    progressStep = numChromatograms > 0 ? 0.45 / numChromatograms : 0.0;

    // Create new feature list
    newFeatureList = new ModularFeatureList(dataFile + " " + suffix, getMemoryMapStorage(),
        dataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(newFeatureList);

    int newFeatureID = 1;
    // add chromatograms that match criteria
    for (ADAPChromatogram chromatogram : finalRangeMap.values()) {
      if (isCanceled()) {
        return;
      }

      progress += progressStep;

      // And remove chromatograms who dont have a certain number of continous points above the
      // IntensityThresh2 level.
      var dps = chromatogram.getNumberOfDataPoints();
      if (dps >= minimumTotalScans && chromatogram.matchesMinContinuousDataPoints(scans,
          minGroupIntensity, minimumConsecutiveScans, minHighestPoint)) {
        // add zeros to edges
        if (!isImaging) {
          chromatogram.addNZeros(scans, 1, 1);
        }

        // add to list
        ModularFeature modular = FeatureConvertors.ADAPChromatogramToModularFeature(newFeatureList,
            dataFile, chromatogram, mzTolerance);
        ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, newFeatureID,
            modular);
        newFeatureList.addRow(newRow);
        // activate shape for this row
        if (!isImaging) {
          newRow.set(FeatureShapeType.class, true);
        }
        newFeatureID++;
      }
    }

    // sort and reset IDs here to have the same sorting for every feature list
    FeatureListUtils.sortByDefault(newFeatureList, true);

    newFeatureList.setSelectedScans(dataFile, Arrays.asList(scans));

    dataFile.getAppliedMethods().forEach(m -> newFeatureList.getAppliedMethods().add(m));
    // Add new feature list to the project
    newFeatureList.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(callingModule, parameters, getModuleCallDate()));
    project.addFeatureList(newFeatureList);

    progress = 1.0;

    setStatus(TaskStatus.FINISHED);

    logger.info(() -> "Finished chromatogram builder on " + dataFile);
  }

  /**
   * Starts a new chromatogram and limits its range so that it does not overlap with existing m/z
   * ranges
   *
   * @param rangeToChromMap started chromatograms with their non overlapping m/z range
   * @param mzFeature       current tested data point
   */
  @SuppressWarnings("UnstableApiUsage")
  private void startNewChromatogramLimitMzRanges(RangeMap<Double, ADAPChromatogram> rangeToChromMap,
      ExpandedDataPoint mzFeature) {
    // start new chromatogram and create new range (subract overlapping existing ranges)
    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mzFeature.getMZ());

    // look +- mz tolerance to see if ther is a range near by.
    // If there is use the proper boundry of that range for the
    // new range to insure than NON OF THE RANGES OVERLAP.
    final Entry<Range<Double>, ADAPChromatogram> minusRange = rangeToChromMap.getEntry(
        toleranceRange.lowerEndpoint());
    final Entry<Range<Double>, ADAPChromatogram> plusRange = rangeToChromMap.getEntry(
        toleranceRange.upperEndpoint());

    // If both of the above ranges are null then we make the new range spaning the full
    // mz tolerance range.
    // If one or both are not null we need to properly modify the range of the new
    // chromatogram so that none of the points are overlapping.
    Double toBeLowerBound =
        minusRange == null ? toleranceRange.lowerEndpoint() : minusRange.getKey().upperEndpoint();
    Double toBeUpperBound =
        plusRange == null ? toleranceRange.upperEndpoint() : plusRange.getKey().lowerEndpoint();

    if (toBeLowerBound < toBeUpperBound) {
      // use closed open so that every value may be captured by rangeMap
      Range<Double> newRange = Range.closedOpen(toBeLowerBound, toBeUpperBound);
      ADAPChromatogram newChrom = new ADAPChromatogram();
      newChrom.addMzFeature(mzFeature.getScan(), mzFeature);

      rangeToChromMap.put(newRange, newChrom);
    } else if (toBeLowerBound.equals(toBeUpperBound) && plusRange != null) {
      plusRange.getValue().addMzFeature(mzFeature.getScan(), mzFeature);
    } else {
      throw new IllegalStateException(
          String.format("Incorrect range [%f, %f] for m/z %f", toBeLowerBound, toBeUpperBound,
              mzFeature.getMZ()));
    }
  }

}

