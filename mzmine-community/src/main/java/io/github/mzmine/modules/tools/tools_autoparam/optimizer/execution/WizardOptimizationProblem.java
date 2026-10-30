/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.BatchParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.BenchmarkFeatureLoader;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationEvidence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameter;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ValueOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.BenchmarkFeatureParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.MetricContext;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SearchScaleProvider;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;
import org.moeaframework.core.constraint.LessThanOrEqual;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.core.variable.Variable;
import org.moeaframework.problem.AbstractProblem;

public class WizardOptimizationProblem extends AbstractProblem implements SearchScaleProvider, AutoCloseable {

  /**
   * Position of a solution in the evaluation order, so the results table can show convergence.
   */
  public static final String ATTR_PROPOSAL_INDEX = "Proposal";
  /**
   * Number of uncached full batches launched up to and including this proposal. Cache hits retain
   * the current value without incrementing it.
   */
  public static final String ATTR_BATCH_EXECUTION_INDEX = "Batch execution";
  /**
   * Seconds from problem construction until this proposal's result became available.
   */
  public static final String ATTR_ELAPSED_OPTIMIZATION_SECONDS = "Optimization elapsed / s";
  /**
   * Wall-clock time of this proposal's batch queue. Zero when no batch ran because of a cache hit.
   */
  public static final String ATTR_BATCH_RUNTIME_SECONDS = "Runtime / s";
  /**
   * Number of benchmark features from the user-supplied file that were matched in the result. Only
   * set when a benchmark features file was supplied.
   */
  public static final String ATTR_BENCHMARK_FEATURES = "Target features";
  /**
   * Number of features in the evaluated feature list.
   */
  public static final String ATTR_TOTAL_FEATURES = "Total features";
  private static final Logger logger = Logger.getLogger(WizardOptimizationProblem.class.getName());
  /**
   * Whether the result was taken from {@link #evaluationCache} instead of running a batch. Kept
   * visible because it is the only honest way to read the runtime column: a hit carries the runtime
   * of the original evaluation, not of itself.
   */
  private static final String ATTR_CACHE_HIT = "Cache hit";
  /**
   * Attributes describing an individual evaluation instead of its parameter vector, so they must
   * never be taken from a cached result.
   */
  private static final Set<String> OWN_ATTRIBUTES = Set.of(ATTR_PROPOSAL_INDEX,
      ATTR_BATCH_EXECUTION_INDEX, ATTR_ELAPSED_OPTIMIZATION_SECONDS, ATTR_BATCH_RUNTIME_SECONDS,
      ATTR_CACHE_HIT, SolutionOrigin.ATTRIBUTE);
  private final List<ParameterDefinition<?>> paramToOptimize;
  private final @NotNull List<IndexedParameter<?>> indexedParameters;
  private final @NotNull BooleanSupplier stopSearchRequestedSupplier;
  private final @NotNull List<SweepMetric> enabledMetrics;
  private final @NotNull OptimizationBatchEvaluator batchEvaluator;
  private final @NotNull List<FeatureRecord> target;
  private final WizardSequence initialSequence;
  /**
   * Raw-data estimates used as fixed baseline values for parameters outside the selected search
   * space. Selected optimizer variables are applied afterwards and therefore take precedence.
   */
  private final @NotNull PreparedParameterSet preparedParameters;
  /**
   * Aggregate diagnostics for the fixed estimate baseline. Candidate evaluations intentionally do
   * not replace this evidence because they do not re-measure the raw data.
   */
  private final @NotNull Map<String, ParameterEstimationEvidence> estimationEvidence;
  private final int estimationFileCount;
  private final @NotNull List<FeatureRecord> fileOnlyBenchmarkFeatures;
  private final @NotNull BatchExecutionBudget batchExecutionBudget;
  private final boolean evaluatesInitialUserSettings;
  private final @NotNull ElapsedTimeTracker elapsedTimeTracker = new ElapsedTimeTracker();
  /**
   * Every completed proposal in evaluation order, including cache hits. Keeping them lets the
   * results table show the whole search instead of only the non-dominated front, while the separate
   * batch-execution attribute supplies the true cost axis.
   * <p>
   * decision: synchronized because evaluation is sequential today but is a candidate for
   * parallelisation. Only the solutions are retained, never their feature lists.
   */
  private final List<Solution> evaluatedSolutions = Collections.synchronizedList(new ArrayList<>());
  /**
   * Results of already evaluated parameter vectors, keyed by the effective values the batch is run
   * with. Differential evolution leaves a vector untouched whenever its crossover rate does not
   * fire, and a converged population produces the same offspring repeatedly, so a noticeable share
   * of a run is spent re-running batches whose result is already known — around 30 % in the
   * single-objective runs.
   * <p>
   * decision: a hit consumes a proposal but not a batch execution. Search termination uses both a
   * real-batch budget and a generous proposal cap, so duplicates do not spend the expensive budget
   * and a converged algorithm still cannot loop forever.
   */
  private final Map<List<Double>, Solution> evaluationCache = new ConcurrentHashMap<>();
  /**
   * Upper limit for the share of features the strict shape filter would reject, in percent.
   * Permissive until {@link #setShapeRejectionLimitPercent(double)} is called, so the raw data
   * estimate itself is never marked infeasible.
   */
  private double shapeRejectionLimitPercent = Double.MAX_VALUE;
  /**
   * Optional observer for live result displays. The optimizer problem remains independent of
   * JavaFX; callers decide which thread consumes the completed solution.
   */
  private volatile @Nullable Consumer<Solution> evaluationListener;
  private final AtomicInteger completedBatchExecutions = new AtomicInteger();
  private final AtomicBoolean initialUserSettingsEvaluated = new AtomicBoolean();

