/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzmine.modules.tools.batchwizard.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.IonSource;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters.MassOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.AnnotationWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowLibraryGeneration;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class LibraryGenerationAdductOverrideBatchTest {

  private static final IonLibrary PROTON_AND_SODIUM = new UnmodifiableIonLibrary(
      "Proton and sodium", List.of(IonTypes.H.asIonType(), IonTypes.NA.asIonType()));

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void explicitAdductsAndMetadataReachLcFlowInjectAndDirectInfusionBatches() {
    for (final IonInterfaceWizardParameterFactory ionInterface : List.of(
        IonInterfaceWizardParameterFactory.HPLC, IonInterfaceWizardParameterFactory.FLOW_INJECT,
        IonInterfaceWizardParameterFactory.DIRECT_INFUSION)) {
      final BatchQueue queue = createQueue(ionInterface, MassOptions.MASS_AND_IONS);

      final LocalCSVDatabaseSearchParameters localCsv = localCsvParameters(queue);
      assertTrue(localCsv.getValue(LocalCSVDatabaseSearchParameters.ionLibrary));
      assertEquals(PROTON_AND_SODIUM,
          localCsv.getEmbeddedParameterValue(LocalCSVDatabaseSearchParameters.ionLibrary));

      final LibraryBatchMetadataParameters metadata = libraryGenerationParameters(queue)
          .getParameter(LibraryBatchGenerationParameters.metadata).getEmbeddedParameters();
      assertEquals("Reference library", metadata.getValue(LibraryBatchMetadataParameters.DESCRIPTION));
      assertEquals(IonSource.DI_ESI, metadata.getValue(LibraryBatchMetadataParameters.ION_SOURCE));
    }
  }

  @Test
  void precursorMzModeDoesNotEnableAdductExpansion() {
    final LocalCSVDatabaseSearchParameters localCsv = localCsvParameters(
        createQueue(IonInterfaceWizardParameterFactory.HPLC, MassOptions.PRECURSOR_MZ));

    assertFalse(localCsv.getValue(LocalCSVDatabaseSearchParameters.ionLibrary));
  }

  private static @NotNull BatchQueue createQueue(
      @NotNull final IonInterfaceWizardParameterFactory ionInterface,
      @NotNull final MassOptions massOptions) {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.DATA_IMPORT, DataImportWizardParameterFactory.Data.create());
    sequence.set(WizardPart.ION_INTERFACE, ionInterface.create());
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    sequence.set(WizardPart.MS, MassSpectrometerWizardParameterFactory.Orbitrap.create());
    sequence.set(WizardPart.FILTER, FilterWizardParameterFactory.Filters.create());
    sequence.set(WizardPart.ANNOTATION, AnnotationWizardParameterFactory.Annotation.create());
    sequence.set(WizardPart.WORKFLOW, new WorkflowLibraryGeneration().create());

    sequence.get(WizardPart.DATA_IMPORT).orElseThrow().setParameter(
        DataImportWizardParameters.fileNames, new File[]{new File("test.mzML")});
    sequence.get(WizardPart.MS).orElseThrow().setParameter(MassSpectrometerWizardParameters.polarity,
        WizardMsPolarity.Positive);

    final AnnotationWizardParameters annotation = (AnnotationWizardParameters) sequence.get(
        WizardPart.ANNOTATION).orElseThrow();
    annotation.setParameter(AnnotationWizardParameters.localCsvSearch, true);
    final var csv = annotation.getParameter(AnnotationWizardParameters.localCsvSearch)
        .getEmbeddedParameters();
    csv.setParameter(AnnotationLocalCSVDatabaseSearchParameters.dataBaseFile,
        new File("compounds.csv"));
    csv.setParameter(AnnotationLocalCSVDatabaseSearchParameters.massOptionsComboParameter,
        massOptions);
    csv.setParameter(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary, true,
        PROTON_AND_SODIUM);

    final WorkflowLibraryGenerationWizardParameters workflow =
        (WorkflowLibraryGenerationWizardParameters) sequence.get(WizardPart.WORKFLOW).orElseThrow();
    workflow.setParameter(WorkflowLibraryGenerationWizardParameters.exportPath,
        new File("library.msp"));
    final LibraryBatchMetadataParameters metadata = workflow.getParameter(
        WorkflowLibraryGenerationWizardParameters.metadata).getEmbeddedParameters();
    metadata.setParameter(LibraryBatchMetadataParameters.DESCRIPTION, "Reference library");
    metadata.setParameter(LibraryBatchMetadataParameters.ION_SOURCE, IonSource.DI_ESI);

    return new WorkflowLibraryGeneration().getBatchBuilder(sequence).createQueue();
  }

  private static @NotNull LocalCSVDatabaseSearchParameters localCsvParameters(
      @NotNull final BatchQueue queue) {
    return queue.stream().filter(step -> step.getModule() instanceof LocalCSVDatabaseSearchModule)
        .map(MZmineProcessingStep::getParameterSet).map(LocalCSVDatabaseSearchParameters.class::cast)
        .findFirst().orElseThrow();
  }

  private static @NotNull LibraryBatchGenerationParameters libraryGenerationParameters(
      @NotNull final BatchQueue queue) {
    return queue.stream().filter(step -> step.getModule() instanceof LibraryBatchGenerationModule)
        .map(MZmineProcessingStep::getParameterSet).map(LibraryBatchGenerationParameters.class::cast)
        .findFirst().orElseThrow();
  }
}
