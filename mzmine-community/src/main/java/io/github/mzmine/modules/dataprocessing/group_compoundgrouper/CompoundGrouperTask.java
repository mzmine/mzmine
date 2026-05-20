package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerModule;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelectorModule;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a {@link CompoundList} for a single {@link ModularFeatureList} using the user-selected
 * {@link CompoundComponentizerStrategy}.
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
  private final CompoundComponentizerStrategy strategy;
  private final long sourceStructuralVersion;
  private final boolean isSubTask;

  private volatile double progress = 0d;

  public CompoundGrouperTask(@NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet parameters, @NotNull final Instant moduleCallDate,
      boolean isSubTask) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;
    this.strategy = createStrategy(parameters);
    this.sourceStructuralVersion = featureList.getStructuralVersion();
    this.isSubTask = isSubTask;
  }

  private static @NotNull CompoundComponentizerStrategy createStrategy(
      @NotNull final ParameterSet parameters) {
    final CompoundRepresentativeSelector selector = createRepresentativeSelector(parameters);
    final ModuleOptionsEnumComboParameter<CompoundComponentizerType> combo = parameters.getParameter(
        CompoundGrouperParameters.COMPONENTIZER);
    final CompoundComponentizerType type = combo.getValue();
    final ParameterSet sub = combo.getEmbeddedParameters();
    final CompoundComponentizerModule module = type.getModuleInstance();
    return module.createStrategy(sub, selector);
  }

  private static @NotNull CompoundRepresentativeSelector createRepresentativeSelector(
      @NotNull final ParameterSet parameters) {
    final ModuleOptionsEnumComboParameter<CompoundRepresentativeSelectorOption> combo = parameters.getParameter(
        CompoundGrouperParameters.REPRESENTATIVE_SELECTOR);
    final CompoundRepresentativeSelectorOption type = combo.getValue();
    final ParameterSet sub = combo.getEmbeddedParameters();
    final CompoundRepresentativeSelectorModule module = type.getModuleInstance();
    return module.createSelector(sub);
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
      final String validation = strategy.validateInputs(featureList);
      if (validation != null) {
        setErrorMessage(validation);
        setStatus(TaskStatus.ERROR);
        return;
      }
      progress = 0.1d;

      final int estimatedCompounds = Math.max(featureList.getNumberOfRows() / 3, 16);
      final CompoundList compoundList = new CompoundList(featureList, getMemoryMapStorage(),
          estimatedCompounds);

      final List<ModularCompoundRow> rows = strategy.componentize(featureList, compoundList);
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

      // if this is a subtask then dont add applied task as this will be done by the parent task
      // otherwise the step may be applied twice when generating a batch from applied steps
      if (!isSubTask) {
        featureList.addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod(CompoundGrouperModule.class, parameters,
                getModuleCallDate()));
      }

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
}
