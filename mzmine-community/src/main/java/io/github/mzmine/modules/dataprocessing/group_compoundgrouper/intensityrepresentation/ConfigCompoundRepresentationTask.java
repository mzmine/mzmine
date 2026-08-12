package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundIntensitySumBinding;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Records the chosen intensity representation as a {@link SimpleFeatureListAppliedMethod} and
 * triggers re-application of all {@link io.github.mzmine.datamodel.features.compoundlist.CompoundRowBinding}s
 * on the feature list's compound list. The actual aggregation happens inside
 * {@link CompoundIntensitySumBinding#apply}, which reads its configuration from the latest applied
 * method on every invocation.
 */
public class ConfigCompoundRepresentationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ConfigCompoundRepresentationTask.class.getName());

  @NotNull
  private final ModularFeatureList featureList;
  @NotNull
  private final ParameterSet parameters;

  private volatile double progress = 0d;

  public ConfigCompoundRepresentationTask(@NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet parameters, @Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Compound intensity representation for feature list " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final CompoundIntensityRepresentation mode = parameters.getValue(
        ConfigCompoundRepresentationParameters.INTENSITY_REPRESENTATION);
    logger.log(Level.INFO,
        () -> "Setting compound intensity representation = " + mode + " on feature list "
            + featureList.getName());

    try {
      final CompoundList compoundList = featureList.getCompoundList();
      if (compoundList == null) {
        setErrorMessage("Feature list '" + featureList.getName()
            + "' has no compound list. Run 'Compound grouping' first.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Record the choice on the feature list FIRST so the binding picks it up when re-applied.
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(ConfigCompoundRepresentationModule.class,
              parameters, getModuleCallDate()));
      progress = 0.2d;

      compoundList.reapplyBindings();
      progress = 1d;

      setStatus(TaskStatus.FINISHED);
      logger.log(Level.INFO,
          () -> "Compound intensity representation applied to " + compoundList.size()
              + " compound row(s) in feature list " + featureList.getName());
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Compound intensity representation failed for " + featureList.getName(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }
}