  public WizardOptimizationProblem(@NotNull ParameterEstimationContext estimationContext,
      @NotNull PreparedParameterSet prepared, @NotNull ParameterSet param,
      @NotNull AtomicReference<TaskStatus> externalStatus, int maxBatchExecutions,
      @NotNull BooleanSupplier stopSearchRequestedSupplier) {
    this(estimationContext, prepared, param, resolveParamToOptimize(prepared, param),
        externalStatus, maxBatchExecutions, stopSearchRequestedSupplier);
  }

  private WizardOptimizationProblem(@NotNull ParameterEstimationContext estimationContext,
      @NotNull PreparedParameterSet prepared, @NotNull ParameterSet param,
      @NotNull List<ParameterDefinition<?>> paramToOptimize,
      @NotNull AtomicReference<TaskStatus> externalStatus, int maxBatchExecutions,
      @NotNull BooleanSupplier stopSearchRequestedSupplier) {

    // decision: super() must be first — use static helper for objective count before enabledMetrics field is assigned
    super(paramToOptimize.size(), calculateNumberOfObjectives(param),
        calculateNumberOfConstraints(param));

    final List<DataFileStatistics> stats = estimationContext.analysis().files();

    fileOnlyBenchmarkFeatures = BenchmarkFeatureParameters.loadBenchmarkFeatures(param);
    batchExecutionBudget = new BatchExecutionBudget(maxBatchExecutions);
    evaluatesInitialUserSettings = param.getValue(OptimizerParameters.evaluateInitialUserSettings);
    target = Objects.requireNonNull(BenchmarkFeatureLoader.fromStatistics(stats));
    this.paramToOptimize = List.copyOf(paramToOptimize);
    this.stopSearchRequestedSupplier = stopSearchRequestedSupplier;
    enabledMetrics = List.copyOf(OptimizerParameters.getOptimizationTargets(param));

    initialSequence = estimationContext.sequence();
    final File[] files = stats.stream().map(DataFileStatistics::file)
        .map(RawDataFile::getAbsoluteFilePath).toArray(File[]::new);
    batchEvaluator = new OptimizationBatchEvaluator(files, enabledMetrics, new MetricContext(target),
        fileOnlyBenchmarkFeatures, externalStatus);

    preparedParameters = prepared;
    estimationEvidence = prepared.parameters().stream().collect(Collectors.toUnmodifiableMap(
        parameter -> parameter.definition().id(),
        parameter -> ParameterEstimationEvidence.describe(estimationContext, parameter)));
    estimationFileCount = stats.size();
    indexedParameters = IndexedParameter.bind(preparedParameters, paramToOptimize);
  }

