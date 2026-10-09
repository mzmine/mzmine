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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.OptimizationParameterRegistry;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerOptions;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.PatternSearchOptimizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.CheckListParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class OptimizerParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<OptimizerOptions> optimizers = new ModuleOptionsEnumComboParameter<>(
      "Optimizer", "Choose the search algorithm and configure its specific settings.",
      OptimizerOptions.values(), OptimizerOptions.PATTERN_SEARCH);

  private static final List<ImportType<?>> DEFAULT_IMPORT_TYPES = List.of(
      new ImportType<>(true, "mz", new MZType()), new ImportType<>(true, "rt", new RTType()),
      new ImportType<>(false, "mobility", new MobilityType()));
  public static final ImportTypeParameter benchmarkFeatureTypes = new ImportTypeParameter(
      "Benchmark feature csv column names", "", DEFAULT_IMPORT_TYPES);

  public static final OptionalParameter<FileNameParameter> benchmarkFeaturesFile = new OptionalParameter<>(
      new FileNameParameter("Benchmark features file (optional)",
          "Optional file with additional benchmark features.", ExtensionFilters.CSV_TSV_IMPORT,
          FileSelectionType.OPEN));

  /**
   * The search needs some batches after the raw data estimate to improve on it.
   */
  public static final int MIN_ITERATIONS = 30;
  private static final int DEFAULT_ITERATIONS = 70;
  public static final IntegerParameter iterations = new IntegerParameter("Iterations",
      "Maximum number of uncached full batch executions, including the raw-data estimate. Cached "
          + "duplicate proposals do not consume this budget.", DEFAULT_ITERATIONS, MIN_ITERATIONS,
      10_000);
  private static final double DEFAULT_SHAPE_REJECTION_FACTOR = 1.5;
  public static final OptionalParameter<DoubleParameter> maxShapeRejectionFactor = new OptionalParameter<>(
      new DoubleParameter("Max shape rejection factor", """
          Rejects parameter sets that produce badly shaped peaks, as a multiple of the rate measured \
          for the raw data estimate.
          Sensitivity metrics reward detecting more signals, which can be satisfied by picking up \
          noise. This limits how much worse than the estimate a solution's chromatographic shape \
          quality may get, without changing any score.""",
          ConfigService.getGuiFormats().scoreFormat(), DEFAULT_SHAPE_REJECTION_FACTOR, 1.0, 100.0),
      true);

  /**
   * Definitions carry stable identities, typed estimators, and wizard/batch bindings. The selection
   * stores definitions; dataset-specific values are prepared only at runtime.
   */
  private static final List<ParameterDefinition<?>> ALL_SOLUTIONS = OptimizationParameterRegistry.allSolutions();
  private static final List<ParameterDefinition<?>> DEFAULT_SOLUTIONS = OptimizationParameterRegistry.defaultSolutions();

  public static final CheckListParameter<ParameterDefinition<?>> paramToOptimize = new CheckListParameter<>(
      "Parameters to optimize", "Select which parameters should be optimized.", ALL_SOLUTIONS,
      new ArrayList<>(DEFAULT_SOLUTIONS));

  public static final BooleanParameter showExtendedStatistics = new BooleanParameter(
      "Show extended statistics", """
      Shows the data file statistics dashboard and all evaluated solutions with their diagnostic \
      attributes in the results window.
      If disabled, only the raw data estimate and the current best solutions are shown with their \
      parameter values and optimization targets.""", false);

  public OptimizerParameters() {
    super(benchmarkFeaturesFile, benchmarkFeatureTypes, optimizers, iterations,
        maxShapeRejectionFactor, paramToOptimize, showExtendedStatistics);
  }

  /**
   * Collects all optimization parameter definitions that are relevant for the given wizard sequence
   * from the optimizer's central parameter registry.
   *
   * @param steps the current wizard sequence
   * @return ordered list of applicable definitions
   */
  public static @NotNull List<ParameterDefinition<?>> collectSolutions(
      @NotNull WizardSequence steps) {
    return OptimizationParameterRegistry.forSequence(steps);
  }

  /**
   * Convenience factory for programmatic use (e.g. tests). Uses MOEA/D, which accepts several
   * metrics, with the given metrics as targets and leaves benchmark file options disabled.
   */
  public static @NotNull ParameterSet create(@NotNull List<SweepMetric> metrics,
      int numIterations) {
    final ParameterSet param = new OptimizerParameters().cloneParameterSet();
    setOptimizerAndTargets(param, OptimizerOptions.MOEAD, metrics);
    param.setParameter(benchmarkFeatureTypes, DEFAULT_IMPORT_TYPES);
    param.setParameter(benchmarkFeaturesFile, false);
    param.setParameter(iterations, numIterations);
    param.setParameter(maxShapeRejectionFactor, false);
    param.setParameter(paramToOptimize, new ArrayList<>(DEFAULT_SOLUTIONS));
    param.setParameter(showExtendedStatistics, false);
    return param;
  }

  /**
   * Maps the simple setup to the full parameters. Only the parameters to optimize are taken from
   * the simple setup, all other settings are set to their defaults.
   *
   * @param simpleParameters the parameters of the {@link SimpleOptimizerModule}
   * @return new full parameters, independent of the module configuration
   */
  public static @NotNull OptimizerParameters create(
      @NotNull SimpleOptimizerParameters simpleParameters) {
    final OptimizerParameters param = (OptimizerParameters) new OptimizerParameters().cloneParameterSet();
    ParameterSet optimizerParameters = param.getParameter(optimizers)
        .setOptionGetParameters(OptimizerOptions.PATTERN_SEARCH);
    optimizerParameters.setParameter(PatternSearchOptimizerParameters.optimizationTarget,
        OptimizationMetrics.ISOTOPE_RATIO_CONSISTENCY_SCORE);
    param.setParameter(benchmarkFeatureTypes, DEFAULT_IMPORT_TYPES);
    param.setParameter(benchmarkFeaturesFile, false);
    param.setParameter(iterations, 50);
    param.setParameter(maxShapeRejectionFactor, true, DEFAULT_SHAPE_REJECTION_FACTOR);
    param.setParameter(paramToOptimize,
        new ArrayList<>(simpleParameters.getValue(SimpleOptimizerParameters.paramToOptimize)));
    param.setParameter(showExtendedStatistics, false);
    return param;
  }

  /**
   * Selects an optimizer and writes its algorithm-specific objective parameter.
   */
  public static void setOptimizerAndTargets(@NotNull ParameterSet parameters,
      @NotNull OptimizerOptions optimizer, @NotNull List<SweepMetric> targets) {
    if (targets.isEmpty()) {
      throw new IllegalArgumentException("At least one optimization target is required.");
    }

    final ParameterSet optimizerParameters = parameters.getParameter(optimizers)
        .setOptionGetParameters(optimizer);
    optimizer.getModuleInstance().setOptimizationTargets(optimizerParameters, targets);
  }

  public static @NotNull ParameterSet getSelectedOptimizerParameters(
      @NotNull ParameterSet parameters) {
    return parameters.getParameter(optimizers).getEmbeddedParameters();
  }

  public static @NotNull List<SweepMetric> getOptimizationTargets(
      @NotNull ParameterSet parameters) {
    final OptimizerOptions optimizer = parameters.getValue(optimizers);
    return optimizer.getModuleInstance()
        .getOptimizationTargets(getSelectedOptimizerParameters(parameters));
  }

  @Override
  public boolean checkParameterValues(@NotNull final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    final boolean benchmarkFileSelected = getValue(benchmarkFeaturesFile);
    final List<ImportType<?>> value = getValue(benchmarkFeatureTypes).stream()
        .filter(ImportType::isSelected)
        .filter(i -> i.getDataType().equals(new MZType()) || i.getDataType().equals(new RTType()))
        .toList();

    if (benchmarkFileSelected && value.size() < 2) {
      errorMessages.add(
          "If %s is selected, RT and MZ values must be imported from the csv file.".formatted(
              benchmarkFeaturesFile.getName()));
    }

    return superCheck && errorMessages.isEmpty();
  }

  static @NonNull TitledPane createOverrideMessage() {
    return FxLayout.newTitledPane("Information", FxTextFlows.newTextFlow(FxTexts.text("""
        Running parameter estimation or optimization will apply multiple mass detection steps to imported and already imported raw data. Present mass detection results will be overridden.""")));
  }

  /**
   * Shows the setup dialog with the parameter checklist limited to the parameters that apply to the
   * wizard presets.
   *
   * @param parameters the parameters to show
   * @param solutions  the parameter checklist of the parameters
   * @param sequence   the wizard sequence, null shows all parameters
   */
  static @NotNull ExitCode showSetupDialogForSequence(@NotNull ParameterSet parameters,
      @NotNull CheckListParameter<ParameterDefinition<?>> solutions, boolean valueCheckRequired,
      @Nullable WizardSequence sequence) {
    if (sequence != null) {
      final Set<ParameterDefinition<?>> applicable = Set.copyOf(collectSolutions(sequence));
      parameters.getParameter(solutions).setVisibleFilter(applicable::contains);
    }
    final ExitCode exitCode = parameters.showSetupDialog(valueCheckRequired);
    parameters.getParameter(solutions).setVisibleFilter(null); // always reset to all
    return exitCode;
  }

  @Override
  public @Nullable Region getMessage() {
    return FxLayout.newAccordion(true, createOverrideMessage(),

        FxLayout.newTitledPane("Citations", FxTextFlows.newTextFlow(FxTexts.text(
                "When optimizing on these respective metrics, please respect the following citations:"),
            FxTexts.linebreak(), FxTexts.boldText(OptimizationMetrics.IPO_ISOTOPE_SCORE.name()),
            FxTexts.text(": "),
            FxTexts.hyperlinkText("IPO", "https://doi.org/10.1186/s12859-015-0562-8"),
            FxTexts.linebreak(),
            FxTexts.boldText(OptimizationMetrics.SLAW_INTEGRATION_SCORE.name()), FxTexts.text(": "),
            FxTexts.hyperlinkText("SLAW",
                "https://pubs.acs.org/doi/10.1021/acs.analchem.1c02687"))));
  }

  public @NotNull ExitCode showSetupDialog(boolean valueCheckRequired,
      @Nullable WizardSequence sequence) {
    return showSetupDialogForSequence(this, paramToOptimize, valueCheckRequired, sequence);
  }
}
