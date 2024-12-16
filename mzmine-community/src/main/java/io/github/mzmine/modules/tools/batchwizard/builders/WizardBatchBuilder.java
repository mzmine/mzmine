/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.builders;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDDA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDIA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDeconvolution;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowImaging;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowLibraryGeneration;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowMs1Only;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowTargetPlate;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a batch queue from a list of {@link WizardStepParameters} making up a workflow defined in
 * {@link WizardPart}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class WizardBatchBuilder {

  private static final Logger logger = Logger.getLogger(WizardBatchBuilder.class.getName());

  protected final WizardSequence steps;

  protected WizardBatchBuilder(WizardSequence steps) {
    this.steps = steps;
  }

  /**
   * Create different workflows in {@link BatchQueue}. Workflows are defined in
   *
   * @return a batch queue
   */
  public abstract BatchQueue createQueue();

  /**
   * Get parameter if available or else return null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return the value of the parameter or null if !params.isPresent
   */
  public static <T> T getValue(@NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final Parameter<T> parameter) {
    if (params.isPresent()) {
      try {
        return params.get().getValue(parameter);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

  /**
   * Get parameter if available or else return default value. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return the value of the parameter or default value if !params.isPresent
   */
  @NotNull
  public static <T> T getOrElse(@NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final Parameter<T> parameter, @NotNull T defaultValue) {
    return requireNonNullElse(getValue(params, parameter), defaultValue);
  }

  /**
   * Get parameter if available or else null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return value and selection state of an OptionalParameter
   */
  public static <V, T extends UserParameter<V, ?>> OptionalValue<V> getOptional(
      @NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final OptionalParameter<T> parameter) {
    if (params.isPresent()) {
      try {
        OptionalParameter<T> param = params.get().getParameter(parameter);
        return new OptionalValue<>(param.getValue(), param.getEmbeddedParameter().getValue());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return new OptionalValue<>(false, null);
  }

  /**
   * Get parameter if available or else null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return embedded parameterset and selection state of an OptionalParameter
   */
  public static <T extends ParameterSet> OptionalValue<T> getOptionalParameters(
      @NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final EmbeddedParameterSet<T, ?> parameter) {
    if (params.isPresent()) {
      try {
        EmbeddedParameterSet<T, ?> param = params.get().getParameter(parameter);
        boolean state = true;
        if (param instanceof AdvancedParametersParameter<?> check) {
          state = check.getValue();
        }
        if (param instanceof OptionalModuleParameter<?> check) {
          state = check.getValue();
        }

        return new OptionalValue<>(state, param.getEmbeddedParameters());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

}