  /**
   * Only the selected parameters that apply to the final wizard presets are optimized. Must be
   * static because it is needed inside the {@code super(...)} call.
   *
   * @throws IllegalStateException if none of the selected parameters applies
   */
  static @NotNull List<ParameterDefinition<?>> resolveParamToOptimize(
      @NotNull PreparedParameterSet prepared, @NotNull ParameterSet param) {
    final List<ParameterDefinition<?>> selected = param.getValue(
        OptimizerParameters.paramToOptimize);
    final List<ParameterDefinition<?>> applicable = prepared.applicable(selected);
    if (applicable.isEmpty()) {
      throw new IllegalStateException("""
          None of the selected parameters to optimize applies to the wizard presets. \
          Select at least one of the listed parameters.""");
    }
    if (applicable.size() < selected.size()) {
      logger.fine(() -> "Not optimized, the wizard presets do not use: " + selected.stream()
          .filter(definition -> !applicable.contains(definition)).map(ParameterDefinition::name)
          .collect(Collectors.joining(", ")));
    }
    return applicable;
  }

  /**
   * One constraint when the shape rejection limit is enabled, none otherwise. Must be static
   * because it is needed inside the {@code super(...)} call.
   */
  static int calculateNumberOfConstraints(@NotNull ParameterSet param) {
    return param.getValue(OptimizerParameters.maxShapeRejectionFactor) ? 1 : 0;
  }

  /**
   * One objective per selected metric. Must be static because it is needed inside the
   * {@code super(...)} call.
   */
  static int calculateNumberOfObjectives(@NotNull ParameterSet param) {
    return OptimizerParameters.getOptimizationTargets(param).size();
  }

  @Override
  public void evaluate(@NotNull Solution solution) {

    // decision: a GUI stop is graceful: finish the batch already in progress, but reject the next
    // proposal before cache lookup or another full batch can start.
    if (stopSearchRequestedSupplier.getAsBoolean()) {
      throw new OptimizationSearchStoppedException();
    }

    // untagged means the variation operators built it, see SolutionOrigin#applyIfAbsent
    SolutionOrigin.EVOLUTION.applyIfAbsent(solution);

    final List<Double> cacheKey = cacheKey(solution);
    final Solution cached = evaluationCache.get(cacheKey);
    if (cached != null) {
      copyResult(cached, solution);
      solution.setAttribute(ATTR_CACHE_HIT, true);
      solution.setAttribute(ATTR_BATCH_RUNTIME_SECONDS, 0d);
      solution.setAttribute(ATTR_PROPOSAL_INDEX, evaluatedSolutions.size() + 1);
      solution.setAttribute(ATTR_BATCH_EXECUTION_INDEX, batchExecutionBudget.count());
      solution.setAttribute(ATTR_ELAPSED_OPTIMIZATION_SECONDS, elapsedTimeTracker.elapsedSeconds());
      evaluatedSolutions.add(solution);
      notifyEvaluationCompleted(solution);
      return;
    }

    final WizardSequence wizardSequence = createWizardSequenceFromSolution(solution);
    final int batchExecutionIndex = batchEvaluator.evaluate(wizardSequence, solution,
        getNumberOfConstraints() > 0, batchExecutionBudget::reserve);

    completedBatchExecutions.incrementAndGet();
    solution.setAttribute(ATTR_CACHE_HIT, false);
    solution.setAttribute(ATTR_PROPOSAL_INDEX, evaluatedSolutions.size() + 1);
    solution.setAttribute(ATTR_BATCH_EXECUTION_INDEX, batchExecutionIndex);
    solution.setAttribute(ATTR_ELAPSED_OPTIMIZATION_SECONDS, elapsedTimeTracker.elapsedSeconds());
    evaluatedSolutions.add(solution);
    evaluationCache.put(cacheKey, solution);
    notifyEvaluationCompleted(solution);
  }

  /**
   * Scores the wizard settings captured before estimation without translating them through the
   * optimizer search domain. In particular, this avoids clamping user values that lie outside that
   * domain.
   * <p>
   * This baseline is intentionally absent from {@link #evaluatedSolutions} and the cache: it is a
   * separately labelled comparison, not an optimizer observation or a rankable candidate. Its
   * batch is explicitly outside the configured estimate and search budget.
   *
   * @param initialUserSettings wizard settings captured before the task began
   * @return the scored initial-user-settings baseline
   */
  public @NotNull Solution evaluateInitialUserSettings(
      @NotNull WizardSequence initialUserSettings) {
    if (!evaluatesInitialUserSettings) {
      throw new IllegalStateException("Initial user settings comparison is disabled.");
    }
    if (!initialUserSettingsEvaluated.compareAndSet(false, true)) {
      throw new IllegalStateException("Initial user settings may only be evaluated once.");
    }
    if (stopSearchRequestedSupplier.getAsBoolean()) {
      throw new OptimizationSearchStoppedException();
    }
    final Solution initial = newInitialUserSettingsSolution(initialUserSettings);
    SolutionOrigin.INITIAL_USER_SETTINGS.applyTo(initial);
    final int batchExecutionIndex = batchEvaluator.evaluate(copyForEvaluation(initialUserSettings),
        initial, getNumberOfConstraints() > 0, batchExecutionBudget::reserveAdditional);
    completedBatchExecutions.incrementAndGet();
    initial.setAttribute(ATTR_CACHE_HIT, false);
    initial.setAttribute(ATTR_BATCH_EXECUTION_INDEX, batchExecutionIndex);
    initial.setAttribute(ATTR_ELAPSED_OPTIMIZATION_SECONDS, elapsedTimeTracker.elapsedSeconds());
    return initial;
  }

