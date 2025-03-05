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

package io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.MaldiSpotType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ExpandedTrace;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ExpandingTrace;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderSubTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaldiSpotFeatureDetectionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      MaldiSpotFeatureDetectionTask.class.getName());

  private final ImagingRawDataFile file;
  private final double minSummedIntensity;
  private final MZTolerance mzTolerance;
  private final ScanSelection selection = new ScanSelection(1);
  private final File spotNameFile;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private String currentDesc = "";
  private double progress = 0d;

  protected MaldiSpotFeatureDetectionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, final ParameterSet parameters,
      @Nullable final MZmineProject project, ImagingRawDataFile file) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.file = file;
    mzTolerance = parameters.getValue(MaldiSpotFeatureDetectionParameters.mzTolerance);

    spotNameFile = parameters.getValue(MaldiSpotFeatureDetectionParameters.spotNameFile)
        ? parameters.getParameter(MaldiSpotFeatureDetectionParameters.spotNameFile)
        .getEmbeddedParameter().getValue() : null;
    minSummedIntensity = parameters.getValue(MaldiSpotFeatureDetectionParameters.minIntensity);
  }

  @Override
  public String getTaskDescription() {
    return "Detection of features on MALDI dried droplet plates for file " + file.getName() + ": "
        + currentDesc;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final List<Frame> scans = selection.getMatchingScans(
        (List<Frame>) (List<? extends Scan>) file.getScans());

    final ScanDataAccess scanAccess = EfficientDataAccess.of(file, ScanDataType.MASS_LIST,
        selection);

    final AtomicInteger rowId = new AtomicInteger(1);
    final Map<String, ModularFeatureList> spotFlistMap = new HashMap<>();

    final int totalFrames = scanAccess.getNumberOfScans();
    int processedFrames = 0;

    // create traces for every spot
    while (scanAccess.hasNextScan()) {

      progress = 0.2 * processedFrames / (double) totalFrames;

      final ImagingScan scan = (ImagingScan) scanAccess.nextScan();
      final String spot = scan.getMaldiSpotInfo().spotName();

      currentDesc = "Detecting traces in spot " + spot;

      final Map<String, String> spotSampleNameMap = parseSpotSampleName(spotNameFile);

      // is it possible to have multiple scans per spot? i guess so.
      final ModularFeatureList flist = spotFlistMap.computeIfAbsent(spot,
          s -> new ModularFeatureList(
              spotSampleNameMap.getOrDefault(s.toUpperCase(), file.getName()) + " - " + s,
              getMemoryMapStorage(), file));

      final List<Scan> selectedScans =
          flist.getSeletedScans(file) != null ? (List<Scan>) flist.getSeletedScans(file)
              : new ArrayList<>();
      if (!selectedScans.contains(scan)) {
        selectedScans.add(scan);
        selectedScans.sort(Scan::compareTo);
        flist.setSelectedScans(file, selectedScans);
      }

      for (int i = 0; i < scanAccess.getNumberOfDataPoints(); i++) {
        if (scanAccess.getIntensityValue(i) < minSummedIntensity) {
          continue;
        }
        final double mz = scanAccess.getMzValue(i);
        final SimpleIonTimeSeries series = new SimpleIonTimeSeries(getMemoryMapStorage(),
            new double[]{mz}, new double[]{scanAccess.getIntensityValue(i)}, List.of(scan));
        final ModularFeature feature = new ModularFeature(flist, file, series,
            FeatureStatus.DETECTED);
        feature.set(MaldiSpotType.class, scan.getMaldiSpotInfo().spotName());
        flist.addRow(new ModularFeatureListRow(flist, rowId.getAndIncrement(), feature));
      }
      processedFrames++;
    }

    // create the ExpandingTraces
    if (file instanceof IMSRawDataFile) {
      expandImsFeatureLists(spotFlistMap);
    }

    spotFlistMap.values().forEach(flist -> {
      flist.getAppliedMethods().addAll(file.getAppliedMethods());
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(MaldiSpotFeatureDetectionModule.class, parameters,
              getModuleCallDate()));
      project.addFeatureList(flist);
    });

    setStatus(TaskStatus.FINISHED);
  }

  private void expandImsFeatureLists(Map<String, ModularFeatureList> spotFlistMap) {
    final int totalSpots = spotFlistMap.size();
    int processedSpots = 0;
    for (Entry<String, ModularFeatureList> spotFlistEntry : spotFlistMap.entrySet()) {
      final String spot = spotFlistEntry.getKey();
      final ModularFeatureList flist = spotFlistEntry.getValue();
      expandSingleFeatureList(spot, flist, processedSpots, totalSpots);
      processedSpots++;
    }
  }

  private void expandSingleFeatureList(String spot, ModularFeatureList flist, int processedSpots,
      double totalSpots) {
    progress = 0.2 + 0.8 * processedSpots / totalSpots;
    currentDesc = "Expanding traces in spot %s".formatted(spot);
    logger.finest(() -> currentDesc);

    final BinningMobilogramDataAccess mobilogramDataAccess = new BinningMobilogramDataAccess(
        (IMSRawDataFile) file,
        BinningMobilogramDataAccess.getRecommendedBinWidth((IMSRawDataFile) file));

    final List<Frame> selectedScans = (List<Frame>) flist.getSeletedScans(file);
    final var traces = createTracesMapForFeatureList(flist).asMapOfRanges().values().stream()
        .sorted(Comparator.comparingDouble(trace -> RangeUtils.rangeCenter(trace.getMzRange())))
        .toList();
    final ParameterSet expanderParameters = createExpanderParameters();

    final ImsExpanderSubTask task = new ImsExpanderSubTask(getMemoryMapStorage(),
        expanderParameters, selectedScans, flist, traces, mobilogramDataAccess,
        (IMSRawDataFile) file);
    task.run();

    final List<ExpandedTrace> expandedTraces = task.getExpandedTraces();

    flist.getRows().clear();
    for (ExpandedTrace expandedTrace : expandedTraces) {
      final ModularFeatureListRow row = new ModularFeatureListRow(flist, expandedTrace.oldRow(),
          false);
      final ModularFeature f = new ModularFeature(flist, expandedTrace.oldFeature());
      f.set(FeatureDataType.class, expandedTrace.series());
      FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
      row.addFeature(file, f);
      flist.addRow(row);
    }
  }

  @NotNull
  private ParameterSet createExpanderParameters() {
    final ParameterSet expanderParameters = ConfigService.getConfiguration()
        .getModuleParameters(ImsExpanderModule.class).cloneParameterSet();
    expanderParameters.setParameter(ImsExpanderParameters.useRawData, false);
    expanderParameters.getParameter(ImsExpanderParameters.mzTolerance).setValue(true);
    expanderParameters.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .setValue(mzTolerance);
    return expanderParameters;
  }

  /**
   * Set up a range map of the expanding traces for the respective modular feature list.
   */
  private TreeRangeMap<Double, ExpandingTrace> createTracesMapForFeatureList(
      ModularFeatureList flist) {
    final TreeRangeMap<Double, ExpandingTrace> traceMap = TreeRangeMap.create();
    final List<FeatureListRow> sortedRows = flist.getRows().stream()
        .sorted(Comparator.comparingDouble(FeatureListRow::getMaxHeight).reversed()).toList();
    for (FeatureListRow row : sortedRows) {
      if (traceMap.get(row.getAverageMZ()) == null) {
        final Range<Double> mzRange = SpectraMerging.createNewNonOverlappingRange(traceMap,
            mzTolerance.getToleranceRange(row.getAverageMZ()));
        traceMap.put(mzRange,
            new ExpandingTrace((ModularFeatureListRow) row, mzRange, Range.all()));
      }// else { // does not make sense to add this range.
//          logger.finest(
//              () -> STR."asked to create an ExpandingTrace for m/z \{row.getAverageMZ()} but already covered by range \{traceMap.get(
//                  row.getAverageMZ()).getMzRange()}. Consider lowering the mz tolerance.");
//      }
    }
    return traceMap;
  }

  private Map<String, String> parseSpotSampleName(@Nullable final File file) {
    Map<String, String> spotNameMap = new HashMap<>();
    if (file == null) {
      return spotNameMap;
    }

    try {
      final List<String[]> spotSampleNames = CSVParsingUtils.readData(file, ";");

      final String[] firstLine = spotSampleNames.getFirst();
      int spotIndex = -1;
      int nameIndex = -1;

      for (int i = 0; i < firstLine.length; i++) {
        if (firstLine[i].equalsIgnoreCase("spot")) {
          spotIndex = i;
        }
        if (firstLine[i].equalsIgnoreCase("name")) {
          nameIndex = i;
        }
      }

      if (spotIndex == -1) {
        throw new RuntimeException("Did not find spot column.");
      }
      if (nameIndex == -1) {
        throw new RuntimeException("Did not find name column.");
      }

      for (int i = 1; i < spotSampleNames.size(); i++) {
        spotNameMap.put(spotSampleNames.get(i)[spotIndex].toUpperCase(),
            spotSampleNames.get(i)[nameIndex]);
      }
    } catch (IOException | CsvException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    return spotNameMap;
  }

}
