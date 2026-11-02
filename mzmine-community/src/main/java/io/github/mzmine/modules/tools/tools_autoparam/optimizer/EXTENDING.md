# Extending the parameter optimizer

The optimizer has three independent extension points: search algorithms, scores evaluated on a
feature list, and parameters varied by the search.

## Package layout

Paths below are relative to `io.github.mzmine.modules.tools.tools_autoparam`.

- `runphases`: run phase detection for the effective RT range of a file.
- `statistics`: per-file statistics (the `AutoParamModule` with its task and pane), raw-data import
  and preparation (`RawDataPreparation`), benchmark feature records, and the m/z tolerance options.
- `preclassification`: settings fixed from the imported files before the statistics, e.g. polarity.
- `estimation`: cross-file analysis, benchmark loading, typed definitions, estimation rules,
  prepared values, and the wizard's estimate-only task. It does not depend on optimizer code.
- `estimation.domain`: typed search domains with plain coordinate bounds and search scales. It does
  not depend on MOEA.
- `optimizer.search`: search algorithms, their module settings, canonical coordinates, warm-start
  initialization, and the ordinal MOEA variable.
- `optimizer.execution`: binding prepared values to vector indices and creating the MOEA
  variables from the domain bounds, batch evaluation, execution budgets, termination, and timing.
- `optimizer.metrics`: metric implementations, their catalog, and score diagnostics.
- `optimizer.gui`: progress and result presentation.
- `optimizer`: module entry points, configuration, main-task orchestration, outcomes, and logging.
- top level: the statistics dashboard.

Packages only depend on packages earlier in this order: `runphases`, `statistics`,
`preclassification`, `estimation`, top level, `optimizer`. Keep it free of cycles.

Keep the sealed `ParameterDefinition` interface and its implementations together in `estimation`.
The registry exposes the applicable definitions; its individual constants and optional wavelet
helpers remain package-private. Search and execution consume prepared values without making
estimation depend on either package.

## Add a search algorithm

1. Implement `OptimizerAlgorithmModule` in `search/` with its own parameter set. The module reads
   and writes its optimization targets (one target or a checklist) and creates the configured
   algorithm, including its start solutions around the raw-data estimate.
2. Add an entry to `OptimizerOptions` with a stable ID.

`BatchOptimizationMainTask` creates the algorithm after the raw-data estimate was evaluated, so
start solutions from `problem.newSolution()` use the final constraint limits. The main task has
no algorithm-specific code.

## Add an evaluation metric

1. Implement `SweepMetric` under `metrics/`, or in another module. The metric holds no
   run-specific state.
2. Add a singleton to the catalog `OptimizationMetrics` and to `OptimizationMetrics.ALL`. Add it to
   `DEFAULT` only if it should be selected for new configurations.

A metric supplies its display name, a stable `getUniqueID()` for save and load, its optimization
direction, and `evaluate(FeatureList, MetricContext)`. Data that only exists during a run, e.g.,
the benchmark targets derived from the raw data statistics, is added to `MetricContext` instead of
the metric. Override `preferredForRanking` to sort and compare results by this metric, and
`applyAttributes` only for additional diagnostic values that should appear in results.
`WizardOptimizationProblem` creates one objective for every selected metric and the run's
`MetricContext`; `OptimizationBatchEvaluator` applies the metrics after each batch.

## Add an optimization parameter

Add a typed definition in `OptimizationParameterRegistry`.
Use `WizardParameterDefinition<T>` for a wizard parameter and `BatchParameterDefinition<T>` for
a processing-module override. Pass the actual `UserParameter<T, ?>` to connect its value type to
the estimator. Search domains convert composite values; optional wavelet reflection is isolated in
`WaveletParameterDefinitions`.

Each definition's estimator receives a `ParameterEstimationContext` and returns a
`ParameterEstimate<T>`: initial value, `ValueOrigin`, and `SearchDomain<T>`. Keep estimation and
range rules together. Use `DoubleSearchDomain`, `IntegerSearchDomain`, or `ChoiceSearchDomain<T>`
as appropriate. `MappedSearchDomain<T>` supports composite or unit-carrying values controlled by one
continuous coordinate, e.g. RT tolerances searched in minutes. Explicitly declare linear or
logarithmic continuous search.

Each definition declares

- its `OptimizationRole`: `ESTIMATE_ONLY` (estimated and applied, never offered to the optimizer),
  `OPTIONAL` (can be selected), or `DEFAULT` (selected in new configurations), and
- the wizard presets it applies to. List them explicitly; presets with a parameter of the same name
  (e.g. direct infusion or GC-EI) do not imply that the definition applies.

Add the constant to the registry's `ALL` list. `allSolutions()`, `defaultSolutions()`, and
`forSequence()` are derived from role and presets. Optional-module definitions must not load their
module classes during registry initialization and use the `OPTIONAL` role.

IDs are derived automatically from the binding: wizard part and parameter name, or module class,
application scope, and parameter name. Optional reflected bindings use their parameter field name
so IDs remain available without loading the module. No separate ID registration is needed.

XML stores these target-derived IDs for optimizer selections. Optimization names are display labels
only; there are no legacy-name aliases. Statistics, prepared parameters, and indexed adapters are
runtime objects.

## Runtime boundary

1. `RawDataPreparation` imports files and computes per-file statistics. `RawDataAnalysis.analyze`
   aggregates those measurements and aligns cross-file benchmark features once. It does not choose
   initial parameter values or search bounds.
2. `PreparedParameterSet.prepare` prepares every applicable definition, retaining the
   typed value, provenance, domain, and definition in an immutable `PreparedParameterSet`.
3. `WizardOptimizationProblem` receives the completed preparation and binds only selected
   parameters to vector indices via `IndexedParameter.bind(prepared, selected)`. It owns
   constraints,
   cache, and evaluation history. `WarmStartInitialization` perturbs the initialized coordinates.

Direct application and optimizer decoding use the same definition's binding. Unselected parameters
remain at their prepared baseline; selected candidate values replace them. Inter-sample RT always
uses its prepared estimate, without a later quantile override. The wizard's estimate-only action
applies raw-data estimates and heuristics while preserving existing values for preset fallbacks.

`OptimizationBatchEvaluator` builds and runs the reduced batch queue, evaluates metrics, and
attaches diagnostics. General wizard preset factories must not depend on optimization definitions.

Verify that direct application and optimizer encode/decode produce equivalent wizard/batch values.
Also check reordered selections, fixed baseline parameters, fallback values, and stable-ID XML
round trips.
