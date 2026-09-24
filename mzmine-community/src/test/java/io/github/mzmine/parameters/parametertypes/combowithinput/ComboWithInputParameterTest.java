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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fxinitializer.InitJavaFX;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.StringParameterComponent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests that {@link ComboWithInputParameter#setValueFromComponent} reads the input field of the
 * component whenever the trigger option is selected in the component. Everything runs on the JavaFX
 * application thread because the component creates a combo box.
 */
@DisabledOnOs(value = {OS.LINUX, OS.MAC})
class ComboWithInputParameterTest {

  private static final String DEFAULT_VALUE = "default";

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  private static DefaultOffCustomParameter<String> newParameter() {
    final StringParameter embedded = new StringParameter("Value", "Test parameter", DEFAULT_VALUE);
    return new DefaultOffCustomParameter<>(embedded, DEFAULT_VALUE, null, false);
  }

  /**
   * Switching from DEFAULT to CUSTOM and typing a value has to store the typed value, not the value
   * the input field held before.
   */
  @Test
  void switchingToCustomKeepsTheTypedValue() throws Exception {
    runOnFxAndWait(() -> {
      final DefaultOffCustomParameter<String> parameter = newParameter();
      assertEquals(DefaultOffCustomOption.DEFAULT, parameter.getValue().getSelectedOption());

      final ComboWithInputComponent<DefaultOffCustomOption> component = parameter.createEditingComponent();
      // the user selects CUSTOM and types a new value in the same dialog
      component.getComboBox().getSelectionModel().select(DefaultOffCustomOption.CUSTOM);
      ((StringParameterComponent) component.getEmbeddedComponent()).setText("typed");

      parameter.setValueFromComponent(component);

      assertEquals(DefaultOffCustomOption.CUSTOM, parameter.getValue().getSelectedOption());
      assertEquals("typed", parameter.getValue().getEmbeddedValue());
      assertEquals("typed", parameter.resolveValue());
    });
  }

  /**
   * The input is disabled while DEFAULT is selected, so its content must not overwrite the stored
   * custom value and the parameter has to resolve to the default.
   */
  @Test
  void stayingOnDefaultResolvesToTheDefault() throws Exception {
    runOnFxAndWait(() -> {
      final DefaultOffCustomParameter<String> parameter = newParameter();

      final ComboWithInputComponent<DefaultOffCustomOption> component = parameter.createEditingComponent();
      ((StringParameterComponent) component.getEmbeddedComponent()).setText("ignored");

      parameter.setValueFromComponent(component);

      assertEquals(DefaultOffCustomOption.DEFAULT, parameter.getValue().getSelectedOption());
      assertEquals(DEFAULT_VALUE, parameter.resolveValue());
    });
  }

  /**
   * Editing the input while CUSTOM is already selected has always worked, this pins it.
   */
  @Test
  void editingWhileOnCustomKeepsTheTypedValue() throws Exception {
    runOnFxAndWait(() -> {
      final DefaultOffCustomParameter<String> parameter = newParameter();
      parameter.setValue(new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM, "first"));

      final ComboWithInputComponent<DefaultOffCustomOption> component = parameter.createEditingComponent();
      ((StringParameterComponent) component.getEmbeddedComponent()).setText("second");

      parameter.setValueFromComponent(component);

      assertEquals("second", parameter.resolveValue());
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