  /**
   * Creates a scored comparison row without optimizer variables. Its values are a snapshot of the
   * native wizard and generated batch queue, so the table and CSV never report clamped estimates
   * as the user's initial settings.
   */
  private @NotNull Solution newInitialUserSettingsSolution(
      @NotNull WizardSequence initialUserSettings) {
    final Solution initial = new Solution(0, getNumberOfObjectives(), getNumberOfConstraints());
    if (getNumberOfConstraints() > 0) {
      initial.setConstraint(0, new LessThanOrEqual(ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT,
          shapeRejectionLimitPercent));
    }
    int objectiveIndex = 0;
    for (final SweepMetric metric : enabledMetrics) {
      initial.setObjective(objectiveIndex++, metric.objective());
    }
    initial.setAttribute(IndexedParameter.ATTR_DISPLAY_VALUES,
        new LinkedHashMap<>(initialUserSettingsDisplayValues(initialUserSettings)));
    return initial;
  }

  /**
   * Snapshots display values only for the initial user batch comparison, not optimizer candidates.
   */
  private @NotNull Map<String, String> initialUserSettingsDisplayValues(
      @NotNull WizardSequence initialUserSettings) {
    final Map<String, String> values = new LinkedHashMap<>();
    final boolean hasBatchParameters = indexedParameters.stream()
        .anyMatch(parameter -> parameter.parameter().definition() instanceof BatchParameterDefinition<?>);
    final BatchQueue queue = hasBatchParameters ? createInitialSettingsQueue(initialUserSettings)
        : null;
    for (final IndexedParameter<?> parameter : indexedParameters) {
      final String value = switch (parameter.parameter().definition()) {
        case WizardParameterDefinition<?> definition -> parameter.formatNativeValue(
            initialUserSettings.get(definition.part()).orElseThrow().getValue(definition.parameter()));
        case BatchParameterDefinition<?> definition -> effectiveBatchParameterValue(parameter,
            Objects.requireNonNull(queue), definition);
      };
      values.put(parameter.parameter().definition().id(), value);
    }
    return values;
  }

  /**
   * Builds the same native queue that is evaluated, including enabled customization overrides,
   * solely to snapshot effective advanced-parameter values for the non-applicable baseline row.
   */
  private @NotNull BatchQueue createInitialSettingsQueue(@NotNull WizardSequence initialUserSettings) {
    return batchEvaluator.createEvaluationQueue(copyForEvaluation(initialUserSettings));
  }

  /**
   * Formats an effective batch parameter only for the initial user batch comparison.
   */
  private static @NotNull String effectiveBatchParameterValue(@NotNull IndexedParameter<?> indexed,
      @NotNull BatchQueue queue, @NotNull BatchParameterDefinition<?> definition) {
    final List<ParameterSet> matching = queue.stream()
        .filter(step -> step.getModule().getClass().getName().equals(definition.moduleClassName()))
        .map(MZmineProcessingStep::getParameterSet).toList();
    final List<ParameterSet> scoped = switch (definition.scope()) {
      case ALL -> matching;
      case FIRST -> matching.isEmpty() ? List.of() : List.of(matching.getFirst());
      case LAST -> matching.isEmpty() ? List.of() : List.of(matching.getLast());
    };
    final List<String> values = scoped.stream()
        .map(parameters -> findParameter(parameters, definition.parameterName()))
        .filter(Objects::nonNull).map(parameter -> indexed.formatNativeValue(parameter.getValue()))
        .distinct().toList();
    return switch (values.size()) {
      case 0 -> "not present in generated batch";
      case 1 -> values.getFirst();
      default -> "varies by batch step: " + String.join("; ", values);
    };
  }

