package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a {@link CompoundList} for a single {@link ModularFeatureList} using
 * {@link SimpleSeederComponentizer}.
 * <p>
 * Requires the source feature list to have at least one IonIdentity Network OR a non-empty RowGroup
 * list — otherwise the task fails fast with a clear error.
 * <p>
 * Captures the source feature list's structural version at start; if it changes by the time the
 * compound list is attached, the result is dropped (with a warning).
 */
public class CompoundGrouperTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(CompoundGrouperTask.class.getName());

  @NotNull
  private final ModularFeatureList featureList;
  @NotNull
  private final ParameterSet parameters;
  @NotNull
  private final MZTolerance mzTolerance;
  @NotNull
  private final RTTolerance rtTolerance;
  private final long sourceStructuralVersion;

  private volatile double progress = 0d;

  public CompoundGrouperTask(@NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet parameters, @Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;
    this.mzTolerance = parameters.getValue(CompoundGrouperParameters.MZ_TOLERANCE);
    this.rtTolerance = parameters.getValue(CompoundGrouperParameters.RT_TOLERANCE);
    this.sourceStructuralVersion = featureList.getStructuralVersion();
  }

  @Override
  public String getTaskDescription() {
    return "Compound grouping in feature list " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.log(Level.INFO,
        () -> "Starting compound grouping for feature list " + featureList.getName() + " ("
            + featureList.getNumberOfRows() + " rows)");
    try {
      if (!precheck()) {
        return;
      }
      progress = 0.1d;

      final int estimatedCompounds = Math.max(featureList.getNumberOfRows() / 3, 16);
      final CompoundList compoundList = new CompoundList(featureList, getMemoryMapStorage(),
          estimatedCompounds);

      final SimpleSeederComponentizer componentizer = new SimpleSeederComponentizer(mzTolerance,
          rtTolerance);
      final List<ModularCompoundRow> rows = componentizer.componentize(featureList, compoundList);
      progress = 0.8d;

      if (isCanceled()) {
        return;
      }

      // staleness check — drop if the source list mutated structurally during processing
      if (featureList.getStructuralVersion() != sourceStructuralVersion) {
        logger.warning(() -> "Source feature list " + featureList.getName()
            + " changed structurally during compound grouping; dropping result.");
        setStatus(TaskStatus.FINISHED);
        return;
      }

      compoundList.setRows(rows);
      featureList.setCompoundList(compoundList);

      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(CompoundGrouperModule.class, parameters,
              getModuleCallDate()));

      progress = 1d;
      setStatus(TaskStatus.FINISHED);
      logger.log(Level.INFO,
          () -> "Compound grouping finished: " + rows.size() + " compound(s) for feature list "
              + featureList.getName());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Compound grouping failed for " + featureList.getName(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  private boolean precheck() {
    if (featureList.getNumberOfRows() == 0) {
      setErrorMessage("CompoundGrouper requires a non-empty feature list.");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    final boolean hasIin = IonNetworkLogic.streamNetworks(featureList, false).findAny().isPresent();
    final List<RowGroup> groups = featureList.getGroups();
    final boolean hasGroups = groups != null && !groups.isEmpty();
    if (!hasIin && !hasGroups) {
      setErrorMessage(
          "CompoundGrouper requires Ion Identity Networking or Correlation Grouping output. "
              + "Run those modules first.");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    return true;
  }
}
