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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderFlowInjectLibraryGen;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderLcLibraryGen;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.general.Result;
import io.mzio.users.service.UserActiveService;
import io.mzio.users.user.MZmineUser;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * uses annotations to build spectral libraries
 */
public class WorkflowLibraryGeneration extends WorkflowWizardParameterFactory {

  private static final Logger logger = Logger.getLogger(WorkflowLibraryGeneration.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    return "LIBRARY_GENERATION";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowLibraryGenerationWizardParameters(null, false, true, true, false);
  }

  @Override
  public String toString() {
    return "Library generation";
  }

  @Override
  public Map<WizardPart, WizardPartFilter> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE, WizardPartFilter.allow(
        List.of(IonInterfaceWizardParameterFactory.DIRECT_INFUSION,
            IonInterfaceWizardParameterFactory.FLOW_INJECT,
            IonInterfaceWizardParameterFactory.GC_CI, IonInterfaceWizardParameterFactory.HPLC,
            IonInterfaceWizardParameterFactory.UHPLC, IonInterfaceWizardParameterFactory.HILIC)));
  }

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    var ionInterface = (IonInterfaceWizardParameterFactory) steps.get(WizardPart.ION_INTERFACE)
        .get().getFactory();

    // requires annotation!
    final var annotation = steps.get(WizardPart.ANNOTATION);
    final var params = WizardBatchBuilder.getOptionalParameters(annotation,
        AnnotationWizardParameters.localCsvSearch);
    final boolean useAnnotation = params.active();
    final boolean sampleFilterValid = !params.value().getEmbeddedParameterValueIfSelectedOrElse(
        AnnotationLocalCSVDatabaseSearchParameters.filterSamplesColumn, "").isBlank();
    final File file = params.value().getValue(LocalCSVDatabaseSearchParameters.dataBaseFile);
    if (sampleFilterValid) {
      logger.warning(
          "It is recommended to specify a column to filter annotations for specific samples that contain the compound.");
    }
    if (!useAnnotation || file == null || file.toString().isBlank()) {
      throw new IllegalArgumentException("""
          Configure local CSV database annotation!
          The library generation workflow requires the local CSV database search active under \
          Annotation, a valid file, and it is recommended to specify a column to filter annotations \
          for specific samples that contain the compound.""");
    }

    return switch (ionInterface.group()) {
      case CHROMATOGRAPHY_SOFT -> new WizardBatchBuilderLcLibraryGen(steps);
      case DIRECT_AND_FLOW -> new WizardBatchBuilderFlowInjectLibraryGen(steps);
      case CHROMATOGRAPHY_HARD, SPATIAL_IMAGING -> throw new UnsupportedWorkflowException(steps);
    };
  }

  @Override
  public @NotNull Set<@NotNull UserActiveService> getUnlockingServices() {
    return EnumSet.allOf(UserActiveService.class);
  }

  @Override
  public Result checkUserForServices(@Nullable MZmineUser user) {
    // this workflow should be displayed in any case, even if no user is logged in to show the capabilities
    // the execution is stopped as soon as a task requires user authentication.
    return Result.OK;
  }
}