  private static @Nullable Parameter<?> findParameter(@NotNull ParameterSet parameters,
      @NotNull String name) {
    return Arrays.stream(parameters.getParameters())
        .filter(parameter -> parameter.getName().equals(name)).findFirst().orElse(null);
  }

  @Override
  public @NotNull SearchScale searchScale(int parameterIndex) {
    return indexedParameters.get(parameterIndex).parameter().searchDomain().searchScale();
  }

  /**
   * @return a copy of the wizard sequence the optimization started from, with the presets that fit
   * the raw data if the user switched to them
   */
  public @NotNull WizardSequence getInitialSequence() {
    return initialSequence.copy();
  }

  public @NotNull List<IndexedParameter<?>> getIndexedParameters() {
    return indexedParameters;
  }

  /**
   * Aggregate evidence for the fixed raw-data estimate baseline, keyed by parameter definition ID.
   */
  public @NotNull Map<String, ParameterEstimationEvidence> getEstimationEvidence() {
    return estimationEvidence;
  }

  /**
   * Number of files from which the fixed estimate baseline was prepared.
   */
  public int getEstimationFileCount() {
    return estimationFileCount;
  }

  private void notifyEvaluationCompleted(@NotNull Solution solution) {
    final Consumer<Solution> listener = evaluationListener;
    if (listener != null) {
      try {
        listener.accept(solution);
      } catch (RuntimeException e) {
        // decision: a presentation observer must never abort an expensive optimization.
        logger.log(Level.WARNING, "Could not publish completed optimizer evaluation", e);
      }
    }
  }

  /**
   * Identifies a parameter vector by the values the batch is actually run with.
   * <p>
   * decision: the effective value, not the raw one. {@link OrdinalIntegerVariable} is backed by a
   * real value that is rounded when read, so two vectors differing only in the discarded fraction
   * build an identical batch queue and must share a key.
   */
  private @NotNull List<Double> cacheKey(@NotNull Solution solution) {
    final List<Double> key = new ArrayList<>(solution.getNumberOfVariables());
    for (int i = 0; i < solution.getNumberOfVariables(); i++) {
      final Variable variable = solution.getVariable(i);
      key.add(variable instanceof OrdinalIntegerVariable ? OrdinalIntegerVariable.effectiveValue(
          solution, i) : RealVariable.getReal(variable));
    }
    return List.copyOf(key);
  }

  /**
   * Copies everything the algorithm and the results table read from an evaluated solution:
   * objectives drive selection, constraints drive feasibility, attributes drive the table.
   * <p>
   * {@link #OWN_ATTRIBUTES} are excluded, because they describe this evaluation rather than the
   * parameter vector - a cache hit is its own position in the search and keeps its own origin.
   */
  private void copyResult(@NotNull Solution from, @NotNull Solution to) {
    for (int i = 0; i < to.getNumberOfObjectives(); i++) {
      to.setObjectiveValue(i, from.getObjectiveValue(i));
    }
    for (int i = 0; i < to.getNumberOfConstraints(); i++) {
      to.setConstraintValue(i, from.getConstraintValue(i));
    }
    from.getAttributes().forEach((key, value) -> {
      if (!OWN_ATTRIBUTES.contains(key)) {
        to.setAttribute(key, value);
      }
    });
  }

