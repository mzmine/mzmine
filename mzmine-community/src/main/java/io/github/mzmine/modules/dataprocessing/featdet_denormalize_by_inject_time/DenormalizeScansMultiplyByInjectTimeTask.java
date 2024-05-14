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
package io.github.mzmine.modules.dataprocessing.featdet_denormalize_by_inject_time;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This module is used to multiply all selected mass lists or scans with the injection time to
 * denormalize the intensities. This is usually done before merging MSn spectra to obtain more
 * comparable mass spectra. Only applicable to trapped MS instruments
 */
public class DenormalizeScansMultiplyByInjectTimeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      DenormalizeScansMultiplyByInjectTimeTask.class.getName());
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final ParameterSet parameterSet;
  private int processedScans, totalScans;

  public DenormalizeScansMultiplyByInjectTimeTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.dataFile = dataFile;
    this.parameterSet = parameters;

    scanSelection = parameters.getValue(
        DenormalizeScansMultiplyByInjectTimeParameters.scanSelection);
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Denormalizing data file by multiplying each scan/masslist by inject time" + dataFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    Scan[] scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;
    if (Arrays.stream(scans).noneMatch(this::hasInjectionTime)) {
      logger.info(
          "No scan has an injection time defined (only applicable for trapped MS). No denormalization of intensities performed.");
      setStatus(TaskStatus.FINISHED);
      return;
    }

    for (var scan : scans) {
      if (isCanceled()) {
        return;
      }
      if (hasInjectionTime(scan)) {
        double[] intensities = ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(scan, true);
        scan.addMassList(
            new SimpleMassList(storage, ScanUtils.getMzValues(scan, true), intensities));

        // apply to mobility scans in IMS dimension
        if (scan instanceof Frame frame) {
          List<double[][]> data = new ArrayList<>(frame.getNumberOfMobilityScans());
          for (var mobscan : frame.getMobilityScans()) {
            double[] mzs = ScanUtils.getMzValues(mobscan, true);
            double[] mobIntensities = ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mobscan,
                true);
            data.add(new double[][]{mzs, mobIntensities});
          }
          frame.getMobilityScanStorage().setMassLists(storage, data);
        }
      }
      processedScans++;
    }

    dataFile.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(DenormalizeScansMultiplyByInjectTimeModule.class,
            parameterSet, getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  private boolean hasInjectionTime(final Scan scan) {
    Float time = scan.getInjectionTime();
    return time != null && time > 0;
  }

}
