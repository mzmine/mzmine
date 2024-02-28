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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Steffen https://github.com/SteffenHeu
 */
public class MobilogramBinningTask extends AbstractTask {

  private final ModularFeatureList originalFeatureList;
  private final ParameterSet parameters;
  private final String suffix;
  private final boolean createNewFlist;
  private final MZmineProject project;
  private final AtomicLong processedFeatures = new AtomicLong(0);
  private final int timsBinningWidth;
  private final int twimsBinningWidth;
  private final int dtimsBinningWidth;
  private final BinningSource binningSource;
  private long totalFeatures = 1;

  public MobilogramBinningTask(@Nullable MemoryMapStorage storage,
      @NotNull final ModularFeatureList originalFeatureList,
      @NotNull final ParameterSet parameters,
      @NotNull final MZmineProject project,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.originalFeatureList = originalFeatureList;
    suffix = parameters.getParameter(MobilogramBinningParameters.suffix).getValue();
    createNewFlist = parameters.getParameter(MobilogramBinningParameters.createNewFeatureList)
        .getValue();
    timsBinningWidth = parameters.getParameter(MobilogramBinningParameters.timsBinningWidth)
        .getValue();
    twimsBinningWidth = parameters.getParameter(MobilogramBinningParameters.twimsBinningWidth)
        .getValue();
    dtimsBinningWidth = parameters.getParameter(MobilogramBinningParameters.dtimsBinningWidth)
        .getValue();
    binningSource = parameters.getParameter(MobilogramBinningParameters.summingSource).getValue();
    this.project = project;
  }


  @Override
  public String getTaskDescription() {
    return originalFeatureList.getName() + ": Summing mobilogram " + processedFeatures.get() + "/"
        + totalFeatures;
  }

  @Override
  public double getFinishedPercentage() {
    return processedFeatures.get() / (double) totalFeatures;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final ModularFeatureList flist = createNewFlist ? originalFeatureList
        .createCopy(originalFeatureList.getName() + " " + suffix, getMemoryMapStorage(), false)
        : originalFeatureList;

    totalFeatures = flist.streamFeatures().count();

    for (RawDataFile file : flist.getRawDataFiles()) {
      if (!(file instanceof IMSRawDataFile)) {
        return;
      }

      final List<ModularFeature> features = (List<ModularFeature>) (List<? extends Feature>) flist
          .getFeatures(file);

      final int binWidth = switch (((IMSRawDataFile) file).getMobilityType()) {
        case TIMS -> timsBinningWidth;
        case TRAVELING_WAVE -> twimsBinningWidth;
        case DRIFT_TUBE -> dtimsBinningWidth;
        default -> throw new UnsupportedOperationException(
            "Summing of the mobility type in raw data file " + file.getName() + " is unsupported.");
      };

      final BinningMobilogramDataAccess summedAccess = new BinningMobilogramDataAccess(
          (IMSRawDataFile) file, binWidth);

      for (ModularFeature feature : features) {
        processedFeatures.getAndIncrement();
        if (!(feature.getFeatureData() instanceof IonMobilogramTimeSeries series)) {
          continue;
        }

        if (binningSource == BinningSource.RAW) {
          summedAccess.setMobilogram(series.getMobilograms());
        } else {
          summedAccess.setMobilogram(series.getSummedMobilogram());
        }
        final SummedIntensityMobilitySeries mobilogram = summedAccess
            .toSummedMobilogram(getMemoryMapStorage());
        feature.set(
            FeatureDataType.class, series.copyAndReplace(flist.getMemoryMapStorage(), mobilogram));
      }
    }

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(MobilogramBinningModule.class, parameters, getModuleCallDate()));
    if (createNewFlist) {
      project.addFeatureList(flist);
    }
    setStatus(TaskStatus.FINISHED);
  }
}
