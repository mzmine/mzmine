/*
 * Copyright (c) 2004-2026 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import fxinitializer.InitJavaFX;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.tools.batchwizard.WizardParameterChanges.Source;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Tests reviewed parameter markers and wizard control readback during batch preparation. */
@DisabledOnOs(OS.LINUX)
class BatchWizardTabParameterChangesTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
    // MZmineTab requires an MZmineDesktop even though this test does not show a window.
    DesktopService.setDesktop(new HeadLessDesktop());
  }

  @Test
  void estimationAndOptimizationMarkChangedReviewedValues() throws Exception {
    for (final Source source : new Source[]{Source.ESTIMATION, Source.OPTIMIZATION,
        Source.EDITED_ESTIMATION, Source.EDITED_OPTIMIZATION}) {
      runOnFxAndWait(() -> {
        final BatchWizardTab wizard = new BatchWizardTab();

        wizard.applyPartialSequence(changedMassSpectrometerStep(wizard), source);

        final WizardParameterChanges changes = wizard.getParameterChanges();
        assertEquals(source, changes.source());
        assertEquals(1, changes.forPart(WizardPart.MS).size());
        assertEquals(MassSpectrometerWizardParameters.minimumFeatureHeight.getName(),
            changes.forPart(WizardPart.MS).getFirst().parameter().getName());
      });
    }
  }

  @Test
  void unchangedReviewedValuesDoNotCreateMarkers() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = new BatchWizardTab();

      wizard.applyPartialSequence(massSpectrometerStep(wizard), Source.ESTIMATION);

      assertEquals(Source.ESTIMATION, wizard.getParameterChanges().source());
      assertTrue(wizard.getParameterChanges().isEmpty());
    });
  }

  @Test
  void genericPartialSequenceClearsAutomaticMarkers() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = new BatchWizardTab();
      final WizardSequence changed = changedMassSpectrometerStep(wizard);
      wizard.applyPartialSequence(changed, Source.OPTIMIZATION);
      assertFalse(wizard.getParameterChanges().isEmpty());

      wizard.applyPartialSequence(changed);

      assertEquals(Source.NONE, wizard.getParameterChanges().source());
      assertTrue(wizard.getParameterChanges().isEmpty());
    });
  }

  @Test
  void reviewedSequencePreservesExistingCustomization() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = new BatchWizardTab();
      final WizardStepParameters customization = wizard.snapshotSequence()
          .get(WizardPart.CUSTOMIZATION).orElseThrow();
      customization.setParameter(CustomizationWizardParameters.enabled, true);
      final WizardSequence customizationOnly = new WizardSequence();
      customizationOnly.add(customization);
      wizard.applyPartialSequence(customizationOnly);
      final WizardStepParameters expected = wizard.snapshotSequence()
          .get(WizardPart.CUSTOMIZATION).orElseThrow();

      wizard.applyPartialSequence(changedMassSpectrometerStep(wizard), Source.ESTIMATION);

      final WizardStepParameters actual = wizard.snapshotSequence()
          .get(WizardPart.CUSTOMIZATION).orElseThrow();
      assertTrue(ParameterUtils.equalValues(expected, actual));
    });
  }

  @Test
  void correctedBlankNumericValueCanBeReadRepeatedly() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = new BatchWizardTab();
      final WizardSequence sequence = massSpectrometerStep(wizard);
      sequence.get(WizardPart.MS).orElseThrow()
          .setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, null);
      // A preset change rebuilds the panes while the numeric field is still empty.
      wizard.applyPartialSequence(sequence);
      final ParameterSetupPane pane = massSpectrometerPane(wizard);
      pane.getComponentForParameter(MassSpectrometerWizardParameters.minimumFeatureHeight)
          .setText("2500");

      assertDoesNotThrow(wizard::updateAllParametersFromUi);
      assertEquals(2500d, wizard.snapshotSequence().get(WizardPart.MS).orElseThrow()
          .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
      assertDoesNotThrow(wizard::updateAllParametersFromUi);
    });
  }

  @Test
  void batchPreparationReportsControlReadbackFailures() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = spy(new BatchWizardTab());
      doThrow(new IllegalArgumentException("Invalid numeric input"))
          .when(wizard).updateAllParametersFromUi();
      try (final var dialogs = mockStatic(DialogLoggerUtil.class)) {
        assertNull(wizard.prepareBatchQueue());
        dialogs.verify(() -> DialogLoggerUtil.showErrorDialog("Cannot create batch",
            "Invalid numeric input"));
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static @NotNull ParameterSetupPane massSpectrometerPane(final @NotNull BatchWizardTab wizard) {
    try {
      final Field panesField = BatchWizardTab.class.getDeclaredField("paramPaneMap");
      panesField.setAccessible(true);
      final var panes = (Map<WizardStepParameters, ParameterSetupPane>) panesField.get(wizard);
      return panes.get(wizard.getSequence().get(WizardPart.MS).orElseThrow());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static WizardSequence changedMassSpectrometerStep(final BatchWizardTab wizard) {
    final WizardSequence sequence = massSpectrometerStep(wizard);
    final WizardStepParameters ms = sequence.get(WizardPart.MS).orElseThrow();
    final double current = ms.getValue(MassSpectrometerWizardParameters.minimumFeatureHeight);
    ms.setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, current * 2);
    return sequence;
  }

  private static WizardSequence massSpectrometerStep(final BatchWizardTab wizard) {
    final WizardStepParameters current = wizard.snapshotSequence().get(WizardPart.MS).orElseThrow();
    final WizardStepParameters copy = current.getFactory().create();
    ParameterUtils.copyParameters(current, copy);
    final WizardSequence sequence = new WizardSequence();
    sequence.add(copy);
    return sequence;
  }

  private static void runOnFxAndWait(final Runnable action) throws Exception {
    final CountDownLatch finished = new CountDownLatch(1);
    final Throwable[] failure = new Throwable[1];
    submitWhenToolkitReady(() -> {
      try {
        action.run();
      } catch (Throwable t) {
        failure[0] = t;
      } finally {
        finished.countDown();
      }
    });
    assertTrue(finished.await(30, TimeUnit.SECONDS), "JavaFX task did not finish");
    if (failure[0] != null) {
      throw new AssertionError(failure[0]);
    }
  }

  private static void submitWhenToolkitReady(final Runnable action) throws InterruptedException {
    final long deadline = System.currentTimeMillis() + 30_000;
    while (true) {
      try {
        Platform.runLater(action);
        return;
      } catch (IllegalStateException toolkitNotInitialized) {
        if (System.currentTimeMillis() > deadline) {
          throw toolkitNotInitialized;
        }
        Thread.sleep(50);
      }
    }
  }
}
