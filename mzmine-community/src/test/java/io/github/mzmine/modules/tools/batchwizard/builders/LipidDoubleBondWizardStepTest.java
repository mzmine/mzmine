package io.github.mzmine.modules.tools.batchwizard.builders;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.tools.batchwizard.LipidDoubleBondWizardSteps;
import io.github.mzmine.modules.tools.batchwizard.LipidDoubleBondWizardTechnique;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.LipidAnnotationWizardSubParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.AnnotationWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDDA;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import testutils.MZmineTestUtil;

class LipidDoubleBondWizardStepTest {

  private final List<LipidDoubleBondWizardTechnique> requestedTechniques = new ArrayList<>();
  private final List<MZTolerance> requestedTolerances = new ArrayList<>();

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterEach
  void unregister() {
    LipidDoubleBondWizardSteps.unregister();
  }

  @Test
  void addsRegisteredStepDirectlyAfterLipidAnnotation() {
    final MZmineProcessingStep<MZmineProcessingModule> doubleBondStep = registerFactory();

    final BatchQueue queue = createQueue(true, true);

    final int lipidIndex = indexOfLipidAnnotation(queue);
    Assertions.assertTrue(lipidIndex >= 0);
    Assertions.assertSame(doubleBondStep, queue.get(lipidIndex + 1));
    Assertions.assertEquals(List.of(LipidDoubleBondWizardTechnique.OAD), requestedTechniques);
    Assertions.assertEquals(1, requestedTolerances.size());
    Assertions.assertNotNull(requestedTolerances.getFirst());
  }

  @Test
  void isOffByDefault() {
    final MZmineProcessingStep<MZmineProcessingModule> doubleBondStep = registerFactory();

    final BatchQueue queue = createQueue(true, false);

    Assertions.assertTrue(indexOfLipidAnnotation(queue) >= 0);
    Assertions.assertFalse(queue.contains(doubleBondStep));
    Assertions.assertTrue(requestedTechniques.isEmpty());
  }

  @Test
  void requiresLipidAnnotation() {
    final MZmineProcessingStep<MZmineProcessingModule> doubleBondStep = registerFactory();

    final BatchQueue queue = createQueue(false, true);

    Assertions.assertEquals(-1, indexOfLipidAnnotation(queue));
    Assertions.assertFalse(queue.contains(doubleBondStep));
  }

  @Test
  void skipsStepWithoutRegisteredFactory() {
    final BatchQueue withOption = createQueue(true, true);
    final BatchQueue withoutOption = createQueue(true, false);

    Assertions.assertEquals(withoutOption.size(), withOption.size());
  }

  @SuppressWarnings("unchecked")
  private @NotNull MZmineProcessingStep<MZmineProcessingModule> registerFactory() {
    final MZmineProcessingStep<MZmineProcessingModule> step = Mockito.mock(
        MZmineProcessingStep.class);
    LipidDoubleBondWizardSteps.register((technique, mzToleranceMs2) -> {
      requestedTechniques.add(technique);
      requestedTolerances.add(mzToleranceMs2);
      return step;
    });
    return step;
  }

  private static int indexOfLipidAnnotation(final @NotNull BatchQueue queue) {
    for (int i = 0; i < queue.size(); i++) {
      if (queue.get(i).getModule() instanceof LipidAnnotationModule) {
        return i;
      }
    }
    return -1;
  }

  private static @NotNull BatchQueue createQueue(final boolean annotateLipids,
      final boolean localizeDoubleBonds) {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.DATA_IMPORT, DataImportWizardParameterFactory.Data.create());
    sequence.set(WizardPart.ION_INTERFACE, IonInterfaceWizardParameterFactory.UHPLC.create());
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    sequence.set(WizardPart.MS, MassSpectrometerWizardParameterFactory.Orbitrap.create());
    sequence.set(WizardPart.FILTER, FilterWizardParameterFactory.Filters.create());
    sequence.set(WizardPart.ANNOTATION, AnnotationWizardParameterFactory.Annotation.create());
    sequence.set(WizardPart.WORKFLOW, new WorkflowDDA().create());

    sequence.get(WizardPart.DATA_IMPORT).orElseThrow()
        .setParameter(DataImportWizardParameters.fileNames, new File[]{new File("test.mzML")});
    sequence.get(WizardPart.MS).orElseThrow()
        .setParameter(MassSpectrometerWizardParameters.polarity, WizardMsPolarity.Positive);

    // exports need a file that this test never writes
    sequence.get(WizardPart.WORKFLOW).orElseThrow()
        .setParameter(WorkflowDdaWizardParameters.exportPath, false);

    final WizardStepParameters annotation = sequence.get(WizardPart.ANNOTATION).orElseThrow();
    annotation.setParameter(AnnotationWizardParameters.lipidAnnotation, annotateLipids);
    annotation.getParameter(AnnotationWizardParameters.lipidAnnotation).getEmbeddedParameters()
        .setParameter(LipidAnnotationWizardSubParameters.lipidDoubleBonds, localizeDoubleBonds,
            LipidDoubleBondWizardTechnique.OAD);

    return new WorkflowDDA().getBatchBuilder(sequence).createQueue();
  }
}
