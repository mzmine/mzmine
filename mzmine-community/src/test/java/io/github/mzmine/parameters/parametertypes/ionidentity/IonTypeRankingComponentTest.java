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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fxinitializer.InitJavaFX;
import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Smoke tests for the ion type ranking UI. Constructing the component and the setup dialog loads
 * icon fonts and the global ion library, so everything runs on the JavaFX application thread.
 */
@DisabledOnOs(value = {OS.LINUX, OS.MAC})
class IonTypeRankingComponentTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  @Test
  void componentShowsOnlyTheEditButton() throws Exception {
    runOnFxAndWait(() -> {
      final IonTypeRankingComponent component = new IonTypeRankingComponent(
          IonTypeRanking.createDefault());

      assertEquals(1, component.getChildren().size());
      final Button edit = (Button) component.getChildren().getFirst();
      assertEquals("Edit ranking", edit.getText());
    });
  }

  @Test
  void componentKeepsTheValue() throws Exception {
    runOnFxAndWait(() -> {
      final IonTypeRankingComponent component = new IonTypeRankingComponent(null);
      // null falls back to the mzmine default
      assertEquals(IonTypeRanking.createDefault(), component.getValue());

      final IonTypeRanking onlySodium = new IonTypeRanking(
          List.of(IonPartFrequency.of(IonParts.NA, 1f)));
      component.setValue(onlySodium);

      assertEquals(onlySodium, component.getValue());
    });
  }

  @Test
  void setupDialogBuilds() throws Exception {
    runOnFxAndWait(() -> {
      final IonTypeRanking ranking = IonTypeRanking.createDefault();
      final IonPartRankingSetupDialog dialog = new IonPartRankingSetupDialog(
          ranking.getFrequencies());

      assertNotNull(dialog.getDialogPane().getContent());
      // OK and cancel have to be present, otherwise the dialog cannot be closed
      assertEquals(2, dialog.getDialogPane().getButtonTypes().size());
      assertFalse(dialog.getTitle().isBlank());
    });
  }

  /**
   * Runs the given code on the JavaFX thread and rethrows any assertion failure.
   */
  private static void runOnFxAndWait(final Runnable runnable) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] error = new Throwable[1];
    submitWhenToolkitReady(() -> {
      try {
        runnable.run();
      } catch (Throwable t) {
        error[0] = t;
      } finally {
        latch.countDown();
      }
    });
    assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX task did not finish");
    if (error[0] != null) {
      throw new AssertionError(error[0]);
    }
  }

  /**
   * {@link InitJavaFX#init()} launches the toolkit on its own thread, so the first test of a class
   * may reach this before the toolkit is up. Retry until it accepts tasks.
   */
  private static void submitWhenToolkitReady(final Runnable task) throws InterruptedException {
    final long deadline = System.currentTimeMillis() + 30_000;
    while (true) {
      try {
        Platform.runLater(task);
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
