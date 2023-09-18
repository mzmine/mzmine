/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.MaldiSpotType;
import io.github.mzmine.main.MZmineCore;
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
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.File;
import java.io.FileReader;
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

  private final IMSImagingRawDataFile file;
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
      @Nullable final MZmineProject project, IMSImagingRawDataFile file) {
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

    final MobilityScanDataAccess mobilityAccess = new MobilityScanDataAccess(file,
        MobilityScanDataType.MASS_LIST, scans);
    final ScanDataAccess frameAccess = EfficientDataAccess.of(file, ScanDataType.MASS_LIST,
        selection);

    final Map<String, RangeMap<Double, ExpandingTrace>> spotTracesMap = new HashMap<>();
    final AtomicInteger rowId = new AtomicInteger(1);
    final Map<String, ModularFeatureList> spotFlistMap = new HashMap<>();

    final int totalFrames = frameAccess.getNumberOfScans();
    int processedFrames = 0;

    // create traces for every spot
    while (frameAccess.hasNextScan()) {

      progress = 0.2 * processedFrames / (double) totalFrames;

      final ImagingFrame frame = (ImagingFrame) frameAccess.nextScan();
      final String spot = frame.getMaldiSpotInfo().spotName();

      currentDesc = "Detecting traces in spot " + spot;

      final Map<String, String> spotSampleNameMap = parseSpotSampleName(spotNameFile);

      var traceMap = spotTracesMap.computeIfAbsent(spot, k -> TreeRangeMap.create());
      final ModularFeatureList flist = spotFlistMap.computeIfAbsent(spot,
          s -> new ModularFeatureList(
              spotSampleNameMap.getOrDefault(s.toUpperCase(), file.getName()) + " - " + s,
              getMemoryMapStorage(), file));

      final List<Scan> selectedScans =
          flist.getSeletedScans(file) != null ? (List<Scan>) flist.getSeletedScans(file)
              : new ArrayList<>();
      if (!selectedScans.contains(frame)) {
        selectedScans.add(frame);
        selectedScans.sort(Scan::compareTo);
        flist.setSelectedScans(file, selectedScans);
      }

      for (int i = 0; i < frameAccess.getNumberOfDataPoints(); i++) {
        if (frameAccess.getIntensityValue(i) < minSummedIntensity) {
          continue;
        }

        final double mz = frameAccess.getMzValue(i);
        ExpandingTrace trace = traceMap.get(mz);
        if (trace == null) {
          final Range<Double> mzRange = SpectraMerging.createNewNonOverlappingRange(traceMap,
              mzTolerance.getToleranceRange(mz));
          final SimpleIonTimeSeries series = new SimpleIonTimeSeries(getMemoryMapStorage(),
              new double[]{mz}, new double[]{frameAccess.getIntensityValue(i)}, List.of(frame));
          final ModularFeature feature = new ModularFeature(flist, file, series,
              FeatureStatus.DETECTED);
          feature.set(MaldiSpotType.class, frame.getMaldiSpotInfo().spotName());
          trace = new ExpandingTrace(
              new ModularFeatureListRow(flist, rowId.getAndIncrement(), feature), mzRange,
              Range.all());
          traceMap.put(mzRange, trace);
        }
      }
      processedFrames++;
    }

    final int totalSpots = spotFlistMap.size();
    int processedSpots = 0;
    // expand the traces
    for (Entry<String, ModularFeatureList> spotFlistEntry : spotFlistMap.entrySet()) {
      progress = 0.2 + 0.8 * processedSpots / (double) totalSpots;

      final String spot = spotFlistEntry.getKey();
      final ModularFeatureList flist = spotFlistEntry.getValue();
      currentDesc = "Expanding traces in spot " + spot;
      final BinningMobilogramDataAccess mobilogramDataAccess = new BinningMobilogramDataAccess(file,
          BinningMobilogramDataAccess.getRecommendedBinWidth(file));

      final List<Frame> selectedScans = (List<Frame>) flist.getSeletedScans(file);

      final ParameterSet expanderParameters = MZmineCore.getConfiguration()
          .getModuleParameters(ImsExpanderModule.class).cloneParameterSet();
      expanderParameters.setParameter(ImsExpanderParameters.useRawData, false);
      expanderParameters.getParameter(ImsExpanderParameters.mzTolerance).setValue(true);
      expanderParameters.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
          .setValue(mzTolerance);

      final List<ExpandingTrace> traces = spotTracesMap.get(spot).asMapOfRanges().values().stream()
          .sorted(Comparator.comparingDouble(trace -> RangeUtils.rangeCenter(trace.getMzRange())))
          .toList();

      final ImsExpanderSubTask task = new ImsExpanderSubTask(getMemoryMapStorage(),
          expanderParameters, selectedScans, flist, traces, mobilogramDataAccess, file);
      task.run();

      final List<ExpandedTrace> expandedTraces = task.getExpandedTraces();

      for (ExpandedTrace expandedTrace : expandedTraces) {
        final ModularFeatureListRow row = new ModularFeatureListRow(flist, expandedTrace.oldRow(),
            false);
        final ModularFeature f = new ModularFeature(flist, expandedTrace.oldFeature());
        f.set(FeatureDataType.class, expandedTrace.series());
        FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
        row.addFeature(file, f);
        flist.addRow(row);
      }

      processedSpots++;
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

  private Map<String, String> parseSpotSampleName(@Nullable final File file) {
    Map<String, String> spotNameMap = new HashMap<>();
    if (file == null) {
      return spotNameMap;
    }

    try {
      final String[][] spotSampleNames = CSVParser.parse(new FileReader(file), ';');

      final String[] firstLine = spotSampleNames[0];
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

      for (int i = 1; i < spotSampleNames.length; i++) {
        spotNameMap.put(spotSampleNames[i][spotIndex].toUpperCase(), spotSampleNames[i][nameIndex]);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    return spotNameMap;
  }

}