  /**
   * Creates a detached candidate from preset defaults and the prepared optimization baseline, then
   * applies the solution's parameter values.
   * <p>
   * The complete original Data configuration is copied instead of reset to defaults, so candidates
   * and the optional scored initial-user-settings comparison use the same import and metadata
   * extraction settings. The evaluator replaces the raw-file selection on this temporary sequence
   * with the fixed analysis files before building the batch. The live wizard's full file selection
   * remains unchanged.
   *
   * @param solution the candidate's optimization variables
   * @return a detached wizard sequence with the candidate's parameter values
   */
  public @NotNull WizardSequence createWizardSequenceFromSolution(@NotNull Solution solution) {

    final WizardSequence wizardSequence = new WizardSequence();

    // decision: candidates keep the same full Data settings as the initial-user baseline. The
    // evaluator replaces only the fixed selected raw files immediately before queue creation.
    final WizardStepParameters sourceData = initialSequence.get(WizardPart.DATA_IMPORT).orElseThrow();
    final WizardStepParameters dataParam = sourceData.getFactory().create();
    ParameterUtils.copyParameters(sourceData, dataParam);
    final WizardStepParameters lcParam = initialSequence.get(WizardPart.ION_INTERFACE).get()
        .createDefaultParameterPreset().getFactory().create();
    final WizardStepParameters filterParam = initialSequence.get(WizardPart.FILTER).get()
        .createDefaultParameterPreset().getFactory().create();
    final WizardStepParameters imsParam = initialSequence.get(WizardPart.IMS).get()
        .createDefaultParameterPreset().getFactory().create();
    final WizardStepParameters msParam = initialSequence.get(WizardPart.MS).get()
        .createDefaultParameterPreset().getFactory().create();
    final WizardStepParameters annotationParam = initialSequence.get(WizardPart.ANNOTATION).get()
        .getFactory().create();
    final WizardStepParameters workflowParam = initialSequence.get(WizardPart.WORKFLOW).get()
        .getFactory().create();
    final WizardStepParameters customizationParameters = initialSequence.get(
        WizardPart.CUSTOMIZATION).get().createDefaultParameterPreset().getFactory().create();

    wizardSequence.add(dataParam);
    wizardSequence.add(lcParam);
    wizardSequence.add(filterParam);
    wizardSequence.add(imsParam);
    wizardSequence.add(msParam);
    wizardSequence.add(annotationParam);
    wizardSequence.add(workflowParam);
    wizardSequence.add(customizationParameters);

    preparedParameters.applyBaseline(wizardSequence, Set.copyOf(paramToOptimize));
    for (final IndexedParameter<?> parameter : indexedParameters) {
      parameter.applyToWizard(solution, wizardSequence);
    }

    disableExportPath(workflowParam);

    return wizardSequence;
  }

  static @NotNull WizardSequence copyForEvaluation(@NotNull WizardSequence source) {
    final WizardSequence copy = source.copy();
    final WizardStepParameters workflow = copy.get(WizardPart.WORKFLOW).orElseThrow();
    disableExportPath(workflow);
    return copy;
  }

  private static void disableExportPath(@NotNull WizardStepParameters workflow) {
    if (workflow.getNameParameterMap()
        .get(WorkflowDdaWizardParameters.exportPath.getName()) instanceof OptionalParameter<?>) {
      workflow.setParameter(WorkflowDdaWizardParameters.exportPath, false);
    }
  }

  /**
   * Applies the estimated and optimized values of a solution to an existing wizard sequence, see
   * {@link #applySolutionToWizard(Solution, WizardSequence, SolutionApplyMode)}.
   */
  public void applySolutionToWizard(@NotNull Solution solution, @NotNull WizardSequence sequence) {
    applySolutionToWizard(solution, sequence, SolutionApplyMode.ESTIMATED_AND_OPTIMIZED);
  }

  /**
   * Applies the optimized values of a solution to an existing wizard sequence and, depending on the
   * mode, also the estimated values the same way as applying the raw data estimates. All other
   * parameters keep their current values. With {@link SolutionApplyMode#ESTIMATED_AND_OPTIMIZED},
   * the applied values equal the ones the solution was evaluated with in
   * {@link #createWizardSequenceFromSolution(Solution)} as long as the sequence uses the same
   * presets as the optimization.
   * <p>
   * assumption: parameters with a {@link ValueOrigin#PRESET_DEFAULT} value that were not optimized
   * are not estimates and are therefore left unchanged.
   *
   * @param solution the solution to apply
   * @param sequence the wizard sequence to modify
   * @param mode     whether the estimates of the not optimized parameters are applied as well
   */
  public void applySolutionToWizard(@NotNull Solution solution, @NotNull WizardSequence sequence,
      @NotNull SolutionApplyMode mode) {
    if (mode == SolutionApplyMode.ESTIMATED_AND_OPTIMIZED) {
      preparedParameters.applyEstimates(sequence, Set.copyOf(paramToOptimize));
    }
    for (final IndexedParameter<?> parameter : indexedParameters) {
      parameter.applyToWizard(solution, sequence);
    }
  }

