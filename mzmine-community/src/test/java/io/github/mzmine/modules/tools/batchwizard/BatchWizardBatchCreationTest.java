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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import fxinitializer.InitJavaFX;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
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

/** Regression test for reporting wizard control failures before the batch editor opens. */
@DisabledOnOs(OS.LINUX)
class BatchWizardBatchCreationTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
    DesktopService.setDesktop(new HeadLessDesktop());
  }

  @Test
  void batchCreationReportsControlReadbackFailures() throws Exception {
    runOnFxAndWait(() -> {
      final BatchWizardTab wizard = new BatchWizardTab();
      final ParameterSetupPane failingPane = mock(ParameterSetupPane.class);
      doThrow(new IllegalArgumentException("Invalid numeric input"))
          .when(failingPane).updateParameterSetFromComponents();
      final var panes = parameterPanes(wizard);
      panes.replaceAll((_, _) -> failingPane);

      try (final var dialogs = mockStatic(DialogLoggerUtil.class)) {
        assertDoesNotThrow(wizard::createBatch);
        dialogs.verify(() -> DialogLoggerUtil.showErrorDialog("Cannot create batch",
            "Invalid numeric input"));
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static @NotNull Map<WizardStepParameters, ParameterSetupPane> parameterPanes(
      final @NotNull BatchWizardTab wizard) {
    try {
      final Field panesField = BatchWizardTab.class.getDeclaredField("paramPaneMap");
      panesField.setAccessible(true);
      return (Map<WizardStepParameters, ParameterSetupPane>) panesField.get(wizard);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static void runOnFxAndWait(final @NotNull Runnable action) throws Exception {
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

  private static void submitWhenToolkitReady(final @NotNull Runnable action)
      throws InterruptedException {
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
