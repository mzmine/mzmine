package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImsExpanderTask.class.getName());

  private final MZmineProject project;
  protected final ParameterSet parameters;
  protected final ModularFeatureList flist;
  private final MZTolerance mzTolerance;
  private final boolean useMzToleranceRange;

  private String desc = "Mobility expanding.";
  private final AtomicInteger processedFrames = new AtomicInteger(0);
  private final AtomicInteger processedRows = new AtomicInteger(0);

  private long totalFrames = 1;
  private long totalRows = 1;
  private final int binWidth;


  public ImsExpanderTask(@Nullable final MemoryMapStorage storage,
      @NotNull final ParameterSet parameters, @NotNull final ModularFeatureList flist,
      MZmineProject project) {
    super(storage);
    this.parameters = parameters;
    this.project = project;
    this.flist = flist;
    useMzToleranceRange = parameters.getParameter(ImsExpanderParameters.mzTolerance).getValue();
    mzTolerance = parameters.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .getValue();
    binWidth = parameters.getParameter(ImsExpanderParameters.mobilogramBinWidth).getValue();
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return 0.5 * (processedFrames.get() / (double) totalFrames) + 0.5 * (processedRows.get()
        / (double) totalRows);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (flist.getNumberOfRawDataFiles() != 1 || !(flist
        .getRawDataFile(0) instanceof IMSRawDataFile imsFile)) {
      setErrorMessage("More than one raw data file in feature list " + flist.getName()
          + " or no mobility dimension in raw data file.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    totalRows = flist.getNumberOfRows();

    logger.finest("Initialising data access for file " + imsFile.getName());
    final MobilityScanDataAccess access = EfficientDataAccess
        .of(imsFile, MobilityScanDataType.CENTROID, new ScanSelection(1));

    totalFrames = access.getNumberOfScans();

    final List<? extends FeatureListRow> rows = new ArrayList<>(flist.getRows());
    rows.sort((Comparator.comparingDouble(FeatureListRow::getAverageMZ)));

    // either we use the row m/z + tolerance range, or we use the mz range of the feature.
    final List<ExpandingTrace> expandingTraces = rows.stream().map(
        row -> new ExpandingTrace((ModularFeatureListRow) row,
            useMzToleranceRange ? mzTolerance.getToleranceRange(row.getAverageMZ())
                : row.getFeature(imsFile).getRawDataPointsMZRange())).toList();

    final int numTraces = expandingTraces.size();
    try {

      for (int i = 0; i < access.getNumberOfScans(); i++) {
        final Frame frame = access.nextFrame();

        desc =
            flist.getName() + ": expanding traces for frame " + processedFrames + "/" + totalFrames
                + ".";

        while (access.hasNextMobilityScan()) {
          final MobilityScan mobilityScan = access.nextMobilityScan();

          int traceIndex = 0;
          for (int dpIndex = 0; dpIndex < access.getNumberOfDataPoints() && traceIndex < numTraces;
              dpIndex++) {
            double mz = access.getMzValue(dpIndex);
            // while the trace upper mz smaller than the current mz, we increment the trace index
            while (expandingTraces.get(traceIndex).getMzRange().upperEndpoint() < mz
                && traceIndex < numTraces - 1) {
              traceIndex++;
            }
            // if the current lower mz passed the current data point, we go to the next data point
            if (expandingTraces.get(traceIndex).getMzRange().lowerEndpoint() > mz) {
              continue;
            }

            // try to offer the current data point to the trace
            while (expandingTraces.get(traceIndex).getMzRange().contains(mz) && !expandingTraces
                .get(traceIndex).offerDataPoint(access, dpIndex) && traceIndex < numTraces - 1) {
              traceIndex++;
            }
          }

          /*int dpIndex = 0;
          for(int traceIndex = 0; i < expandingTraces.size(); traceIndex++) {
            final double mz = access.getMzValue(dpIndex);
            final ExpandingTrace trace = expandingTraces.get(traceIndex);

            if(trace.getMzRange().lowerEndpoint() > mz) {
              continue;
            } else if(trace.getMzRange().upperEndpoint() < mz) {
              continue;
            }
          }*/
        }
        processedFrames.getAndIncrement();
      }
    } catch (MissingMassListException e) {
      e.printStackTrace();
    }

    final BinningMobilogramDataAccess mobilogramDataAccess = EfficientDataAccess
        .of(imsFile, binWidth);

    final ModularFeatureList newFlist = new ModularFeatureList(flist.getName() + " expanded ",
        getMemoryMapStorage(), imsFile);
    newFlist.setSelectedScans(imsFile, flist.getSeletedScans(imsFile));
    DataTypeUtils.addDefaultIonMobilityTypeColumns(newFlist);

    for (ExpandingTrace expandingTrace : expandingTraces) {
      desc = "Creating new features " + processedRows.getAndIncrement() + "/" + totalRows;

      if (expandingTrace.getNumberOfMobilityScans() > 1) {
        final IonMobilogramTimeSeries series = expandingTrace
            .toIonMobilogramTimeSeries(getMemoryMapStorage(), mobilogramDataAccess);
        final ModularFeatureListRow row = new ModularFeatureListRow(newFlist,
            expandingTrace.getRow(), false);
        final ModularFeature f = new ModularFeature(newFlist,
            expandingTrace.getRow().getFeature(imsFile));
        f.set(FeatureDataType.class, series);
        row.addFeature(imsFile, f);
        FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
        newFlist.addRow(row);
      }

      processedRows.getAndIncrement();
    }

    project.addFeatureList(newFlist);
    setStatus(TaskStatus.FINISHED);
  }
}