  /**
   * @return one line per parameter that
   * {@link #applySolutionToWizard(Solution, WizardSequence, SolutionApplyMode)} sets, formatted as
   * {@code part: name -> value (Optimized|Estimated)}, optimized parameters first. Advanced
   * parameters use the module name instead of the part
   */
  public @NotNull List<String> describeAppliedValues(@NotNull Solution solution,
      @NotNull SolutionApplyMode mode) {
    final List<String> lines = new ArrayList<>();
    for (final IndexedParameter<?> parameter : indexedParameters) {
      lines.add("%s: %s -> %s (Optimized)".formatted(switch (parameter.parameter().definition()) {
        case WizardParameterDefinition<?> w -> w.part().toString();
        case BatchParameterDefinition<?> b ->
            Optional.ofNullable(MZmineCore.getModuleInstance(b.moduleClassName()))
                .map(MZmineModule::getName).orElse("Unknown") + " (Advanced)";
      }, parameter.parameter().definition().name(), parameter.formatValue(solution)));
    }
    if (mode == SolutionApplyMode.ESTIMATED_AND_OPTIMIZED) {
      for (final PreparedParameter<?> parameter : preparedParameters.estimates(
          Set.copyOf(paramToOptimize))) {
        lines.add("%s: %s -> %s (Estimated)".formatted(switch (parameter.definition()) {
          case WizardParameterDefinition<?> w -> w.part().toString();
          case BatchParameterDefinition<?> b ->
              Optional.ofNullable(MZmineCore.getModuleInstance(b.moduleClassName()))
                  .map(MZmineModule::getName).orElse("Unknown") + " (Advanced)";
        }, parameter.definition().name(), parameter.formatInitialValue()));
      }
    }
    return lines;
  }

  @Override
  public @NotNull Solution newSolution() {

    final Solution solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(),
        getNumberOfConstraints());

    if (getNumberOfConstraints() > 0) {
      solution.setConstraint(0, new LessThanOrEqual(ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT,
          shapeRejectionLimitPercent));
    }

    for (final IndexedParameter<?> parameter : indexedParameters) {
      parameter.initialize(solution);
    }

    int objectiveIndex = 0;
    for (SweepMetric metric : enabledMetrics) {
      solution.setObjective(objectiveIndex++, metric.objective());
    }

    return solution;
  }

  /**
   * Sets the shape rejection limit used by the constraint. Call after the raw data estimate has
   * been evaluated, so the limit can be derived from a measured baseline rather than guessed.
   */
  public void setShapeRejectionLimitPercent(double limitPercent) {
    this.shapeRejectionLimitPercent = limitPercent;
    logger.info("Shape rejection limit set to %.1f %%".formatted(limitPercent));
  }

  /**
   * Every evaluated solution in evaluation order, including infeasible ones and the raw data
   * estimate.
   */
  public @NotNull List<Solution> getEvaluatedSolutions() {
    synchronized (evaluatedSolutions) {
      return List.copyOf(evaluatedSolutions);
    }
  }

  /**
   * Sets a single observer that is called after each proposal has a complete score and diagnostic
   * attributes. Passing {@code null} detaches the observer.
   */
  public void setEvaluationListener(@Nullable Consumer<Solution> evaluationListener) {
    this.evaluationListener = evaluationListener;
  }

  /**
   * Number of uncached full batches reserved for execution, including the raw-data estimate.
   */
  public int getBatchExecutionCount() {
    return batchExecutionBudget.count();
  }

  /** Number of batches consumed from the configured estimate and search budget. */
  public int getSearchBatchExecutionCount() {
    return batchExecutionBudget.searchCount();
  }

  /**
   * Number of uncached full batches that finished with a score, including the raw-data estimate.
   * Lower than {@link #getBatchExecutionCount()} while a batch runs.
   */
  public int getCompletedBatchExecutionCount() {
    return completedBatchExecutions.get();
  }

  /**
   * @return all full batches expected for this run, including the optional initial-user-settings
   *     comparison batch
   */
  public int getMaxBatchExecutions() {
    return batchExecutionBudget.maximum() + (evaluatesInitialUserSettings ? 1 : 0);
  }

  public @NotNull List<FeatureRecord> getAllTargets() {
    return target;
  }

  public @NotNull List<FeatureRecord> getFileOnlyBenchmarkFeatures() {
    return fileOnlyBenchmarkFeatures;
  }

  public @NotNull List<SweepMetric> getEnabledMetrics() {
    return enabledMetrics;
  }

  @Override
  public void close() {
    batchEvaluator.close();
  }

  /** Requests cancellation without blocking a caller such as the JavaFX status listener. */
  public void cancel() {
    batchEvaluator.cancel();
  }

}
